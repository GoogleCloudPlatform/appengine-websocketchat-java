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

/**
 * An entity class which represents a single websocket server node and holds a number of
 * active connections for this server node.
 */
@Entity
public class WebSocketServerNode {

  @Id
  private String webSocketUrl;

  @Parent
  private Key<WebSocketServerNode> parentKey;

  private long numberOfParticipants;

  /**
   * Returns an objectify key commonly used as the parent key for every instance of this class.
   *
   * @return an objectify key commonly used as the parent key for every instance of this class.
   */
  public static Key<WebSocketServerNode> getRootKey() {
    return Key.create(WebSocketServerNode.class, "Root");
  }

  /**
   * Returns an objectify key representing the given websocket server node.
   *
   * @param webSocketUrl an identifier of a single server node, in the form of websocket URL,
   *                     e.x. "ws://173.255.112.201:65080/".
   * @return an objectify key representing the given websocket server node.
   */
  public static Key<WebSocketServerNode> getKeyFromWebSocketUrl(String webSocketUrl) {
    return Key.create(getRootKey(), WebSocketServerNode.class, webSocketUrl);
  }

  /* Objectify needs the default constructor. */
  private WebSocketServerNode() {
  }

  /**
   * Creates an instance representing a single websocket server node.
   *
   * @param webSocketUrl an identifier of a single server node, in the form of websocket URL,
   *                     e.x. "ws://173.255.112.201:65080/".
   */
  public WebSocketServerNode(String webSocketUrl) {
    this.parentKey = getRootKey();
    this.webSocketUrl = webSocketUrl;
    this.numberOfParticipants = 0L;
  }

  /**
   * Returns an objectify key representing this entity.
   *
   * @return an objectify key representing this entity.
   */
  public Key<WebSocketServerNode> getKey() {
    return getKeyFromWebSocketUrl(this.webSocketUrl);
  }
}
