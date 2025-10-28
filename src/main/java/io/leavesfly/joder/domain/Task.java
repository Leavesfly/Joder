package io.leavesfly.joder.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;

/**
 * 任务数据模型
 */
public class Task {
    
    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETE,
        CANCELLED,
        ERROR
    }
    
    private String id;
    private String content;
    private Status status;
    
    @JsonProperty("parent_task_id")
    private String parentTaskId;
    
    @JsonProperty("after_task_id")
    private String afterTaskId;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    public Task() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public Task(String id, String content) {
        this();
        this.id = id;
        this.content = content;
        this.status = Status.PENDING;
    }
    
    public Task(String id, String content, Status status) {
        this(id, content);
        this.status = status;
    }
    
    // Getters and Setters
    
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
        this.updatedAt = Instant.now();
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public String getParentTaskId() {
        return parentTaskId;
    }
    
    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }
    
    public String getAfterTaskId() {
        return afterTaskId;
    }
    
    public void setAfterTaskId(String afterTaskId) {
        this.afterTaskId = afterTaskId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", parentTaskId='" + parentTaskId + '\'' +
                '}';
    }
}
