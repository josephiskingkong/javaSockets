package com.chat.client;

public class Message {
    private MessageType type;
    private String content;
    private String recipient;

    public Message(MessageType type, String content, String recipient) {
        this.type = type;
        this.content = content;
        this.recipient = recipient;
    }

    public MessageType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getRecipient() {
        return recipient;
    }

    public static Message fromJson(String json) {
        String[] parts = json.split(";", 3);
        MessageType type = MessageType.valueOf(parts[0]);
        String recipient = parts[1];
        String content = parts[2];
        return new Message(type, content, recipient);
    }

    public String toJson() {
        return type + ";" + recipient + ";" + content;
    }
}