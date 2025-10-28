package io.leavesfly.joder.util;

import javax.inject.Singleton;

/**
 * 状态管理器
 * 对应 Kode 的 state.ts
 * 
 * 注意：尽量减少全局状态，遵循最佳实践
 */
@Singleton
public class StateManager {
    
    private String originalCwd;
    private String currentCwd;
    
    public StateManager() {
        this.originalCwd = System.getProperty("user.dir");
        this.currentCwd = this.originalCwd;
    }
    
    /**
     * 设置原始工作目录
     */
    public void setOriginalCwd(String cwd) {
        this.originalCwd = cwd;
    }
    
    /**
     * 获取原始工作目录
     */
    public String getOriginalCwd() {
        return originalCwd;
    }
    
    /**
     * 设置当前工作目录
     */
    public void setCwd(String cwd) {
        this.currentCwd = cwd;
        System.setProperty("user.dir", cwd);
    }
    
    /**
     * 获取当前工作目录
     */
    public String getCwd() {
        return currentCwd;
    }
}
