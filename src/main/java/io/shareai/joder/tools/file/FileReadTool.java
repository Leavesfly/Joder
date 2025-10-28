package io.shareai.joder.tools.file;

import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 文件读取工具
 */
public class FileReadTool extends AbstractTool {
    
    @Override
    public String getName() {
        return "FileReadTool";
    }
    
    @Override
    public String getDescription() {
        return "读取文件内容，支持指定行范围";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        try {
            // 验证必需参数
            requireParameter(input, "path");
            
            String pathStr = getString(input, "path");
            Integer startLine = getInt(input, "startLine");
            Integer endLine = getInt(input, "endLine");
            
            // 解析路径
            Path path = Paths.get(pathStr);
            
            // 检查文件是否存在
            if (!Files.exists(path)) {
                return ToolResult.error("文件不存在: " + pathStr);
            }
            
            // 检查是否为文件
            if (!Files.isRegularFile(path)) {
                return ToolResult.error("不是一个文件: " + pathStr);
            }
            
            // 读取文件内容
            List<String> lines = Files.readAllLines(path);
            
            // 应用行范围过滤
            if (startLine != null || endLine != null) {
                int start = startLine != null ? Math.max(0, startLine) : 0;
                int end = endLine != null ? Math.min(lines.size(), endLine) : lines.size();
                
                if (start >= lines.size()) {
                    return ToolResult.error("起始行号超出文件范围");
                }
                
                lines = lines.subList(start, end);
            }
            
            // 构建输出
            StringBuilder output = new StringBuilder();
            output.append("文件: ").append(pathStr).append("\n");
            output.append("总行数: ").append(lines.size()).append("\n");
            output.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            int lineNumber = startLine != null ? startLine : 0;
            for (String line : lines) {
                output.append(String.format("%4d: %s\n", lineNumber + 1, line));
                lineNumber++;
            }
            
            return ToolResult.success(output.toString());
            
        } catch (IOException e) {
            logger.error("Failed to read file", e);
            return ToolResult.error("读取文件失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in FileReadTool", e);
            return ToolResult.error("发生错误: " + e.getMessage());
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String path = getString(input, "path");
        Integer startLine = getInt(input, "startLine");
        Integer endLine = getInt(input, "endLine");
        
        StringBuilder sb = new StringBuilder();
        sb.append("▶ 读取文件: ").append(path);
        
        if (startLine != null || endLine != null) {
            sb.append(" (");
            if (startLine != null) {
                sb.append("起始行: ").append(startLine);
            }
            if (endLine != null) {
                if (startLine != null) {
                    sb.append(", ");
                }
                sb.append("结束行: ").append(endLine);
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
}
