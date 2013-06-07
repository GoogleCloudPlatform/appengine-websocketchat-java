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

package com.google.appengine.demos.websocketchat.message;

import com.google.gson.Gson;

/**
 * A class that is sent via websocket between the servers and clients.
 *
 * The instances are serialized by Gson when being sent via the websocket connection.
 */
public class ChatMessage implements OutgoingMessage {

  private MessageType type;

  private String name;

  private String room;

  private String message;

  /**
   * Returns another ChatMessage instance which can be used for propagating the original
   * OutgoingMessage between multiple websocket server nodes.
   *
   * @param original an original OutgoingMessage instance that you want to wrap.
   * @param gson a Gson object which used for serialization.
   * @return a new ChatMessage with {@code MessageType = MessageType.PROPAGATE} that wraps the
   * given original message in the message property.
   */
  public static ChatMessage createPropagateMessage(OutgoingMessage original, Gson gson) {
    return new ChatMessage(MessageType.PROPAGATE, null, original.getRoom(), original.toJson(gson));
  }

  /**
   * Creates a ChatMessage instance with given parameters.
   *
   * @param messageType one of the MessageType enum, represents the type of this message.
   * @param name a name of the message owner.
   * @param room a name of the chat room that the message belongs to.
   * @param message actual message contents.
   */
  public ChatMessage(MessageType messageType, String name, String room, String message) {
    this.type = messageType;
    this.name = name;
    this.room = room;
    this.message = message;
  }

  @Override
  public MessageType getType() {
    return type;
  }

  /**
   * Returns the name of the owner of this message.
   *
   * @return the name of the owner of this message.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the contents of this message.
   *
   * @return the contents of this message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns a JSON object that will be sent to the clients.
   *
   * If the type of this message is {@code MessageType.PROPAGATE}, the return value will be the
   * serialized form of the <strong>wrapped message</strong>.
   *
   * @param gson a Gson object for serializing this message.
   * @return a JSON object that will be sent to the clients.
   */
  @Override
  public String toJson(Gson gson) {
    if (type.equals(MessageType.PROPAGATE)) {
      return message;
    } else {
      return gson.toJson(this);
    }
  }

  @Override
  public boolean shouldSendTo(String room) {
    return this.room.equals(room);
  }

  @Override
  public String getRoom() {
    return room;
  }
}
