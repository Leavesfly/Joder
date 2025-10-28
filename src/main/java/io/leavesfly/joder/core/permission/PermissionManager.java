package io.leavesfly.joder.core.permission;

import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.ui.permission.PermissionDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 权限管理器
 */
@Singleton
public class PermissionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionManager.class);
    
    private final ConfigManager configManager;
    private final PermissionDialog permissionDialog;
    private PermissionMode currentMode;
    private final Set<String> trustedTools;
    
    @Inject
    public PermissionManager(
            ConfigManager configManager,
            PermissionDialog permissionDialog) {
        this.configManager = configManager;
        this.permissionDialog = permissionDialog;
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
     * @param input 工具参数
     * @return 是否允许执行
     */
    public boolean checkPermission(Tool tool, Map<String, Object> input) {
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
        return requestUserConfirmation(tool, input);
    }
    
    /**
     * 检查工具是否可以执行（兼容旧版）
     */
    public boolean checkPermission(Tool tool) {
        return checkPermission(tool, null);
    }
    
    /**
     * 请求用户确认
     */
    private boolean requestUserConfirmation(Tool tool, Map<String, Object> input) {
        PermissionDialog.PermissionDecision decision = 
            permissionDialog.requestPermission(tool, input);
        
        switch (decision) {
            case ALLOW_ONCE:
                System.out.println("✅ 已批准（仅本次）");
                return true;
                
            case ALLOW_PERMANENT:
                addTrustedTool(tool.getName());
                System.out.println("✅ 已永久批准");
                return true;
                
            case DENY:
            default:
                System.out.println("❌ 已拒绝");
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
