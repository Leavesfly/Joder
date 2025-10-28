package io.shareai.joder.hooks;

import io.shareai.joder.core.permission.PermissionManager;
import io.shareai.joder.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * 工具权限检查钩子
 * 对应 Kode 的 useCanUseTool hook
 */
@Singleton
public class ToolPermissionHook {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolPermissionHook.class);
    
    private final PermissionManager permissionManager;
    
    @Inject
    public ToolPermissionHook(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }
    
    /**
     * 检查是否可以使用工具
     */
    public PermissionResult canUseTool(Tool tool, Map<String, Object> input) {
        try {
            // 使用权限管理器检查
            boolean allowed = permissionManager.checkPermission(tool);
            
            if (allowed) {
                return PermissionResult.allowed();
            }
            
            // 需要请求权限
            return PermissionResult.needsApproval(
                String.format("工具 '%s' 需要权限批准", tool.getName())
            );
            
        } catch (Exception e) {
            logger.error("权限检查失败", e);
            return PermissionResult.denied("权限检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 批准工具使用（永久）
     */
    public void approvePermanent(Tool tool) {
        permissionManager.addTrustedTool(tool.getName());
        logger.info("工具 '{}' 已永久批准", tool.getName());
    }
    
    /**
     * 批准工具使用（临时）
     */
    public void approveTemporary(Tool tool) {
        // TODO: 实现临时权限管理
        logger.info("工具 '{}' 已临时批准", tool.getName());
    }
    
    /**
     * 拒绝工具使用
     */
    public void reject(Tool tool) {
        logger.info("工具 '{}' 使用被拒绝", tool.getName());
    }
    
    /**
     * 检查是否有临时权限
     */
    private boolean hasTemporaryPermission(Tool tool) {
        // TODO: 实现临时权限检查
        return false;
    }
    
    /**
     * 权限检查结果
     */
    public static class PermissionResult {
        private final boolean allowed;
        private final boolean needsApproval;
        private final String message;
        
        private PermissionResult(boolean allowed, boolean needsApproval, String message) {
            this.allowed = allowed;
            this.needsApproval = needsApproval;
            this.message = message;
        }
        
        public static PermissionResult allowed() {
            return new PermissionResult(true, false, "");
        }
        
        public static PermissionResult needsApproval(String message) {
            return new PermissionResult(false, true, message);
        }
        
        public static PermissionResult denied(String message) {
            return new PermissionResult(false, false, message);
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public boolean needsApproval() {
            return needsApproval;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
