package io.leavesfly.joder.core;

/**
 * 交互模式枚举
 * <p>
 * 定义三种对话模式,平衡自主性与用户控制
 * </p>
 */
public enum InteractionMode {
    
    /**
     * 默认模式 (Default)
     * <p>
     * 行为: AI 提出变更建议 → 等待用户确认 → 执行操作
     * 适用场景: 日常开发、学习探索、不确定的重构
     * 权限控制: 所有写操作需要确认
     * </p>
     */
    DEFAULT("default", "默认模式", "AI提出建议并等待确认后执行"),
    
    /**
     * 自动模式 (Auto)
     * <p>
     * 行为: AI 自主执行所有操作,仅危险命令需确认
     * 适用场景: 信任的开发任务、批量处理、按计划执行
     * 权限控制: 使用权限白名单,可配置自动批准的工具和命令
     * </p>
     */
    AUTO("auto", "自动模式", "AI自主执行操作,危险操作除外"),
    
    /**
     * 规划模式 (Plan)
     * <p>
     * 行为: AI 使用扩展思考能力创建详细计划,不执行任何写操作
     * 适用场景: 功能设计、架构评审、复杂问题分析
     * 权限控制: 只允许只读工具(read_file、list_dir、grep等)
     * </p>
     */
    PLAN("plan", "规划模式", "AI只分析和规划,不执行写操作");
    
    private final String key;
    private final String displayName;
    private final String description;
    
    InteractionMode(String key, String displayName, String description) {
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
     * 从字符串键获取模式
     */
    public static InteractionMode fromKey(String key) {
        for (InteractionMode mode : values()) {
            if (mode.key.equalsIgnoreCase(key)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown interaction mode: " + key);
    }
    
    /**
     * 获取下一个模式(用于循环切换)
     */
    public InteractionMode next() {
        InteractionMode[] modes = values();
        int nextIndex = (this.ordinal() + 1) % modes.length;
        return modes[nextIndex];
    }
    
    /**
     * 检查是否允许写操作
     */
    public boolean allowsWriteOperations() {
        return this != PLAN;
    }
    
    /**
     * 检查是否需要用户确认
     */
    public boolean requiresConfirmation() {
        return this == DEFAULT;
    }
    
    /**
     * 检查是否为自动模式
     */
    public boolean isAuto() {
        return this == AUTO;
    }
    
    @Override
    public String toString() {
        return displayName + " (" + description + ")";
    }
}
