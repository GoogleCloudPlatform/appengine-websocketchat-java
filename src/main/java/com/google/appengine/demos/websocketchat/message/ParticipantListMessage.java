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

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;

import java.util.Set;

/**
 * A message for notifying the participant list of the chat room.
 */
public class ParticipantListMessage implements OutgoingMessage {

  private MessageType type;

  private String room;

  private Set<String> participantSet;

  /**
   * Creates a ParticipantListMessage instance with the given parameters.
   *
   * @param room a name of the chat room.
   * @param participantSet a set of the names of the participants.
   */
  public ParticipantListMessage(String room, Set<String> participantSet) {
    this.type = MessageType.PARTICIPANTS;
    this.room = room;
    this.participantSet = ImmutableSet.copyOf(participantSet);
  }

  @Override
  public MessageType getType() {
    return type;
  }

  @Override
  public String toJson(Gson gson) {
    return gson.toJson(this);
  }

  @Override
  public boolean shouldSendTo(String room) {
    return this.room.equals(room);
  }

  @Override
  public String getRoom() {
    return room;
  }

  /**
   * Returns the set of the names of the participants.
   *
   * @return the set of the names of the participants.
   */
  public Set<String> getParticipantSet() {
    return participantSet;
  }
}
