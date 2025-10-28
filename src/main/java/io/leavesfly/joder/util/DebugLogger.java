package io.leavesfly.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调试日志工具类
 * 对应 Kode 的 debugLogger.ts
 */
public class DebugLogger {

    private static final Logger logger = LoggerFactory.getLogger(DebugLogger.class);
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * 日志级别
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FLOW, API, STATE
    }

    /**
     * 日志条目
     */
    public static class LogEntry {
        public String timestamp;
        public LogLevel level;
        public String phase;
        public String requestId;
        public Object data;
        public Long elapsed;

        public LogEntry(LogLevel level, String phase, Object data) {
            this.timestamp = ISO_FORMAT.format(new Date());
            this.level = level;
            this.phase = phase;
            this.data = data;
        }
    }

    private static final String DEBUG_MODE = System.getenv("DEBUG");
    private static final String DEBUG_VERBOSE = System.getenv("DEBUG_VERBOSE");
    private static final String KODE_DIR = System.getProperty("user.home") + "/.kode";
    private static final Path DEBUG_BASE = Paths.get(KODE_DIR, "debug");

    private static final Map<String, Long> recentLogs = new ConcurrentHashMap<>();
    private static final long DEDUPE_WINDOW_MS = 5000;

    static {
        try {
            Files.createDirectories(DEBUG_BASE);
        } catch (IOException e) {
            logger.warn("无法创建调试目录: {}", DEBUG_BASE);
        }
    }

    /**
     * 检查是否启用调试模式
     */
    public static boolean isDebugMode() {
        return DEBUG_MODE != null && !DEBUG_MODE.isEmpty();
    }

    /**
     * 检查是否启用详细调试模式
     */
    public static boolean isDebugVerbose() {
        return DEBUG_VERBOSE != null && !DEBUG_VERBOSE.isEmpty();
    }

    /**
     * 记录流程日志
     */
    public static void logFlow(String phase, Object data) {
        logWithLevel(LogLevel.FLOW, phase, data);
    }

    /**
     * 记录 API 日志
     */
    public static void logAPI(String phase, Object data) {
        logWithLevel(LogLevel.API, phase, data);
    }

    /**
     * 记录状态日志
     */
    public static void logState(String phase, Object data) {
        logWithLevel(LogLevel.STATE, phase, data);
    }

    /**
     * 记录信息日志
     */
    public static void logInfo(String phase, Object data) {
        logWithLevel(LogLevel.INFO, phase, data);
    }

    /**
     * 记录警告日志
     */
    public static void logWarn(String phase, Object data) {
        logWithLevel(LogLevel.WARN, phase, data);
    }

    /**
     * 记录错误日志
     */
    public static void logError(String phase, Object data) {
        logWithLevel(LogLevel.ERROR, phase, data);
    }

    /**
     * 核心日志记录方法
     */
    private static void logWithLevel(LogLevel level, String phase, Object data) {
        if (!isDebugMode()) {
            return;
        }

        // 去重检查
        String dedupeKey = level + ":" + phase;
        long now = System.currentTimeMillis();
        Long lastLogTime = recentLogs.get(dedupeKey);

        if (lastLogTime != null && (now - lastLogTime) < DEDUPE_WINDOW_MS) {
            return; // 跳过重复日志
        }

        recentLogs.put(dedupeKey, now);

        // 清理过期日志记录
        recentLogs.entrySet().removeIf(entry -> (now - entry.getValue()) > DEDUPE_WINDOW_MS);

        // 创建日志条目
        LogEntry entry = new LogEntry(level, phase, data);

        // 写入文件
        writeToFile(entry);

        // 终端输出
        logToTerminal(entry);
    }

    /**
     * 写入日志文件
     */
    private static void writeToFile(LogEntry entry) {
        try {
            String logFileName = DEBUG_BASE.resolve(entry.level.toString().toLowerCase() + ".log").toString();
            try (FileWriter writer = new FileWriter(logFileName, true)) {
                writer.write(String.format("[%s] %s: %s\n", entry.timestamp, entry.phase, entry.data));
                writer.flush();
            }
        } catch (IOException e) {
            logger.debug("写入日志文件失败", e);
        }
    }

    /**
     * 终端输出
     */
    private static void logToTerminal(LogEntry entry) {
        boolean shouldShow = false;

        if (isDebugVerbose()) {
            shouldShow = true;
        } else {
            shouldShow = entry.level == LogLevel.ERROR ||
                        entry.level == LogLevel.WARN ||
                        entry.level == LogLevel.INFO;
        }

        if (shouldShow) {
            String prefix = getPrefix(entry.level);
            System.out.println(String.format("[%s] %s %s: %s",
                entry.timestamp, prefix, entry.phase, entry.data));
        }
    }

    /**
     * 获取日志前缀
     */
    private static String getPrefix(LogLevel level) {
        switch (level) {
            case FLOW:
                return "🔄";
            case API:
                return "🌐";
            case STATE:
                return "📊";
            case ERROR:
                return "❌";
            case WARN:
                return "⚠️";
            case INFO:
                return "ℹ️";
            case TRACE:
                return "📈";
            default:
                return "🔍";
        }
    }

    /**
     * 格式化消息列表
     */
    public static String formatMessages(List<?> messages) {
        if (messages == null || messages.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, messages.size() - 5);
        for (int i = start; i < messages.size(); i++) {
            Object msg = messages.get(i);
            String content = String.valueOf(msg);
            if (content.length() > 300) {
                content = content.substring(0, 300) + "...";
            }
            sb.append(String.format("[%d] %s\n", i, content));
        }

        return sb.toString();
    }

    /**
     * 初始化调试日志系统
     */
    public static void init() {
        if (isDebugMode()) {
            logInfo("DEBUG_LOGGER_INIT", "调试日志系统已初始化");
            if (isDebugVerbose()) {
                logInfo("DEBUG_MODE", "详细调试模式已启用");
            }
        }
    }

    /**
     * API 错误日志
     */
    public static void logAPIError(String model, String endpoint, int status, String error) {
        logError("API_ERROR", String.format(
            "Model: %s, Endpoint: %s, Status: %d, Error: %s",
            model, endpoint, status, error
        ));
    }

    /**
     * 获取调试日志目录
     */
    public static String getDebugDir() {
        return DEBUG_BASE.toString();
    }

    /**
     * 清空调试日志
     */
    public static void clearDebugLogs() {
        try {
            File dir = DEBUG_BASE.toFile();
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".log")) {
                        Files.delete(file.toPath());
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("清空调试日志失败", e);
        }
    }
}
