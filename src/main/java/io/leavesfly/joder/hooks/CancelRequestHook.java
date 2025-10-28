package io.leavesfly.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 请求取消钩子
 * 对应 Kode 的 useCancelRequest hook
 * 处理 ESC 键取消请求的逻辑
 */
@Singleton
public class CancelRequestHook {
    
    private static final Logger logger = LoggerFactory.getLogger(CancelRequestHook.class);
    
    private final AtomicBoolean isCancelled;
    private Runnable onCancelCallback;
    
    public CancelRequestHook() {
        this.isCancelled = new AtomicBoolean(false);
    }
    
    /**
     * 注册取消回调
     */
    public void setOnCancel(Runnable callback) {
        this.onCancelCallback = callback;
    }
    
    /**
     * 处理取消请求
     */
    public void cancel() {
        if (isCancelled.compareAndSet(false, true)) {
            logger.info("请求已被取消");
            
            if (onCancelCallback != null) {
                try {
                    onCancelCallback.run();
                } catch (Exception e) {
                    logger.error("取消回调执行失败", e);
                }
            }
        }
    }
    
    /**
     * 重置取消状态
     */
    public void reset() {
        isCancelled.set(false);
    }
    
    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }
    
    /**
     * 抛出取消异常（如果已取消）
     */
    public void throwIfCancelled() throws CancellationException {
        if (isCancelled.get()) {
            throw new CancellationException("请求已被取消");
        }
    }
    
    /**
     * 取消异常
     */
    public static class CancellationException extends Exception {
        public CancellationException(String message) {
            super(message);
        }
    }
}
