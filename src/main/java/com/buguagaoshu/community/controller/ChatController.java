package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.component.ChatPresenceRegistry;
import com.buguagaoshu.community.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * 聊天室 STOMP 消息处理。
 *
 * <p>同一账号只允许一个窗口加入：JOIN 时通过 {@link ChatPresenceRegistry}
 * 占用席位，被拒绝的窗口会收到带自身 clientToken 的 DUPLICATE 消息并自行断开。
 */
@Controller
public class ChatController {

    private final ChatPresenceRegistry presence;

    public ChatController(ChatPresenceRegistry presence) {
        this.presence = presence;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage message,
                               SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        boolean allowed = presence.tryJoin(message.getId(), sessionId,
                message.getClientToken(), message.getSender(), message.getImage());

        if (!allowed) {
            ChatMessage rejected = new ChatMessage();
            rejected.setType(ChatMessage.MessageType.DUPLICATE);
            rejected.setClientToken(message.getClientToken());
            rejected.setId(message.getId());
            rejected.setSender(message.getSender());
            rejected.setContent("该账号已在其他窗口加入聊天室，请先关闭其他窗口后再试。");
            rejected.setCount(presence.count());
            return rejected;
        }

        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        if (attributes != null) {
            attributes.put("username", message.getSender());
            attributes.put("chatUserId", message.getId());
            attributes.put("chatClientToken", message.getClientToken());
            attributes.put("chatSessionId", sessionId);
        }
        message.setType(ChatMessage.MessageType.JOIN);
        message.setCount(presence.count());
        return message;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage message) {
        // 被拒绝的窗口即使绕过前端也不允许发消息
        if (!presence.isActive(message.getId(), message.getClientToken())) {
            return null;
        }
        message.setCount(presence.count());
        return message;
    }
}
