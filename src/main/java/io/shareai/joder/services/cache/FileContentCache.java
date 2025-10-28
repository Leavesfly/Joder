package io.shareai.joder.services.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件内容缓存服务
 * 基于文件修改时间戳缓存文件内容,减少重复文件读取
 */
@Singleton
public class FileContentCache {
    
    private static final Logger logger = LoggerFactory.getLogger(FileContentCache.class);
    private static final int MAX_CACHE_SIZE = 100; // 最大缓存文件数
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 最大缓存文件大小: 1MB
    
    // 使用LinkedHashMap实现LRU缓存
    private final Map<String, CachedFile> cache = new LinkedHashMap<String, CachedFile>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedFile> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    
    private long hits = 0;
    private long misses = 0;
    
    /**
     * 读取文件内容(带缓存)
     */
    public List<String> readLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        
        // 检查文件是否存在
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        // 检查文件大小
        long fileSize = Files.size(path);
        if (fileSize > MAX_FILE_SIZE) {
            logger.debug("文件过大,不使用缓存: {} ({} bytes)", filePath, fileSize);
            misses++;
            return Files.readAllLines(path);
        }
        
        // 获取文件修改时间
        FileTime lastModified = Files.getLastModifiedTime(path);
        
        // 检查缓存
        CachedFile cached = cache.get(filePath);
        if (cached != null && cached.lastModified.equals(lastModified)) {
            // 缓存命中
            hits++;
            logger.debug("缓存命中: {}", filePath);
            return cached.lines;
        }
        
        // 缓存未命中或已过期,读取文件
        misses++;
        logger.debug("缓存未命中: {}", filePath);
        List<String> lines = Files.readAllLines(path);
        
        // 更新缓存
        cache.put(filePath, new CachedFile(lines, lastModified));
        
        return lines;
    }
    
    /**
     * 读取文件内容为字符串(带缓存)
     */
    public String readString(String filePath) throws IOException {
        List<String> lines = readLines(filePath);
        return String.join("\n", lines);
    }
    
    /**
     * 使缓存失效
     */
    public void invalidate(String filePath) {
        cache.remove(filePath);
        logger.debug("缓存已失效: {}", filePath);
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        hits = 0;
        misses = 0;
        logger.info("缓存已清空: {} 个文件", size);
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long total = hits + misses;
        if (total == 0) {
            return 0.0;
        }
        return (double) hits / total;
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(hits, misses, cache.size(), MAX_CACHE_SIZE);
    }
    
    /**
     * 缓存的文件
     */
    private static class CachedFile {
        final List<String> lines;
        final FileTime lastModified;
        
        CachedFile(List<String> lines, FileTime lastModified) {
            this.lines = List.copyOf(lines); // 不可变副本
            this.lastModified = lastModified;
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final long hits;
        public final long misses;
        public final int currentSize;
        public final int maxSize;
        public final double hitRate;
        
        CacheStats(long hits, long misses, int currentSize, int maxSize) {
            this.hits = hits;
            this.misses = misses;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            long total = hits + misses;
            this.hitRate = total > 0 ? (double) hits / total : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, size=%d/%d}",
                hits, misses, hitRate * 100, currentSize, maxSize
            );
        }
    }
}
