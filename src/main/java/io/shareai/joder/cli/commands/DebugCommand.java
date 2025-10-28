package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.util.Map;

/**
 * Debug Command - 调试工具命令
 * 提供调试信息和日志级别控制
 */
@Singleton
public class DebugCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(DebugCommand.class);
    
    private static boolean debugMode = false;
    
    @Override
    public String getDescription() {
        return "调试工具和日志控制";
    }
    
    @Override
    public String getUsage() {
        return "debug [on|off|info|env|threads] - 调试工具";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            String subCommand = args != null ? args.trim().toLowerCase() : "info";
            
            return switch (subCommand) {
                case "on" -> enableDebug();
                case "off" -> disableDebug();
                case "info", "" -> showDebugInfo();
                case "env" -> showEnvironment();
                case "threads" -> showThreads();
                case "heap" -> showHeapInfo();
                default -> CommandResult.error("未知子命令: " + subCommand);
            };
            
        } catch (Exception e) {
            logger.error("Failed to execute debug command", e);
            return CommandResult.error("调试命令失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用调试模式
     */
    private CommandResult enableDebug() {
        debugMode = true;
        System.setProperty("joder.debug", "true");
        
        return CommandResult.success(
                "🐛 调试模式已启用\n\n" +
                "调试功能:\n" +
                "  ✓ 详细日志输出\n" +
                "  ✓ 异常堆栈跟踪\n" +
                "  ✓ 性能计时信息\n" +
                "  ✓ API 请求/响应日志\n\n" +
                "💡 使用 /debug off 关闭调试模式"
        );
    }
    
    /**
     * 禁用调试模式
     */
    private CommandResult disableDebug() {
        debugMode = false;
        System.clearProperty("joder.debug");
        
        return CommandResult.success("✅ 调试模式已关闭");
    }
    
    /**
     * 显示调试信息
     */
    private CommandResult showDebugInfo() {
        StringBuilder output = new StringBuilder();
        
        output.append("🐛 调试信息\n\n");
        
        // 调试状态
        output.append("═══════════════════════════════════════\n");
        output.append("📊 调试状态\n");
        output.append("═══════════════════════════════════════\n\n");
        output.append(String.format("  调试模式:    %s\n", debugMode ? "开启 ✓" : "关闭"));
        output.append(String.format("  日志级别:    %s\n", getLogLevel()));
        output.append(String.format("  运行模式:    %s\n", getRunMode()));
        output.append("\n");
        
        // 运行时信息
        output.append("═══════════════════════════════════════\n");
        output.append("⚡ 运行时信息\n");
        output.append("═══════════════════════════════════════\n\n");
        
        Runtime runtime = Runtime.getRuntime();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        
        output.append(String.format("  运行时间:    %s\n", formatUptime(uptime)));
        output.append(String.format("  PID:         %d\n", ProcessHandle.current().pid()));
        output.append(String.format("  线程数:      %d\n", Thread.activeCount()));
        output.append(String.format("  内存使用:    %s / %s\n",
                formatBytes(runtime.totalMemory() - runtime.freeMemory()),
                formatBytes(runtime.maxMemory())));
        output.append("\n");
        
        // 快速命令
        output.append("═══════════════════════════════════════\n");
        output.append("🔧 可用命令\n");
        output.append("═══════════════════════════════════════\n\n");
        output.append("  /debug on       - 启用调试模式\n");
        output.append("  /debug off      - 关闭调试模式\n");
        output.append("  /debug env      - 查看环境变量\n");
        output.append("  /debug threads  - 查看线程信息\n");
        output.append("  /debug heap     - 查看堆内存信息\n");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 显示环境变量
     */
    private CommandResult showEnvironment() {
        StringBuilder output = new StringBuilder();
        
        output.append("🌍 环境变量\n\n");
        
        // 系统属性
        output.append("═══════════════════════════════════════\n");
        output.append("📋 系统属性\n");
        output.append("═══════════════════════════════════════\n\n");
        
        Map<String, String> relevantProps = Map.of(
                "java.version", "Java 版本",
                "java.home", "Java 主目录",
                "user.dir", "工作目录",
                "user.home", "用户主目录",
                "os.name", "操作系统",
                "os.arch", "系统架构"
        );
        
        relevantProps.forEach((key, label) -> {
            String value = System.getProperty(key, "N/A");
            output.append(String.format("  %-15s %s\n", label + ":", value));
        });
        
        output.append("\n");
        
        // Joder 特定环境变量
        output.append("═══════════════════════════════════════\n");
        output.append("🎯 Joder 配置\n");
        output.append("═══════════════════════════════════════\n\n");
        
        String[] joderEnvs = {
                "joder.debug",
                "joder.config.path",
                "joder.model.default",
                "joder.api.key"
        };
        
        for (String env : joderEnvs) {
            String value = System.getProperty(env, "未设置");
            // 隐藏敏感信息
            if (env.contains("key") && !value.equals("未设置")) {
                value = maskSensitive(value);
            }
            output.append(String.format("  %-20s %s\n", env + ":", value));
        }
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 显示线程信息
     */
    private CommandResult showThreads() {
        StringBuilder output = new StringBuilder();
        
        output.append("🧵 线程信息\n\n");
        
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        Thread[] threads = new Thread[rootGroup.activeCount()];
        int count = rootGroup.enumerate(threads);
        
        output.append(String.format("活跃线程数: %d\n\n", count));
        
        output.append("线程列表:\n");
        output.append("─────────────────────────────────────\n");
        
        for (int i = 0; i < count; i++) {
            Thread thread = threads[i];
            if (thread != null) {
                output.append(String.format("  [%d] %s (%s)%s\n",
                        thread.getId(),
                        thread.getName(),
                        thread.getState(),
                        thread.isDaemon() ? " [守护]" : ""));
            }
        }
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 显示堆内存信息
     */
    private CommandResult showHeapInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usagePercent = (usedMemory * 100.0) / maxMemory;
        
        StringBuilder output = new StringBuilder();
        output.append("💾 堆内存信息\n\n");
        
        output.append(String.format("  最大内存:    %s\n", formatBytes(maxMemory)));
        output.append(String.format("  已分配:      %s\n", formatBytes(totalMemory)));
        output.append(String.format("  已使用:      %s (%.1f%%)\n", formatBytes(usedMemory), usagePercent));
        output.append(String.format("  可用:        %s\n", formatBytes(freeMemory)));
        output.append("\n");
        
        // 内存警告
        if (usagePercent > 80) {
            output.append("⚠️  内存使用率较高，建议：\n");
            output.append("   - 增加 JVM 堆内存 (-Xmx)\n");
            output.append("   - 检查内存泄漏\n");
            output.append("   - 运行垃圾回收 (System.gc())\n");
        } else {
            output.append("✅ 内存使用正常\n");
        }
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 获取日志级别
     */
    private String getLogLevel() {
        // 简化实现，实际应从日志配置读取
        return debugMode ? "DEBUG" : "INFO";
    }
    
    /**
     * 获取运行模式
     */
    private String getRunMode() {
        String mode = System.getProperty("joder.mode", "production");
        return switch (mode) {
            case "dev", "development" -> "开发模式";
            case "test" -> "测试模式";
            default -> "生产模式";
        };
    }
    
    /**
     * 格式化运行时间
     */
    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d 小时 %d 分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d 分钟 %d 秒", minutes, seconds % 60);
        } else {
            return String.format("%d 秒", seconds);
        }
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 掩码敏感信息
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
    
    public static boolean isDebugMode() {
        return debugMode;
    }
}
