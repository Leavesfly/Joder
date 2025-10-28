package io.leavesfly.joder.tools.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 搜索执行器
 * 基于给定的搜索策略执行高性能文件内容搜索
 */
public class SearchExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchExecutor.class);
    
    private static final int MAX_LINE_LENGTH = 500; // 超长行截断
    private static final int MAX_MATCHES_PER_FILE = 50; // 每个文件最多匹配行数
    
    /**
     * 执行搜索
     * 
     * @param strategy 搜索策略
     * @param workingDir 工作目录
     * @return 搜索结果列表
     */
    public List<SearchMatch> execute(SearchStrategy strategy, Path workingDir) throws IOException {
        
        Path searchPath = resolveSearchPath(strategy.getSearchPath(), workingDir);
        
        if (!Files.exists(searchPath)) {
            throw new IOException("搜索路径不存在: " + searchPath);
        }
        
        if (!Files.isDirectory(searchPath)) {
            throw new IOException("搜索路径不是目录: " + searchPath);
        }
        
        // 编译正则表达式
        int flags = strategy.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern searchPattern = Pattern.compile(strategy.getRegex(), flags);
        
        // 编译文件过滤器
        List<PathMatcher> includeMatchers = compilePatterns(strategy.getIncludePatterns());
        List<PathMatcher> excludeMatchers = compilePatterns(strategy.getExcludePatterns());
        
        // 执行搜索
        List<SearchMatch> matches = new ArrayList<>();
        
        Files.walkFileTree(searchPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (shouldExcludeDirectory(dir, searchPath, excludeMatchers)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 检查是否已达到最大结果数
                if (matches.size() >= strategy.getMaxResults()) {
                    return FileVisitResult.TERMINATE;
                }
                
                // 应用文件过滤
                if (!shouldIncludeFile(file, searchPath, includeMatchers, excludeMatchers)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // 只搜索文本文件
                if (!isTextFile(file)) {
                    return FileVisitResult.CONTINUE;
                }
                
                // 搜索文件内容
                try {
                    SearchMatch match = searchFile(file, searchPattern, attrs.lastModifiedTime().toMillis(), 
                                                   strategy.isFilesOnly());
                    if (match != null) {
                        matches.add(match);
                    }
                } catch (Exception e) {
                    logger.debug("搜索文件失败: {}", file, e);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        // 计算相关性分数并排序
        rankAndSort(matches);
        
        return matches;
    }
    
    /**
     * 解析搜索路径
     */
    private Path resolveSearchPath(String searchPath, Path workingDir) {
        if (searchPath == null || searchPath.trim().isEmpty() || ".".equals(searchPath)) {
            return workingDir;
        }
        
        Path path = Paths.get(searchPath);
        if (path.isAbsolute()) {
            return path;
        }
        
        return workingDir.resolve(searchPath).normalize();
    }
    
    /**
     * 编译 glob 模式列表
     */
    private List<PathMatcher> compilePatterns(List<String> patterns) {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                matchers.add(matcher);
            } catch (Exception e) {
                logger.warn("无效的 glob 模式: {}", pattern, e);
            }
        }
        return matchers;
    }
    
    /**
     * 判断是否应排除目录
     */
    private boolean shouldExcludeDirectory(Path dir, Path basePath, List<PathMatcher> excludeMatchers) {
        String dirName = dir.getFileName().toString();
        
        // 跳过隐藏目录
        if (dirName.startsWith(".") && !dirName.equals(".")) {
            return true;
        }
        
        // 默认排除的目录
        if (dirName.equals("node_modules") || dirName.equals("target") || 
            dirName.equals("build") || dirName.equals("dist") || 
            dirName.equals("__pycache__")) {
            return true;
        }
        
        // 应用自定义排除规则
        Path relativePath = basePath.relativize(dir);
        for (PathMatcher matcher : excludeMatchers) {
            if (matcher.matches(relativePath) || matcher.matches(dir.getFileName())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否应包含文件
     */
    private boolean shouldIncludeFile(Path file, Path basePath, 
                                     List<PathMatcher> includeMatchers, 
                                     List<PathMatcher> excludeMatchers) {
        Path relativePath = basePath.relativize(file);
        Path fileName = file.getFileName();
        
        // 应用排除规则
        for (PathMatcher matcher : excludeMatchers) {
            if (matcher.matches(relativePath) || matcher.matches(fileName)) {
                return false;
            }
        }
        
        // 如果没有包含规则，则包含所有文件
        if (includeMatchers.isEmpty()) {
            return true;
        }
        
        // 应用包含规则
        for (PathMatcher matcher : includeMatchers) {
            if (matcher.matches(relativePath) || matcher.matches(fileName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 搜索单个文件
     */
    private SearchMatch searchFile(Path file, Pattern pattern, long lastModified, boolean filesOnly) {
        SearchMatch match = null;
        
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            int matchCount = 0;
            
            while ((line = reader.readLine()) != null && matchCount < MAX_MATCHES_PER_FILE) {
                lineNumber++;
                
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (match == null) {
                        match = new SearchMatch();
                        match.setFilePath(file);
                        match.setLastModified(lastModified);
                    }
                    
                    if (!filesOnly) {
                        // 截断超长行
                        String displayLine = line.length() > MAX_LINE_LENGTH 
                            ? line.substring(0, MAX_LINE_LENGTH) + "..." 
                            : line;
                        
                        match.addMatchedLine(new SearchMatch.MatchedLine(
                            lineNumber, 
                            displayLine, 
                            matcher.start(), 
                            matcher.end() - matcher.start()
                        ));
                        matchCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("读取文件失败: {}", file, e);
        }
        
        return match;
    }
    
    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        
        // 常见文本文件扩展名
        String[] textExtensions = {
            ".txt", ".md", ".java", ".js", ".ts", ".tsx", ".jsx",
            ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h", ".hpp",
            ".cs", ".php", ".html", ".css", ".scss", ".sass", ".less",
            ".xml", ".json", ".yaml", ".yml", ".toml", ".ini", ".conf",
            ".sh", ".bash", ".zsh", ".properties", ".gradle", ".kt", ".scala"
        };
        
        for (String ext : textExtensions) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        
        // 无扩展名的常见文本文件
        String[] textFiles = {
            "readme", "license", "makefile", "dockerfile", 
            "gemfile", "rakefile", "gradlew", "changelog"
        };
        
        for (String textFile : textFiles) {
            if (name.equals(textFile)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算相关性分数并排序
     */
    private void rankAndSort(List<SearchMatch> matches) {
        for (SearchMatch match : matches) {
            double score = calculateRelevanceScore(match);
            match.setRelevanceScore(score);
        }
        
        // 按相关性分数降序排序
        matches.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
    }
    
    /**
     * 计算相关性分数
     * 考虑因素：
     * 1. 匹配次数（更多匹配 = 更相关）
     * 2. 文件新鲜度（最近修改的文件得分更高）
     * 3. 文件路径深度（浅层文件可能更重要）
     */
    private double calculateRelevanceScore(SearchMatch match) {
        double score = 0.0;
        
        // 匹配次数权重（最多50行）
        int matchCount = match.getMatchedLines().size();
        score += Math.min(matchCount, 50) * 2.0;
        
        // 文件新鲜度权重（最近7天内修改的文件加分）
        long now = System.currentTimeMillis();
        long ageInDays = (now - match.getLastModified()) / (1000 * 60 * 60 * 24);
        if (ageInDays <= 7) {
            score += 50.0 * (1.0 - ageInDays / 7.0);
        }
        
        // 路径深度权重（浅层文件加分）
        int depth = match.getFilePath().getNameCount();
        score += Math.max(0, 20 - depth * 2);
        
        return score;
    }
}
