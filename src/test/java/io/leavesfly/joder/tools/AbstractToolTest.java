package io.leavesfly.joder.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抽象工具基类单元测试
 */
@DisplayName("AbstractTool 抽象工具测试")
public class AbstractToolTest {
    
    /**
     * 简单的工具实现用于测试
     */
    private static class SimpleTool extends AbstractTool {
        @Override
        public String getName() {
            return "SimpleTool";
        }
        
        @Override
        public String getDescription() {
            return "Simple tool for testing";
        }
        
        @Override
        public ToolResult call(Map<String, Object> input) {
            return ToolResult.success("Simple tool executed");
        }
    }
    
    /**
     * 只读工具实现
     */
    private static class ReadOnlyTool extends AbstractTool {
        @Override
        public String getName() {
            return "ReadOnlyTool";
        }
        
        @Override
        public String getDescription() {
            return "Read-only tool for testing";
        }
        
        @Override
        public boolean isReadOnly() {
            return true;
        }
        
        @Override
        public ToolResult call(Map<String, Object> input) {
            return ToolResult.success("Read-only tool executed");
        }
    }
    
    @Test
    @DisplayName("应该默认启用工具")
    public void testIsEnabledByDefault() {
        // Arrange
        Tool tool = new SimpleTool();
        
        // Act & Assert
        assertTrue(tool.isEnabled());
    }
    
    @Test
    @DisplayName("应该默认为非只读工具")
    public void testIsNotReadOnlyByDefault() {
        // Arrange
        Tool tool = new SimpleTool();
        
        // Act & Assert
        assertFalse(tool.isReadOnly());
    }
    
    @Test
    @DisplayName("应该默认支持并发调用")
    public void testIsConcurrencySafeByDefault() {
        // Arrange
        Tool tool = new SimpleTool();
        
        // Act & Assert
        assertTrue(tool.isConcurrencySafe());
    }
    
    @Test
    @DisplayName("修改权限的工具应该需要权限")
    public void testModifyingToolNeedsPermissions() {
        // Arrange
        Tool tool = new SimpleTool();
        
        // Act & Assert
        assertTrue(tool.needsPermissions());
    }
    
    @Test
    @DisplayName("只读工具不需要权限")
    public void testReadOnlyToolDoesNotNeedPermissions() {
        // Arrange
        Tool tool = new ReadOnlyTool();
        
        // Act & Assert
        assertFalse(tool.needsPermissions());
    }
    
    @Test
    @DisplayName("getPrompt应该返回描述作为默认")
    public void testGetPromptReturnsDescription() {
        // Arrange
        Tool tool = new SimpleTool();
        
        // Act
        String prompt = tool.getPrompt();
        
        // Assert
        assertEquals(tool.getDescription(), prompt);
    }
    
    @Test
    @DisplayName("应该生成工具使用消息")
    public void testRenderToolUseMessage() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        input.put("param1", "value1");
        
        // Act
        String message = tool.renderToolUseMessage(input);
        
        // Assert
        assertNotNull(message);
        assertTrue(message.contains("SimpleTool"));
    }
    
    @Test
    @DisplayName("应该生成成功结果消息")
    public void testRenderSuccessResultMessage() {
        // Arrange
        Tool tool = new SimpleTool();
        ToolResult result = ToolResult.success("Test output");
        
        // Act
        String message = tool.renderToolResultMessage(result);
        
        // Assert
        assertNotNull(message);
        assertTrue(message.contains("✓"));
        assertTrue(message.contains("Test output"));
    }
    
    @Test
    @DisplayName("应该生成失败结果消息")
    public void testRenderErrorResultMessage() {
        // Arrange
        Tool tool = new SimpleTool();
        ToolResult result = ToolResult.error("Test error");
        
        // Act
        String message = tool.renderToolResultMessage(result);
        
        // Assert
        assertNotNull(message);
        assertTrue(message.contains("✗"));
        assertTrue(message.contains("Test error"));
    }
    
    @Test
    @DisplayName("应该验证必需参数存在")
    public void testRequireParameterThrowsWhenMissing() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ((AbstractTool) tool).requireParameter(input, "missingParam");
        });
    }
    
    @Test
    @DisplayName("应该获取字符串参数")
    public void testGetStringParameter() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        input.put("name", "testValue");
        
        // Act
        String value = ((AbstractTool) tool).getString(input, "name");
        
        // Assert
        assertEquals("testValue", value);
    }
    
    @Test
    @DisplayName("应该获取字符串参数带默认值")
    public void testGetStringParameterWithDefault() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        
        // Act
        String value = ((AbstractTool) tool).getString(input, "name", "defaultValue");
        
        // Assert
        assertEquals("defaultValue", value);
    }
    
    @Test
    @DisplayName("应该获取整数参数")
    public void testGetIntParameter() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        input.put("count", 42);
        
        // Act
        Integer value = ((AbstractTool) tool).getInt(input, "count");
        
        // Assert
        assertEquals(42, value);
    }
    
    @Test
    @DisplayName("应该获取整数参数带默认值")
    public void testGetIntParameterWithDefault() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        
        // Act
        int value = ((AbstractTool) tool).getInt(input, "count", 100);
        
        // Assert
        assertEquals(100, value);
    }
    
    @Test
    @DisplayName("应该获取布尔参数")
    public void testGetBooleanParameter() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        input.put("enabled", true);
        
        // Act
        Boolean value = ((AbstractTool) tool).getBoolean(input, "enabled");
        
        // Assert
        assertTrue(value);
    }
    
    @Test
    @DisplayName("应该获取布尔参数带默认值")
    public void testGetBooleanParameterWithDefault() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        
        // Act
        boolean value = ((AbstractTool) tool).getBoolean(input, "enabled", false);
        
        // Assert
        assertFalse(value);
    }
    
    @Test
    @DisplayName("应该从字符串转换整数参数")
    public void testGetIntFromStringParameter() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        input.put("count", "42");
        
        // Act
        Integer value = ((AbstractTool) tool).getInt(input, "count");
        
        // Assert
        assertEquals(42, value);
    }
    
    @Test
    @DisplayName("应该从字符串转换布尔参数")
    public void testGetBooleanFromStringParameter() {
        // Arrange
        Tool tool = new SimpleTool();
        Map<String, Object> input = new HashMap<>();
        input.put("enabled", "true");
        
        // Act
        Boolean value = ((AbstractTool) tool).getBoolean(input, "enabled");
        
        // Assert
        assertTrue(value);
    }
}
