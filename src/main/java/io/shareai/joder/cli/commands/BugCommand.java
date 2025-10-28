package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bug Command - Bug 报告命令
 * 生成 Bug 报告并保存到文件
 */
@Singleton
public class BugCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(BugCommand.class);
    
    private static final String BUG_DIR = ".joder/bugs";
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Override
    public String getDescription() {
        return "创建 Bug 报告";
    }
    
    @Override
    public String getUsage() {
        return "bug [描述] - 创建 Bug 报告";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            String description = args != null ? args.trim() : "";
            
            // 收集系统信息
            BugReport report = collectBugReport(description);
            
            // 保存到文件
            Path reportPath = saveBugReport(report);
            
            // 返回结果
            return CommandResult.success(formatBugReportResult(reportPath, report));
            
        } catch (Exception e) {
            logger.error("Failed to create bug report", e);
            return CommandResult.error("创建 Bug 报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 收集 Bug 报告信息
     */
    private BugReport collectBugReport(String description) {
        BugReport report = new BugReport();
        
        report.timestamp = LocalDateTime.now();
        report.description = description.isEmpty() ? "未提供描述" : description;
        
        // 系统信息
        report.osName = System.getProperty("os.name");
        report.osVersion = System.getProperty("os.version");
        report.javaVersion = System.getProperty("java.version");
        report.userDir = System.getProperty("user.dir");
        
        // 运行时信息
        Runtime runtime = Runtime.getRuntime();
        report.maxMemory = runtime.maxMemory();
        report.totalMemory = runtime.totalMemory();
        report.freeMemory = runtime.freeMemory();
        report.processors = runtime.availableProcessors();
        
        return report;
    }
    
    /**
     * 保存 Bug 报告
     */
    private Path saveBugReport(BugReport report) throws IOException {
        // 确保目录存在
        Path bugDir = Paths.get(BUG_DIR);
        Files.createDirectories(bugDir);
        
        // 生成文件名
        String fileName = "bug_" + report.timestamp.format(FORMATTER) + ".md";
        Path reportPath = bugDir.resolve(fileName);
        
        // 生成 Markdown 内容
        String content = generateMarkdownReport(report);
        
        // 保存文件
        Files.writeString(reportPath, content);
        
        logger.info("Bug report saved to: {}", reportPath);
        
        return reportPath;
    }
    
    /**
     * 生成 Markdown 报告
     */
    private String generateMarkdownReport(BugReport report) {
        StringBuilder md = new StringBuilder();
        
        md.append("# Bug 报告\n\n");
        
        // 基本信息
        md.append("## 基本信息\n\n");
        md.append("- **报告时间**: ").append(report.timestamp).append("\n");
        md.append("- **Bug 描述**: ").append(report.description).append("\n\n");
        
        // 系统环境
        md.append("## 系统环境\n\n");
        md.append("```\n");
        md.append("操作系统:    ").append(report.osName).append(" ").append(report.osVersion).append("\n");
        md.append("Java 版本:   ").append(report.javaVersion).append("\n");
        md.append("工作目录:    ").append(report.userDir).append("\n");
        md.append("处理器核心:  ").append(report.processors).append("\n");
        md.append("```\n\n");
        
        // 内存信息
        md.append("## 内存信息\n\n");
        md.append("```\n");
        md.append("最大内存:    ").append(formatBytes(report.maxMemory)).append("\n");
        md.append("已分配:      ").append(formatBytes(report.totalMemory)).append("\n");
        md.append("空闲内存:    ").append(formatBytes(report.freeMemory)).append("\n");
        md.append("已使用:      ").append(formatBytes(report.totalMemory - report.freeMemory)).append("\n");
        md.append("```\n\n");
        
        // 重现步骤
        md.append("## 重现步骤\n\n");
        md.append("1. \n");
        md.append("2. \n");
        md.append("3. \n\n");
        
        // 期望行为
        md.append("## 期望行为\n\n");
        md.append("（请描述期望的正确行为）\n\n");
        
        // 实际行为
        md.append("## 实际行为\n\n");
        md.append("（请描述实际发生的错误行为）\n\n");
        
        // 附加信息
        md.append("## 附加信息\n\n");
        md.append("（日志、截图、堆栈跟踪等）\n\n");
        
        // 生成时间
        md.append("---\n");
        md.append("*自动生成于 ").append(report.timestamp).append("*\n");
        
        return md.toString();
    }
    
    /**
     * 格式化 Bug 报告结果
     */
    private String formatBugReportResult(Path reportPath, BugReport report) {
        StringBuilder output = new StringBuilder();
        
        output.append("🐛 Bug 报告已创建\n\n");
        
        output.append("═══════════════════════════════════════\n");
        output.append("📋 报告信息\n");
        output.append("═══════════════════════════════════════\n\n");
        
        output.append("  文件位置: ").append(reportPath.toAbsolutePath()).append("\n");
        output.append("  创建时间: ").append(report.timestamp).append("\n");
        output.append("  描述: ").append(report.description).append("\n\n");
        
        output.append("═══════════════════════════════════════\n");
        output.append("📝 下一步\n");
        output.append("═══════════════════════════════════════\n\n");
        
        output.append("1. 编辑报告文件，填写详细信息\n");
        output.append("2. 添加重现步骤和截图\n");
        output.append("3. 提交到 GitHub Issues 或内部系统\n\n");
        
        output.append("💡 提示:\n");
        output.append("  - 使用 cat ").append(reportPath).append(" 查看报告\n");
        output.append("  - 报告保存在 .joder/bugs/ 目录\n");
        output.append("  - 可以附加日志文件和截图\n");
        
        return output.toString();
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
     * Bug 报告数据
     */
    private static class BugReport {
        LocalDateTime timestamp;
        String description;
        String osName;
        String osVersion;
        String javaVersion;
        String userDir;
        long maxMemory;
        long totalMemory;
        long freeMemory;
        int processors;
    }
}
