package io.leavesfly.joder.tools;

import io.leavesfly.joder.hooks.CancelRequestHook;
import io.leavesfly.joder.hooks.ToolPermissionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 工具执行器
 * 统一管理工具执行流程，集成权限检查与取消机制
 */
@Singleton
public class ToolExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutor.class);
    private static final long DEFAULT_TIMEOUT_MS = 300000; // 5 分钟超时
    
    private final ToolPermissionHook permissionHook;
    private final CancelRequestHook cancelHook;
    private final ExecutorService executorService;
    
    @Inject
    public ToolExecutor(
            ToolPermissionHook permissionHook,
            CancelRequestHook cancelHook) {
        this.permissionHook = permissionHook;
        this.cancelHook = cancelHook;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("ToolExecutor-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * 执行工具（同步）
     * 
     * @param tool 要执行的工具
     * @param input 输入参数
     * @return 执行结果
     */
    public ToolResult execute(Tool tool, Map<String, Object> input) {
        return execute(tool, input, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 执行工具（同步，带超时）
     * 
     * @param tool 要执行的工具
     * @param input 输入参数
     * @param timeoutMs 超时时间（毫秒）
     * @return 执行结果
     */
    public ToolResult execute(Tool tool, Map<String, Object> input, long timeoutMs) {
        logger.info("执行工具: {}", tool.getName());
        
        // 1. 权限检查
        ToolPermissionHook.PermissionResult permissionResult = 
            permissionHook.canUseTool(tool, input);
        
        if (!permissionResult.isAllowed()) {
            logger.warn("工具 {} 权限被拒绝: {}", tool.getName(), permissionResult.getMessage());
            return ToolResult.error("权限被拒绝: " + permissionResult.getMessage());
        }
        
        // 2. 重置取消状态
        cancelHook.reset();
        
        // 3. 异步执行工具
        Future<ToolResult> future = executorService.submit((Callable<ToolResult>) () -> {
            try {
                // 检查是否已取消
                cancelHook.throwIfCancelled();
                
                // 执行工具
                return tool.call(input);
                
            } catch (CancelRequestHook.CancellationException e) {
                logger.info("工具 {} 执行被取消", tool.getName());
                return ToolResult.error("执行已取消");
            } catch (Exception e) {
                logger.error("工具 {} 执行失败", tool.getName(), e);
                return ToolResult.error("执行失败: " + e.getMessage());
            }
        });
        
        // 4. 等待结果（支持超时与取消）
        try {
            long startTime = System.currentTimeMillis();
            
            while (!future.isDone()) {
                // 检查是否超时
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    logger.warn("工具 {} 执行超时 ({}ms)", tool.getName(), timeoutMs);
                    future.cancel(true);
                    return ToolResult.error("执行超时");
                }
                
                // 检查是否已取消
                if (cancelHook.isCancelled()) {
                    future.cancel(true);
                    logger.info("工具 {} 执行被取消", tool.getName());
                    return ToolResult.error("执行已取消");
                }
                
                // 短暂等待，避免忙等待
                try {
                    return future.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // 继续等待
                }
            }
            
            return future.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("工具 {} 执行被中断", tool.getName());
            future.cancel(true);
            return ToolResult.error("执行被中断");
            
        } catch (ExecutionException e) {
            logger.error("工具 {} 执行异常", tool.getName(), e);
            return ToolResult.error("执行异常: " + e.getCause().getMessage());
        }
    }
    
    /**
     * 执行工具（异步）
     * 
     * @param tool 要执行的工具
     * @param input 输入参数
     * @param callback 完成回调
     */
    public void executeAsync(
            Tool tool, 
            Map<String, Object> input,
            java.util.function.Consumer<ToolResult> callback) {
        
        executorService.submit(() -> {
            ToolResult result = execute(tool, input);
            if (callback != null) {
                callback.accept(result);
            }
        });
    }
    
    /**
     * 批量执行工具（顺序执行）
     * 
     * @param executions 工具执行列表
     * @return 执行结果列表
     */
    public java.util.List<ToolResult> executeBatch(java.util.List<ToolExecution> executions) {
        java.util.List<ToolResult> results = new java.util.ArrayList<>();
        
        for (ToolExecution execution : executions) {
            // 检查是否已取消
            if (cancelHook.isCancelled()) {
                logger.info("批量执行已取消，停止后续工具");
                break;
            }
            
            ToolResult result = execute(execution.tool, execution.input);
            results.add(result);
            
            // 如果执行失败且配置为停止，则中断批量执行
            if (!result.isSuccess() && execution.stopOnError) {
                logger.warn("工具 {} 执行失败，停止批量执行", execution.tool.getName());
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 工具执行配置
     */
    public static class ToolExecution {
        public final Tool tool;
        public final Map<String, Object> input;
        public final boolean stopOnError;
        
        public ToolExecution(Tool tool, Map<String, Object> input) {
            this(tool, input, true);
        }
        
        public ToolExecution(Tool tool, Map<String, Object> input, boolean stopOnError) {
            this.tool = tool;
            this.input = input;
            this.stopOnError = stopOnError;
        }
    }
}
