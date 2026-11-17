package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.component.ChatHandshakeInterceptor;
import com.buguagaoshu.community.component.ChatPresenceRegistry;
import com.buguagaoshu.community.model.ChatMessage;
import com.buguagaoshu.community.service.SystemConfigService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * 聊天室 STOMP 消息处理。
 *
 * <p>身份（userId/昵称/头像）一律取自握手时绑定的会话用户，忽略客户端 payload 中的身份字段，
 * 防止冒充他人发言。同一账号只允许一个窗口加入：JOIN 时通过 {@link ChatPresenceRegistry}
 * 占用席位，被拒绝的窗口会收到带自身 clientToken 的 DUPLICATE 消息并自行断开。
 */
@Controller
public class ChatController {

    private final ChatPresenceRegistry presence;
    private final SystemConfigService systemConfigService;

    public ChatController(ChatPresenceRegistry presence, SystemConfigService systemConfigService) {
        this.presence = presence;
        this.systemConfigService = systemConfigService;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage message,
                               SimpMessageHeaderAccessor headerAccessor) {
        if (!systemConfigService.isChatEnabled()) {
            return null;
        }
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        String userId = attribute(attributes, ChatHandshakeInterceptor.ATTR_USER_ID);
        if (userId == null) {
            return null;
        }
        String userName = attribute(attributes, ChatHandshakeInterceptor.ATTR_USER_NAME);
        String image = attribute(attributes, ChatHandshakeInterceptor.ATTR_IMAGE);
        String sessionId = headerAccessor.getSessionId();
        boolean allowed = presence.tryJoin(userId, sessionId,
                message.getClientToken(), userName, image);

        if (!allowed) {
            ChatMessage rejected = new ChatMessage();
            rejected.setType(ChatMessage.MessageType.DUPLICATE);
            rejected.setClientToken(message.getClientToken());
            rejected.setId(userId);
            rejected.setSender(userName);
            rejected.setContent("该账号已在其他窗口加入聊天室，请先关闭其他窗口后再试。");
            rejected.setCount(presence.count());
            return rejected;
        }

        if (attributes != null) {
            attributes.put("username", userName);
            attributes.put("chatUserId", userId);
            attributes.put("chatClientToken", message.getClientToken());
            attributes.put("chatSessionId", sessionId);
        }
        message.setId(userId);
        message.setSender(userName);
        message.setImage(image);
        message.setType(ChatMessage.MessageType.JOIN);
        message.setCount(presence.count());
        return message;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage message,
                                   SimpMessageHeaderAccessor headerAccessor) {
        if (!systemConfigService.isChatEnabled()) {
            return null;
        }
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        String userId = attribute(attributes, ChatHandshakeInterceptor.ATTR_USER_ID);
        String clientToken = attribute(attributes, "chatClientToken");
        // 未 JOIN 成功（含被拒绝的窗口）不允许发消息；身份以会话绑定为准
        if (userId == null || !presence.isActive(userId, clientToken)) {
            return null;
        }
        message.setId(userId);
        message.setSender(attribute(attributes, ChatHandshakeInterceptor.ATTR_USER_NAME));
        message.setImage(attribute(attributes, ChatHandshakeInterceptor.ATTR_IMAGE));
        message.setCount(presence.count());
        return message;
    }

    private String attribute(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value instanceof String s ? s : null;
    }
}