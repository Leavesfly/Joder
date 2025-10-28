package io.shareai.joder.services.model;

import io.shareai.joder.domain.Message;

import java.util.List;

/**
 * 模型适配器接口
 * 统一不同 AI 提供商的 API 调用
 */
public interface ModelAdapter {
    
    /**
     * 发送消息并获取响应
     * 
     * @param messages 消息历史
     * @param systemPrompt 系统提示词
     * @return AI 响应内容
     */
    String sendMessage(List<Message> messages, String systemPrompt);
    
    /**
     * 获取模型名称
     */
    String getModelName();
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 检查 API 密钥是否配置
     */
    boolean isConfigured();
}
