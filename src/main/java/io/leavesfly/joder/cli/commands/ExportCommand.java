package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
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
 * Export Command - 导出会话命令
 * 导出当前会话到文件
 */
@Singleton
public class ExportCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportCommand.class);
    
    private static final String EXPORT_DIR = ".joder/exports";
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Override
    public String getDescription() {
        return "导出当前会话到文件";
    }
    
    @Override
    public String getUsage() {
        return "export [文件名] [--format <json|md>] - 导出会话";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            ExportOptions options = parseArguments(args);
            
            // 生成导出文件名
            String fileName = generateFileName(options);
            Path exportPath = Paths.get(EXPORT_DIR, fileName);
            
            // 确保目录存在
            Files.createDirectories(exportPath.getParent());
            
            // 导出内容
            String content = generateExportContent(options);
            Files.writeString(exportPath, content);
            
            return CommandResult.success(formatSuccessMessage(exportPath, options));
            
        } catch (Exception e) {
            logger.error("Failed to export session", e);
            return CommandResult.error("导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析命令参数
     */
    private ExportOptions parseArguments(String args) {
        ExportOptions options = new ExportOptions();
        
        if (args == null || args.trim().isEmpty()) {
            return options;
        }
        
        String[] parts = args.trim().split("\\s+");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.equals("--format") && i + 1 < parts.length) {
                String format = parts[++i].toLowerCase();
                if (format.equals("json") || format.equals("md") || format.equals("markdown")) {
                    options.format = format.equals("md") || format.equals("markdown") ? "md" : "json";
                }
            } else if (!part.startsWith("--") && options.fileName == null) {
                options.fileName = part;
            }
        }
        
        return options;
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(ExportOptions options) {
        if (options.fileName != null && !options.fileName.trim().isEmpty()) {
            String name = options.fileName.trim();
            // 确保有正确的扩展名
            if (!name.endsWith("." + options.format)) {
                name += "." + options.format;
            }
            return name;
        }
        
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return "session_" + timestamp + "." + options.format;
    }
    
    /**
     * 生成导出内容
     */
    private String generateExportContent(ExportOptions options) {
        if (options.format.equals("md")) {
            return generateMarkdownExport();
        } else {
            return generateJsonExport();
        }
    }
    
    /**
     * 生成 Markdown 导出
     */
    private String generateMarkdownExport() {
        StringBuilder md = new StringBuilder();
        
        md.append("# Joder 会话导出\n\n");
        md.append("**导出时间**: ").append(LocalDateTime.now()).append("\n\n");
        md.append("---\n\n");
        
        md.append("## 会话信息\n\n");
        md.append("- 项目: ").append(System.getProperty("user.dir")).append("\n");
        md.append("- 用户: ").append(System.getProperty("user.name")).append("\n\n");
        
        md.append("---\n\n");
        
        md.append("## 会话内容\n\n");
        md.append("> 注意: 当前为示例导出格式\n\n");
        md.append("### 对话记录\n\n");
        md.append("（会话消息将在此显示）\n\n");
        
        md.append("---\n\n");
        
        md.append("## 使用的工具\n\n");
        md.append("- FileRead\n");
        md.append("- FileWrite\n");
        md.append("- Bash\n\n");
        
        md.append("---\n\n");
        md.append("_导出时间: ").append(LocalDateTime.now()).append("_\n");
        
        return md.toString();
    }
    
    /**
     * 生成 JSON 导出
     */
    private String generateJsonExport() {
        return String.format("""
                {
                  "exportedAt": "%s",
                  "project": "%s",
                  "user": "%s",
                  "session": {
                    "messages": [],
                    "tools": ["FileRead", "FileWrite", "Bash"],
                    "metadata": {}
                  },
                  "note": "This is a sample export format"
                }
                """,
                LocalDateTime.now(),
                System.getProperty("user.dir"),
                System.getProperty("user.name")
        );
    }
    
    /**
     * 格式化成功消息
     */
    private String formatSuccessMessage(Path exportPath, ExportOptions options) {
        StringBuilder message = new StringBuilder();
        
        message.append("✅ 会话导出成功！\n\n");
        message.append("📄 文件: ").append(exportPath.toAbsolutePath()).append("\n");
        message.append("📊 格式: ").append(options.format.toUpperCase()).append("\n");
        
        try {
            long size = Files.size(exportPath);
            message.append("💾 大小: ").append(formatFileSize(size)).append("\n");
        } catch (IOException e) {
            // 忽略
        }
        
        message.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        message.append("💡 提示:\n");
        message.append("  - 使用 /export myfile.md 指定文件名\n");
        message.append("  - 使用 /export --format md 指定格式\n");
        message.append("  - 支持格式: json, md\n");
        
        return message.toString();
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 导出选项
     */
    private static class ExportOptions {
        String fileName = null;
        String format = "json";  // json 或 md
    }
}
