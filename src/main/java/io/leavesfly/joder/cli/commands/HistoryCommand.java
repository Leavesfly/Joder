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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * History Command - 会话历史命令
 * 显示和管理会话历史记录
 */
@Singleton
public class HistoryCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(HistoryCommand.class);
    
    private static final String HISTORY_DIR = ".joder/history";
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public String getDescription() {
        return "查看会话历史记录";
    }
    
    @Override
    public String getUsage() {
        return "history [--limit <n>] [--clear] - 管理会话历史";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            HistoryOptions options = parseArguments(args);
            
            if (options.clear) {
                return clearHistory();
            }
            
            return listHistory(options);
            
        } catch (Exception e) {
            logger.error("Failed to execute history command", e);
            return CommandResult.error("查看历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析命令参数
     */
    private HistoryOptions parseArguments(String args) {
        HistoryOptions options = new HistoryOptions();
        
        if (args == null || args.trim().isEmpty()) {
            return options;
        }
        
        String[] parts = args.trim().split("\\s+");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.equals("--limit") && i + 1 < parts.length) {
                try {
                    options.limit = Integer.parseInt(parts[++i]);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid limit value: {}", parts[i]);
                }
            } else if (part.equals("--clear")) {
                options.clear = true;
            }
        }
        
        return options;
    }
    
    /**
     * 列出历史记录
     */
    private CommandResult listHistory(HistoryOptions options) throws IOException {
        Path historyDir = Paths.get(HISTORY_DIR);
        
        if (!Files.exists(historyDir)) {
            return CommandResult.success(
                    "📜 会话历史\n\n" +
                    "ℹ️  暂无历史记录\n\n" +
                    "💡 提示: 会话记录将自动保存在 .joder/history/ 目录"
            );
        }
        
        // 扫描历史文件
        List<HistoryEntry> entries = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(historyDir, 1)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".json"))
                 .forEach(path -> {
                     try {
                         HistoryEntry entry = new HistoryEntry();
                         entry.path = path;
                         entry.timestamp = Files.getLastModifiedTime(path).toInstant();
                         entry.size = Files.size(path);
                         entries.add(entry);
                     } catch (IOException e) {
                         logger.warn("Failed to read history file: {}", path, e);
                     }
                 });
        }
        
        if (entries.isEmpty()) {
            return CommandResult.success(
                    "📜 会话历史\n\n" +
                    "ℹ️  暂无历史记录"
            );
        }
        
        // 排序（最新的在前）
        entries.sort(Comparator.comparing(e -> e.timestamp, Comparator.reverseOrder()));
        
        // 限制数量
        int displayCount = Math.min(entries.size(), options.limit);
        
        StringBuilder output = new StringBuilder();
        output.append("📜 会话历史记录\n\n");
        output.append(String.format("共 %d 条记录，显示最近 %d 条:\n\n", 
                entries.size(), displayCount));
        
        for (int i = 0; i < displayCount; i++) {
            HistoryEntry entry = entries.get(i);
            LocalDateTime dateTime = LocalDateTime.ofInstant(entry.timestamp, 
                    ZoneId.systemDefault());
            
            output.append(String.format("  %d. %s\n", 
                    i + 1, 
                    FORMATTER.format(dateTime)));
            output.append(String.format("     文件: %s\n", 
                    entry.path.getFileName()));
            output.append(String.format("     大小: %s\n\n", 
                    formatFileSize(entry.size)));
        }
        
        output.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        output.append("💡 提示:\n");
        output.append("  /history --limit 10  - 显示最近 10 条\n");
        output.append("  /history --clear     - 清除所有历史\n");
        output.append("  /export              - 导出当前会话\n");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 清除历史记录
     */
    private CommandResult clearHistory() throws IOException {
        Path historyDir = Paths.get(HISTORY_DIR);
        
        if (!Files.exists(historyDir)) {
            return CommandResult.success("ℹ️  历史目录不存在，无需清除");
        }
        
        int deletedCount = 0;
        
        try (Stream<Path> paths = Files.walk(historyDir, 1)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                try {
                    Files.delete(path);
                    deletedCount++;
                } catch (IOException e) {
                    logger.warn("Failed to delete history file: {}", path, e);
                }
            }
        }
        
        return CommandResult.success(
                String.format("✅ 已清除 %d 条历史记录", deletedCount)
        );
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
     * 历史条目
     */
    private static class HistoryEntry {
        Path path;
        Instant timestamp;
        long size;
    }
    
    /**
     * 命令选项
     */
    private static class HistoryOptions {
        int limit = 20;
        boolean clear = false;
    }
}
