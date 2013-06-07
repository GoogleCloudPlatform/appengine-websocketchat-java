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

package com.google.appengine.demos.websocketchat.servlet;

import com.google.appengine.api.LifecycleManager;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.demos.websocketchat.server.ChatSocketServer;
import com.google.appengine.demos.websocketchat.domain.ChatRoomParticipants;
import com.google.appengine.demos.websocketchat.domain.WebSocketServerNode;
import com.google.appengine.demos.websocketchat.server.ChatSocketServerShutdownHook;
import com.googlecode.objectify.ObjectifyService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A servlet that is called when accesing /_ah/start.
 */
public class StartupServlet extends HttpServlet {

  static {
    ObjectifyService.register(WebSocketServerNode.class);
    ObjectifyService.register(ChatRoomParticipants.class);
  }

  private static final Logger LOG = Logger.getLogger(StartupServlet.class.getName());

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    LOG.info("The startup servlet called.");
    LifecycleManager.getInstance().setShutdownHook(new ChatSocketServerShutdownHook());
    String version = SystemProperty.applicationVersion.get();
    String majorVersion = version.substring(0, version.indexOf('.'));
    NamespaceManager.set(majorVersion);
    LOG.info("Namespace set to " + majorVersion);
    ChatSocketServer.ChatServerBridge chatServerBridge =
        ChatSocketServer.ChatServerBridge.getInstance();
    chatServerBridge.start();
    response.setStatus(204);
  }
}
