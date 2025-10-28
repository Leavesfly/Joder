package io.leavesfly.joder.domain;

/**
 * 提醒分类枚举
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public enum ReminderCategory {
    
    /** 常规提醒 */
    GENERAL("general"),
    
    /** 任务提醒 */
    TASK("task"),
    
    /** 安全提醒 */
    SECURITY("security"),
    
    /** 性能提醒 */
    PERFORMANCE("performance"),
    
    /** 文件提醒 */
    FILE("file");
    
    private final String value;
    
    ReminderCategory(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
