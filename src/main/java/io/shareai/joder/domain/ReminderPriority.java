package io.shareai.joder.domain;

/**
 * 提醒优先级枚举
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public enum ReminderPriority {
    
    /** 低优先级 */
    LOW("low", 1),
    
    /** 中优先级 */
    MEDIUM("medium", 2),
    
    /** 高优先级 */
    HIGH("high", 3);
    
    private final String value;
    private final int level;
    
    ReminderPriority(String value, int level) {
        this.value = value;
        this.level = level;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getLevel() {
        return level;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
