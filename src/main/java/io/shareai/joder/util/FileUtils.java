package io.shareai.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件工具类
 * 对应 Kode 的 file.ts
 */
public class FileUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    
    /**
     * 行结束符类型
     */
    public enum LineEndingType {
        CRLF, // Windows
        LF    // Unix/Linux/macOS
    }
    
    /**
     * 文件内容类
     */
    public static class FileContent {
        private final String content;
        private final int lineCount;
        private final int totalLines;
        
        public FileContent(String content, int lineCount, int totalLines) {
            this.content = content;
            this.lineCount = lineCount;
            this.totalLines = totalLines;
        }
        
        public String getContent() {
            return content;
        }
        
        public int getLineCount() {
            return lineCount;
        }
        
        public int getTotalLines() {
            return totalLines;
        }
    }
    
    /**
     * 安全地读取文件内容
     * 
     * @param filepath 文件路径
     * @return 文件内容，失败时返回 null
     */
    public static String readFileSafe(String filepath) {
        try {
            return Files.readString(Paths.get(filepath));
        } catch (IOException e) {
            logger.error("读取文件失败: {}", filepath, e);
            return null;
        }
    }
    
    /**
     * 安全地读取文件内容（指定编码）
     */
    public static String readFileSafe(String filepath, Charset charset) {
        try {
            return Files.readString(Paths.get(filepath), charset);
        } catch (IOException e) {
            logger.error("读取文件失败: {}", filepath, e);
            return null;
        }
    }
    
    /**
     * 检查路径是否在指定目录内
     * 
     * @param relativePath 相对路径
     * @param relativeCwd 相对工作目录
     * @return 是否在目录内
     */
    public static boolean isInDirectory(String relativePath, String relativeCwd) {
        if (".".equals(relativePath)) {
            return true;
        }
        
        // 拒绝以 ~ 开头的路径（home 目录）
        if (relativePath.startsWith("~")) {
            return false;
        }
        
        // 拒绝包含 null 字节的路径
        if (relativePath.contains("\0") || relativeCwd.contains("\0")) {
            return false;
        }
        
        try {
            // 规范化路径
            Path normalizedPath = Paths.get(relativePath).normalize();
            Path normalizedCwd = Paths.get(relativeCwd).normalize();
            
            // 解析为绝对路径
            Path fullPath = Paths.get(System.getProperty("user.dir"))
                .resolve(normalizedCwd)
                .resolve(normalizedPath)
                .normalize();
            
            Path fullCwd = Paths.get(System.getProperty("user.dir"))
                .resolve(normalizedCwd)
                .normalize();
            
            // 使用 relativize 检查是否为子路径
            Path relativized = fullCwd.relativize(fullPath);
            String relStr = relativized.toString();
            
            if (relStr.isEmpty()) {
                return true;
            }
            
            // 检查是否以 .. 开头（父目录）
            if (relStr.startsWith("..")) {
                return false;
            }
            
            // 检查是否为绝对路径
            if (relativized.isAbsolute()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("路径检查失败", e);
            return false;
        }
    }
    
    /**
     * 读取文本内容（支持偏移和行数限制）
     * 
     * @param filePath 文件路径
     * @param offset 起始行（从0开始）
     * @param maxLines 最大行数（null 表示无限制）
     * @return 文件内容信息
     */
    public static FileContent readTextContent(String filePath, int offset, Integer maxLines) {
        try {
            List<String> allLines = Files.readAllLines(Paths.get(filePath));
            int totalLines = allLines.size();
            
            // 计算要返回的行
            List<String> linesToReturn;
            if (maxLines != null && totalLines - offset > maxLines) {
                linesToReturn = allLines.subList(offset, offset + maxLines);
            } else if (offset < totalLines) {
                linesToReturn = allLines.subList(offset, totalLines);
            } else {
                linesToReturn = new ArrayList<>();
            }
            
            String content = String.join("\n", linesToReturn);
            
            return new FileContent(content, linesToReturn.size(), totalLines);
            
        } catch (IOException e) {
            logger.error("读取文件内容失败: {}", filePath, e);
            return new FileContent("", 0, 0);
        }
    }
    
    /**
     * 写入文本内容
     * 
     * @param filePath 文件路径
     * @param content 内容
     * @param charset 字符编码
     * @param lineEnding 行结束符类型
     */
    public static void writeTextContent(String filePath, String content, 
                                       Charset charset, LineEndingType lineEnding) {
        try {
            String toWrite = content;
            if (lineEnding == LineEndingType.CRLF) {
                toWrite = content.replace("\n", "\r\n");
            }
            
            Files.writeString(Paths.get(filePath), toWrite, charset);
            
        } catch (IOException e) {
            logger.error("写入文件失败: {}", filePath, e);
        }
    }
    
    /**
     * 检测文件的行结束符类型
     * 
     * @param filePath 文件路径
     * @return 行结束符类型
     */
    public static LineEndingType detectLineEndings(String filePath) {
        try {
            String content = Files.readString(Paths.get(filePath));
            
            // 检查是否包含 CRLF
            if (content.contains("\r\n")) {
                return LineEndingType.CRLF;
            }
            
            return LineEndingType.LF;
            
        } catch (IOException e) {
            logger.debug("检测行结束符失败: {}", filePath, e);
            // 默认返回 LF（Unix 风格）
            return LineEndingType.LF;
        }
    }
    
    /**
     * 检测文件编码
     * 
     * @param filePath 文件路径
     * @return 文件编码
     */
    public static Charset detectFileEncoding(String filePath) {
        // 简化实现：默认使用 UTF-8
        // 完整实现需要使用字节检测库如 juniversalchardet
        return StandardCharsets.UTF_8;
    }
    
    /**
     * 递归遍历目录查找文件
     * 
     * @param directory 目录路径
     * @param pattern 文件名模式（支持通配符）
     * @param maxDepth 最大深度（-1 表示无限制）
     * @return 文件列表
     */
    public static List<Path> findFiles(Path directory, String pattern, int maxDepth) {
        List<Path> result = new ArrayList<>();
        
        try {
            PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern);
            
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                private int currentDepth = 0;
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (maxDepth >= 0 && currentDepth > maxDepth) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    currentDepth++;
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file.getFileName())) {
                        result.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    currentDepth--;
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e) {
            logger.error("遍历目录失败: {}", directory, e);
        }
        
        return result;
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
    
    /**
     * 确保目录存在
     */
    public static boolean ensureDirectoryExists(String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
            return true;
        } catch (IOException e) {
            logger.error("创建目录失败: {}", dirPath, e);
            return false;
        }
    }
}
