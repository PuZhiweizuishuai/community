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

    public enum MessageType {
        /**
         * 聊天
         * */
        CHAT,

        /**
         * 加入
         * */
        JOIN,

        /**
         * 离开
         * */
        LEAVE
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
