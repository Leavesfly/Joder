package io.leavesfly.joder.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 消息模型
 */
public class Message {
    private final String id;
    private final MessageRole role;
    private final String content;
    private final Instant timestamp;
    
    public Message(MessageRole role, String content) {
        this.id = UUID.randomUUID().toString();
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }
    
    public Message(String id, MessageRole role, String content, Instant timestamp) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }
    
    public String getId() {
        return id;
    }
    
    public MessageRole getRole() {
        return role;
    }
    
    public String getContent() {
        return content;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", role=" + role +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
