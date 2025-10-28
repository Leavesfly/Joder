package io.shareai.joder.services.model.dto;

/**
 * 流式响应处理器
 */
@FunctionalInterface
public interface StreamHandler {
    
    /**
     * 处理流式事件
     * 
     * @param event 流式事件
     */
    void onEvent(StreamEvent event);
}
