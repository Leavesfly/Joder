package io.leavesfly.joder.tools.grep;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Grep Tool - 文件内容搜索工具
 * 使用正则表达式搜索文件内容
 */
public class GrepTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(GrepTool.class);
    private static final int MAX_RESULTS = 100;
    
    private final String workingDirectory;
    
    @Inject
    public GrepTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public String getName() {
        return "Grep";
    }
    
    @Override
    public String getDescription() {
        return "快速内容搜索工具，适用于任意规模的代码库。\n" +
               "- 使用正则表达式搜索文件内容\n" +
               "- 支持完整的正则语法（如 \"log.*Error\", \"function\\\\s+\\\\w+\" 等）\n" +
               "- 使用 include 参数按模式过滤文件（如 \"*.java\", \"*.{ts,tsx}\"）\n" +
               "- 返回按修改时间排序的匹配文件路径\n" +
               "- 当需要查找包含特定模式的文件时使用此工具\n" +
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
        return true; // GrepTool 是只读的，支持并发执行
    }
    
    @Override
    public boolean needsPermissions() {
        return false; // 内容搜索通常不需要特殊权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String patternStr = (String) input.get("pattern");
        String pathStr = (String) input.get("path");
        String includePattern = (String) input.get("include");
        
        if (patternStr == null || patternStr.trim().isEmpty()) {
            return ToolResult.error("搜索模式不能为空");
        }
        
        // 编译正则表达式
        Pattern searchPattern;
        try {
            searchPattern = Pattern.compile(patternStr);
        } catch (PatternSyntaxException e) {
            return ToolResult.error("无效的正则表达式: " + e.getMessage());
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
            // 编译 include 模式（如果提供）
            PathMatcher includeMatcher = null;
            if (includePattern != null && !includePattern.trim().isEmpty()) {
                includeMatcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + includePattern);
            }
            
            List<FileMatch> matches = searchFiles(searchPattern, searchPath, includeMatcher);
            
            // 按修改时间降序排序（最新的在前）
            matches.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
            
            boolean truncated = matches.size() > MAX_RESULTS;
            int resultCount = Math.min(matches.size(), MAX_RESULTS);
            
            long duration = System.currentTimeMillis() - start;
            
            // 构建结果
            StringBuilder result = new StringBuilder();
            Path cwd = Paths.get(workingDirectory);
            
            if (matches.isEmpty()) {
                result.append("未找到匹配的文件");
            } else {
                result.append(String.format("找到 %d 个文件\n", matches.size()));
                
                for (int i = 0; i < resultCount; i++) {
                    FileMatch match = matches.get(i);
                    String relativePath = cwd.relativize(match.path).toString();
                    result.append(relativePath).append("\n");
                }
                
                if (truncated) {
                    result.append("\n(结果已截断，考虑使用更具体的路径或模式)");
                }
            }
            
            // 添加元数据
            String metadata = String.format("\n\n匹配文件数: %d (耗时: %dms)", 
                matches.size(), duration);
            
            return ToolResult.success(result.toString().trim() + metadata);
            
        } catch (IOException e) {
            logger.error("Grep 搜索失败: pattern={}, path={}", patternStr, searchPath, e);
            return ToolResult.error("搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索文件内容
     */
    private List<FileMatch> searchFiles(Pattern searchPattern, Path searchPath, 
                                       PathMatcher includeMatcher) throws IOException {
        List<FileMatch> matches = new ArrayList<>();
        
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
                
                // 检查 include 过滤
                if (includeMatcher != null) {
                    Path relativePath = searchPath.relativize(file);
                    if (!includeMatcher.matches(relativePath) && 
                        !includeMatcher.matches(file.getFileName())) {
                        return FileVisitResult.CONTINUE;
                    }
                }
                
                // 只搜索文本文件
                if (!isTextFile(file)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // 搜索文件内容
                if (fileContainsPattern(file, searchPattern)) {
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
     * 检查文件内容是否包含模式
     */
    private boolean fileContainsPattern(Path file, Pattern pattern) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.debug("读取文件失败: {}", file, e);
        } catch (Exception e) {
            // 可能是二进制文件，跳过
            logger.debug("解析文件失败（可能是二进制文件）: {}", file);
        }
        return false;
    }
    
    /**
     * 简单判断是否为文本文件
     */
    private boolean isTextFile(Path file) {
        String name = file.getFileName().toString();
        String lowerName = name.toLowerCase();
        
        // 常见文本文件扩展名
        String[] textExtensions = {
            ".txt", ".md", ".java", ".js", ".ts", ".tsx", ".jsx",
            ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h", ".hpp",
            ".cs", ".php", ".html", ".css", ".scss", ".sass", ".less",
            ".xml", ".json", ".yaml", ".yml", ".toml", ".ini", ".conf",
            ".sh", ".bash", ".zsh", ".properties", ".gradle", ".maven"
        };
        
        for (String ext : textExtensions) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        
        // 无扩展名的常见文本文件
        String[] textFiles = {
            "readme", "license", "makefile", "dockerfile", 
            "gemfile", "rakefile", "gradlew"
        };
        
        for (String textFile : textFiles) {
            if (lowerName.equals(textFile)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否应该跳过该路径
     */
    private boolean shouldSkip(Path path) {
        String name = path.getFileName().toString();
        
        // 跳过隐藏文件和目录
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
        String includePattern = (String) input.get("include");
        
        StringBuilder msg = new StringBuilder();
        msg.append("搜索内容: \"").append(pattern).append("\"");
        
        if (pathStr != null && !pathStr.isEmpty()) {
            Path searchPath = Paths.get(pathStr);
            if (!searchPath.isAbsolute()) {
                searchPath = Paths.get(workingDirectory).resolve(pathStr);
            }
            Path cwd = Paths.get(workingDirectory);
            String relativePath = cwd.relativize(searchPath).toString();
            msg.append(", 路径: \"").append(relativePath.isEmpty() ? "." : relativePath).append("\"");
        }
        
        if (includePattern != null && !includePattern.isEmpty()) {
            msg.append(", 文件过滤: \"").append(includePattern).append("\"");
        }
        
        return msg.toString();
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            String output = result.getOutput();
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
