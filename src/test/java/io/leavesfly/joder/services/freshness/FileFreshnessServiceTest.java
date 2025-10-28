package io.leavesfly.joder.services.freshness;

import io.leavesfly.joder.domain.FileTimestamp;
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
 * FileFreshnessService 测试
 */
class FileFreshnessServiceTest {
    
    @TempDir
    Path tempDir;
    
    private FileFreshnessService service;
    
    @BeforeEach
    void setUp() {
        service = new FileFreshnessService();
    }
    
    @AfterEach
    void tearDown() {
        service.resetSession();
    }
    
    @Test
    void testRecordFileRead() throws IOException, InterruptedException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello World");
        
        service.recordFileRead(testFile.toString());
        
        assertTrue(service.isFileTracked(testFile.toString()));
        FileTimestamp info = service.getFileInfo(testFile.toString());
        assertNotNull(info);
        assertEquals(testFile.toString(), info.getPath());
    }
    
    @Test
    void testCheckFileFreshness_Fresh() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Content");
        
        service.recordFileRead(testFile.toString());
        
        FileFreshnessService.FreshnessCheckResult result = service.checkFileFreshness(testFile.toString());
        
        assertTrue(result.isFresh());
        assertFalse(result.hasConflict());
    }
    
    @Test
    void testCheckFileFreshness_Modified() throws IOException, InterruptedException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Original");
        
        service.recordFileRead(testFile.toString());
        
        // 等待一小段时间确保时间戳不同
        Thread.sleep(10);
        
        // 修改文件
        Files.writeString(testFile, "Modified");
        
        FileFreshnessService.FreshnessCheckResult result = service.checkFileFreshness(testFile.toString());
        
        assertFalse(result.isFresh());
        assertTrue(result.hasConflict());
    }
    
    @Test
    void testCheckFileFreshness_Deleted() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Content");
        
        service.recordFileRead(testFile.toString());
        
        // 删除文件
        Files.delete(testFile);
        
        FileFreshnessService.FreshnessCheckResult result = service.checkFileFreshness(testFile.toString());
        
        assertFalse(result.isFresh());
        assertTrue(result.hasConflict());
    }
    
    @Test
    void testRecordFileEdit() throws IOException, InterruptedException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Original");
        
        service.recordFileRead(testFile.toString());
        
        Thread.sleep(10);
        Files.writeString(testFile, "Edited by Agent");
        
        service.recordFileEdit(testFile.toString());
        
        // Agent 编辑后应该是新鲜的
        FileFreshnessService.FreshnessCheckResult result = service.checkFileFreshness(testFile.toString());
        assertTrue(result.isFresh());
    }
    
    @Test
    void testGenerateFileModificationReminder_NoModification() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Content");
        
        service.recordFileRead(testFile.toString());
        
        String reminder = service.generateFileModificationReminder(testFile.toString());
        
        assertNull(reminder);
    }
    
    @Test
    void testGenerateFileModificationReminder_ExternalModification() throws IOException, InterruptedException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Original");
        
        service.recordFileRead(testFile.toString());
        
        Thread.sleep(10);
        Files.writeString(testFile, "Modified externally");
        
        String reminder = service.generateFileModificationReminder(testFile.toString());
        
        assertNotNull(reminder);
        assertTrue(reminder.contains("modified externally"));
    }
    
    @Test
    void testGenerateFileModificationReminder_Deleted() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Content");
        
        service.recordFileRead(testFile.toString());
        Files.delete(testFile);
        
        String reminder = service.generateFileModificationReminder(testFile.toString());
        
        assertNotNull(reminder);
        assertTrue(reminder.contains("deleted"));
    }
    
    @Test
    void testGetConflictedFiles() throws IOException, InterruptedException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        
        Files.writeString(file1, "Content1");
        Files.writeString(file2, "Content2");
        
        service.recordFileRead(file1.toString());
        service.recordFileRead(file2.toString());
        
        Thread.sleep(10);
        Files.writeString(file1, "Modified");
        
        service.checkFileFreshness(file1.toString());
        
        List<String> conflicts = service.getConflictedFiles();
        
        assertEquals(1, conflicts.size());
        assertTrue(conflicts.contains(file1.toString()));
    }
    
    @Test
    void testGetSessionFiles() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        
        Files.writeString(file1, "Content1");
        Files.writeString(file2, "Content2");
        
        service.recordFileRead(file1.toString());
        service.recordFileRead(file2.toString());
        
        List<String> sessionFiles = service.getSessionFiles();
        
        assertEquals(2, sessionFiles.size());
        assertTrue(sessionFiles.contains(file1.toString()));
        assertTrue(sessionFiles.contains(file2.toString()));
    }
    
    @Test
    void testGetImportantFiles() throws IOException {
        // 创建多个文件
        Path file1 = tempDir.resolve("important1.txt");
        Path file2 = tempDir.resolve("important2.txt");
        Path nodeModules = tempDir.resolve("node_modules/lib.js");
        
        Files.createDirectories(nodeModules.getParent());
        Files.writeString(file1, "Important 1");
        Files.writeString(file2, "Important 2");
        Files.writeString(nodeModules, "Library");
        
        service.recordFileRead(file1.toString());
        service.recordFileRead(file2.toString());
        service.recordFileRead(nodeModules.toString());
        
        List<FileFreshnessService.ImportantFile> important = service.getImportantFiles(5);
        
        // node_modules 应该被过滤掉
        assertEquals(2, important.size());
        assertTrue(important.stream().noneMatch(f -> f.getPath().contains("node_modules")));
    }
    
    @Test
    void testResetSession() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Content");
        
        service.recordFileRead(testFile.toString());
        assertTrue(service.isFileTracked(testFile.toString()));
        
        service.resetSession();
        
        assertFalse(service.isFileTracked(testFile.toString()));
        assertEquals(0, service.getSessionFiles().size());
    }
}
