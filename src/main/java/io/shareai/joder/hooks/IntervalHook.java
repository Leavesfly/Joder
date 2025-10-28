package io.shareai.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 定时器钩子
 * 对应 Kode 的 useInterval hook
 */
public class IntervalHook {
    
    private static final Logger logger = LoggerFactory.getLogger(IntervalHook.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private ScheduledFuture<?> scheduledTask;
    private final Runnable callback;
    private final long delay;
    
    /**
     * 创建定时器钩子
     * 
     * @param callback 回调函数
     * @param delay 延迟时间（毫秒）
     */
    public IntervalHook(Runnable callback, long delay) {
        this.callback = callback;
        this.delay = delay;
    }
    
    /**
     * 启动定时器
     */
    public void start() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            logger.warn("定时器已经在运行");
            return;
        }
        
        scheduledTask = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.error("定时器回调执行失败", e);
                }
            },
            delay,
            delay,
            TimeUnit.MILLISECONDS
        );
        
        logger.debug("定时器已启动，间隔: {}ms", delay);
    }
    
    /**
     * 停止定时器
     */
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
            logger.debug("定时器已停止");
        }
    }
    
    /**
     * 重启定时器
     */
    public void restart() {
        stop();
        start();
    }
    
    /**
     * 检查定时器是否在运行
     */
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }
    
    /**
     * 关闭调度器（应用退出时调用）
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
