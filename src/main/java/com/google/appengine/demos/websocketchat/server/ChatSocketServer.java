/*
 * Copyright (c) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.appengine.demos.websocketchat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.demos.websocketchat.domain.ChatRoomParticipants;
import com.google.appengine.demos.websocketchat.domain.WebSocketServerNode;
import com.google.appengine.demos.websocketchat.message.ChatMessage;
import com.google.appengine.demos.websocketchat.message.OutgoingMessage;
import com.google.appengine.demos.websocketchat.message.ParticipantListMessage;
import com.google.apphosting.api.ApiProxy;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.googlecode.objectify.Key;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * A simple WebSocketServerNode implementation. Keeps track of a "websocketchat".
 */
public class ChatSocketServer extends WebSocketServer {

  private static final Logger LOG = Logger.getLogger(ChatSocketServer.class.getName());

  private static final int DEFAULT_PORT = 65080;

  private static final Gson GSON = new Gson();

  private static final String NETWORK_INTERFACE_METADATA_URL =
      "http://metadata/computeMetadata/v1beta1/instance/network-interfaces/0/access-configs/0/" +
          "external-ip";

  private final MetaInfoManager metaInfoManager;

  private ConcurrentLinkedQueue<String> updateAndSendParticipantListQueue;

  private ConcurrentLinkedQueue<OutgoingMessage> propagateQueue;

  private String hostname;

  private String getHostname() throws IOException {
    if (hostname == null) {
      if (SystemProperty.environment.value().equals(SystemProperty.Environment.Value.Production)) {
        URL url = new URL(NETWORK_INTERFACE_METADATA_URL);
        HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(httpUrlConnection.getInputStream()));
        String result, line = reader.readLine();
        result = line;
        while ((line = reader.readLine()) != null) {
          result += line;
        }
        hostname = result;
      } else {
        hostname = "localhost";
      }
    }
    return hostname;
  }

  /**
   * Returns a Websocket URL of this server.
   *
   * @return a Websocket URL of this server.
   * @throws IOException when failed to get the external IP address from the metadata server.
   */
  public String getWebSocketURL() throws IOException {
    return "ws://" + getHostname() + ":" + this.getPort() + "/";
  }

  /**
   * A class that runs in another thread and becomes a bridge between App Engine and the
   * websocket server.
   */
  public static class ChatServerBridge implements Runnable {

    private ChatSocketServer chatSocketServer;

    private Thread watcherThread;

    private String namespace;

    private static ChatServerBridge chatServerBridge;

    private CopyOnWriteArrayList<Key<ChatRoomParticipants>> chatRoomParticipantsKeyList;

    private ApiProxy.Environment backgroundEnvironment;

    private ChatServerBridge() {
      namespace = NamespaceManager.get();
      chatRoomParticipantsKeyList = new CopyOnWriteArrayList<>();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return a singleton instance of this class.
     */
    public static ChatServerBridge getInstance() {
      if (chatServerBridge == null) {
        chatServerBridge = new ChatServerBridge();
      }
      return chatServerBridge;
    }

    private void registerWebSocketServerNode() throws IOException {
      WebSocketServerNode webSocketServerNode = new WebSocketServerNode(
          chatSocketServer.getWebSocketURL());
      ofy().save().entity(webSocketServerNode).now();
    }

    private void removeWebSocketServerNode() throws IOException {
      Key<WebSocketServerNode> key = WebSocketServerNode.getKeyFromWebSocketUrl(
          chatSocketServer.getWebSocketURL());
      ofy().delete().key(key).now();
    }

    protected ApiProxy.Environment getBackgroundEnvironment() {
      return backgroundEnvironment;
    }

    /**
     * Starts the websocket server, registers necessary information and then starts the bridge
     * thread.
     */
    public void start() {
      if (chatSocketServer != null) {
        throw new IllegalStateException("We already have a chatSocketServer.");
      }
      chatSocketServer = new ChatSocketServer(DEFAULT_PORT);
      chatSocketServer.start();
      LOG.info("Server started on port: " + chatSocketServer.getPort());
      try {
        registerWebSocketServerNode();
      } catch (IOException e) {
        LOG.warning(Throwables.getStackTraceAsString(e));
      }
      ThreadManager.createBackgroundThread(this).start();
    }

    /**
     * Stops the websocket server, cleans up some info, and then stop the main bridge thread.
     */
    public void stop() {
      try {
        removeWebSocketServerNode();
        chatSocketServer.stop();
        watcherThread.interrupt();
        watcherThread.join();
        watcherThread = null;
        // delete participant list in the datastore
        ofy().delete().keys(chatRoomParticipantsKeyList).now();
        // initialize variables
        chatRoomParticipantsKeyList = new CopyOnWriteArrayList<>();
        chatSocketServer = null;
      } catch (IOException|InterruptedException e) {
        LOG.warning(Throwables.getStackTraceAsString(e));
      }
    }

    /**
     * Pops a name of a chat room from the updateAndSendParticipantListQueue and update the
     * participant list in the datastore, then creates the global list of the given chat room and
     * distribute it to the clients who is participating to that chat room.
     *
     * @throws IOException
     */
    private void updateParticipantListAndDistribute() throws IOException {
      if (! chatSocketServer.updateAndSendParticipantListQueue.isEmpty()) {
        // Update the participant list in the datastore
        String room = chatSocketServer.updateAndSendParticipantListQueue.remove();
        ChatRoomParticipants chatRoomParticipants = new ChatRoomParticipants(room,
            chatSocketServer.getWebSocketURL(),
            chatSocketServer.metaInfoManager.getParticipantList(room));
        ofy().save().entity(chatRoomParticipants).now();
        chatRoomParticipantsKeyList.add(chatRoomParticipants.getKey());
        // Retrieve the full participant list in the room and distribute it
        Set<String> participantSet = ChatRoomParticipants.getParticipants(room);
        ParticipantListMessage participantListMessage = new ParticipantListMessage(room,
            participantSet);
        chatSocketServer.sendToClients(participantListMessage);
      }
    }

    /**
     * Propagate a message popped from the propagateQueue to other active server nodes.
     *
     * @throws IOException
     */
    private void propagateOneMessage() throws IOException {
      if (! chatSocketServer.propagateQueue.isEmpty()) {
        // handle message propagation between the server nodes.
        OutgoingMessage message = chatSocketServer.propagateQueue.remove();
        LOG.info("Handling a propagate message: " + message.toJson(GSON));
        Key<WebSocketServerNode> parentKey = WebSocketServerNode.getRootKey();
        List<Key<WebSocketServerNode>> serverKeys = ofy().load()
            .type(WebSocketServerNode.class).ancestor(parentKey).keys().list();
        final ChatMessage propagateMessage =
            ChatMessage.createPropagateMessage(message, GSON);
        for (Key<WebSocketServerNode> key: serverKeys) {
          LOG.info("Server: " + key.getName());
          if (! key.getName().equals(chatSocketServer.getWebSocketURL())) {
            // Send a propagate message
            LOG.info("Trying to send a message to the server: " + key.getName());
            try {
              final WebSocketClient chatClient = new WebSocketClient(new URI(key.getName())) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                  // Send propagateMessage itself.
                  this.send(GSON.toJson(propagateMessage));
                  this.close();
                }

                @Override
                public void onMessage(String message) {
                  LOG.info("Message received: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                  LOG.info("Connection closed.");
                }

                @Override
                public void onError(Exception ex) {
                  LOG.warning(Throwables.getStackTraceAsString(ex));
                }
              };
              chatClient.connect();
            } catch (URISyntaxException e) {
              LOG.warning(Throwables.getStackTraceAsString(e));
            }
          }
        }
      }
    }

    /**
     * A main loop of this bridge thread.
     *
     * <p>The chat server requests us the following 2 things.</p>
     * <ul>
     *   <li>Update and distribute the participant list in a particular chat room.</li>
     *   <li>Propagate a message to other active server nodes.</li>
     * </ul>
     * <p>This thread watches the 2 queues on the ChatSocketServer instance,
     * and handles those requests in the main loop.</p>
     * <p>If this loop becomes the performance bottleneck, distribute these work loads into
     * multiple thread worker.</p>
     */
    public void run() {
      if (watcherThread != null) {
        throw new IllegalStateException("A watcherThread already exists.");
      }
      watcherThread = Thread.currentThread();
      LOG.info("Namespace is set to " + namespace + " in thread " + watcherThread.toString());
      NamespaceManager.set(namespace);
      // Store the environment for later use.
      backgroundEnvironment = ApiProxy.getCurrentEnvironment();

      while (true) {
        if (Thread.currentThread().isInterrupted()) {
          LOG.info("ChatServerBridge is stopping.");
          return;
        } else {
          try {
            updateParticipantListAndDistribute();
            propagateOneMessage();
            Thread.sleep(100);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          } catch (IOException e) {
            LOG.warning(Throwables.getStackTraceAsString(e));
          }
        }
      }
    }

    /**
     * Returns a Websocket URL of the chat server.
     *
     * @return a Websocket URL of the chat server.
     * @throws IOException when failed to get the external IP address from the metadata server.
     */
    public String getWebSocketURL() throws IOException {
      return this.chatSocketServer.getWebSocketURL();
    }
  }

  /**
   * Creates a ChatSoccketServer instance with the given network port.
   *
   * @param port a port number on which this chat server will listen.
   */
  public ChatSocketServer(int port) {
    super(new InetSocketAddress(port));
    metaInfoManager = new MetaInfoManager();
    updateAndSendParticipantListQueue = new ConcurrentLinkedQueue<>();
    propagateQueue = new ConcurrentLinkedQueue<>();
  }

  /**
   * Records the incoming connection to the log.
   * @param conn a websocket connection object.
   * @param handshake a websocket handshake object.
   */
  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    LOG.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
  }

  /**
   * Removes a ConnectionInfo object associated with a given websocket connection from the
   * MetaInfoManager.
   *
   * @param conn a websocket connection object.
   * @param code an integer code that you can look up at
   *             {@link org.java_websocket.framing.CloseFrame}.
   * @param reason an additional information.
   * @param remote Returns whether or not the closing of the connection was initiated by the remote
   *               host.
   */
  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    LOG.info(conn + " has left the room!");
    MetaInfoManager.ConnectionInfo connectionInfo = metaInfoManager.getConnectionInfo(conn);
    if (connectionInfo != null) {
      this.sendToClients(new ChatMessage(OutgoingMessage.MessageType.LEAVE,
          connectionInfo.getName(), connectionInfo.getRoom(), null));
      metaInfoManager.removeConnection(conn);
      if (! updateAndSendParticipantListQueue.contains(connectionInfo.getRoom())) {
        updateAndSendParticipantListQueue.add(connectionInfo.getRoom());
      }
    }
  }

  /**
   * Handles incoming messages.
   *
   * If the type of the incoming message is MessageType.ENTER, we need to check the username
   * against the current participant list and change the requested name with trailing underscores.
   * Regardless of the type, we invoke sendToClient method with every incoming messages.
   *
   * @param conn a websocket connection object.
   * @param rawMessage a raw message from the clients.
   */
  @Override
  public void onMessage(WebSocket conn, String rawMessage) {
    // TODO: Make it threadsafe
    LOG.info(conn + ": " + rawMessage);
    ApiProxy.setEnvironmentForCurrentThread(
        ChatServerBridge.getInstance().getBackgroundEnvironment());
    ChatMessage message = GSON.fromJson(rawMessage, ChatMessage.class);
    if (message.getType().equals(OutgoingMessage.MessageType.ENTER)) {
      // Check if there's a participant with the same name in the room.
      Set<String> participantSet = ChatRoomParticipants.getParticipants(message.getRoom());
      if (participantSet.contains(message.getName())) {
        // Adding a trailing underscore until the conflict resolves.
        String newName = message.getName() + "_";
        while (participantSet.contains(newName)) {
          newName = newName + "_";
        }
        // New name decided.
        message = new ChatMessage(message.getType(), newName, message.getRoom(),
            message.getMessage());
        ChatMessage systemMessage = new ChatMessage(OutgoingMessage.MessageType.SYSTEM, newName,
            message.getRoom(), "Changed the name to " + newName + ".");
        conn.send(GSON.toJson(systemMessage));
      }
      metaInfoManager.addConnection(conn, message.getName(), message.getRoom());
      if (! updateAndSendParticipantListQueue.contains(message.getRoom())) {
        updateAndSendParticipantListQueue.add(message.getRoom());
      }
    }
    this.sendToClients(message);
  }

  /**
   * Just logs the exception.
   * @param conn a websocket connection object.
   * @param ex an exception.
   */
  @Override
  public void onError(WebSocket conn, Exception ex) {
    LOG.warning(Throwables.getStackTraceAsString(ex));
  }

  /**
   * Sends <var>message</var> to currently connected WebSocket clients in the same room as the
   * message.
   *
   * @param message An object representing a message to send across the network.
   */
  public void sendToClients(OutgoingMessage message) {
    if (! message.getType().equals(OutgoingMessage.MessageType.PROPAGATE)) {
      propagateQueue.add(message);
    } else {
      ParticipantListMessage participantListMessage = GSON.fromJson(message.toJson(GSON),
          ParticipantListMessage.class);
      if (participantListMessage.getType().equals(OutgoingMessage.MessageType.PARTICIPANTS)) {
        LOG.info("ParticipantList arrived for the room:" + message.getRoom());
      }
    }
    Collection<WebSocket> webSocketConnections = connections();
    synchronized (webSocketConnections) {
      for (WebSocket connection : webSocketConnections) {
        MetaInfoManager.ConnectionInfo info = metaInfoManager.getConnectionInfo(connection);
        if (info != null) {
          if (message.shouldSendTo(info.getRoom())) {
            connection.send(message.toJson(GSON));
          }
        }
      }
    }
  }
}