package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listen Command - 文件监听命令
 * 监听文件变化并自动触发分析
 */
@Singleton
public class ListenCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(ListenCommand.class);
    
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private ExecutorService executor;
    private WatchService watchService;
    
    @Override
    public String getDescription() {
        return "监听文件变化并自动分析";
    }
    
    @Override
    public String getUsage() {
        return "listen [目录] [--stop] - 开始/停止文件监听";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            // 解析参数
            ListenOptions options = parseArguments(args);
            
            if (options.stop) {
                return stopListening();
            }
            
            return startListening(options);
            
        } catch (Exception e) {
            logger.error("Failed to execute listen command", e);
            return CommandResult.error("文件监听失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析命令参数
     */
    private ListenOptions parseArguments(String args) {
        ListenOptions options = new ListenOptions();
        
        if (args == null || args.trim().isEmpty()) {
            return options;
        }
        
        String[] parts = args.trim().split("\\s+");
        
        for (String part : parts) {
            if (part.equals("--stop")) {
                options.stop = true;
            } else if (!part.startsWith("--")) {
                options.directory = part;
            }
        }
        
        return options;
    }
    
    /**
     * 开始监听
     */
    private CommandResult startListening(ListenOptions options) throws IOException {
        if (isListening.get()) {
            return CommandResult.error("文件监听已在运行，使用 /listen --stop 停止");
        }
        
        Path watchPath = Paths.get(options.directory);
        
        if (!Files.exists(watchPath)) {
            return CommandResult.error("目录不存在: " + options.directory);
        }
        
        if (!Files.isDirectory(watchPath)) {
            return CommandResult.error("不是有效的目录: " + options.directory);
        }
        
        // 创建 WatchService
        watchService = FileSystems.getDefault().newWatchService();
        
        // 注册目录
        watchPath.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        
        // 启动监听线程
        executor = Executors.newSingleThreadExecutor();
        isListening.set(true);
        
        executor.submit(() -> {
            try {
                watchLoop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("File watching interrupted");
            }
        });
        
        StringBuilder output = new StringBuilder();
        output.append("🎧 文件监听已启动\n\n");
        output.append("监听目录: ").append(watchPath.toAbsolutePath()).append("\n");
        output.append("监听事件: 创建、修改、删除\n\n");
        output.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        output.append("💡 提示:\n");
        output.append("  - 文件变化将实时显示\n");
        output.append("  - 使用 /listen --stop 停止监听\n");
        output.append("  - 监听在后台运行，不影响其他操作\n");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 停止监听
     */
    private CommandResult stopListening() {
        if (!isListening.get()) {
            return CommandResult.error("文件监听未在运行");
        }
        
        try {
            isListening.set(false);
            
            if (watchService != null) {
                watchService.close();
            }
            
            if (executor != null) {
                executor.shutdownNow();
            }
            
            return CommandResult.success("✅ 文件监听已停止");
            
        } catch (IOException e) {
            logger.error("Failed to stop watch service", e);
            return CommandResult.error("停止监听失败: " + e.getMessage());
        }
    }
    
    /**
     * 监听循环
     */
    private void watchLoop() throws InterruptedException {
        while (isListening.get()) {
            WatchKey key = watchService.take();
            
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                
                handleFileEvent(kind, fileName);
            }
            
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
    
    /**
     * 处理文件事件
     */
    private void handleFileEvent(WatchEvent.Kind<?> kind, Path fileName) {
        String eventType;
        String emoji;
        
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            eventType = "创建";
            emoji = "➕";
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            eventType = "修改";
            emoji = "✏️";
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            eventType = "删除";
            emoji = "🗑️";
        } else {
            return;
        }
        
        // 过滤临时文件和隐藏文件
        String name = fileName.toString();
        if (name.startsWith(".") || name.endsWith("~") || name.endsWith(".swp")) {
            return;
        }
        
        logger.info("File {}: {}", eventType, fileName);
        
        // 在控制台输出（通过日志）
        System.out.println(String.format("%s 文件%s: %s", emoji, eventType, fileName));
    }
    
    /**
     * 监听选项
     */
    private static class ListenOptions {
        String directory = ".";
        boolean stop = false;
    }
}
