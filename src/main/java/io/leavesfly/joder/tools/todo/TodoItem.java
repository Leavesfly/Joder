package io.leavesfly.joder.tools.todo;

/**
 * Todo 项数据模型
 */
public class TodoItem {
    private String id;
    private String content;
    private String status; // pending, in_progress, completed
    private String priority; // high, medium, low
    
    public TodoItem() {
    }
    
    public TodoItem(String id, String content, String status, String priority) {
        this.id = id;
        this.content = content;
        this.status = status;
        this.priority = priority;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    @Override
    public String toString() {
        String checkbox = switch (status) {
            case "completed" -> "☒";
            case "in_progress" -> "▶";
            default -> "☐";
        };
        
        String priorityMark = switch (priority) {
            case "high" -> "[!]";
            case "medium" -> "[·]";
            case "low" -> "[-]";
            default -> "";
        };
        
        return String.format("%s %s %s", checkbox, priorityMark, content);
    }
}
