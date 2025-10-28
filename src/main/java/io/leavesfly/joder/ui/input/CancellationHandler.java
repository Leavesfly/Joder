package io.leavesfly.joder.ui.input;

import io.leavesfly.joder.hooks.CancelRequestHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 取消处理器
 * 统一处理 Ctrl+C (SIGINT) 和 ESC 键的取消逻辑
 */
@Singleton
public class CancellationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CancellationHandler.class);
    
    private final CancelRequestHook cancelHook;
    private volatile boolean shutdownRequested = false;
    private volatile long lastCancelTime = 0;
    private static final long DOUBLE_CANCEL_THRESHOLD_MS = 2000; // 2秒内双击 Ctrl+C 强制退出
    
    @Inject
    public CancellationHandler(CancelRequestHook cancelHook) {
        this.cancelHook = cancelHook;
        setupSignalHandler();
    }
    
    /**
     * 设置系统信号处理器（Ctrl+C）
     */
    private void setupSignalHandler() {
        try {
            // 注册 SIGINT 处理器（Unix/Linux/macOS）
            sun.misc.Signal.handle(
                new sun.misc.Signal("INT"),
                signal -> handleCancellation()
            );
            logger.info("SIGINT 处理器已注册");
            
        } catch (Exception e) {
            logger.warn("无法注册 SIGINT 处理器（可能不支持）: {}", e.getMessage());
        }
    }
    
    /**
     * 处理取消请求
     */
    public void handleCancellation() {
        long now = System.currentTimeMillis();
        
        // 检查是否为双击 Ctrl+C
        if (now - lastCancelTime < DOUBLE_CANCEL_THRESHOLD_MS) {
            handleForceShutdown();
            return;
        }
        
        lastCancelTime = now;
        
        // 如果已经在关闭流程中，忽略
        if (shutdownRequested) {
            logger.debug("已在关闭流程中，忽略取消请求");
            return;
        }
        
        // 触发优雅取消
        logger.info("收到取消请求，正在优雅取消当前任务...");
        System.out.println("\n⚠️  收到取消信号，正在停止当前任务...");
        System.out.println("💡 再次按 Ctrl+C 可强制退出程序");
        
        cancelHook.cancel();
    }
    
    /**
     * 处理强制关闭
     */
    private void handleForceShutdown() {
        logger.warn("收到强制关闭信号（双击 Ctrl+C）");
        System.out.println("\n🛑 强制退出程序...");
        
        shutdownRequested = true;
        
        // 给程序 500ms 时间清理资源
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 强制退出
        System.exit(130); // 130 = 128 + SIGINT(2)
    }
    
    /**
     * 处理 ESC 键取消
     */
    public void handleEscapeKey() {
        logger.info("收到 ESC 键取消请求");
        System.out.println("\n⚠️  ESC 键按下，正在取消当前操作...");
        
        cancelHook.cancel();
    }
    
    /**
     * 重置取消状态
     */
    public void reset() {
        cancelHook.reset();
    }
    
    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return cancelHook.isCancelled();
    }
    
    /**
     * 设置取消回调
     */
    public void setOnCancel(Runnable callback) {
        cancelHook.setOnCancel(callback);
    }
    
    /**
     * 获取取消钩子
     */
    public CancelRequestHook getCancelHook() {
        return cancelHook;
    }
}
