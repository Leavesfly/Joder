package io.leavesfly.joder.services.agents;

import io.leavesfly.joder.domain.AgentConfig;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.services.model.ModelAdapterFactory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 执行器
 * <p>
 * 负责执行配置化的 AI 代理，包括：
 * 1. 根据 AgentConfig 创建独立的执行上下文
 * 2. 动态加载和切换模型
 * 3. 过滤和限制可用工具
 * 4. 管理独立的消息历史
 * 5. 支持工具调用和流式响应
 * </p>
 */
@Singleton
public class AgentExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentExecutor.class);
    
    private final AgentsManager agentsManager;
    private final ModelAdapterFactory modelAdapterFactory;
    private final ToolRegistry toolRegistry;
    
    @Inject
    public AgentExecutor(
            AgentsManager agentsManager,
            ModelAdapterFactory modelAdapterFactory,
            ToolRegistry toolRegistry) {
        this.agentsManager = agentsManager;
        this.modelAdapterFactory = modelAdapterFactory;
        this.toolRegistry = toolRegistry;
    }
    
    /**
     * 执行 Agent 并获取响应
     * 
     * @param agentName Agent 名称
     * @param userInput 用户输入
     * @return Agent 响应消息
     */
    public Message execute(String agentName, String userInput) {
        return execute(agentName, userInput, Collections.emptyList());
    }
    
    /**
     * 执行 Agent 并获取响应（带历史记录）
     * 
     * @param agentName Agent 名称
     * @param userInput 用户输入
     * @param messageHistory 消息历史
     * @return Agent 响应消息
     */
    public Message execute(String agentName, String userInput, List<Message> messageHistory) {
        logger.info("Executing agent: {}", agentName);
        
        // 获取 Agent 配置
        AgentConfig agentConfig = agentsManager.getAgent(agentName)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentName));
        
        return executeWithConfig(agentConfig, userInput, messageHistory);
    }
    
    /**
     * 使用指定的 AgentConfig 执行
     * 
     * @param agentConfig Agent 配置
     * @param userInput 用户输入
     * @param messageHistory 消息历史
     * @return Agent 响应消息
     */
    public Message executeWithConfig(AgentConfig agentConfig, String userInput, List<Message> messageHistory) {
        logger.debug("Executing agent with config: {}", agentConfig.getName());
        
        // 验证配置
        if (!agentConfig.isValid()) {
            throw new IllegalArgumentException("Invalid agent config: " + agentConfig.getName());
        }
        
        try {
            // 1. 获取或创建模型适配器
            ModelAdapter modelAdapter = getModelAdapter(agentConfig);
            
            // 2. 准备系统提示词
            String systemPrompt = prepareSystemPrompt(agentConfig);
            
            // 3. 准备消息历史
            List<Message> fullHistory = prepareMessageHistory(messageHistory, userInput);
            
            // 4. 过滤可用工具（如果配置了工具限制）
            List<Tool> availableTools = filterTools(agentConfig);
            
            // 5. 调用模型获取响应
            String response = modelAdapter.sendMessage(fullHistory, systemPrompt);
            
            // 6. 创建响应消息
            Message assistantMessage = new Message(MessageRole.ASSISTANT, response);
            
            logger.info("Agent {} executed successfully", agentConfig.getName());
            return assistantMessage;
            
        } catch (Exception e) {
            logger.error("Failed to execute agent: {}", agentConfig.getName(), e);
            String errorContent = String.format("Agent 执行失败: %s", e.getMessage());
            return new Message(MessageRole.ASSISTANT, errorContent);
        }
    }
    
    /**
     * 获取模型适配器
     * 如果 Agent 配置了特定模型，使用该模型；否则使用默认模型
     */
    private ModelAdapter getModelAdapter(AgentConfig agentConfig) {
        String modelName = agentConfig.getModel();
        
        if (modelName != null && !modelName.trim().isEmpty()) {
            logger.debug("Creating model adapter for: {}", modelName);
            return modelAdapterFactory.createAdapter(modelName);
        }
        
        // 使用默认模型
        logger.debug("Using default model adapter");
        return modelAdapterFactory.createDefaultAdapter();
    }
    
    /**
     * 准备系统提示词
     */
    private String prepareSystemPrompt(AgentConfig agentConfig) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 添加 Agent 的系统提示词
        if (agentConfig.getSystemPrompt() != null && !agentConfig.getSystemPrompt().isEmpty()) {
            promptBuilder.append(agentConfig.getSystemPrompt());
        }
        
        // 如果配置了工具限制，添加工具使用说明
        if (agentConfig.getTools() != null && !agentConfig.getTools().isEmpty()) {
            promptBuilder.append("\n\n");
            promptBuilder.append("## 可用工具\n");
            promptBuilder.append("你可以使用以下工具：\n");
            
            List<Tool> availableTools = filterTools(agentConfig);
            for (Tool tool : availableTools) {
                promptBuilder.append("- ").append(tool.getName())
                        .append(": ").append(tool.getDescription())
                        .append("\n");
            }
            
            // 如果不是通配符，添加限制说明
            if (!agentConfig.getTools().contains("*")) {
                promptBuilder.append("\n⚠️ 重要：你只能使用上述列出的工具，不能使用其他工具。\n");
            }
        }
        
        return promptBuilder.toString();
    }
    
    /**
     * 准备消息历史
     */
    private List<Message> prepareMessageHistory(List<Message> existingHistory, String userInput) {
        List<Message> fullHistory = new ArrayList<>();
        
        // 添加现有历史
        if (existingHistory != null && !existingHistory.isEmpty()) {
            fullHistory.addAll(existingHistory);
        }
        
        // 添加当前用户输入
        fullHistory.add(new Message(MessageRole.USER, userInput));
        
        return fullHistory;
    }
    
    /**
     * 过滤可用工具
     * 根据 Agent 配置的工具列表过滤
     */
    private List<Tool> filterTools(AgentConfig agentConfig) {
        List<String> allowedTools = agentConfig.getTools();
        
        // 如果未配置工具或配置了通配符，返回所有工具
        if (allowedTools == null || allowedTools.isEmpty() || allowedTools.contains("*")) {
            return new ArrayList<>(toolRegistry.getAllTools());
        }
        
        // 过滤出允许的工具
        return toolRegistry.getAllTools().stream()
                .filter(tool -> allowedTools.contains(tool.getName()))
                .collect(Collectors.toList());
    }
    
    /**
     * 验证 Agent 是否存在
     * 
     * @param agentName Agent 名称
     * @return 是否存在
     */
    public boolean hasAgent(String agentName) {
        return agentsManager.hasAgent(agentName);
    }
    
    /**
     * 获取 Agent 配置
     * 
     * @param agentName Agent 名称
     * @return Agent 配置（如果存在）
     */
    public AgentConfig getAgentConfig(String agentName) {
        return agentsManager.getAgent(agentName).orElse(null);
    }
}
