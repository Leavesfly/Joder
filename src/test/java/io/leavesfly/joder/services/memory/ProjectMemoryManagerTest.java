package io.leavesfly.joder.services.memory;

import io.leavesfly.joder.core.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectMemoryManager 单元测试
 */
class ProjectMemoryManagerTest {
    
    @TempDir
    Path tempDir;
    
    private ProjectMemoryManager memoryManager;
    private ConfigManager configManager;
    
    @BeforeEach
    void setUp() {
        configManager = new ConfigManager(tempDir.toString());
        memoryManager = new ProjectMemoryManager(tempDir.toString(), configManager);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up
        Path memoryFile = memoryManager.getMemoryFilePath();
        if (Files.exists(memoryFile)) {
            Files.delete(memoryFile);
        }
        Path joderDir = memoryFile.getParent();
        if (Files.exists(joderDir)) {
            Files.delete(joderDir);
        }
    }
    
    @Test
    void testInitialize() {
        // When
        boolean result = memoryManager.initialize(false);
        
        // Then
        assertTrue(result, "Initialize should succeed");
        assertTrue(memoryManager.exists(), "Memory file should exist after initialization");
    }
    
    @Test
    void testInitializeDoesNotOverwriteExisting() throws IOException {
        // Given
        memoryManager.initialize(false);
        String originalContent = memoryManager.load();
        
        // When - initialize again without force
        boolean result = memoryManager.initialize(false);
        
        // Then
        assertTrue(result, "Initialize should succeed");
        assertEquals(originalContent, memoryManager.load(), "Content should not change");
    }
    
    @Test
    void testInitializeWithForceOverwrites() throws IOException {
        // Given
        memoryManager.initialize(false);
        memoryManager.save("Custom content");
        
        // When - force re-initialization
        boolean result = memoryManager.initialize(true);
        
        // Then
        assertTrue(result, "Force initialize should succeed");
        String content = memoryManager.load();
        assertFalse(content.contains("Custom content"), "Custom content should be overwritten");
        assertTrue(content.contains("# Project Memory"), "Should contain default header");
    }
    
    @Test
    void testGenerateInitialMemory() {
        // When
        String memory = memoryManager.generateInitialMemory();
        
        // Then
        assertNotNull(memory);
        assertTrue(memory.contains("# Project Memory"));
        assertTrue(memory.contains("## Architecture"));
        assertTrue(memory.contains("## Conventions"));
        assertTrue(memory.contains("## Decisions"));
        assertTrue(memory.contains("## Preferences"));
        assertTrue(memory.contains("## Context"));
        assertTrue(memory.contains("## Constraints"));
    }
    
    @Test
    void testLoadAndSave() throws IOException {
        // Given
        String testContent = "# Test Project Memory\n\nTest content";
        
        // When
        memoryManager.save(testContent);
        String loaded = memoryManager.load();
        
        // Then
        assertEquals(testContent, loaded, "Loaded content should match saved content");
    }
    
    @Test
    void testLoadNonExistent() {
        // Given - no memory file
        
        // When
        String content = memoryManager.load();
        
        // Then
        assertEquals("", content, "Should return empty string for non-existent file");
    }
    
    @Test
    void testCaching() throws IOException {
        // Given
        memoryManager.initialize(false);
        
        // When - load twice
        String firstLoad = memoryManager.load();
        String secondLoad = memoryManager.load();
        
        // Then
        assertSame(firstLoad, secondLoad, "Should return cached content on second load");
    }
    
    @Test
    void testForceReload() throws IOException, InterruptedException {
        // Given
        memoryManager.initialize(false);
        String firstLoad = memoryManager.load();
        
        // 等待一下确保文件修改时间不同
        Thread.sleep(100);
        
        // Modify file externally
        Path memoryFile = memoryManager.getMemoryFilePath();
        Files.writeString(memoryFile, "Modified content");
        
        // When - load with force
        String forcedLoad = memoryManager.load(true);
        
        // Then
        assertEquals("Modified content", forcedLoad, "Should reload from file with force=true");
    }
    
    @Test
    void testClearCache() throws IOException {
        // Given
        memoryManager.initialize(false);
        memoryManager.load(); // Load and cache
        
        // When
        memoryManager.clearCache();
        
        // Modify file
        Path memoryFile = memoryManager.getMemoryFilePath();
        Files.writeString(memoryFile, "New content");
        
        // Then - should reload after cache clear
        String content = memoryManager.load();
        assertEquals("New content", content, "Should reload after cache clear");
    }
    
    @Test
    void testUpdateSection() throws IOException {
        // Given
        memoryManager.initialize(false);
        
        // When
        memoryManager.updateSection("Context", "- **Current Phase**: Development\n- **Active Features**: User Auth");
        
        // Then
        String content = memoryManager.load(true);
        assertTrue(content.contains("## Context"));
        assertTrue(content.contains("Current Phase"));
        assertTrue(content.contains("Development"));
    }
    
    @Test
    void testUpdateNonExistentSectionAppendsToEnd() throws IOException {
        // Given
        memoryManager.initialize(false);
        
        // When
        memoryManager.updateSection("CustomSection", "Custom content here");
        
        // Then
        String content = memoryManager.load(true);
        assertTrue(content.contains("## CustomSection"));
        assertTrue(content.contains("Custom content here"));
    }
    
    @Test
    void testMemoryFilePathStructure() {
        // When
        Path memoryPath = memoryManager.getMemoryFilePath();
        
        // Then
        assertTrue(memoryPath.toString().endsWith(".joder/claude.md"), 
                   "Memory file should be at .joder/claude.md");
        assertEquals(tempDir.resolve(".joder").resolve("claude.md"), memoryPath);
    }
}
