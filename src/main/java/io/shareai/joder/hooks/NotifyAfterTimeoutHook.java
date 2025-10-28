package io.shareai.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 超时通知钩子
 * 对应 Kode 的 useNotifyAfterTimeout hook
 * 在超过指定时间无用户交互时发送通知
 */
@Singleton
public class NotifyAfterTimeoutHook {
    
    private static final Logger logger = LoggerFactory.getLogger(NotifyAfterTimeoutHook.class);
    private static final long DEFAULT_INTERACTION_THRESHOLD_MS = 6000;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private long lastInteractionTime;
    
    public NotifyAfterTimeoutHook() {
        this.lastInteractionTime = System.currentTimeMillis();
        
        // 监听标准输入来检测用户交互
        // 注意：这需要在合适的地方调用 updateLastInteractionTime()
    }
    
    /**
     * 更新最后交互时间
     */
    public void updateLastInteractionTime() {
        lastInteractionTime = System.currentTimeMillis();
    }
    
    /**
     * 获取距离最后交互的时间
     */
    public long getTimeSinceLastInteraction() {
        return System.currentTimeMillis() - lastInteractionTime;
    }
    
    /**
     * 检查是否有最近的交互
     */
    public boolean hasRecentInteraction(long threshold) {
        return getTimeSinceLastInteraction() < threshold;
    }
    
    /**
     * 启动超时通知
     * 
     * @param message 通知消息
     * @param timeout 超时时间（毫秒）
     * @param notificationHandler 通知处理器
     */
    public void startNotifyAfterTimeout(
            String message,
            long timeout,
            Consumer<String> notificationHandler) {
        
        startNotifyAfterTimeout(message, timeout, DEFAULT_INTERACTION_THRESHOLD_MS, notificationHandler);
    }
    
    /**
     * 启动超时通知
     * 
     * @param message 通知消息
     * @param timeout 超时时间（毫秒）
     * @param interactionThreshold 交互阈值（毫秒）
     * @param notificationHandler 通知处理器
     */
    public void startNotifyAfterTimeout(
            String message,
            long timeout,
            long interactionThreshold,
            Consumer<String> notificationHandler) {
        
        // 重置交互时间
        updateLastInteractionTime();
        
        // 调度通知任务
        scheduler.scheduleAtFixedRate(() -> {
            if (!hasRecentInteraction(interactionThreshold)) {
                try {
                    notificationHandler.accept(message);
                } catch (Exception e) {
                    logger.error("通知处理失败", e);
                }
            }
        }, timeout, timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 发送桌面通知（简化实现）
     */
    public void sendNotification(String message) {
        // 简化实现：打印到日志
        logger.info("通知: {}", message);
        
        // TODO: 可以集成真实的桌面通知系统
        // 例如使用 java-gnome, JNA 调用系统 API 等
    }
    
    /**
     * 关闭调度器
     */
    public void shutdown() {
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
