package io.shareai.joder.domain;

/**
 * 提醒消息数据模型
 * 
 * <p>表示一条系统提醒消息。
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class ReminderMessage {
    
    /** 提醒类型 */
    private String type;
    
    /** 提醒分类 */
    private ReminderCategory category;
    
    /** 优先级 */
    private ReminderPriority priority;
    
    /** 提醒内容 */
    private String content;
    
    /** 创建时间戳 */
    private long timestamp;
    
    /** 是否已发送 */
    private boolean sent;
    
    // 构造函数
    
    public ReminderMessage() {}
    
    public ReminderMessage(String type, ReminderCategory category, 
                          ReminderPriority priority, String content, long timestamp) {
        this.type = type;
        this.category = category;
        this.priority = priority;
        this.content = content;
        this.timestamp = timestamp;
        this.sent = false;
    }
    
    // Getters and Setters
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public ReminderCategory getCategory() {
        return category;
    }
    
    public void setCategory(ReminderCategory category) {
        this.category = category;
    }
    
    public ReminderPriority getPriority() {
        return priority;
    }
    
    public void setPriority(ReminderPriority priority) {
        this.priority = priority;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSent() {
        return sent;
    }
    
    public void setSent(boolean sent) {
        this.sent = sent;
    }
    
    @Override
    public String toString() {
        return "ReminderMessage{" +
                "type='" + type + '\'' +
                ", category=" + category +
                ", priority=" + priority +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", sent=" + sent +
                '}';
    }
}
