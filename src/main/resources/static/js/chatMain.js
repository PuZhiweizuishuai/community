'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var username = null;
var userId = null;
var userImage = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    username = document.querySelector('#name').value.trim();
    userId = document.querySelector("#userId").value.trim();
    userImage = document.querySelector("#userImage").value.trim();
    if(username && userId && userImage) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}


function onConnected() {
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({
            sender: username,
            type: 'JOIN',
            image: userImage,
            id: userId
        })
    );

    connectingElement.classList.add('hidden');
}


function onError(error) {
    connectingElement.textContent = '无法连接到WebSocket服务器。请刷新此页面以重试！';
    connectingElement.style.color = 'red';
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();

    if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'CHAT',
            image: userImage,
            id: userId
        };

        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}


function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    var messageElement = document.createElement('li');

    if(message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        //message.content = "<a target='_blank' href='/user/" + message.id + "'>" + message.sender  + "</a>" + ' 加入了聊天';
        message.content = message.sender + ' 加入了聊天';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        //message.content = "<a target='_blank' href='/user/" + message.id + "'>" + message.sender + "</a>" + ' 离开了聊天！';
        message.content = message.sender + ' 离开了聊天！';
    } else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        var userUrl = document.createElement("a");
        userUrl.href = "/user/" + message.id;
        userUrl.target = "_blank";
        var headImage = document.createElement("img");
        headImage.width = 42;
        headImage.src = message.image;

        userUrl.append(headImage);
        avatarElement.append(userUrl);

        var avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        //avatarElement.style['background-color'] = getAvatarColor(message.sender);
        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('p');
    var messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}


function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    var index = Math.abs(hash % colors.length);
    return colors[index];
}

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
