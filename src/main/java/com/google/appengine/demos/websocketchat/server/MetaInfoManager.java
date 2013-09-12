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

import com.google.common.collect.ImmutableSet;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A class that manages connections from the clients.
 */
public class MetaInfoManager {

  /**
   * A class that holds the name of the participant and the name of the chat room.
   */
  public class ConnectionInfo {

    private String name;

    private String room;

    /**
     * Creates a ConnectionInfo instance with the given name and room.
     *
     * @param name a name of the participant.
     * @param room a name of the chat room.
     */
    ConnectionInfo(String name, String room) {
      this.name = name;
      this.room = room;
    }

    /**
     * Returns a name of the participant.
     *
     * @return a name of the participant.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns a name of the chat room.
     *
     * @return a name of the chat room.
     */
    public String getRoom() {
      return room;
    }
  }

  private static final Set<String> EMPTY_PARTICIPANT_SET = ImmutableSet.of();

  private static String createIdFromConnection(WebSocket connection) {
    return connection.getRemoteSocketAddress().getAddress().getHostAddress() + ":"
        + connection.getRemoteSocketAddress().getPort();
  }

  private Map<String, ConnectionInfo> connectionMap;

  private Map<String, Set<String>> participantMap;

  /**
   * Creates a MetaInfoManager with the initialized map objects.
   */
  public MetaInfoManager() {
    connectionMap = new ConcurrentHashMap<>();
    participantMap = new ConcurrentHashMap<>();
  }

  /**
   * Returns a set of the names of the participants in a given chat room.
   *
   * @param room a name of the chat room.
   * @return a set of the names of the participants in a given chat room.
   */
  public Set<String> getParticipantList(String room) {
    if (participantMap.containsKey(room)) {
      return participantMap.get(room);
    } else {
      return EMPTY_PARTICIPANT_SET;
    }
  }

  /**
   * Adds a map entry to the participantMap property with a connection identifier as the key and
   * ConnectionInfo with the given name and room as the value.
   *
   * @param connection a websocket connection object.
   * @param name a name of the participant.
   * @param room a name of the chatroom.
   */
  public void addConnection(WebSocket connection, String name, String room) {
    connectionMap.put(createIdFromConnection(connection), new ConnectionInfo(name, room));
    if (! participantMap.containsKey(room)) {
      participantMap.put(room, new TreeSet<String>());
    }
    participantMap.get(room).add(name);
  }

  /**
   * Returns a ConnectionInfo object corresponding to a given websocket connection.
   * @param connection a websocket connection object.
   * @return a ConnectionInfo object corresponding to a given websocket connection.
   */
  public ConnectionInfo getConnectionInfo(WebSocket connection) {
    return connectionMap.get(createIdFromConnection(connection));
  }

  /**
   * Removes a ConnectionInfo associated to a given websocket connection.
   *
   * @param connection a websocket connection object.
   */
  public void removeConnection(WebSocket connection) {
    ConnectionInfo connectionInfo = getConnectionInfo(connection);
    if (participantMap.containsKey(connectionInfo.getRoom())) {
      participantMap.get(connectionInfo.getRoom()).remove(connectionInfo.getName());
    }
    connectionMap.remove(createIdFromConnection(connection));
  }
}
