package io.shareai.joder.services.context;

import io.shareai.joder.core.config.ConfigManager;
import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import io.shareai.joder.services.model.ModelAdapter;
import io.shareai.joder.services.model.ModelRouter;
import io.shareai.joder.services.model.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文压缩器
 * <p>
 * 核心功能:
 * - 当上下文使用超过阈值(默认60%)时触发压缩
 * - 使用轻量模型总结早期对话
 * - 保留关键决策和状态变更
 * - 智能文件缓存和工具结果精简
 * </p>
 */
@Singleton
public class ContextCompressor {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextCompressor.class);
    
    /**
     * 默认压缩阈值 (60%)
     */
    private static final double DEFAULT_COMPRESSION_THRESHOLD = 0.6;
    
    /**
     * 保留最近消息的数量
     */
    private static final int KEEP_RECENT_MESSAGES = 10;
    
    private final ConfigManager configManager;
    private final ModelRouter modelRouter;
    private final TokenCounter tokenCounter;
    
    private double compressionThreshold;
    private boolean autoCompressionEnabled;
    
    @Inject
    public ContextCompressor(
            ConfigManager configManager,
            ModelRouter modelRouter,
            TokenCounter tokenCounter) {
        this.configManager = configManager;
        this.modelRouter = modelRouter;
        this.tokenCounter = tokenCounter;
        
        // 从配置读取设置
        this.compressionThreshold = configManager.getDouble(
            "joder.context.max-usage-percent", DEFAULT_COMPRESSION_THRESHOLD);
        this.autoCompressionEnabled = configManager.getBoolean(
            "joder.context.auto-compress", true);
        
        logger.info("Context compressor initialized. Threshold: {}%, Auto-compression: {}", 
                    (int)(compressionThreshold * 100), autoCompressionEnabled);
    }
    
    /**
     * 检查是否需要压缩
     * 
     * @param messages 消息历史
     * @param maxTokens 最大token限制
     * @return 是否需要压缩
     */
    public boolean needsCompression(List<Message> messages, int maxTokens) {
        if (!autoCompressionEnabled) {
            return false;
        }
        
        int currentTokens = tokenCounter.countTokens(messages);
        double usage = (double) currentTokens / maxTokens;
        
        boolean needs = usage >= compressionThreshold;
        if (needs) {
            logger.info("Context compression needed: {}/{} tokens ({}%)", 
                       currentTokens, maxTokens, (int)(usage * 100));
        }
        
        return needs;
    }
    
    /**
     * 压缩消息历史
     * 
     * @param messages 原始消息历史
     * @param maxTokens 最大token限制
     * @return 压缩结果
     */
    public CompressionResult compress(List<Message> messages, int maxTokens) {
        logger.info("Starting context compression for {} messages", messages.size());
        
        Instant startTime = Instant.now();
        
        // 1. 分离最近的消息(保留原样)
        int totalMessages = messages.size();
        int keepCount = Math.min(KEEP_RECENT_MESSAGES, totalMessages);
        
        List<Message> recentMessages = messages.subList(
            Math.max(0, totalMessages - keepCount), 
            totalMessages
        );
        
        List<Message> oldMessages = messages.subList(0, Math.max(0, totalMessages - keepCount));
        
        logger.debug("Splitting messages: {} old, {} recent", oldMessages.size(), recentMessages.size());
        
        // 2. 总结旧消息
        List<Message> compressedMessages = new ArrayList<>();
        
        if (!oldMessages.isEmpty()) {
            String summary = summarizeMessages(oldMessages);
            
            // 创建总结消息
            Message summaryMessage = new Message(
                MessageRole.SYSTEM,
                "📋 **上下文总结** (原始 " + oldMessages.size() + " 条消息)\n\n" + summary
            );
            compressedMessages.add(summaryMessage);
        }
        
        // 3. 添加最近消息
        compressedMessages.addAll(recentMessages);
        
        // 4. 计算压缩统计
        int originalTokens = tokenCounter.countTokens(messages);
        int compressedTokens = tokenCounter.countTokens(compressedMessages);
        
        Instant endTime = Instant.now();
        long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        
        CompressionResult result = new CompressionResult(
            compressedMessages,
            messages.size(),
            compressedMessages.size(),
            originalTokens,
            compressedTokens,
            durationMs
        );
        
        logger.info("Compression completed: {} -> {} messages, {} -> {} tokens ({} saved, {}ms)", 
                   result.getOriginalMessageCount(),
                   result.getCompressedMessageCount(),
                   result.getOriginalTokens(),
                   result.getCompressedTokens(),
                   result.getSavedTokens(),
                   result.getDurationMs());
        
        return result;
    }
    
    /**
     * 总结消息列表
     * 使用轻量模型进行总结
     * 
     * @param messages 要总结的消息
     * @return 总结内容
     */
    private String summarizeMessages(List<Message> messages) {
        logger.debug("Summarizing {} messages using lightweight model", messages.size());
        
        try {
            // 使用轻量模型进行总结
            ModelAdapter lightweightModel = modelRouter.routeModel(TaskType.SUMMARIZATION);
            
            // 构建总结提示
            String prompt = buildSummarizationPrompt(messages);
            
            // 创建临时消息列表用于总结
            List<Message> summaryMessages = List.of(
                new Message(MessageRole.USER, prompt)
            );
            
            String summary = lightweightModel.sendMessage(summaryMessages, 
                "你是一个专业的对话总结助手。请简洁准确地总结对话内容,保留关键信息。");
            
            return summary;
            
        } catch (Exception e) {
            logger.error("Failed to summarize messages using AI, falling back to simple summary", e);
            return createSimpleSummary(messages);
        }
    }
    
    /**
     * 构建总结提示词
     */
    private String buildSummarizationPrompt(List<Message> messages) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请总结以下对话内容,保留:\n");
        prompt.append("1. 用户的主要需求和问题\n");
        prompt.append("2. 重要的技术决策\n");
        prompt.append("3. 已完成的关键操作\n");
        prompt.append("4. 待解决的问题\n\n");
        prompt.append("对话内容:\n");
        prompt.append("---\n");
        
        for (Message msg : messages) {
            prompt.append(msg.getRole()).append(": ");
            
            // 限制单条消息长度
            String content = msg.getContent();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            prompt.append(content).append("\n\n");
        }
        
        prompt.append("---\n\n");
        prompt.append("请用中文总结,控制在300字以内。");
        
        return prompt.toString();
    }
    
    /**
     * 创建简单总结(fallback)
     */
    private String createSimpleSummary(List<Message> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("早期对话包含以下内容:\n\n");
        
        // 统计用户消息
        long userMessages = messages.stream()
            .filter(m -> m.getRole() == MessageRole.USER)
            .count();
        
        // 提取关键词
        String keywords = messages.stream()
            .filter(m -> m.getRole() == MessageRole.USER)
            .map(Message::getContent)
            .limit(5)
            .collect(Collectors.joining(", "))
            .substring(0, Math.min(200, messages.size() * 20));
        
        summary.append("- 用户消息数: ").append(userMessages).append("\n");
        summary.append("- 主要讨论: ").append(keywords).append("...\n");
        summary.append("- 时间范围: ").append(messages.get(0).getTimestamp())
               .append(" 至 ").append(messages.get(messages.size()-1).getTimestamp()).append("\n");
        
        return summary.toString();
    }
    
    /**
     * 设置压缩阈值
     * 
     * @param threshold 阈值 (0.0 - 1.0)
     */
    public void setCompressionThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        this.compressionThreshold = threshold;
        logger.info("Compression threshold updated to {}%", (int)(threshold * 100));
    }
    
    /**
     * 启用或禁用自动压缩
     */
    public void setAutoCompressionEnabled(boolean enabled) {
        this.autoCompressionEnabled = enabled;
        logger.info("Auto-compression {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * 获取压缩阈值
     */
    public double getCompressionThreshold() {
        return compressionThreshold;
    }
    
    /**
     * 检查自动压缩是否启用
     */
    public boolean isAutoCompressionEnabled() {
        return autoCompressionEnabled;
    }
}
