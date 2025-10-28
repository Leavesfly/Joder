package io.leavesfly.joder.services.model.dto;

/**
 * 流式响应事件
 */
public class StreamEvent {
    
    public enum Type {
        CONTENT_DELTA,  // 内容增量
        DONE,           // 完成
        ERROR           // 错误
    }
    
    private final Type type;
    private final String content;
    private final String error;
    
    private StreamEvent(Type type, String content, String error) {
        this.type = type;
        this.content = content;
        this.error = error;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getError() {
        return error;
    }
    
    public boolean isContentDelta() {
        return type == Type.CONTENT_DELTA;
    }
    
    public boolean isDone() {
        return type == Type.DONE;
    }
    
    public boolean isError() {
        return type == Type.ERROR;
    }
    
    public static StreamEvent contentDelta(String content) {
        return new StreamEvent(Type.CONTENT_DELTA, content, null);
    }
    
    public static StreamEvent done() {
        return new StreamEvent(Type.DONE, null, null);
    }
    
    public static StreamEvent error(String error) {
        return new StreamEvent(Type.ERROR, null, error);
    }
}
