package io.leavesfly.joder.tools.task;

import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task Tool - 任务分解与执行工具
 * 
 * 注意：这是一个简化版本
 * 完整实现需要集成子代理、模型调度等复杂功能
 */
@Singleton
public class TaskTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskTool.class);
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    
    @Override
    public String getName() {
        return "Task";
    }
    
    @Override
    public String getDescription() {
        return "启动一个新的任务。\n" +
               "- 将复杂任务分解为子任务\n" +
               "- 支持指定子代理类型\n" +
               "- 支持自定义模型选择\n" +
               "- 独立的执行上下文";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具启动新任务。适用于：\n" +
               "- 需要并行执行的独立任务\n" +
               "- 需要专门模型或代理的特定任务\n" +
               "- 需要隔离上下文的任务\n\n" +
               "参数：\n" +
               "- description: 任务的简短描述（3-5 个字）\n" +
               "- prompt: 任务的详细提示\n" +
               "- model_name: (可选) 要使用的具体模型名称\n" +
               "- subagent_type: (可选) 要使用的专门代理类型\n\n" +
               "可用的代理类型：\n" +
               "- general-purpose: 通用代理（默认）\n" +
               "- code-expert: 代码专家\n" +
               "- research-agent: 研究代理\n" +
               "- data-analyst: 数据分析师\n\n" +
               "注意：当前版本为简化实现，完整功能需要集成子代理系统。";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return false; // 任务执行可能修改状态
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true; // 支持并发任务
    }
    
    @Override
    public boolean needsPermissions() {
        return false; // 由具体执行的工具决定
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String description = (String) input.get("description");
        String prompt = (String) input.get("prompt");
        String modelName = (String) input.get("model_name");
        String subagentType = (String) input.get("subagent_type");
        
        // 验证输入
        if (description == null || description.trim().isEmpty()) {
            return ToolResult.error("任务描述不能为空");
        }
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return ToolResult.error("任务提示不能为空");
        }
        
        // 生成任务 ID
        int taskId = taskIdCounter.incrementAndGet();
        
        // 确定代理类型
        String agentType = (subagentType != null && !subagentType.trim().isEmpty()) 
                ? subagentType : "general-purpose";
        
        // 确定模型
        String model = (modelName != null && !modelName.trim().isEmpty()) 
                ? modelName : "task";
        
        logger.info("启动任务 #{}: {} (代理: {}, 模型: {})", taskId, description, agentType, model);
        logger.debug("任务提示: {}", prompt);
        
        // 简化实现：返回任务信息
        String result = String.format(
            "🚀 任务已创建\n\n" +
            "任务 ID: #%d\n" +
            "描述: %s\n" +
            "代理类型: %s\n" +
            "模型: %s\n\n" +
            "--- 任务提示 ---\n%s\n\n" +
            "注意：当前版本为简化实现。完整的任务执行功能包括：\n" +
            "1. 子代理系统 - 根据 subagent_type 加载专门的代理配置\n" +
            "2. 模型调度 - 动态选择和切换模型\n" +
            "3. 工具过滤 - 根据代理配置限制可用工具\n" +
            "4. 独立日志 - 每个任务有独立的执行日志\n" +
            "5. 并行执行 - 支持多个任务同时运行\n\n" +
            "要实现完整功能，需要：\n" +
            "- 实现 SubagentManager 子代理管理器\n" +
            "- 扩展 ModelAdapterFactory 支持动态模型切换\n" +
            "- 实现 TaskExecutor 任务执行器\n" +
            "- 添加任务状态跟踪和结果汇总",
            taskId,
            description,
            agentType,
            model,
            prompt
        );
        
        return ToolResult.success(result);
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String description = (String) input.get("description");
        String subagentType = (String) input.get("subagent_type");
        
        if (subagentType != null && !subagentType.trim().isEmpty()) {
            return String.format("启动任务: %s (使用 %s 代理)", description, subagentType);
        }
        
        return String.format("启动任务: %s", description);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 任务已创建（简化实现）";
        } else {
            return "❌ 任务创建失败: " + result.getError();
        }
    }
}
