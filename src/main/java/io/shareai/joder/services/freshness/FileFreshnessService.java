package io.shareai.joder.services.freshness;

import io.shareai.joder.domain.FileTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文件新鲜度服务
 * 
 * <p>追踪文件的读取和修改历史，检测文件冲突，防止覆盖外部修改。
 * 
 * <p>核心功能：
 * <ul>
 *   <li>记录文件读取时间和状态</li>
 *   <li>记录 Agent 编辑操作</li>
 *   <li>检测文件在读取后是否被外部修改</li>
 *   <li>生成文件修改提醒</li>
 *   <li>追踪会话中访问的文件</li>
 *   <li>获取重要文件列表用于上下文恢复</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
@Singleton
public class FileFreshnessService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileFreshnessService.class);
    
    /** 时间容差（毫秒），用于处理文件系统时间戳精度问题 */
    private static final long TIME_TOLERANCE_MS = 100;
    
    // 状态存储（线程安全）
    private final Map<String, FileTimestamp> readTimestamps;
    private final Set<String> editConflicts;
    private final Set<String> sessionFiles;
    
    public FileFreshnessService() {
        this.readTimestamps = new ConcurrentHashMap<>();
        this.editConflicts = ConcurrentHashMap.newKeySet();
        this.sessionFiles = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * 记录文件读取操作
     * 
     * @param filePath 文件路径
     */
    public void recordFileRead(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                logger.debug("File does not exist: {}", filePath);
                return;
            }
            
            FileTime modifiedTime = Files.getLastModifiedTime(path);
            long size = Files.size(path);
            long now = System.currentTimeMillis();
            
            FileTimestamp timestamp = new FileTimestamp(
                    filePath,
                    now,
                    modifiedTime.toMillis(),
                    size
            );
            
            readTimestamps.put(filePath, timestamp);
            sessionFiles.add(filePath);
            
            logger.debug("Recorded file read: {} (size={}, modified={})", 
                    filePath, size, modifiedTime);
            
        } catch (IOException e) {
            logger.error("Failed to record file read: {}", filePath, e);
        }
    }
    
    /**
     * 检查文件新鲜度
     * 
     * @param filePath 文件路径
     * @return 新鲜度检查结果
     */
    public FreshnessCheckResult checkFileFreshness(String filePath) {
        FileTimestamp recorded = readTimestamps.get(filePath);
        
        if (recorded == null) {
            // 文件未被读取过，视为新鲜
            return new FreshnessCheckResult(true, false, null, null);
        }
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                // 文件已被删除
                editConflicts.add(filePath);
                return new FreshnessCheckResult(
                        false, 
                        true, 
                        recorded.getLastReadTime(), 
                        null
                );
            }
            
            FileTime currentModifiedTime = Files.getLastModifiedTime(path);
            long currentModified = currentModifiedTime.toMillis();
            boolean isFresh = currentModified <= recorded.getModifiedTimeAtRead();
            
            if (!isFresh) {
                editConflicts.add(filePath);
                logger.warn("File modified externally: {} (recorded={}, current={})", 
                        filePath, recorded.getModifiedTimeAtRead(), currentModified);
            }
            
            return new FreshnessCheckResult(
                    isFresh,
                    !isFresh,
                    recorded.getLastReadTime(),
                    currentModified
            );
            
        } catch (IOException e) {
            logger.error("Failed to check file freshness: {}", filePath, e);
            editConflicts.add(filePath);
            return new FreshnessCheckResult(false, true, recorded.getLastReadTime(), null);
        }
    }
    
    /**
     * 记录 Agent 编辑操作
     * 
     * @param filePath 文件路径
     */
    public void recordFileEdit(String filePath) {
        try {
            long now = System.currentTimeMillis();
            Path path = Paths.get(filePath);
            
            if (Files.exists(path)) {
                FileTime modifiedTime = Files.getLastModifiedTime(path);
                long size = Files.size(path);
                
                FileTimestamp existing = readTimestamps.get(filePath);
                if (existing != null) {
                    // 更新现有记录
                    existing.setModifiedTimeAtRead(modifiedTime.toMillis());
                    existing.setSize(size);
                    existing.setLastAgentEditTime(now);
                } else {
                    // 创建新记录
                    FileTimestamp timestamp = new FileTimestamp(
                            filePath,
                            now,
                            modifiedTime.toMillis(),
                            size
                    );
                    timestamp.setLastAgentEditTime(now);
                    readTimestamps.put(filePath, timestamp);
                }
            }
            
            // 从冲突列表中移除（Agent 刚编辑过）
            editConflicts.remove(filePath);
            
            logger.debug("Recorded Agent file edit: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to record file edit: {}", filePath, e);
        }
    }
    
    /**
     * 生成文件修改提醒
     * 
     * @param filePath 文件路径
     * @return 提醒消息，如果文件未修改返回 null
     */
    public String generateFileModificationReminder(String filePath) {
        FileTimestamp recorded = readTimestamps.get(filePath);
        
        if (recorded == null) {
            return null;
        }
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return String.format("Note: %s was deleted since last read.", filePath);
            }
            
            FileTime currentModifiedTime = Files.getLastModifiedTime(path);
            long currentModified = currentModifiedTime.toMillis();
            boolean isModified = currentModified > recorded.getModifiedTimeAtRead();
            
            if (!isModified) {
                return null;
            }
            
            // 检查是否是 Agent 自己的修改
            Long lastAgentEdit = recorded.getLastAgentEditTime();
            if (lastAgentEdit != null && 
                lastAgentEdit >= (recorded.getModifiedTimeAtRead() - TIME_TOLERANCE_MS)) {
                // Agent 最近修改过，不需要提醒
                return null;
            }
            
            return String.format(
                    "Note: %s was modified externally since last read. " +
                    "The file may have changed outside of this session.",
                    filePath
            );
            
        } catch (IOException e) {
            logger.error("Failed to generate modification reminder: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 获取冲突文件列表
     * 
     * @return 有冲突的文件路径列表
     */
    public List<String> getConflictedFiles() {
        return new ArrayList<>(editConflicts);
    }
    
    /**
     * 获取会话中访问的文件列表
     * 
     * @return 会话文件列表
     */
    public List<String> getSessionFiles() {
        return new ArrayList<>(sessionFiles);
    }
    
    /**
     * 获取重要文件列表（用于上下文恢复）
     * 
     * <p>选择标准：
     * <ul>
     *   <li>最近访问的文件</li>
     *   <li>排除依赖和构建产物</li>
     *   <li>排除系统目录</li>
     * </ul>
     * 
     * @param maxFiles 最大文件数
     * @return 重要文件列表
     */
    public List<ImportantFile> getImportantFiles(int maxFiles) {
        return readTimestamps.values().stream()
                .map(ts -> new ImportantFile(
                        ts.getPath(),
                        ts.getLastReadTime(),
                        ts.getSize()
                ))
                .filter(f -> isValidForRecovery(f.getPath()))
                .sorted(Comparator.comparingLong(ImportantFile::getTimestamp).reversed())
                .limit(maxFiles)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查文件是否适合恢复
     */
    private boolean isValidForRecovery(String filePath) {
        return !filePath.contains("node_modules")
                && !filePath.contains(".git")
                && !filePath.startsWith("/tmp")
                && !filePath.contains(".cache")
                && !filePath.contains("target/") // Maven 构建目录
                && !filePath.contains("build/")
                && !filePath.contains("dist/");
    }
    
    /**
     * 重置会话
     */
    public void resetSession() {
        readTimestamps.clear();
        editConflicts.clear();
        sessionFiles.clear();
        logger.info("File freshness session reset");
    }
    
    /**
     * 获取文件信息
     * 
     * @param filePath 文件路径
     * @return 文件时间戳信息，如果不存在返回 null
     */
    public FileTimestamp getFileInfo(String filePath) {
        return readTimestamps.get(filePath);
    }
    
    /**
     * 检查文件是否已被追踪
     * 
     * @param filePath 文件路径
     * @return 如果文件已被追踪返回 true
     */
    public boolean isFileTracked(String filePath) {
        return readTimestamps.containsKey(filePath);
    }
    
    /**
     * 新鲜度检查结果
     */
    public static class FreshnessCheckResult {
        private final boolean fresh;
        private final boolean conflict;
        private final Long lastReadTime;
        private final Long currentModifiedTime;
        
        public FreshnessCheckResult(boolean fresh, boolean conflict, 
                                   Long lastReadTime, Long currentModifiedTime) {
            this.fresh = fresh;
            this.conflict = conflict;
            this.lastReadTime = lastReadTime;
            this.currentModifiedTime = currentModifiedTime;
        }
        
        public boolean isFresh() {
            return fresh;
        }
        
        public boolean hasConflict() {
            return conflict;
        }
        
        public Long getLastReadTime() {
            return lastReadTime;
        }
        
        public Long getCurrentModifiedTime() {
            return currentModifiedTime;
        }
    }
    
    /**
     * 重要文件信息
     */
    public static class ImportantFile {
        private final String path;
        private final long timestamp;
        private final long size;
        
        public ImportantFile(String path, long timestamp, long size) {
            this.path = path;
            this.timestamp = timestamp;
            this.size = size;
        }
        
        public String getPath() {
            return path;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public long getSize() {
            return size;
        }
    }
}
