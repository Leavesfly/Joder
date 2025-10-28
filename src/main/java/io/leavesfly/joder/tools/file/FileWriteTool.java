package io.leavesfly.joder.tools.file;

import io.leavesfly.joder.tools.AbstractTool;
import io.leavesfly.joder.tools.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * 文件写入工具
 */
public class FileWriteTool extends AbstractTool {
    
    @Override
    public String getName() {
        return "FileWriteTool";
    }
    
    @Override
    public String getDescription() {
        return "创建或覆盖写入文件";
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        try {
            // 验证必需参数
            requireParameter(input, "path");
            requireParameter(input, "content");
            
            String pathStr = getString(input, "path");
            String content = getString(input, "content");
            
            // 解析路径
            Path path = Paths.get(pathStr);
            
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 写入文件
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // 构建输出
            StringBuilder output = new StringBuilder();
            output.append("文件写入成功\n");
            output.append("路径: ").append(pathStr).append("\n");
            output.append("大小: ").append(content.length()).append(" 字符\n");
            output.append("行数: ").append(content.split("\n").length).append("\n");
            
            return ToolResult.success(output.toString());
            
        } catch (IOException e) {
            logger.error("Failed to write file", e);
            return ToolResult.error("写入文件失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in FileWriteTool", e);
            return ToolResult.error("发生错误: " + e.getMessage());
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String path = getString(input, "path");
        String content = getString(input, "content", "");
        int lineCount = content.split("\n").length;
        
        return String.format("▶ 写入文件: %s (%d 行)", path, lineCount);
    }
}
