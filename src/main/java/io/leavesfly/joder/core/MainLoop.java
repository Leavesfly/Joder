package io.leavesfly.joder.core;

import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.tools.ToolRegistry;
import io.leavesfly.joder.ui.components.MessageRenderer;
import io.leavesfly.joder.services.context.ContextCompressor;
import io.leavesfly.joder.services.context.CompressionResult;
import io.leavesfly.joder.services.memory.ProjectMemoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 主控制循环
 * <p>
 * 核心设计理念:
 * 1. 维护单一的消息历史列表,避免复杂的消息分支
 * 2. 统一管理所有状态变更
 * 3. 支持子 Agent (最多1层分支),结果合并回主历史
 * 4. 提供清晰的控制流程和错误处理
 * </p>
 */
@Singleton
public class MainLoop {
    
    private static final Logger logger = LoggerFactory.getLogger(MainLoop.class);
    
    private final ToolRegistry toolRegistry;
    private final MessageRenderer messageRenderer;
    private final ProjectMemoryManager projectMemoryManager;
    private final ContextCompressor contextCompressor;
    
    /**
     * 单一消息历史 - 核心数据结构
     * 所有对话、工具调用、子Agent结果都存储在这里
     */
    private final List<Message> messageHistory;
    
    /**
     * 当前活跃的模型适配器
     */
    private ModelAdapter currentModel;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 当前交互模式
     */
    private InteractionMode interactionMode;
    
    @Inject
    public MainLoop(
            ToolRegistry toolRegistry,
            MessageRenderer messageRenderer,
            ProjectMemoryManager projectMemoryManager,
            ContextCompressor contextCompressor) {
        this.toolRegistry = toolRegistry;
        this.messageRenderer = messageRenderer;
        this.projectMemoryManager = projectMemoryManager;
        this.contextCompressor = contextCompressor;
        this.messageHistory = new ArrayList<>();
        this.systemPrompt = "";
        this.interactionMode = InteractionMode.DEFAULT; // 默认模式
    }
    
    /**
     * 设置当前使用的模型
     */
    public void setCurrentModel(ModelAdapter model) {
        this.currentModel = model;
        logger.info("Switched to model: {} ({})", 
                    model.getModelName(), 
                    model.getProviderName());
    }
    
    /**
     * 设置系统提示词
     */
    public void setSystemPrompt(String prompt) {
        this.systemPrompt = prompt;
    }
    
    /**
     * 追加系统提示词内容
     */
    public void appendSystemPrompt(String additionalPrompt) {
        this.systemPrompt += "\n" + additionalPrompt;
    }
    
    /**
     * 加载项目记忆并添加到系统提示词
     * <p>
     * 这是核心功能:将 claude.md 内容注入到系统提示词中,
     * 让 AI 能够理解项目全貌、规范和偏好
     * </p>
     */
    public void loadProjectMemory() {
        if (!projectMemoryManager.exists()) {
            logger.debug("Project memory file does not exist, skipping...");
            return;
        }
        
        String memoryContent = projectMemoryManager.load();
        if (!memoryContent.isEmpty()) {
            // 将项目记忆添加到系统提示词前面
            String memorySection = "<project_memory>\n" + memoryContent + "\n</project_memory>\n\n";
            this.systemPrompt = memorySection + this.systemPrompt;
            logger.info("Project memory loaded and injected into system prompt");
        }
    }
    
    /**
     * 处理用户输入并返回 AI 响应
     * 
     * @param userInput 用户输入的内容
     * @return AI 响应消息
     */
    public Message processUserInput(String userInput) {
        if (currentModel == null) {
            throw new IllegalStateException("No model configured. Please set a model first.");
        }
        
        logger.debug("Processing user input: {}", userInput);
        
        // 创建用户消息并添加到历史
        Message userMessage = new Message(MessageRole.USER, userInput);
        addMessageToHistory(userMessage);
        
        // 检查是否需要压缩上下文
        checkAndCompressContext();
        
        try {
            // 调用模型获取响应
            String aiResponse = currentModel.sendMessage(
                Collections.unmodifiableList(messageHistory), 
                systemPrompt
            );
            
            // 创建助手消息并添加到历史
            Message assistantMessage = new Message(MessageRole.ASSISTANT, aiResponse);
            addMessageToHistory(assistantMessage);
            
            logger.debug("AI response generated successfully");
            return assistantMessage;
            
        } catch (Exception e) {
            logger.error("Failed to get AI response", e);
            
            // 创建错误消息
            String errorContent = "抱歉,处理您的请求时发生错误: " + e.getMessage();
            Message errorMessage = new Message(MessageRole.ASSISTANT, errorContent);
            addMessageToHistory(errorMessage);
            
            return errorMessage;
        }
    }
    
    /**
     * 检查并在需要时压缩上下文
     */
    private void checkAndCompressContext() {
        // 获取模型的最大token限制(默认8096)
        int maxTokens = 8096; // TODO: 从模型配置中获取
        
        if (contextCompressor.needsCompression(messageHistory, maxTokens)) {
            logger.info("触发上下文压缩...");
            
            try {
                CompressionResult result = contextCompressor.compress(messageHistory, maxTokens);
                
                // 替换消息历史
                messageHistory.clear();
                messageHistory.addAll(result.getCompressedMessages());
                
                logger.info("上下文压缩完成: {}", result);
                
            } catch (Exception e) {
                logger.error("上下文压缩失败", e);
                // 失败时不影响正常流程,继续使用原始历史
            }
        }
    }
    
    /**
     * 添加消息到历史记录
     * 
     * @param message 要添加的消息
     */
    public void addMessageToHistory(Message message) {
        messageHistory.add(message);
        logger.debug("Added message to history: role={}, id={}", 
                    message.getRole(), 
                    message.getId());
    }
    
    /**
     * 获取消息历史的只读副本
     * 
     * @return 消息历史列表
     */
    public List<Message> getMessageHistory() {
        return Collections.unmodifiableList(messageHistory);
    }
    
    /**
     * 获取消息历史的可变副本(用于高级操作)
     * 
     * @return 消息历史列表的副本
     */
    public List<Message> getMessageHistoryCopy() {
        return new ArrayList<>(messageHistory);
    }
    
    /**
     * 清空消息历史
     */
    public void clearHistory() {
        messageHistory.clear();
        logger.info("Message history cleared");
    }
    
    /**
     * 获取历史消息数量
     */
    public int getHistorySize() {
        return messageHistory.size();
    }
    
    /**
     * 移除最后 N 条消息
     * 
     * @param count 要移除的消息数量
     */
    public void removeLastMessages(int count) {
        if (count <= 0) {
            return;
        }
        
        int toRemove = Math.min(count, messageHistory.size());
        for (int i = 0; i < toRemove; i++) {
            messageHistory.remove(messageHistory.size() - 1);
        }
        
        logger.info("Removed last {} messages from history", toRemove);
    }
    
    /**
     * 撤销最后一轮对话(移除最后的用户消息和AI响应)
     */
    public boolean undoLastInteraction() {
        if (messageHistory.size() < 2) {
            logger.warn("Not enough messages to undo");
            return false;
        }
        
        // 检查最后两条消息是否是一轮完整对话
        int lastIndex = messageHistory.size() - 1;
        Message lastMessage = messageHistory.get(lastIndex);
        Message secondLastMessage = messageHistory.get(lastIndex - 1);
        
        if (lastMessage.getRole() == MessageRole.ASSISTANT && 
            secondLastMessage.getRole() == MessageRole.USER) {
            removeLastMessages(2);
            logger.info("Undone last interaction");
            return true;
        }
        
        logger.warn("Last messages are not a complete user-assistant pair");
        return false;
    }
    
    /**
     * 获取当前模型
     */
    public ModelAdapter getCurrentModel() {
        return currentModel;
    }
    
    /**
     * 获取系统提示词
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    /**
     * 设置交互模式
     * 
     * @param mode 交互模式
     */
    public void setInteractionMode(InteractionMode mode) {
        this.interactionMode = mode;
        logger.info("Interaction mode changed to: {}", mode.getDisplayName());
    }
    
    /**
     * 获取当前交互模式
     */
    public InteractionMode getInteractionMode() {
        return interactionMode;
    }
    
    /**
     * 切换到下一个交互模式(循环)
     */
    public InteractionMode switchToNextMode() {
        InteractionMode nextMode = interactionMode.next();
        setInteractionMode(nextMode);
        return nextMode;
    }
}
