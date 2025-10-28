package io.leavesfly.joder.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.domain.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryManager 单元测试
 */
@DisplayName("MemoryManager 记忆管理器测试")
public class MemoryManagerTest {
    
    @TempDir
    Path tempDir;
    
    private MemoryManager memoryManager;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        memoryManager = new MemoryManager(objectMapper);
    }
    
    @Test
    @DisplayName("应该能保存记忆")
    void testSaveMemory() {
        // Arrange
        Memory memory = createTestMemory("标题", "内容");
        
        // Act
        Memory saved = memoryManager.saveMemory(memory);
        
        // Assert
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("标题", saved.getTitle());
        assertEquals("内容", saved.getContent());
    }
    
    @Test
    @DisplayName("应该为新记忆生成唯一ID")
    void testGenerateUniqueIdForNewMemory() {
        // Arrange
        Memory memory1 = createTestMemory("记忆1", "内容1");
        Memory memory2 = createTestMemory("记忆2", "内容2");
        
        // Act
        Memory saved1 = memoryManager.saveMemory(memory1);
        Memory saved2 = memoryManager.saveMemory(memory2);
        
        // Assert
        assertNotNull(saved1.getId());
        assertNotNull(saved2.getId());
        assertNotEquals(saved1.getId(), saved2.getId());
    }
    
    @Test
    @DisplayName("应该能搜索记忆")
    void testSearchMemories() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("Java编程", "Java是一门面向对象的语言"));
        memoryManager.saveMemory(createTestMemory("Python编程", "Python是一门动态类型语言"));
        memoryManager.saveMemory(createTestMemory("数据库设计", "关系型数据库设计原则"));
        
        // Act
        List<Memory> results = memoryManager.searchMemories("Java");
        
        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(m -> m.getTitle().contains("Java")));
    }
    
    @Test
    @DisplayName("应该能通过标题搜索记忆")
    void testSearchMemoriesByTitle() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("Spring框架", "Spring是企业级应用框架"));
        memoryManager.saveMemory(createTestMemory("React框架", "React是前端UI框架"));
        
        // Act
        List<Memory> results = memoryManager.searchMemories("Spring");
        
        // Assert
        assertEquals(1, results.size());
        assertEquals("Spring框架", results.get(0).getTitle());
    }
    
    @Test
    @DisplayName("应该能通过内容搜索记忆")
    void testSearchMemoriesByContent() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("标题1", "这是关于微服务的内容"));
        memoryManager.saveMemory(createTestMemory("标题2", "这是关于单体应用的内容"));
        
        // Act
        List<Memory> results = memoryManager.searchMemories("微服务");
        
        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).getContent().contains("微服务"));
    }
    
    @Test
    @DisplayName("应该能通过关键词搜索记忆")
    void testSearchMemoriesByKeywords() {
        // Arrange
        Memory memory = createTestMemory("Docker容器", "容器化技术");
        memory.setKeywords(Arrays.asList("docker", "container", "devops"));
        memoryManager.saveMemory(memory);
        
        // Act
        List<Memory> results = memoryManager.searchMemories("docker");
        
        // Assert
        assertFalse(results.isEmpty());
        assertEquals("Docker容器", results.get(0).getTitle());
    }
    
    @Test
    @DisplayName("空查询应该返回所有记忆")
    void testSearchWithEmptyQueryReturnsAll() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("记忆1", "内容1"));
        memoryManager.saveMemory(createTestMemory("记忆2", "内容2"));
        memoryManager.saveMemory(createTestMemory("记忆3", "内容3"));
        
        // Act
        List<Memory> results = memoryManager.searchMemories("");
        
        // Assert
        assertEquals(3, results.size());
    }
    
    @Test
    @DisplayName("应该能按分类获取记忆")
    void testGetMemoriesByCategory() {
        // Arrange
        Memory memory1 = createTestMemory("技术1", "内容1");
        memory1.setCategory("技术");
        memoryManager.saveMemory(memory1);
        
        Memory memory2 = createTestMemory("技术2", "内容2");
        memory2.setCategory("技术");
        memoryManager.saveMemory(memory2);
        
        Memory memory3 = createTestMemory("业务1", "内容3");
        memory3.setCategory("业务");
        memoryManager.saveMemory(memory3);
        
        // Act
        List<Memory> techMemories = memoryManager.getMemoriesByCategory("技术");
        
        // Assert
        assertEquals(2, techMemories.size());
        assertTrue(techMemories.stream().allMatch(m -> "技术".equals(m.getCategory())));
    }
    
    @Test
    @DisplayName("应该能获取所有记忆")
    void testGetAllMemories() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("记忆1", "内容1"));
        memoryManager.saveMemory(createTestMemory("记忆2", "内容2"));
        memoryManager.saveMemory(createTestMemory("记忆3", "内容3"));
        
        // Act
        List<Memory> allMemories = memoryManager.getAllMemories();
        
        // Assert
        assertEquals(3, allMemories.size());
    }
    
    @Test
    @DisplayName("应该能删除记忆")
    void testDeleteMemory() {
        // Arrange
        Memory memory = memoryManager.saveMemory(createTestMemory("待删除", "内容"));
        String id = memory.getId();
        
        // Act
        boolean deleted = memoryManager.deleteMemory(id);
        
        // Assert
        assertTrue(deleted);
        assertFalse(memoryManager.getAllMemories().stream()
            .anyMatch(m -> m.getId().equals(id)));
    }
    
    @Test
    @DisplayName("删除不存在的记忆应该返回false")
    void testDeleteNonexistentMemoryReturnsFalse() {
        // Act
        boolean deleted = memoryManager.deleteMemory("nonexistent_id");
        
        // Assert
        assertFalse(deleted);
    }
    
    @Test
    @DisplayName("应该能清空所有记忆")
    void testClearMemories() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("记忆1", "内容1"));
        memoryManager.saveMemory(createTestMemory("记忆2", "内容2"));
        
        // Act
        memoryManager.clearMemories();
        
        // Assert
        assertEquals(0, memoryManager.getAllMemories().size());
    }
    
    @Test
    @DisplayName("应该能获取记忆统计信息")
    void testGetMemoryStats() {
        // Arrange
        Memory memory1 = createTestMemory("记忆1", "内容1");
        memory1.setCategory("技术");
        memoryManager.saveMemory(memory1);
        
        Memory memory2 = createTestMemory("记忆2", "内容2");
        memory2.setCategory("技术");
        memoryManager.saveMemory(memory2);
        
        Memory memory3 = createTestMemory("记忆3", "内容3");
        memory3.setCategory("业务");
        memoryManager.saveMemory(memory3);
        
        // Act
        Map<String, Integer> stats = memoryManager.getMemoryStats();
        
        // Assert
        assertNotNull(stats);
        assertEquals(3, stats.get("total"));
        assertEquals(2, stats.get("技术"));
        assertEquals(1, stats.get("业务"));
    }
    
    @Test
    @DisplayName("搜索应该不区分大小写")
    void testSearchIsCaseInsensitive() {
        // Arrange
        memoryManager.saveMemory(createTestMemory("Java编程", "Java内容"));
        
        // Act
        List<Memory> results1 = memoryManager.searchMemories("java");
        List<Memory> results2 = memoryManager.searchMemories("JAVA");
        List<Memory> results3 = memoryManager.searchMemories("Java");
        
        // Assert
        assertEquals(1, results1.size());
        assertEquals(1, results2.size());
        assertEquals(1, results3.size());
    }
    
    @Test
    @DisplayName("搜索结果应该限制为10个")
    void testSearchResultsLimitedToTen() {
        // Arrange
        for (int i = 0; i < 15; i++) {
            memoryManager.saveMemory(createTestMemory("记忆" + i, "通用内容"));
        }
        
        // Act
        List<Memory> results = memoryManager.searchMemories("通用");
        
        // Assert
        assertTrue(results.size() <= 10);
    }
    
    @Test
    @DisplayName("应该保留已有ID的记忆")
    void testPreserveExistingMemoryId() {
        // Arrange
        Memory memory = createTestMemory("测试", "内容");
        memory.setId("custom_id_123");
        
        // Act
        Memory saved = memoryManager.saveMemory(memory);
        
        // Assert
        assertEquals("custom_id_123", saved.getId());
    }
    
    /**
     * 创建测试记忆
     */
    private Memory createTestMemory(String title, String content) {
        Memory memory = new Memory();
        memory.setTitle(title);
        memory.setContent(content);
        memory.setCreatedAt(Instant.now());
        memory.setUpdatedAt(Instant.now());
        return memory;
    }
}
