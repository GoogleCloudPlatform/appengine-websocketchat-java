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

import com.google.appengine.api.LifecycleManager.ShutdownHook;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.utils.SystemProperty;

import java.util.logging.Logger;

/**
 * An implementation for the ShutdownHook.
 *
 * @see <a href="https://developers.google.com/appengine/docs/java/backends/?hl=ja#Shutdown">
 * Backends Java API Overview#Shutdown</a>
 */
public class ChatSocketServerShutdownHook implements ShutdownHook{

  private static final Logger LOG = Logger.getLogger(ShutdownHook.class.getName());

  @Override
  public void shutdown() {
    LOG.info("The ChatSocketServerShutdownHook is called.");
    String version = SystemProperty.applicationVersion.get();
    String majorVersion = version.substring(0, version.indexOf('.'));
    NamespaceManager.set(majorVersion);
    LOG.info("Namespace set to " + majorVersion);
    ChatSocketServer.ChatServerBridge chatServerBridge =
        ChatSocketServer.ChatServerBridge.getInstance();
    chatServerBridge.stop();
  }
}
