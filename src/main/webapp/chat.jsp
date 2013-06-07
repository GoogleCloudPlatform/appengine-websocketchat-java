<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="com.google.appengine.demos.websocketchat.server.ChatSocketServer" %>

<%--
  ~ Copyright (c) 2013 Google Inc. All Rights Reserved.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you
  ~ may not use this file except in compliance with the License. You may
  ~ obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  ~ implied. See the License for the specific language governing
  ~ permissions and limitations under the License.
  --%>

<%
  UserService userService = UserServiceFactory.getUserService();
  User user = userService.getCurrentUser();
  String webSocketURL = ChatSocketServer.ChatServerBridge.getInstance().getWebSocketURL();
%>

<!DOCTYPE html>

<html>

<head>
  <link href='//fonts.googleapis.com/css?family=Marmelad' rel='stylesheet' type='text/css'>
  <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
  <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js"></script>
</head>

<body>
<div id="container">

  <div id="header">
    <h2>WebSocket chat on App Engine VM Runtime</h2>
    Name: <input id="name" value="<%= user.getNickname() %>">
    Room: <input id="room" value="#vmruntime">
    <input type="button" id="enter" value="Enter">
    <input type="button" id="leave" value="Leave" style="display: none;">

    <form id="chatForm">
      Message: <input id="message" size="50" autocomplete="off">
    </form>
  </div>

  <div id="participants">Participants:</div>
  <div id="messages"></div>
</div>

</body>
<script>
  var wschat = {
    connection: null,
    should_be_connected: false
  }

  function updateParticipantsList(message) {
    var participants = message.participantSet;
    $('#participants').children().remove();
    for (var i = 0; i < participants.length; i++) {
      $('#participants').append('<p>' + $('<div/>').text(participants[i]).html());
    }
  }

  function resetDOMsOnClose() {
    $('#participants').children().remove();
    $('#enter').show();
    $('#leave').hide();
    $('#name').attr('readonly', false);
    $('#room').attr('readonly', false);
  }

  function closeWebSocketConnection() {
    wschat.should_be_connected = false;
    wschat.connection.close();
    resetDOMsOnClose();
  }

  function openWebSocketConnection() {
    $('#enter').hide();
    $('#leave').show();
    $('#name').attr('readonly', true);
    $('#room').attr('readonly', true);
    wschat.connection = new WebSocket('<%= webSocketURL %>');
    wschat.connection.onopen = function () {
      console.log('Websocket opened');
      var messageBody = {
        type: 'ENTER',
        name: $('#name').val(),
        room: $('#room').val(),
        message: ''
      }
      wschat.connection.send(JSON.stringify(messageBody));
    }
    wschat.connection.onerror = function (error) {
      console.log('Error detected: ' + error);
      closeWebSocketConnection();
    }
    wschat.connection.onmessage = function (e) {
      var server_message = e.data;
      console.log('Message received: ' + server_message);
      var message = JSON.parse(server_message);
      if (message.type == 'PARTICIPANTS') {
        updateParticipantsList(message);
        return;
      }
      if (message.type == 'SYSTEM') {
        $('#messages').prepend('<p class="system">' + $('<div/>').text(message.message).html());
        $('#name').val(message.name);
        return;
      }
      var text_message;
      if (message.type == 'MESSAGE') {
        text_message = message.name + ': ' + message.message;
      } else if (message.type == 'ENTER') {
        text_message = message.name + ' entered ' + message.room;
      } else if (message.type == 'LEAVE') {
        text_message = message.name + ' left ' + message.room;
      }
      $('#messages').prepend('<p>' + $('<div/>').text(text_message).html());
    }
    wschat.connection.onclose = function () {
      console.log('Connection closed.');
      resetDOMsOnClose();
    }
    wschat.should_be_connected = true;
  }
  $(document).ready(function () {
    $('#enter').click(function(e) {
      openWebSocketConnection();
    });
    $('#leave').click(function(e) {
      closeWebSocketConnection();
    });
    $('#chatForm').submit(function(e) {
      e.preventDefault();
      if (wschat.connection === undefined ||
          wschat.should_be_connected === false) {
        window.alert('Please enter a room before posting.');
        return false;
      }
      var messageBody = {
        type: 'MESSAGE',
        name: $('#name').val(),
        room: $('#room').val(),
        message: $('#message').val()
      }
      wschat.connection.send(JSON.stringify(messageBody));
      $('#message').val('');
      return false;
    });
  });

</script>
</html>
