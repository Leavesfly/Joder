package io.leavesfly.joder.core.permission;

import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限管理器
 */
@Singleton
public class PermissionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionManager.class);
    
    private final ConfigManager configManager;
    private PermissionMode currentMode;
    private final Set<String> trustedTools;
    
    @Inject
    public PermissionManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.currentMode = loadPermissionMode();
        this.trustedTools = new HashSet<>(
            configManager.getStringList("joder.permissions.trustedTools", java.util.List.of())
        );
        
        logger.info("Permission manager initialized with mode: {}", currentMode);
    }
    
    /**
     * 加载权限模式
     */
    private PermissionMode loadPermissionMode() {
        String modeValue = configManager.getString("joder.permissions.mode", "default");
        return PermissionMode.fromValue(modeValue);
    }
    
    /**
     * 检查工具是否可以执行
     * 
     * @param tool 要执行的工具
     * @return 是否允许执行
     */
    public boolean checkPermission(Tool tool) {
        // 如果工具不需要权限确认，直接通过
        if (!tool.needsPermissions()) {
            return true;
        }
        
        // 绕过权限模式，直接通过
        if (currentMode == PermissionMode.BYPASS_PERMISSIONS) {
            logger.debug("Permission bypassed for tool: {}", tool.getName());
            return true;
        }
        
        // 计划模式下，只允许只读工具
        if (currentMode == PermissionMode.PLAN) {
            boolean allowed = tool.isReadOnly();
            if (!allowed) {
                logger.warn("Tool {} blocked in PLAN mode", tool.getName());
            }
            return allowed;
        }
        
        // 接受编辑模式，自动批准所有工具
        if (currentMode == PermissionMode.ACCEPT_EDITS) {
            logger.debug("Auto-approved tool in ACCEPT_EDITS mode: {}", tool.getName());
            return true;
        }
        
        // 默认模式 - 检查是否为受信任的工具
        if (trustedTools.contains(tool.getName())) {
            logger.debug("Trusted tool auto-approved: {}", tool.getName());
            return true;
        }
        
        // 需要用户确认
        return requestUserConfirmation(tool);
    }
    
    /**
     * 请求用户确认
     */
    private boolean requestUserConfirmation(Tool tool) {
        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│ 🔐 权限确认请求                         │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ 工具: " + tool.getName());
        System.out.println("│ 描述: " + tool.getDescription());
        System.out.println("│ 只读: " + (tool.isReadOnly() ? "是" : "否"));
        System.out.println("├─────────────────────────────────────────┤");
        System.out.print("│ 是否允许执行？[Y/n] ");
        System.out.flush();
        
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(System.in)
            );
            String response = reader.readLine();
            
            boolean approved = response == null || response.trim().isEmpty() || 
                             response.trim().equalsIgnoreCase("y") ||
                             response.trim().equalsIgnoreCase("yes");
            
            if (approved) {
                System.out.println("│ ✓ 已批准");
            } else {
                System.out.println("│ ✗ 已拒绝");
            }
            System.out.println("└─────────────────────────────────────────┘\n");
            
            return approved;
            
        } catch (Exception e) {
            logger.error("Failed to read user confirmation", e);
            return false;
        }
    }
    
    /**
     * 获取当前权限模式
     */
    public PermissionMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * 设置权限模式
     */
    public void setMode(PermissionMode mode) {
        this.currentMode = mode;
        logger.info("Permission mode changed to: {}", mode);
    }
    
    /**
     * 添加受信任的工具
     */
    public void addTrustedTool(String toolName) {
        trustedTools.add(toolName);
        logger.info("Added trusted tool: {}", toolName);
    }
    
    /**
     * 移除受信任的工具
     */
    public void removeTrustedTool(String toolName) {
        trustedTools.remove(toolName);
        logger.info("Removed trusted tool: {}", toolName);
    }
}
