'use strict';

window.onload = function(){
  let body = document.getElementsByTagName('body')[0];
  let ursNamInput = document.getElementById('ursNamInput');
  let roomNamInput = document.getElementById('roomNamInput');
  var chatForm;
  var messageInput;
  var oepnsocket;
  let xhr = new XMLHttpRequest();
  let mySocket = new WebSocket("ws://" + location.host);

/***********************************************************************************
 Join room listeners and events*/
 let joinRoomButton = document.getElementById('joinRoomButton');
  joinRoomButton.addEventListener('click', joinRoomButtonListener);
  let homeForm = document.getElementById('homeForm');
  enterEventHandler(homeForm, joinRoomButtonListener);

  function joinRoomButtonListener() {
    xhr.addEventListener("load", joinRoom);
    xhr.open("GET", "chatPage.html", true);
    xhr.send();
  }


  // Establishes the chatroom page.
  function joinRoom() {
    console.log(this.responseText);
    // refreshing to chat page
    body.innerHTML = this.responseText;
    let sendMessageButton = document.getElementById('sendMessageButton');
    sendMessageButton.addEventListener('click', sendMessageButtonListener);
    // Takes you back to the home page
    let leaveRoomButton = document.getElementById('leaveRoomButton');
    leaveRoomButton.addEventListener('click', function () {
      window.location.href = "webClient.html"
      mySocket.close();
      oepnsocket = false;
    });
    messageInput = document.getElementById('MessageInput');
    mySocket.onopen = function(){oepnsocket = true;};
    mySocket.send("join " + roomNamInput.value);
    chatForm = document.getElementById('chatForm');
    enterEventHandler(chatForm, sendMessageButtonListener);
  }

  // Handles scrbing the message in a JSON format and sending the message to the server.
  function sendMessageButtonListener() {
  let date = new Date();
  let fullMessage = {user:ursNamInput.value, message:messageInput.value, time:date.toLocaleTimeString()};
  let jsonMessage = JSON.stringify(fullMessage);
  mySocket.send(jsonMessage);
  messageInput.value = "";
  }

  // Handles receiving a message from the server and parsing the JSON message
  mySocket.onmessage = function(event) {
    let obj = JSON.parse(event.data);
    console.log(obj.user + " " + obj.message + " " + obj.time);
    if (obj.message == ""){ return}
    let message = document.createElement('p');
    let responce = document.createTextNode(obj.time + " " + obj.user + ": " + obj.message);
    let messageDiv = document.createElement('div');
    messageDiv.style= "list-style-type:none";
    message.appendChild(responce);
    messageDiv.appendChild(message);;
    body.insertBefore(messageDiv, chatForm);
    chatForm.scrollIntoView()
  } 
}

// Used to handle pressing enter.
function enterEventHandler(form, functionToCall) {
  form.addEventListener('keydown', function (e) {
    if (e.keyCode == 13) {
      e.preventDefault();
      functionToCall();
    }
  });
}


