package com.buguagaoshu.community.model;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-10 19:12
 * 聊天消息
 */
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String image;
    private String id;
    private int count;
    /** 每个聊天窗口生成一个随机令牌，用于把「重复登录」提示定向回该窗口 */
    private String clientToken;
    /** 服务端时间戳（毫秒），用于前端按日期分隔与显示发送时间 */
    private Long time;
    /**
     * 在线成员快照，仅在 JOIN / LEAVE 时下发。
     * 聊天消息不带该字段，避免每条消息都重复传输整份名单。
     */
    private List<Member> members;

    /** 在线成员：只暴露展示所需的最小信息 */
    public static class Member {
        private String id;
        private String name;
        private String image;

        public Member() {
        }

        public Member(String id, String name, String image) {
            this.id = id;
            this.name = name;
            this.image = image;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }

    public enum MessageType {
        /** 聊天 */
        CHAT,

        /** 加入 */
        JOIN,

        /** 离开 */
        LEAVE,

        /** 同一账号已在其他窗口加入，当前窗口被拒绝 */
        DUPLICATE,

        /** 管理员关闭了聊天室，客户端应主动退出 */
        DISABLED
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", image='" + image + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
