package io.leavesfly.joder.services.maintenance;

import io.leavesfly.joder.util.CleanupUtils;
import io.leavesfly.joder.util.DebugLogger;
import io.leavesfly.joder.util.EnvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.nio.file.Paths;
import java.util.concurrent.*;

/**
 * 维护调度器
 * 负责定期执行系统维护任务，如日志清理、缓存清理等
 */
@Singleton
public class MaintenanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceScheduler.class);
    
    private final ScheduledExecutorService scheduler;
    private boolean started = false;

    // 维护任务配置
    private static final long INITIAL_DELAY_MINUTES = 1;      // 启动后1分钟开始
    private static final long CLEANUP_INTERVAL_HOURS = 24;    // 每24小时执行一次清理
    private static final long STATS_INTERVAL_HOURS = 6;       // 每6小时记录统计信息

    public MaintenanceScheduler() {
        // 创建单线程调度器（守护线程）
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "maintenance-scheduler");
            thread.setDaemon(true);  // 守护线程，不阻止 JVM 退出
            return thread;
        });
    }

    /**
     * 启动维护调度器
     */
    public void start() {
        if (started) {
            logger.warn("维护调度器已经启动");
            return;
        }

        logger.info("启动维护调度器...");

        // 调度日志清理任务
        scheduler.scheduleAtFixedRate(
            this::cleanupOldLogs,
            INITIAL_DELAY_MINUTES,
            TimeUnit.HOURS.toMinutes(CLEANUP_INTERVAL_HOURS),
            TimeUnit.MINUTES
        );

        // 调度统计信息记录任务
        scheduler.scheduleAtFixedRate(
            this::recordStats,
            INITIAL_DELAY_MINUTES,
            TimeUnit.HOURS.toMinutes(STATS_INTERVAL_HOURS),
            TimeUnit.MINUTES
        );

        started = true;
        logger.info("维护调度器已启动 (清理周期: {}小时, 统计周期: {}小时)", 
            CLEANUP_INTERVAL_HOURS, STATS_INTERVAL_HOURS);
    }

    /**
     * 停止维护调度器
     */
    public void stop() {
        if (!started) {
            return;
        }

        logger.info("停止维护调度器...");
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        started = false;
        logger.info("维护调度器已停止");
    }

    /**
     * 清理旧日志文件
     */
    private void cleanupOldLogs() {
        try {
            DebugLogger.logInfo("MAINTENANCE_CLEANUP_START", "开始清理旧日志文件");

            String baseDir = EnvUtils.getJoderBaseDir().toString();
            String logsDir = baseDir + "/logs";
            String errorsDir = baseDir + "/errors";
            String messagesDir = baseDir + "/messages";

            // 执行清理
            CleanupUtils.CleanupResult result = CleanupUtils.cleanupOldMessageFiles(
                messagesDir,
                errorsDir
            );

            DebugLogger.logInfo("MAINTENANCE_CLEANUP_COMPLETE", 
                String.format("清理完成: 删除 %d 个消息文件, %d 个错误文件", 
                    result.getMessages(), result.getErrors()));

            // 额外清理：清空目录
            try {
                int emptyDirs = CleanupUtils.cleanupEmptyDirectories(Paths.get(logsDir));
                if (emptyDirs > 0) {
                    logger.info("清理了 {} 个空目录", emptyDirs);
                }
            } catch (Exception e) {
                logger.warn("清理空目录失败", e);
            }

            // 额外清理：大文件
            try {
                long maxFileSize = 50 * 1024 * 1024;  // 50MB
                int largeFiles = CleanupUtils.cleanupLargeFiles(Paths.get(logsDir), maxFileSize);
                if (largeFiles > 0) {
                    logger.info("清理了 {} 个大文件 (>50MB)", largeFiles);
                }
            } catch (Exception e) {
                logger.warn("清理大文件失败", e);
            }

        } catch (Exception e) {
            logger.error("清理旧日志失败", e);
            DebugLogger.logError("MAINTENANCE_CLEANUP_ERROR", e.getMessage());
        }
    }

    /**
     * 记录统计信息
     */
    private void recordStats() {
        try {
            String baseDir = EnvUtils.getJoderBaseDir().toString();
            String logsDir = baseDir + "/logs";

            // 计算日志目录大小
            long totalSize = CleanupUtils.getDirectorySize(Paths.get(logsDir));
            long sizeMB = totalSize / (1024 * 1024);

            DebugLogger.logInfo("MAINTENANCE_STATS", 
                String.format("日志目录大小: %d MB", sizeMB));

            // 如果日志太大，发出警告
            if (sizeMB > 500) {
                logger.warn("日志目录占用空间较大: {} MB，建议手动清理", sizeMB);
            }

        } catch (Exception e) {
            logger.error("记录统计信息失败", e);
        }
    }

    /**
     * 立即执行一次清理
     */
    public void cleanupNow() {
        logger.info("立即执行清理任务");
        cleanupOldLogs();
    }

    /**
     * 立即执行一次统计
     */
    public void recordStatsNow() {
        logger.info("立即记录统计信息");
        recordStats();
    }

    /**
     * 检查调度器是否正在运行
     */
    public boolean isRunning() {
        return started && !scheduler.isShutdown();
    }

    /**
     * 获取维护配置信息
     */
    public MaintenanceConfig getConfig() {
        return new MaintenanceConfig(
            CLEANUP_INTERVAL_HOURS,
            STATS_INTERVAL_HOURS,
            isRunning()
        );
    }

    /**
     * 维护配置信息类
     */
    public static class MaintenanceConfig {
        private final long cleanupIntervalHours;
        private final long statsIntervalHours;
        private final boolean running;

        public MaintenanceConfig(long cleanupIntervalHours, long statsIntervalHours, boolean running) {
            this.cleanupIntervalHours = cleanupIntervalHours;
            this.statsIntervalHours = statsIntervalHours;
            this.running = running;
        }

        public long getCleanupIntervalHours() { return cleanupIntervalHours; }
        public long getStatsIntervalHours() { return statsIntervalHours; }
        public boolean isRunning() { return running; }

        @Override
        public String toString() {
            return String.format("MaintenanceConfig{cleanup=%dh, stats=%dh, running=%s}",
                cleanupIntervalHours, statsIntervalHours, running);
        }
    }
}
