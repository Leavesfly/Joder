package io.shareai.joder.tools.glob;

import io.shareai.joder.WorkingDirectory;
import io.shareai.joder.tools.Tool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Glob Tool - 文件模式匹配工具
 * 支持 glob 模式快速查找文件
 */
public class GlobTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobTool.class);
    private static final int DEFAULT_LIMIT = 100;
    
    private final String workingDirectory;
    
    @Inject
    public GlobTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public String getName() {
        return "Glob";
    }
    
    @Override
    public String getDescription() {
        return "快速文件模式匹配工具，适用于任意规模的代码库。\n" +
               "- 支持 glob 模式如 \"**/*.java\" 或 \"src/**/*.txt\"\n" +
               "- 返回按修改时间排序的匹配文件路径\n" +
               "- 当需要按名称模式查找文件时使用此工具\n" +
               "- 对于需要多轮 glob 和 grep 的开放式搜索，建议使用 Task 工具";
    }
    
    @Override
    public String getPrompt() {
        return getDescription();
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true; // GlobTool 是只读的，支持并发执行
    }
    
    @Override
    public boolean needsPermissions() {
        return false; // 文件搜索通常不需要特殊权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String pattern = (String) input.get("pattern");
        String pathStr = (String) input.get("path");
        
        if (pattern == null || pattern.trim().isEmpty()) {
            return ToolResult.error("模式参数不能为空");
        }
        
        Path searchPath;
        if (pathStr != null && !pathStr.trim().isEmpty()) {
            searchPath = Paths.get(pathStr);
            if (!searchPath.isAbsolute()) {
                searchPath = Paths.get(workingDirectory).resolve(pathStr).normalize();
            }
        } else {
            searchPath = Paths.get(workingDirectory);
        }
        
        if (!Files.exists(searchPath)) {
            return ToolResult.error("路径不存在: " + searchPath);
        }
        
        if (!Files.isDirectory(searchPath)) {
            return ToolResult.error("路径不是目录: " + searchPath);
        }
        
        long start = System.currentTimeMillis();
        
        try {
            List<FileMatch> matches = findMatches(pattern, searchPath);
            
            // 按修改时间降序排序（最新的在前）
            matches.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
            
            boolean truncated = matches.size() > DEFAULT_LIMIT;
            if (truncated) {
                matches = matches.subList(0, DEFAULT_LIMIT);
            }
            
            long duration = System.currentTimeMillis() - start;
            
            // 构建结果
            StringBuilder result = new StringBuilder();
            Path cwd = Paths.get(workingDirectory);
            
            if (matches.isEmpty()) {
                result.append("未找到匹配的文件");
            } else {
                for (FileMatch match : matches) {
                    String relativePath = cwd.relativize(match.path).toString();
                    result.append(relativePath).append("\n");
                }
                
                if (truncated) {
                    result.append("\n(结果已截断，考虑使用更具体的路径或模式)");
                }
            }
            
            // 添加元数据
            String metadata = String.format("\n\n找到 %d 个文件 (耗时: %dms)", 
                matches.size(), duration);
            
            return ToolResult.success(result.toString().trim() + metadata);
            
        } catch (IOException e) {
            logger.error("Glob 搜索失败: pattern={}, path={}", pattern, searchPath, e);
            return ToolResult.error("搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找匹配的文件
     */
    private List<FileMatch> findMatches(String globPattern, Path searchPath) throws IOException {
        List<FileMatch> matches = new ArrayList<>();
        
        // 将 glob 模式转换为 PathMatcher
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
        
        // 遍历目录树
        Files.walkFileTree(searchPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (shouldSkip(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (shouldSkip(file)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // 获取相对路径进行匹配
                Path relativePath = searchPath.relativize(file);
                
                // 同时匹配完整路径和文件名
                if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                    FileMatch match = new FileMatch();
                    match.path = file;
                    match.lastModified = attrs.lastModifiedTime().toMillis();
                    matches.add(match);
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("访问文件失败: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return matches;
    }
    
    /**
     * 判断是否应该跳过该路径
     */
    private boolean shouldSkip(Path path) {
        String name = path.getFileName().toString();
        
        // 跳过隐藏文件和目录（以 . 开头）
        if (!name.equals(".") && name.startsWith(".")) {
            return true;
        }
        
        // 跳过常见的构建和依赖目录
        if (name.equals("node_modules") || 
            name.equals("target") || 
            name.equals("build") ||
            name.equals("dist") ||
            name.equals("__pycache__")) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String pattern = (String) input.get("pattern");
        String pathStr = (String) input.get("path");
        
        StringBuilder msg = new StringBuilder();
        msg.append("搜索文件: \"").append(pattern).append("\"");
        
        if (pathStr != null && !pathStr.isEmpty()) {
            Path searchPath = Paths.get(pathStr);
            if (!searchPath.isAbsolute()) {
                searchPath = Paths.get(workingDirectory).resolve(pathStr);
            }
            Path cwd = Paths.get(workingDirectory);
            String relativePath = cwd.relativize(searchPath).toString();
            msg.append(", 路径: \"").append(relativePath.isEmpty() ? "." : relativePath).append("\"");
        }
        
        return msg.toString();
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            String output = result.getOutput();
            // 从输出中提取文件数量
            if (output.contains("找到")) {
                int start = output.indexOf("找到 ") + 3;
                int end = output.indexOf(" 个文件", start);
                if (start > 0 && end > start) {
                    String countStr = output.substring(start, end);
                    return String.format("✅ 找到 %s 个文件", countStr);
                }
            }
            return "✅ 搜索完成";
        } else {
            return "❌ 搜索失败: " + result.getError();
        }
    }
    
    /**
     * 文件匹配结果
     */
    private static class FileMatch {
        Path path;
        long lastModified;
    }
}
