package io.shareai.joder.domain;

/**
 * Mention 类型枚举
 * 
 * <p>定义支持的 mention 引用类型。
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public enum MentionType {
    
    /**
     * 文件引用：@file:path/to/file.txt
     */
    FILE("@file", "File reference"),
    
    /**
     * Agent 引用：@agent:agent-name
     */
    AGENT("@agent", "Agent reference"),
    
    /**
     * 模型引用：@model:model-name
     */
    MODEL("@model", "Model reference"),
    
    /**
     * 运行 Agent：@run-agent:agent-name
     */
    RUN_AGENT("@run-agent", "Run agent"),
    
    /**
     * 询问模型：@ask-model:model-name
     */
    ASK_MODEL("@ask-model", "Ask model");
    
    private final String prefix;
    private final String description;
    
    MentionType(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 从字符串解析 MentionType
     * 
     * @param str 字符串（如 "@file" 或 "@agent"）
     * @return 对应的 MentionType，如果不匹配返回 null
     */
    public static MentionType fromString(String str) {
        if (str == null) {
            return null;
        }
        
        // 按长度降序匹配（确保 @run-agent 在 @agent 之前）
        for (MentionType type : values()) {
            if (str.startsWith(type.prefix)) {
                return type;
            }
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        return prefix;
    }
}
