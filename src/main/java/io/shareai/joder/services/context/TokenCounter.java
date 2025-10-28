package io.shareai.joder.services.context;

import io.shareai.joder.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;

/**
 * Token 计数器
 * <p>
 * 提供 Token 估算功能
 * 注意:这是一个简化的实现,实际token数可能有所不同
 * </p>
 */
@Singleton
public class TokenCounter {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCounter.class);
    
    /**
     * 平均每个字符的token数 (粗略估算)
     * - 英文: 约 0.25 tokens/char (1 token = 4 chars)
     * - 中文: 约 0.5 tokens/char (1 token = 2 chars)
     * 这里取折中值
     */
    private static final double AVG_TOKENS_PER_CHAR = 0.35;
    
    /**
     * 计算消息列表的总 token 数
     * 
     * @param messages 消息列表
     * @return 估算的 token 数
     */
    public int countTokens(List<Message> messages) {
        int totalTokens = 0;
        
        for (Message message : messages) {
            totalTokens += countTokens(message);
        }
        
        return totalTokens;
    }
    
    /**
     * 计算单个消息的 token 数
     * 
     * @param message 消息
     * @return 估算的 token 数
     */
    public int countTokens(Message message) {
        if (message == null || message.getContent() == null) {
            return 0;
        }
        
        // 简单估算: 字符数 * 平均token比例
        int charCount = message.getContent().length();
        int tokens = (int) Math.ceil(charCount * AVG_TOKENS_PER_CHAR);
        
        // 添加角色元数据的token (约10个)
        tokens += 10;
        
        return tokens;
    }
    
    /**
     * 计算文本的 token 数
     * 
     * @param text 文本内容
     * @return 估算的 token 数
     */
    public int countTokens(String text) {
        if (text == null) {
            return 0;
        }
        
        return (int) Math.ceil(text.length() * AVG_TOKENS_PER_CHAR);
    }
    
    /**
     * 检查消息列表是否超过 token 限制
     * 
     * @param messages 消息列表
     * @param maxTokens 最大 token 数
     * @return 是否超过限制
     */
    public boolean exceedsLimit(List<Message> messages, int maxTokens) {
        int totalTokens = countTokens(messages);
        return totalTokens > maxTokens;
    }
    
    /**
     * 计算 token 使用率
     * 
     * @param messages 消息列表
     * @param maxTokens 最大 token 数
     * @return 使用率 (0.0 - 1.0)
     */
    public double calculateUsage(List<Message> messages, int maxTokens) {
        if (maxTokens <= 0) {
            return 0.0;
        }
        
        int totalTokens = countTokens(messages);
        return Math.min(1.0, (double) totalTokens / maxTokens);
    }
    
    /**
     * 格式化 token 数为易读字符串
     * 
     * @param tokens token 数
     * @return 格式化字符串 (如 "1.2K", "15.3K")
     */
    public String formatTokens(int tokens) {
        if (tokens < 1000) {
            return String.valueOf(tokens);
        } else if (tokens < 1000000) {
            return String.format("%.1fK", tokens / 1000.0);
        } else {
            return String.format("%.1fM", tokens / 1000000.0);
        }
    }
}
