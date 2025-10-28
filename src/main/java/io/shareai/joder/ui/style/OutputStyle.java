package io.shareai.joder.ui.style;

/**
 * 输出样式枚举
 * 定义四种不同的输出详细程度
 */
public enum OutputStyle {
    /**
     * 简洁模式 - 只显示核心结果,适合快速查看
     */
    CONCISE("concise", "简洁模式", "只显示核心结果和关键信息"),
    
    /**
     * 平衡模式 - 默认模式,显示重要信息和部分过程
     */
    BALANCED("balanced", "平衡模式", "显示重要信息和部分执行过程"),
    
    /**
     * 详细模式 - 显示完整信息,包括所有步骤和细节
     */
    DETAILED("detailed", "详细模式", "显示完整信息,包括所有步骤和细节"),
    
    /**
     * 思考模式 - 显示AI的思考过程和推理链
     */
    THINKING("thinking", "思考模式", "显示AI的思考过程、推理链和决策依据");
    
    private final String key;
    private final String displayName;
    private final String description;
    
    OutputStyle(String key, String displayName, String description) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否显示工具调用详情
     */
    public boolean showToolDetails() {
        return this == DETAILED || this == THINKING;
    }
    
    /**
     * 是否显示思考过程
     */
    public boolean showThinking() {
        return this == THINKING;
    }
    
    /**
     * 是否显示进度信息
     */
    public boolean showProgress() {
        return this != CONCISE;
    }
    
    /**
     * 是否显示统计信息
     */
    public boolean showStats() {
        return this == DETAILED || this == THINKING;
    }
    
    /**
     * 从key解析OutputStyle
     */
    public static OutputStyle fromKey(String key) {
        for (OutputStyle style : values()) {
            if (style.key.equalsIgnoreCase(key)) {
                return style;
            }
        }
        return BALANCED; // 默认
    }
    
    /**
     * 获取所有可用样式的描述
     */
    public static String getAllStylesDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用的输出样式:\n");
        for (OutputStyle style : values()) {
            sb.append(String.format("  - %s (%s): %s\n", 
                style.key, style.displayName, style.description));
        }
        return sb.toString();
    }
}
