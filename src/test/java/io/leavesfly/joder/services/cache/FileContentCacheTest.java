package io.leavesfly.joder.services.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileContentCache 测试
 */
class FileContentCacheTest {
    
    @TempDir
    Path tempDir;
    
    private FileContentCache cache;
    
    @BeforeEach
    void setUp() {
        cache = new FileContentCache();
    }
    
    @AfterEach
    void tearDown() {
        cache.clear();
    }
    
    @Test
    void testReadLines_Success() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "line1\nline2\nline3");
        
        List<String> lines = cache.readLines(file.toString());
        
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
        assertEquals("line3", lines.get(2));
    }
    
    @Test
    void testReadLines_CacheHit() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");
        
        // 第一次读取 - 缓存未命中
        cache.readLines(file.toString());
        FileContentCache.CacheStats stats1 = cache.getStats();
        assertEquals(0, stats1.hits);
        assertEquals(1, stats1.misses);
        
        // 第二次读取 - 缓存命中
        cache.readLines(file.toString());
        FileContentCache.CacheStats stats2 = cache.getStats();
        assertEquals(1, stats2.hits);
        assertEquals(1, stats2.misses);
        assertEquals(0.5, stats2.hitRate, 0.01);
    }
    
    @Test
    void testReadLines_FileModified() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "version1");
        
        // 第一次读取
        List<String> lines1 = cache.readLines(file.toString());
        assertEquals("version1", lines1.get(0));
        
        try {
            // 修改文件
            Thread.sleep(10); // 确保修改时间不同
            Files.writeString(file, "version2");
            
            // 第二次读取 - 应该检测到文件变更
            List<String> lines2 = cache.readLines(file.toString());
            assertEquals("version2", lines2.get(0));
            
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }
    }
    
    @Test
    void testReadLines_FileNotFound() {
        Path file = tempDir.resolve("nonexistent.txt");
        
        assertThrows(IOException.class, () -> {
            cache.readLines(file.toString());
        });
    }
    
    @Test
    void testReadString() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "line1\nline2\nline3");
        
        String content = cache.readString(file.toString());
        
        assertEquals("line1\nline2\nline3", content);
    }
    
    @Test
    void testInvalidate() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");
        
        // 读取并缓存
        cache.readLines(file.toString());
        assertEquals(1, cache.size());
        
        // 使缓存失效
        cache.invalidate(file.toString());
        assertEquals(0, cache.size());
        
        // 再次读取应该重新加载
        cache.readLines(file.toString());
        FileContentCache.CacheStats stats = cache.getStats();
        assertEquals(2, stats.misses); // 两次都未命中
    }
    
    @Test
    void testClear() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");
        
        cache.readLines(file1.toString());
        cache.readLines(file2.toString());
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
        
        FileContentCache.CacheStats stats = cache.getStats();
        assertEquals(0, stats.hits);
        assertEquals(0, stats.misses);
    }
    
    @Test
    void testGetHitRate() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");
        
        // 初始状态
        assertEquals(0.0, cache.getHitRate(), 0.01);
        
        // 1次未命中
        cache.readLines(file.toString());
        assertEquals(0.0, cache.getHitRate(), 0.01);
        
        // 1次命中
        cache.readLines(file.toString());
        assertEquals(0.5, cache.getHitRate(), 0.01);
        
        // 再1次命中
        cache.readLines(file.toString());
        assertEquals(0.667, cache.getHitRate(), 0.01);
    }
    
    @Test
    void testCacheSize() throws IOException {
        assertEquals(0, cache.size());
        
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");
        
        cache.readLines(file.toString());
        assertEquals(1, cache.size());
    }
    
    @Test
    void testCacheStats() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");
        
        cache.readLines(file.toString());
        cache.readLines(file.toString());
        cache.readLines(file.toString());
        
        FileContentCache.CacheStats stats = cache.getStats();
        
        assertEquals(2, stats.hits);
        assertEquals(1, stats.misses);
        assertEquals(1, stats.currentSize);
        assertTrue(stats.maxSize > 0);
        assertEquals(0.667, stats.hitRate, 0.01);
        
        String statsStr = stats.toString();
        assertTrue(statsStr.contains("hits=2"));
        assertTrue(statsStr.contains("misses=1"));
    }
}
