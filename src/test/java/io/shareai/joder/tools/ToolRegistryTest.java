package io.shareai.joder.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具注册表单元测试
 */
@DisplayName("ToolRegistry 工具注册表测试")
public class ToolRegistryTest {
    
    private ToolRegistry registry;
    private TestTool testTool1;
    private TestTool testTool2;
    
    /**
     * 测试工具实现
     */
    private static class TestTool extends AbstractTool {
        private final String name;
        private final String description;
        private final boolean readOnly;
        
        public TestTool(String name, String description) {
            this(name, description, false);
        }
        
        public TestTool(String name, String description, boolean readOnly) {
            this.name = name;
            this.description = description;
            this.readOnly = readOnly;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getDescription() {
            return description;
        }
        
        @Override
        public boolean isReadOnly() {
            return readOnly;
        }
        
        @Override
        public ToolResult call(Map<String, Object> input) {
            return ToolResult.success("Tool " + name + " executed");
        }
    }
    
    @BeforeEach
    public void setUp() {
        registry = new ToolRegistry();
        testTool1 = new TestTool("TestTool1", "Description 1");
        testTool2 = new TestTool("TestTool2", "Description 2", true);
    }
    
    @Test
    @DisplayName("应该能注册工具")
    public void testRegisterTool() {
        // Act
        registry.registerTool(testTool1);
        
        // Assert
        assertTrue(registry.hasTool("TestTool1"));
        assertEquals(testTool1, registry.getTool("TestTool1"));
    }
    
    @Test
    @DisplayName("应该不允许注册空工具")
    public void testCannotRegisterNullTool() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerTool(null);
        });
    }
    
    @Test
    @DisplayName("应该不允许注册名称为空的工具")
    public void testCannotRegisterToolWithEmptyName() {
        // Arrange
        Tool toolWithEmptyName = new AbstractTool() {
            @Override
            public String getName() {
                return "";
            }
            
            @Override
            public String getDescription() {
                return "Test";
            }
            
            @Override
            public ToolResult call(Map<String, Object> input) {
                return ToolResult.success("Test");
            }
        };
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerTool(toolWithEmptyName);
        });
    }
    
    @Test
    @DisplayName("应该能注册多个工具")
    public void testRegisterMultipleTools() {
        // Act
        registry.registerTools(testTool1, testTool2);
        
        // Assert
        assertTrue(registry.hasTool("TestTool1"));
        assertTrue(registry.hasTool("TestTool2"));
        assertEquals(2, registry.size());
    }
    
    @Test
    @DisplayName("应该能获取已注册的工具")
    public void testGetRegisteredTool() {
        // Arrange
        registry.registerTool(testTool1);
        
        // Act
        Tool retrieved = registry.getTool("TestTool1");
        
        // Assert
        assertEquals(testTool1, retrieved);
    }
    
    @Test
    @DisplayName("获取不存在的工具应该返回null")
    public void testGetNonexistentToolReturnsNull() {
        // Act
        Tool tool = registry.getTool("NonexistentTool");
        
        // Assert
        assertNull(tool);
    }
    
    @Test
    @DisplayName("应该能检查工具是否存在")
    public void testCheckToolExists() {
        // Arrange
        registry.registerTool(testTool1);
        
        // Act & Assert
        assertTrue(registry.hasTool("TestTool1"));
        assertFalse(registry.hasTool("NonexistentTool"));
    }
    
    @Test
    @DisplayName("应该能获取所有工具名称")
    public void testGetAllToolNames() {
        // Arrange
        registry.registerTool(testTool1);
        registry.registerTool(testTool2);
        
        // Act
        Set<String> names = registry.getToolNames();
        
        // Assert
        assertEquals(2, names.size());
        assertTrue(names.contains("TestTool1"));
        assertTrue(names.contains("TestTool2"));
    }
    
    @Test
    @DisplayName("应该能获取所有工具")
    public void testGetAllTools() {
        // Arrange
        registry.registerTool(testTool1);
        registry.registerTool(testTool2);
        
        // Act
        var tools = registry.getAllTools();
        
        // Assert
        assertEquals(2, tools.size());
        assertTrue(tools.contains(testTool1));
        assertTrue(tools.contains(testTool2));
    }
    
    @Test
    @DisplayName("应该能获取已启用的工具")
    public void testGetEnabledTools() {
        // Arrange
        registry.registerTool(testTool1);
        registry.registerTool(testTool2);
        
        // Act
        var enabledTools = registry.getEnabledTools();
        
        // Assert
        assertEquals(2, enabledTools.size());
        assertTrue(enabledTools.stream().allMatch(Tool::isEnabled));
    }
    
    @Test
    @DisplayName("应该能移除工具")
    public void testRemoveTool() {
        // Arrange
        registry.registerTool(testTool1);
        registry.registerTool(testTool2);
        
        // Act
        registry.removeTool("TestTool1");
        
        // Assert
        assertFalse(registry.hasTool("TestTool1"));
        assertTrue(registry.hasTool("TestTool2"));
        assertEquals(1, registry.size());
    }
    
    @Test
    @DisplayName("应该能清空所有工具")
    public void testClearAllTools() {
        // Arrange
        registry.registerTool(testTool1);
        registry.registerTool(testTool2);
        
        // Act
        registry.clear();
        
        // Assert
        assertEquals(0, registry.size());
        assertFalse(registry.hasTool("TestTool1"));
        assertFalse(registry.hasTool("TestTool2"));
    }
    
    @Test
    @DisplayName("应该能获取工具数量")
    public void testGetToolCount() {
        // Arrange
        registry.registerTool(testTool1);
        registry.registerTool(testTool2);
        
        // Act
        int size = registry.size();
        
        // Assert
        assertEquals(2, size);
    }
    
    @Test
    @DisplayName("应该在初始化时为空")
    public void testEmptyOnInitialization() {
        // Act & Assert
        assertEquals(0, registry.size());
        assertTrue(registry.getToolNames().isEmpty());
    }
    
    @Test
    @DisplayName("应该支持工具覆盖（重新注册同名工具）")
    public void testToolOverride() {
        // Arrange
        TestTool tool1 = new TestTool("SameName", "Description 1");
        TestTool tool2 = new TestTool("SameName", "Description 2");
        
        registry.registerTool(tool1);
        
        // Act
        registry.registerTool(tool2);
        
        // Assert
        assertEquals(1, registry.size());
        assertEquals(tool2, registry.getTool("SameName"));
    }
    
    @Test
    @DisplayName("应该支持从不同来源的工具")
    public void testSupportMultipleToolTypes() {
        // Arrange
        Tool tool1 = new TestTool("ReadOnly", "Read only tool", true);
        Tool tool2 = new TestTool("WriteEnabled", "Write tool", false);
        
        // Act
        registry.registerTool(tool1);
        registry.registerTool(tool2);
        
        // Assert
        assertEquals(2, registry.size());
        assertTrue(registry.getTool("ReadOnly").isReadOnly());
        assertFalse(registry.getTool("WriteEnabled").isReadOnly());
    }
}
