package io.leavesfly.joder.tools;

import io.leavesfly.joder.hooks.CancelRequestHook;
import io.leavesfly.joder.hooks.ToolPermissionHook;
import io.leavesfly.joder.core.permission.PermissionManager;
import io.leavesfly.joder.core.permission.PermissionMode;
import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.ui.permission.PermissionDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ToolExecutor 单元测试
 */
@DisplayName("ToolExecutor 工具执行器测试")
class ToolExecutorTest {
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private PermissionDialog permissionDialog;
    
    private PermissionManager permissionManager;
    private ToolPermissionHook permissionHook;
    private CancelRequestHook cancelHook;
    private ToolExecutor executor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置 ConfigManager 返回 BYPASS_PERMISSIONS 模式
        when(configManager.getString(eq("joder.permissions.mode"), eq("default")))
            .thenReturn("bypass_permissions");
        java.util.List<String> emptyList = java.util.Collections.emptyList();
        when(configManager.getStringList(eq("joder.permissions.trustedTools"), eq(emptyList)))
            .thenReturn(emptyList);
        
        permissionManager = new PermissionManager(configManager, permissionDialog);
        permissionHook = new ToolPermissionHook(permissionManager);
        cancelHook = new CancelRequestHook();
        executor = new ToolExecutor(permissionHook, cancelHook);
    }
    
    @Test
    @DisplayName("应该成功执行获得权限的工具")
    void testExecuteWithPermission() {
        // 模拟工具
        Tool tool = createMockTool("test_tool", false);  // needsPermission=false 才不需要权限检查
        
        Map<String, Object> input = Map.of("arg", "value");
        
        // 执行
        ToolResult result = executor.execute(tool, input);
        
        // 验证
        assertTrue(result.isSuccess());
        assertEquals("success", result.getOutput());
        verify(tool).call(input);
    }
    
    @Test
    @DisplayName("应该拒绝执行无权限的工具")
    void testExecuteWithoutPermission() {
        // 模拟需要权限的工具
        Tool tool = createMockTool("test_tool", true);  // needsPermission=true
        when(permissionManager.checkPermission(eq(tool), any()))
            .thenReturn(false);  // 权限被拒绝
        
        Map<String, Object> input = Map.of("arg", "value");
        
        // 执行
        ToolResult result = executor.execute(tool, input);
        
        // 验证
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("权限被拒绝"));
        verify(tool, never()).call(any());
    }
    
    @Test
    @DisplayName("应该支持取消正在执行的工具")
    void testCancelExecution() throws Exception {
        // 模拟长时运行的工具
        Tool tool = createSlowMockTool("slow_tool", 2000);
        when(permissionManager.checkPermission(eq(tool), any()))
            .thenReturn(true);
        
        Map<String, Object> input = Map.of();
        
        // 在另一个线程执行工具
        Thread executionThread = new Thread(() -> {
            ToolResult result = executor.execute(tool, input, 5000);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("取消"));
        });
        
        executionThread.start();
        
        // 等待 500ms 后取消
        Thread.sleep(500);
        cancelHook.cancel();
        
        executionThread.join(3000);
        assertFalse(executionThread.isAlive());
    }
    
    @Test
    @DisplayName("应该在工具执行失败时返回错误")
    void testExecuteWithException() {
        // 模拟会抛异常的工具
        Tool tool = mock(Tool.class);
        when(tool.getName()).thenReturn("error_tool");
        when(tool.needsPermissions()).thenReturn(false);
        when(tool.call(any())).thenThrow(new RuntimeException("Tool error"));
        
        Map<String, Object> input = Map.of();
        
        // 执行
        ToolResult result = executor.execute(tool, input);
        
        // 验证
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Tool error"));
    }
    
    @Test
    @DisplayName("应该支持批量执行工具")
    void testExecuteBatch() {
        Tool tool1 = createMockTool("tool1", false);  // 不需要权限
        Tool tool2 = createMockTool("tool2", false);
        Tool tool3 = createMockTool("tool3", false);
        
        java.util.List<ToolExecutor.ToolExecution> executions = java.util.List.of(
            new ToolExecutor.ToolExecution(tool1, Map.of()),
            new ToolExecutor.ToolExecution(tool2, Map.of()),
            new ToolExecutor.ToolExecution(tool3, Map.of())
        );
        
        // 执行批量
        java.util.List<ToolResult> results = executor.executeBatch(executions);
        
        // 验证
        assertEquals(3, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
        assertTrue(results.get(2).isSuccess());
    }
    
    @Test
    @DisplayName("批量执行时应在错误时停止（stopOnError=true）")
    void testExecuteBatchStopOnError() {
        Tool tool1 = createMockTool("tool1", false);
        Tool tool2 = createMockTool("tool2", true);  // 需要权限
        Tool tool3 = createMockTool("tool3", false);
        
        // tool2 权限被拒绝
        when(permissionManager.checkPermission(eq(tool2), any()))
            .thenReturn(false);
        
        java.util.List<ToolExecutor.ToolExecution> executions = java.util.List.of(
            new ToolExecutor.ToolExecution(tool1, Map.of(), false),
            new ToolExecutor.ToolExecution(tool2, Map.of(), true),  // stopOnError=true
            new ToolExecutor.ToolExecution(tool3, Map.of(), false)
        );
        
        // 执行批量
        java.util.List<ToolResult> results = executor.executeBatch(executions);
        
        // 验证：应该只执行到 tool2 就停止
        assertEquals(2, results.size());
        verify(tool3, never()).call(any());
    }
    
    /**
     * 创建模拟工具
     */
    private Tool createMockTool(String name, boolean needsPermission) {
        Tool tool = mock(Tool.class);
        when(tool.getName()).thenReturn(name);
        when(tool.needsPermissions()).thenReturn(needsPermission);
        when(tool.call(any())).thenReturn(ToolResult.success("success"));
        return tool;
    }
    
    /**
     * 创建慢速模拟工具（用于测试取消）
     */
    private Tool createSlowMockTool(String name, long delayMs) {
        Tool tool = mock(Tool.class);
        when(tool.getName()).thenReturn(name);
        when(tool.needsPermissions()).thenReturn(false);
        when(tool.call(any())).thenAnswer(invocation -> {
            Thread.sleep(delayMs);
            return ToolResult.success("success");
        });
        return tool;
    }
}
