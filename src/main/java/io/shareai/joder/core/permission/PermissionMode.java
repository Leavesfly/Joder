package io.shareai.joder.core.permission;

/**
 * 权限模式枚举
 */
public enum PermissionMode {
    /**
     * 默认模式 - 编辑操作需要用户确认，只读操作自动通过
     */
    DEFAULT("default"),
    
    /**
     * 接受编辑模式 - 自动批准所有编辑操作
     */
    ACCEPT_EDITS("acceptEdits"),
    
    /**
     * 计划模式 - 只允许只读工具，禁止任何修改操作
     */
    PLAN("plan"),
    
    /**
     * 绕过权限模式 - 绕过所有权限检查（高风险）
     */
    BYPASS_PERMISSIONS("bypassPermissions");
    
    private final String value;
    
    PermissionMode(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static PermissionMode fromValue(String value) {
        for (PermissionMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
