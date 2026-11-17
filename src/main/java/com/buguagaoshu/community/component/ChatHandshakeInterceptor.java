package com.buguagaoshu.community.component;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 聊天室握手鉴权：把 HTTP 会话中的登录用户绑定到 WebSocket attributes，
 * 未登录或管理员已关闭聊天室时拒绝握手。后续 STOMP 消息的身份一律以这里绑定的为准，
 * 不信任客户端 payload。
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "chatAuthUserId";
    public static final String ATTR_USER_NAME = "chatAuthUserName";
    public static final String ATTR_IMAGE = "chatAuthImage";

    private final SystemConfigService systemConfigService;

    public ChatHandshakeInterceptor(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!systemConfigService.isChatEnabled()) {
            return false;
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            Object value = session == null ? null : session.getAttribute(AuthUtil.SESSION_USER);
            if (value instanceof User user && user.getUserId() != null) {
                attributes.put(ATTR_USER_ID, user.getUserId());
                attributes.put(ATTR_USER_NAME, user.getUserName());
                attributes.put(ATTR_IMAGE, user.getHeadUrl());
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}