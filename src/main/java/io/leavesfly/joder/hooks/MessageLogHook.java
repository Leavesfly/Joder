package io.leavesfly.joder.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 消息日志钩子
 * 对应 Kode 的 useLogMessages hook
 */
@Singleton
public class MessageLogHook {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageLogHook.class);
    private final ObjectMapper objectMapper;
    private final Path logDir;
    
    public MessageLogHook() {
        this.objectMapper = new ObjectMapper();
        String homeDir = System.getProperty("user.home");
        this.logDir = Paths.get(homeDir, ".local", "share", "joder", "logs", "messages");
        
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            logger.error("创建日志目录失败", e);
        }
    }
    
    /**
     * 记录消息
     */
    public void logMessages(List<Message> messages, String sessionName, int forkNumber) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        try {
            Path logFile = getLogPath(sessionName, forkNumber);
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(messages);
            
            Files.writeString(
                logFile,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            
            logger.debug("消息已记录到: {}", logFile);
            
        } catch (IOException e) {
            logger.error("记录消息失败", e);
        }
    }
    
    /**
     * 追加消息
     */
    public void appendMessage(Message message, String sessionName, int forkNumber) {
        try {
            Path logFile = getLogPath(sessionName, forkNumber);
            String json = objectMapper.writeValueAsString(message) + "\n";
            
            Files.writeString(
                logFile,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
            
        } catch (IOException e) {
            logger.error("追加消息失败", e);
        }
    }
    
    /**
     * 读取消息日志
     */
    public List<Message> readMessages(String sessionName, int forkNumber) {
        try {
            Path logFile = getLogPath(sessionName, forkNumber);
            
            if (!Files.exists(logFile)) {
                return List.of();
            }
            
            String json = Files.readString(logFile);
            return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Message.class)
            );
            
        } catch (IOException e) {
            logger.error("读取消息日志失败", e);
            return List.of();
        }
    }
    
    /**
     * 获取日志文件路径
     */
    private Path getLogPath(String sessionName, int forkNumber) {
        String filename = String.format("%s_fork%d.json", sessionName, forkNumber);
        return logDir.resolve(filename);
    }
    
    /**
     * 清理旧日志
     */
    public void cleanOldLogs(int daysToKeep) {
        try {
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);
            
            Files.list(logDir)
                .filter(Files::isRegularFile)
                .filter(file -> {
                    try {
                        return Files.getLastModifiedTime(file).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(file -> {
                    try {
                        Files.delete(file);
                        logger.debug("已删除旧日志: {}", file);
                    } catch (IOException e) {
                        logger.error("删除日志失败: {}", file, e);
                    }
                });
                
        } catch (IOException e) {
            logger.error("清理旧日志失败", e);
        }
    }
}
