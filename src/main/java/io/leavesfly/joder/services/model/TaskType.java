package io.leavesfly.joder.services.model;

/**
 * 任务类型枚举
 * <p>
 * 用于智能模型路由,根据任务类型选择最合适的模型
 * </p>
 */
public enum TaskType {
    /**
     * 核心对话和复杂推理
     * 使用重量级模型 (如 Claude 3.5 Sonnet, GPT-4)
     */
    CORE_REASONING,
    
    /**
     * 代码生成和修改
     * 使用标准模型 (如 Claude 3.5 Sonnet, Qwen Coder)
     */
    CODE_GENERATION,
    
    /**
     * 简单的内容读取和解析
     * 使用轻量模型 (如 Claude 3.5 Haiku, Qwen Turbo)
     */
    CONTENT_PARSING,
    
    /**
     * 文件和项目结构分析
     * 使用轻量模型 (如 Claude 3.5 Haiku)
     */
    STRUCTURE_ANALYSIS,
    
    /**
     * 搜索查询优化
     * 使用轻量模型 (如 Claude 3.5 Haiku)
     */
    SEARCH_OPTIMIZATION,
    
    /**
     * 内容总结和压缩
     * 使用轻量模型 (如 Claude 3.5 Haiku)
     */
    SUMMARIZATION
}
