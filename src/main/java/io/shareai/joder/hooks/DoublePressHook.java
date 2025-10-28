package io.shareai.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.function.Consumer;

/**
 * 双击/双按键检测钩子
 * 对应 Kode 的 useDoublePress hook
 */
@Singleton
public class DoublePressHook {
    
    private static final Logger logger = LoggerFactory.getLogger(DoublePressHook.class);
    private static final long DOUBLE_PRESS_TIMEOUT_MS = 2000;
    
    private long lastPressTime;
    private boolean isPending;
    private Runnable timeoutTask;
    
    public DoublePressHook() {
        this.lastPressTime = 0;
        this.isPending = false;
    }
    
    /**
     * 处理按键
     * 
     * @param onFirstPress 第一次按键回调
     * @param onDoublePress 双击回调
     * @param setPending 设置待处理状态回调
     */
    public void handlePress(
            Runnable onFirstPress,
            Runnable onDoublePress,
            Consumer<Boolean> setPending) {
        
        long now = System.currentTimeMillis();
        long timeSinceLastPress = now - lastPressTime;
        
        // 检查是否为双击
        if (timeSinceLastPress <= DOUBLE_PRESS_TIMEOUT_MS && isPending) {
            // 双击触发
            cancelTimeout();
            onDoublePress.run();
            setPending.accept(false);
            isPending = false;
        } else {
            // 第一次按键
            if (onFirstPress != null) {
                onFirstPress.run();
            }
            setPending.accept(true);
            isPending = true;
            
            // 设置超时
            scheduleTimeout(() -> {
                setPending.accept(false);
                isPending = false;
            });
        }
        
        lastPressTime = now;
    }
    
    /**
     * 简化版本：只处理双击
     */
    public void handlePress(Runnable onDoublePress, Consumer<Boolean> setPending) {
        handlePress(null, onDoublePress, setPending);
    }
    
    /**
     * 调度超时任务
     */
    private void scheduleTimeout(Runnable task) {
        cancelTimeout();
        
        timeoutTask = task;
        new Thread(() -> {
            try {
                Thread.sleep(DOUBLE_PRESS_TIMEOUT_MS);
                if (timeoutTask == task) {
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 取消超时任务
     */
    private void cancelTimeout() {
        timeoutTask = null;
    }
    
    /**
     * 重置状态
     */
    public void reset() {
        lastPressTime = 0;
        isPending = false;
        cancelTimeout();
    }
    
    public boolean isPending() {
        return isPending;
    }
}
