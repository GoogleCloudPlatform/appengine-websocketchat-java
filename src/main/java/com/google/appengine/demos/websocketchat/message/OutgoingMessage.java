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
 * An interface that is supposed to be passed to {@code ChatSocketServer#sendToClients()} method.
 */
public interface OutgoingMessage {
  /**
   * A type of the message.
   */
  public enum MessageType {
    /** A normal chat message. */
    MESSAGE,

    /** A message for notifying the participant list. */
    PARTICIPANTS,

    /** A system message. */
    SYSTEM,

    /** A message indicating someone entered the room. */
    ENTER,

    /** A message indicating someone left the room. */
    LEAVE,

    /** A special message for propagating various message between multiple server nodes. */
    PROPAGATE
  }
  /**
   * Returns the type of this message.
   *
   * @return the type of this message.
   */
  public MessageType getType();

  /**
   * Returns a JSON object that will be sent to the clients.
   *
   * @param gson a Gson object for serializing this message.
   * @return a JSON object that will be sent to the clients.
   */
  public String toJson(Gson gson);

  /**
   * Returns whether or not we should send this message to the given chat room.
   *
   * @param room a name of the chat room.
   * @return whether or not we should send this message to the given chat room.
   */
  public boolean shouldSendTo(String room);

  /**
   * Returns the name of the chat room that this message belongs to.
   *
   * @return the name of the chat room that this message belongs to.
   */
  public String getRoom();
}
