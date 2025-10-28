package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

/**
 * Doctor 命令 - 检查 Joder 安装的健康状况
 */
@Singleton
public class DoctorCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(DoctorCommand.class);
    
    private final ToolRegistry toolRegistry;
    
    @Inject
    public DoctorCommand(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
    
    @Override
    public String getDescription() {
        return "检查 Joder 安装的健康状况";
    }
    
    @Override
    public String getUsage() {
        return "/doctor - 诊断系统健康状况";
    }
    
    @Override
    public CommandResult execute(String args) {
        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("🔍 Joder 系统诊断报告\n");
        report.append("═══════════════════════════════════\n\n");
        
        boolean allChecksPass = true;
        
        // 检查 1: Java 版本
        report.append("Java 环境:\n");
        String javaVersion = System.getProperty("java.version");
        String javaHome = System.getProperty("java.home");
        report.append(String.format("  ✅ Java 版本: %s\n", javaVersion));
        report.append(String.format("  ✅ JAVA_HOME: %s\n", javaHome));
        report.append("\n");
        
        // 检查 2: 工具系统
        report.append("工具系统:\n");
        int toolCount = toolRegistry.size();
        int enabledToolCount = (int) toolRegistry.getEnabledTools().stream().count();
        report.append(String.format("  ✅ 已注册工具: %d\n", toolCount));
        report.append(String.format("  ✅ 已启用工具: %d\n", enabledToolCount));
        
        if (toolCount == 0) {
            report.append("  ⚠️  警告: 没有注册任何工具\n");
            allChecksPass = false;
        }
        report.append("\n");
        
        // 检查 3: 环境变量
        report.append("环境变量:\n");
        String[] requiredEnvVars = {
            "ANTHROPIC_API_KEY",
            "OPENAI_API_KEY",
            "QWEN_API_KEY",
            "DEEPSEEK_API_KEY"
        };
        
        int configuredKeys = 0;
        for (String envVar : requiredEnvVars) {
            String value = System.getenv(envVar);
            if (value != null && !value.trim().isEmpty()) {
                report.append(String.format("  ✅ %s 已配置\n", envVar));
                configuredKeys++;
            } else {
                report.append(String.format("  ⚠️  %s 未配置\n", envVar));
            }
        }
        
        if (configuredKeys == 0) {
            report.append("  ⚠️  警告: 至少需要配置一个 API Key\n");
            allChecksPass = false;
        }
        report.append("\n");
        
        // 检查 4: 工作目录
        report.append("工作目录:\n");
        String workingDir = System.getProperty("user.dir");
        File workingDirFile = new File(workingDir);
        report.append(String.format("  ✅ 当前目录: %s\n", workingDir));
        report.append(String.format("  ✅ 目录可写: %s\n", workingDirFile.canWrite() ? "是" : "否"));
        report.append("\n");
        
        // 检查 5: 系统资源
        report.append("系统资源:\n");
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        
        report.append(String.format("  ✅ 最大内存: %d MB\n", maxMemory));
        report.append(String.format("  ✅ 已用内存: %d MB\n", usedMemory));
        report.append(String.format("  ✅ 可用内存: %d MB\n", freeMemory));
        report.append(String.format("  ✅ 处理器数: %d\n", runtime.availableProcessors()));
        report.append("\n");
        
        // 总结
        report.append("═══════════════════════════════════\n");
        if (allChecksPass) {
            report.append("✅ 所有检查通过！Joder 系统健康。\n");
        } else {
            report.append("⚠️  发现一些问题，请检查上述警告。\n");
        }
        
        return CommandResult.success(report.toString());
    }
}
