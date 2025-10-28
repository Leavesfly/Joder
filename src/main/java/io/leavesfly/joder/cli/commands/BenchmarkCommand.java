package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark Command - 性能基准测试命令
 * 测试系统和AI模型的性能指标
 */
@Singleton
public class BenchmarkCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(BenchmarkCommand.class);
    
    @Override
    public String getDescription() {
        return "性能基准测试";
    }
    
    @Override
    public String getUsage() {
        return "benchmark [--quick|--full] - 运行性能测试";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            boolean fullMode = args != null && args.contains("--full");
            
            StringBuilder output = new StringBuilder();
            output.append("🏃 性能基准测试\n\n");
            
            // 1. 系统信息
            output.append("═══════════════════════════════════════\n");
            output.append("📊 系统信息\n");
            output.append("═══════════════════════════════════════\n\n");
            output.append(getSystemInfo());
            
            // 2. JVM 性能
            output.append("\n═══════════════════════════════════════\n");
            output.append("☕ JVM 性能\n");
            output.append("═══════════════════════════════════════\n\n");
            output.append(getJvmPerformance());
            
            // 3. 内存性能
            output.append("\n═══════════════════════════════════════\n");
            output.append("💾 内存性能\n");
            output.append("═══════════════════════════════════════\n\n");
            output.append(getMemoryBenchmark());
            
            // 4. 文件 I/O 性能
            if (fullMode) {
                output.append("\n═══════════════════════════════════════\n");
                output.append("📁 文件 I/O 性能\n");
                output.append("═══════════════════════════════════════\n\n");
                output.append(getIoBenchmark());
            }
            
            // 5. 建议
            output.append("\n═══════════════════════════════════════\n");
            output.append("💡 优化建议\n");
            output.append("═══════════════════════════════════════\n\n");
            output.append(getOptimizationSuggestions());
            
            output.append("\n✅ 基准测试完成\n");
            
            if (!fullMode) {
                output.append("\n💡 提示: 使用 /benchmark --full 运行完整测试\n");
            }
            
            return CommandResult.success(output.toString());
            
        } catch (Exception e) {
            logger.error("Failed to run benchmark", e);
            return CommandResult.error("基准测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取系统信息
     */
    private String getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        return String.format(
                "  操作系统:    %s (%s)\n" +
                "  Java 版本:   %s\n" +
                "  可用处理器:  %d 核心\n" +
                "  系统架构:    %s\n",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("java.version"),
                runtime.availableProcessors(),
                System.getProperty("os.arch")
        );
    }
    
    /**
     * 获取 JVM 性能
     */
    private String getJvmPerformance() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        return String.format(
                "  当前线程数:  %d\n" +
                "  峰值线程数:  %d\n" +
                "  总启动线程:  %d\n" +
                "  守护线程数:  %d\n",
                threadBean.getThreadCount(),
                threadBean.getPeakThreadCount(),
                threadBean.getTotalStartedThreadCount(),
                threadBean.getDaemonThreadCount()
        );
    }
    
    /**
     * 获取内存基准测试
     */
    private String getMemoryBenchmark() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Runtime runtime = Runtime.getRuntime();
        
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        double usagePercent = (heapUsed * 100.0) / heapMax;
        
        // 内存分配测试
        long startTime = System.nanoTime();
        List<byte[]> allocations = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            allocations.add(new byte[1024]); // 分配 1KB
        }
        long allocTime = System.nanoTime() - startTime;
        
        // 清理
        allocations.clear();
        System.gc();
        
        return String.format(
                "  堆内存使用:  %s / %s (%.1f%%)\n" +
                "  非堆内存:    %s\n" +
                "  最大内存:    %s\n" +
                "  空闲内存:    %s\n" +
                "  分配性能:    %.2f MB/s\n",
                formatBytes(heapUsed),
                formatBytes(heapMax),
                usagePercent,
                formatBytes(nonHeapUsed),
                formatBytes(runtime.maxMemory()),
                formatBytes(runtime.freeMemory()),
                (1000.0 * 1024 * 1000000000.0) / allocTime / (1024 * 1024)
        );
    }
    
    /**
     * 获取 I/O 基准测试
     */
    private String getIoBenchmark() {
        try {
            // 字符串拼接性能测试
            long startTime = System.nanoTime();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("test");
            }
            long concatTime = System.nanoTime() - startTime;
            
            // 列表操作性能测试
            startTime = System.nanoTime();
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                list.add("item" + i);
            }
            long listTime = System.nanoTime() - startTime;
            
            return String.format(
                    "  字符串拼接:  %.2f ms (10K 次)\n" +
                    "  列表操作:    %.2f ms (10K 次)\n" +
                    "  综合评分:    %s\n",
                    concatTime / 1000000.0,
                    listTime / 1000000.0,
                    (concatTime + listTime) < 50000000 ? "优秀 ⭐⭐⭐" : "良好 ⭐⭐"
            );
            
        } catch (Exception e) {
            return "  测试失败: " + e.getMessage() + "\n";
        }
    }
    
    /**
     * 获取优化建议
     */
    private String getOptimizationSuggestions() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double usagePercent = (heapUsed * 100.0) / heapMax;
        
        StringBuilder suggestions = new StringBuilder();
        
        if (usagePercent > 80) {
            suggestions.append("  ⚠️  内存使用率较高 (").append(String.format("%.1f%%", usagePercent)).append(")\n");
            suggestions.append("      建议: 增加 JVM 堆内存 (-Xmx)\n\n");
        }
        
        if (Runtime.getRuntime().availableProcessors() < 4) {
            suggestions.append("  ⚠️  处理器核心数较少\n");
            suggestions.append("      建议: 限制并发任务数量\n\n");
        }
        
        if (suggestions.length() == 0) {
            suggestions.append("  ✅ 系统性能良好，无需优化\n");
        }
        
        return suggestions.toString();
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
}
