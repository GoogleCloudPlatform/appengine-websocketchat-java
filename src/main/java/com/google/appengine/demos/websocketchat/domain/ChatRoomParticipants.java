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

package com.google.appengine.demos.websocketchat.domain;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * An entity class which holds a list of participants for a chat room within a single server node.
 *
 * <p>The parentKey parameter should be set with the name of the chat room as the key name. This
 * is forced by only providing a single public constructor which has {@code String room} as the
 * first parameter.
 * </p>
 */
@Entity
public class ChatRoomParticipants {

  private static final Logger LOG = Logger.getLogger(ChatRoomParticipants.class.getName());

  /* parentKey has a room name as the key name. */
  @Parent
  private Key<ChatRoomParticipants> parentKey;

  @Id
  private String serverNode;

  private Set<String> participants;

  /* Objectify needs the default constructor. */
  private ChatRoomParticipants() {}

  /**
   * Returns the global list of participants of the given chat room.
   *
   * <p>This method aggregates the participants list among multiple servers and return the
   * global list of the given chat room.</p>
   *
   * @param room a name of the chat room.
   * @return the global list of participants of the given chat room.
   */
  public static Set<String> getParticipants(String room) {
    List<Key<WebSocketServerNode>> serverNodeKeyList = ofy().load()
        .type(WebSocketServerNode.class).ancestor(WebSocketServerNode.getRootKey()).keys().list();
    List<Key<ChatRoomParticipants>> chatRoomParticipantsKeys = new ArrayList<>();
    Key<ChatRoomParticipants> parentKey = Key.create(ChatRoomParticipants.class, room);
    for (Key<WebSocketServerNode> serverNodeKey: serverNodeKeyList) {
      Key<ChatRoomParticipants> chatRoomParticipantsKey =
          Key.create(parentKey, ChatRoomParticipants.class, serverNodeKey.getName());
      LOG.info("chatRoomParticipantsKey: " + chatRoomParticipantsKey);
      chatRoomParticipantsKeys.add(chatRoomParticipantsKey);
    }
    Collection<ChatRoomParticipants> chatRoomParticipantsCollection =
        ofy().transaction().load().keys(chatRoomParticipantsKeys).values();
    Set<String> participantSet = new TreeSet<>();
    for (ChatRoomParticipants participants: chatRoomParticipantsCollection) {
      LOG.info("Adding " + participants.getParticipants());
      participantSet.addAll(participants.getParticipants());
    }
    return participantSet;
  }

  /**
   * Creates an entity representing the list of the participants in a chat room within a single
   * server node.
   *
   * @param room a name of the chat room.
   * @param serverNode an identifier of a single server node, in the form of websocket URL,
   *                   e.x. "ws://173.255.112.201:65080/".
   * @param participants a set of participants in the given chat room within a single server node.
   */
  public ChatRoomParticipants(String room, String serverNode, Set<String> participants) {
    this.parentKey = Key.create(ChatRoomParticipants.class, room);
    this.serverNode = serverNode;
    this.participants = new TreeSet<>(participants);
  }

  /**
   * Returns the list of the participants in this entity.
   *
   * @return the list of the participants in the given chat room within a single server node.
   */
  public Set<String> getParticipants() {
    if (participants == null) {
      return new TreeSet<>();
    }
    return new TreeSet<>(participants);
  }

  /**
   * Returns the objectify key object representing this entity.
   *
   * @return the objectify key object representing this entity.
   */
  public Key<ChatRoomParticipants> getKey() {
    return Key.create(this.parentKey, ChatRoomParticipants.class, serverNode);
  }
}
