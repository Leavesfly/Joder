package io.shareai.joder.services.completion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件补全提供者
 * 提供 @file: 文件引用的补全
 */
public class FileCompletionProvider implements CompletionProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(FileCompletionProvider.class);
    private static final String PREFIX = "@file";
    private static final int MAX_RESULTS = 20;
    private static final int MAX_DEPTH = 5;
    
    private final Path workingDirectory;
    
    @Inject
    public FileCompletionProvider(@io.shareai.joder.WorkingDirectory String workingDir) {
        this.workingDirectory = Paths.get(workingDir);
    }
    
    @Override
    public String getName() {
        return "FileCompletionProvider";
    }
    
    @Override
    public int getPriority() {
        return 90;  // 高优先级
    }
    
    @Override
    public boolean supports(String input, int cursorPosition) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String prefix = CompletionManager.extractPrefix(input, cursorPosition);
        return prefix.startsWith("@file");
    }
    
    @Override
    public List<CompletionSuggestion> getCompletions(String input, int cursorPosition) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        
        String prefix = CompletionManager.extractPrefix(input, cursorPosition);
        String query = extractFileQuery(prefix);
        
        try {
            // 搜索匹配的文件
            searchFiles(workingDirectory, query, suggestions);
        } catch (IOException e) {
            logger.error("Failed to search files", e);
        }
        
        return suggestions;
    }
    
    /**
     * 提取文件查询部分
     */
    private String extractFileQuery(String prefix) {
        return prefix.toLowerCase()
                .replace("@file:", "")
                .replace("@file", "")
                .trim();
    }
    
    /**
     * 搜索文件
     */
    private void searchFiles(Path directory, String query, List<CompletionSuggestion> suggestions) 
            throws IOException {
        
        if (suggestions.size() >= MAX_RESULTS) {
            return;
        }
        
        try (Stream<Path> paths = Files.walk(directory, MAX_DEPTH)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> !isIgnored(path))
                 .filter(path -> matches(path, query))
                 .limit(MAX_RESULTS)
                 .forEach(path -> {
                     String relativePath = workingDirectory.relativize(path).toString();
                     int score = calculateScore(query, relativePath);
                     
                     String description = getFileDescription(path);
                     
                     suggestions.add(CompletionSuggestion.file(
                             relativePath,
                             description,
                             score
                     ));
                 });
        }
    }
    
    /**
     * 检查文件是否应该忽略
     */
    private boolean isIgnored(Path path) {
        String pathStr = path.toString();
        
        // 忽略隐藏文件和目录
        if (pathStr.contains("/.")) {
            return true;
        }
        
        // 忽略常见的构建和依赖目录
        return pathStr.contains("/target/") ||
               pathStr.contains("/build/") ||
               pathStr.contains("/node_modules/") ||
               pathStr.contains("/.git/") ||
               pathStr.contains("/.idea/") ||
               pathStr.contains("/.vscode/");
    }
    
    /**
     * 检查文件是否匹配查询
     */
    private boolean matches(Path path, String query) {
        if (query.isEmpty()) {
            return true;
        }
        
        String fileName = path.getFileName().toString().toLowerCase();
        String relativePath = workingDirectory.relativize(path).toString().toLowerCase();
        
        // 文件名匹配
        if (fileName.contains(query)) {
            return true;
        }
        
        // 相对路径匹配
        if (relativePath.contains(query)) {
            return true;
        }
        
        // 模糊匹配
        return fuzzyMatch(query, fileName) || fuzzyMatch(query, relativePath);
    }
    
    /**
     * 计算相关性分数
     */
    private int calculateScore(String query, String relativePath) {
        if (query.isEmpty()) {
            return 50;
        }
        
        String lowerPath = relativePath.toLowerCase();
        String fileName = Paths.get(relativePath).getFileName().toString().toLowerCase();
        
        // 文件名完全匹配
        if (fileName.equals(query)) {
            return 100;
        }
        
        // 文件名前缀匹配
        if (fileName.startsWith(query)) {
            return 95;
        }
        
        // 文件名包含
        if (fileName.contains(query)) {
            return 85;
        }
        
        // 路径前缀匹配
        if (lowerPath.startsWith(query)) {
            return 80;
        }
        
        // 路径包含
        if (lowerPath.contains(query)) {
            return 70;
        }
        
        // 模糊匹配
        if (fuzzyMatch(query, fileName)) {
            return 60;
        }
        
        if (fuzzyMatch(query, lowerPath)) {
            return 50;
        }
        
        return 30;
    }
    
    /**
     * 获取文件描述
     */
    private String getFileDescription(Path path) {
        try {
            long size = Files.size(path);
            String sizeStr = formatFileSize(size);
            String extension = getFileExtension(path);
            
            return String.format("%s (%s)", extension, sizeStr);
        } catch (IOException e) {
            return getFileExtension(path);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toUpperCase();
        }
        return "FILE";
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
     * 模糊匹配
     */
    private boolean fuzzyMatch(String query, String target) {
        int queryIndex = 0;
        for (int i = 0; i < target.length() && queryIndex < query.length(); i++) {
            if (target.charAt(i) == query.charAt(queryIndex)) {
                queryIndex++;
            }
        }
        return queryIndex == query.length();
    }
}
