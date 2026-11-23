package com.buguagaoshu.community.component;

import com.buguagaoshu.community.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * WebSocket 生命周期监听：连接建立时不再简单 +1，断开时按 clientToken
 * 精确释放 {@link ChatPresenceRegistry} 中的席位，并广播 LEAVE。
 */
@Component
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatPresenceRegistry presence;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate,
                                  ChatPresenceRegistry presence) {
        this.messagingTemplate = messagingTemplate;
        this.presence = presence;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.debug("聊天室建立连接，当前在线 {} 人", presence.count());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return;
        }
        String userId = (String) attributes.get("chatUserId");
        String clientToken = (String) attributes.get("chatClientToken");
        ChatPresenceRegistry.Presence removed = presence.leave(userId, clientToken);
        if (removed == null) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(ChatMessage.MessageType.LEAVE);
        message.setSender(removed.userName());
        message.setImage(removed.image());
        message.setId(userId);
        message.setCount(presence.count());
        message.setTime(System.currentTimeMillis());
        message.setMembers(presence.members());
        messagingTemplate.convertAndSend("/topic/public", message);
    }
}
