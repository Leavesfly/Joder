package io.leavesfly.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * 启动时间钩子
 * 对应 Kode 的 useLogStartupTime hook
 */
@Singleton
public class StartupTimeHook {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupTimeHook.class);
    private final long startupTime;
    
    public StartupTimeHook() {
        this.startupTime = System.currentTimeMillis();
    }
    
    /**
     * 记录启动时间
     */
    public void logStartupTime() {
        long uptimeMs = getUptimeMs();
        logger.info("应用启动时间: {}ms", uptimeMs);
        
        // 可选：如果启动时间过长，输出警告
        if (uptimeMs > 5000) {
            logger.warn("启动时间较长，可能需要优化");
        }
    }
    
    /**
     * 获取运行时间（毫秒）
     */
    public long getUptimeMs() {
        return System.currentTimeMillis() - startupTime;
    }
    
    /**
     * 获取运行时间（秒）
     */
    public long getUptimeSeconds() {
        return getUptimeMs() / 1000;
    }
    
    /**
     * 获取格式化的运行时间
     */
    public String getFormattedUptime() {
        long ms = getUptimeMs();
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
