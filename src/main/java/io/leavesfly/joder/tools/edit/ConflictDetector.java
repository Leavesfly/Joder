package io.leavesfly.joder.tools.edit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件冲突检测器
 * 检测文件是否在读取后被外部修改
 */
public class ConflictDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(ConflictDetector.class);
    
    /**
     * 文件快照：记录文件读取时的状态
     */
    private final Map<String, FileSnapshot> snapshots = new HashMap<>();
    
    /**
     * 记录文件读取快照
     * 
     * @param filePath 文件路径
     */
    public void recordSnapshot(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                logger.warn("文件不存在，无法记录快照: {}", filePath);
                return;
            }
            
            FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            String content = Files.readString(filePath);
            String contentHash = calculateHash(content);
            
            FileSnapshot snapshot = new FileSnapshot(
                filePath.toString(),
                lastModifiedTime.toMillis(),
                contentHash,
                content.length()
            );
            
            snapshots.put(filePath.toString(), snapshot);
            logger.debug("记录文件快照: {} (hash: {})", filePath, contentHash);
            
        } catch (IOException e) {
            logger.error("记录文件快照失败: {}", filePath, e);
        }
    }
    
    /**
     * 检测文件冲突
     * 
     * @param filePath 文件路径
     * @return 冲突检测结果
     */
    public ConflictResult detectConflict(Path filePath) {
        String pathStr = filePath.toString();
        
        if (!snapshots.containsKey(pathStr)) {
            return new ConflictResult(false, "文件未记录快照");
        }
        
        try {
            if (!Files.exists(filePath)) {
                return new ConflictResult(true, "文件已被删除");
            }
            
            FileSnapshot snapshot = snapshots.get(pathStr);
            FileTime currentModifiedTime = Files.getLastModifiedTime(filePath);
            long currentTimestamp = currentModifiedTime.toMillis();
            
            // 检查修改时间
            if (currentTimestamp > snapshot.timestamp) {
                String currentContent = Files.readString(filePath);
                String currentHash = calculateHash(currentContent);
                
                // 即使时间变化，如果内容哈希相同，则无冲突
                if (!currentHash.equals(snapshot.contentHash)) {
                    return new ConflictResult(true, 
                        String.format("文件已被外部修改 (时间: %d -> %d, 哈希: %s -> %s)",
                            snapshot.timestamp, currentTimestamp, 
                            snapshot.contentHash.substring(0, 8), 
                            currentHash.substring(0, 8)));
                }
            }
            
            return new ConflictResult(false, "无冲突");
            
        } catch (IOException e) {
            logger.error("检测文件冲突失败: {}", filePath, e);
            return new ConflictResult(true, "冲突检测失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新文件快照（编辑成功后）
     * 
     * @param filePath 文件路径
     */
    public void updateSnapshot(Path filePath) {
        recordSnapshot(filePath);
    }
    
    /**
     * 清除文件快照
     * 
     * @param filePath 文件路径
     */
    public void clearSnapshot(Path filePath) {
        snapshots.remove(filePath.toString());
    }
    
    /**
     * 清除所有快照
     */
    public void clearAllSnapshots() {
        snapshots.clear();
    }
    
    /**
     * 计算内容哈希（简单实现）
     */
    private String calculateHash(String content) {
        // 使用简单的哈希算法
        int hash = content.hashCode();
        return String.format("%08x", hash);
    }
    
    /**
     * 文件快照
     */
    private static class FileSnapshot {
        final String filePath;
        final long timestamp;
        final String contentHash;
        final long size;
        
        FileSnapshot(String filePath, long timestamp, String contentHash, long size) {
            this.filePath = filePath;
            this.timestamp = timestamp;
            this.contentHash = contentHash;
            this.size = size;
        }
    }
    
    /**
     * 冲突检测结果
     */
    public static class ConflictResult {
        private final boolean hasConflict;
        private final String message;
        
        public ConflictResult(boolean hasConflict, String message) {
            this.hasConflict = hasConflict;
            this.message = message;
        }
        
        public boolean hasConflict() {
            return hasConflict;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("ConflictResult{hasConflict=%s, message='%s'}", 
                hasConflict, message);
        }
    }
}
