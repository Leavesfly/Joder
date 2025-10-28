package io.leavesfly.joder.tools.memory;

import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

/**
 * Memory Tool - 记忆管理工具
 * 
 * 用于存储和检索跨会话的记忆信息
 */
@Singleton
public class MemoryTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryTool.class);
    private static final String MEMORY_DIR = System.getProperty("user.home") + "/.joder/memory";
    
    @Override
    public String getName() {
        return "Memory";
    }
    
    @Override
    public String getDescription() {
        return "存储和检索跨会话的记忆信息。\n" +
               "- 写入记忆到文件\n" +
               "- 读取已保存的记忆\n" +
               "- 列出所有记忆\n" +
               "- 删除记忆";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具管理跨会话的记忆。\n\n" +
               "**操作类型**：\n" +
               "- write: 写入记忆\n" +
               "- read: 读取记忆\n" +
               "- list: 列出所有记忆\n" +
               "- delete: 删除记忆\n\n" +
               "**参数**：\n" +
               "- operation: 操作类型（必需）\n" +
               "- key: 记忆的键名（write/read/delete 时必需）\n" +
               "- content: 记忆内容（write 时必需）\n\n" +
               "**适用场景**：\n" +
               "- 保存用户偏好\n" +
               "- 记录项目信息\n" +
               "- 存储常用命令\n" +
               "- 保存上下文信息\n\n" +
               "**存储位置**：~/.joder/memory/\n\n" +
               "**示例**：\n" +
               "```json\n" +
               "// 写入记忆\n" +
               "{\n" +
               "  \"operation\": \"write\",\n" +
               "  \"key\": \"project_settings\",\n" +
               "  \"content\": \"使用 Java 17, Maven 构建, Spring Boot 3.0\"\n" +
               "}\n\n" +
               "// 读取记忆\n" +
               "{\n" +
               "  \"operation\": \"read\",\n" +
               "  \"key\": \"project_settings\"\n" +
               "}\n\n" +
               "// 列出所有记忆\n" +
               "{\n" +
               "  \"operation\": \"list\"\n" +
               "}\n" +
               "```";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return false; // 可以写入
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return false; // 文件操作不安全并发
    }
    
    @Override
    public boolean needsPermissions() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String operation = (String) input.get("operation");
        String key = (String) input.get("key");
        String content = (String) input.get("content");
        
        // 验证操作类型
        if (operation == null || operation.trim().isEmpty()) {
            return ToolResult.error("操作类型不能为空");
        }
        
        try {
            // 确保记忆目录存在
            ensureMemoryDir();
            
            return switch (operation.toLowerCase()) {
                case "write" -> writeMemory(key, content);
                case "read" -> readMemory(key);
                case "list" -> listMemories();
                case "delete" -> deleteMemory(key);
                default -> ToolResult.error("不支持的操作: " + operation);
            };
            
        } catch (Exception e) {
            logger.error("记忆操作失败", e);
            return ToolResult.error("操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保记忆目录存在
     */
    private void ensureMemoryDir() throws IOException {
        Path memoryPath = Paths.get(MEMORY_DIR);
        if (!Files.exists(memoryPath)) {
            Files.createDirectories(memoryPath);
            logger.info("创建记忆目录: {}", MEMORY_DIR);
        }
    }
    
    /**
     * 写入记忆
     */
    private ToolResult writeMemory(String key, String content) {
        if (key == null || key.trim().isEmpty()) {
            return ToolResult.error("键名不能为空");
        }
        
        if (content == null) {
            return ToolResult.error("内容不能为空");
        }
        
        try {
            // 验证键名安全性
            if (!isValidKey(key)) {
                return ToolResult.error("无效的键名: " + key);
            }
            
            Path memoryFile = Paths.get(MEMORY_DIR, key + ".txt");
            Files.writeString(memoryFile, content);
            
            logger.info("写入记忆: {}", key);
            return ToolResult.success(String.format("✅ 记忆已保存: %s\n内容长度: %d 字符", key, content.length()));
            
        } catch (IOException e) {
            logger.error("写入记忆失败: {}", key, e);
            return ToolResult.error("写入失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取记忆
     */
    private ToolResult readMemory(String key) {
        if (key == null || key.trim().isEmpty()) {
            return ToolResult.error("键名不能为空");
        }
        
        try {
            if (!isValidKey(key)) {
                return ToolResult.error("无效的键名: " + key);
            }
            
            Path memoryFile = Paths.get(MEMORY_DIR, key + ".txt");
            
            if (!Files.exists(memoryFile)) {
                return ToolResult.error("记忆不存在: " + key);
            }
            
            String content = Files.readString(memoryFile);
            logger.info("读取记忆: {}", key);
            
            return ToolResult.success(String.format("📖 记忆内容 (%s):\n\n%s", key, content));
            
        } catch (IOException e) {
            logger.error("读取记忆失败: {}", key, e);
            return ToolResult.error("读取失败: " + e.getMessage());
        }
    }
    
    /**
     * 列出所有记忆
     */
    private ToolResult listMemories() {
        try {
            Path memoryPath = Paths.get(MEMORY_DIR);
            
            if (!Files.exists(memoryPath)) {
                return ToolResult.success("📋 没有保存的记忆");
            }
            
            StringBuilder list = new StringBuilder();
            list.append("📋 已保存的记忆:\n\n");
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(memoryPath, "*.txt")) {
                int count = 0;
                for (Path entry : stream) {
                    String fileName = entry.getFileName().toString();
                    String key = fileName.substring(0, fileName.lastIndexOf('.'));
                    long size = Files.size(entry);
                    
                    list.append(String.format("• %s (%d 字节)\n", key, size));
                    count++;
                }
                
                if (count == 0) {
                    return ToolResult.success("📋 没有保存的记忆");
                }
                
                list.append(String.format("\n共 %d 个记忆", count));
            }
            
            return ToolResult.success(list.toString());
            
        } catch (IOException e) {
            logger.error("列出记忆失败", e);
            return ToolResult.error("列出失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除记忆
     */
    private ToolResult deleteMemory(String key) {
        if (key == null || key.trim().isEmpty()) {
            return ToolResult.error("键名不能为空");
        }
        
        try {
            if (!isValidKey(key)) {
                return ToolResult.error("无效的键名: " + key);
            }
            
            Path memoryFile = Paths.get(MEMORY_DIR, key + ".txt");
            
            if (!Files.exists(memoryFile)) {
                return ToolResult.error("记忆不存在: " + key);
            }
            
            Files.delete(memoryFile);
            logger.info("删除记忆: {}", key);
            
            return ToolResult.success("✅ 记忆已删除: " + key);
            
        } catch (IOException e) {
            logger.error("删除记忆失败: {}", key, e);
            return ToolResult.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证键名是否安全
     */
    private boolean isValidKey(String key) {
        // 只允许字母、数字、下划线、连字符
        return key.matches("[a-zA-Z0-9_-]+");
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String operation = (String) input.get("operation");
        String key = (String) input.get("key");
        
        return switch (operation != null ? operation.toLowerCase() : "") {
            case "write" -> String.format("写入记忆: %s", key);
            case "read" -> String.format("读取记忆: %s", key);
            case "list" -> "列出所有记忆";
            case "delete" -> String.format("删除记忆: %s", key);
            default -> "记忆操作: " + operation;
        };
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 记忆操作完成";
        } else {
            return "❌ 记忆操作失败: " + result.getError();
        }
    }
}

