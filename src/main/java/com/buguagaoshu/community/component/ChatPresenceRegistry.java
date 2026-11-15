package com.buguagaoshu.community.component;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天室在线注册表：保证同一个账号同一时间只有一个浏览器窗口在聊天室里。
 *
 * <p>key 是用户 userId（不是昵称，昵称可能重复），value 记录该用户当前占用的
 * STOMP sessionId 与前端生成的 clientToken。前端的 JOIN 消息带上 clientToken，
 * 被拒绝时服务端把同一个 token 原样发回，只有该窗口才会处理拒绝提示。
 */
@Component
public class ChatPresenceRegistry {

    public record Presence(String sessionId, String clientToken, String userName, String image) {
    }

    private final Map<String, Presence> online = new ConcurrentHashMap<>();

    /**
     * 尝试占用一个聊天席位。
     *
     * @return true 表示占用成功（或同一个窗口重复 JOIN）；false 表示该账号已在别的窗口登录
     */
    public boolean tryJoin(String userId, String sessionId, String clientToken,
                           String userName, String image) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        Presence candidate = new Presence(sessionId, clientToken, userName, image);
        Presence existing = online.putIfAbsent(userId, candidate);
        if (existing == null) {
            return true;
        }
        // 同一个 STOMP 会话重发 JOIN（例如网络重连）视为同一窗口
        if (sessionId != null && sessionId.equals(existing.sessionId())) {
            online.put(userId, candidate);
            return true;
        }
        return false;
    }

    /** 只释放属于该令牌的席位，避免旧窗口的延迟断开把新窗口踢掉 */
    public Presence leave(String userId, String clientToken) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        Presence current = online.get(userId);
        if (current == null) {
            return null;
        }
        if (clientToken != null && !clientToken.equals(current.clientToken())) {
            return null;
        }
        return online.remove(userId);
    }

    /** 判断某个窗口是否仍是该账号的合法窗口 */
    public boolean isActive(String userId, String clientToken) {
        Presence current = userId == null ? null : online.get(userId);
        return current != null && clientToken != null && clientToken.equals(current.clientToken());
    }

    public int count() {
        return online.size();
    }
}
