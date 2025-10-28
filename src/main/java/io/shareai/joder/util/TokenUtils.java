package io.shareai.joder.util;

import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Token 计数工具类
 * 对应 Kode 的 tokens.ts
 */
public class TokenUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenUtils.class);
    
    /**
     * 计算消息列表的总 token 数
     * 
     * 注意：这是一个简化实现，实际应该根据 AI 模型的响应获取 token 信息
     * 后续需要在 Message 类中添加 tokenUsage 字段
     * 
     * @param messages 消息列表
     * @return 总 token 数
     */
    public static int countTokens(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        // TODO: 实现实际的 token 计算逻辑
        // 这里仅作为占位实现
        int totalChars = 0;
        for (Message message : messages) {
            if (message.getContent() != null) {
                totalChars += message.getContent().length();
            }
        }
        
        // 粗略估计：平均 4 个字符 约等于 1 个 token
        return totalChars / 4;
    }
    
    /**
     * 计算缓存的 token 数
     * 
     * @param messages 消息列表
     * @return 缓存的 token 数
     */
    public static int countCachedTokens(List<Message> messages) {
        // TODO: 实现实际的缓存 token 计算逻辑
        return 0;
    }
    
    /**
     * Token 使用情况
     */
    public static class TokenUsage {
        private final int inputTokens;
        private final int outputTokens;
        private final int cacheCreationInputTokens;
        private final int cacheReadInputTokens;
        
        public TokenUsage(int inputTokens, int outputTokens) {
            this(inputTokens, outputTokens, 0, 0);
        }
        
        public TokenUsage(int inputTokens, int outputTokens, 
                         int cacheCreationInputTokens, int cacheReadInputTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.cacheCreationInputTokens = cacheCreationInputTokens;
            this.cacheReadInputTokens = cacheReadInputTokens;
        }
        
        public int getInputTokens() {
            return inputTokens;
        }
        
        public int getOutputTokens() {
            return outputTokens;
        }
        
        public int getCacheCreationInputTokens() {
            return cacheCreationInputTokens;
        }
        
        public int getCacheReadInputTokens() {
            return cacheReadInputTokens;
        }
        
        /**
         * 获取总 token 数
         */
        public int getTotalTokens() {
            return inputTokens + cacheCreationInputTokens + cacheReadInputTokens + outputTokens;
        }
        
        /**
         * 获取缓存的 token 数
         */
        public int getCachedTokens() {
            return cacheCreationInputTokens + cacheReadInputTokens;
        }
        
        @Override
        public String toString() {
            return String.format("TokenUsage{input=%d, output=%d, cacheCreation=%d, cacheRead=%d, total=%d}",
                inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens, getTotalTokens());
        }
    }
}
