package com.buguagaoshu.community.model;

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

    public enum MessageType {
        /** 聊天 */
        CHAT,

        /** 加入 */
        JOIN,

        /** 离开 */
        LEAVE,

        /** 同一账号已在其他窗口加入，当前窗口被拒绝 */
        DUPLICATE
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
