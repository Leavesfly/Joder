package io.leavesfly.joder.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具执行结果单元测试
 */
@DisplayName("ToolResult 工具结果测试")
public class ToolResultTest {
    
    @Test
    @DisplayName("应该创建成功的工具结果")
    public void testCreateSuccessResult() {
        // Arrange
        String output = "Command executed successfully";
        
        // Act
        ToolResult result = ToolResult.success(output);
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(output, result.getOutput());
        assertNull(result.getError());
    }
    
    @Test
    @DisplayName("应该创建失败的工具结果")
    public void testCreateErrorResult() {
        // Arrange
        String error = "Command failed with error";
        
        // Act
        ToolResult result = ToolResult.error(error);
        
        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getOutput());
        assertEquals(error, result.getError());
    }
    
    @Test
    @DisplayName("成功结果应该设置success为true")
    public void testSuccessResultHasSuccessTrue() {
        // Act
        ToolResult result = ToolResult.success("output");
        
        // Assert
        assertTrue(result.isSuccess());
    }
    
    @Test
    @DisplayName("错误结果应该设置success为false")
    public void testErrorResultHasSuccessFalse() {
        // Act
        ToolResult result = ToolResult.error("error");
        
        // Assert
        assertFalse(result.isSuccess());
    }
    
    @Test
    @DisplayName("成功结果的getError应该返回null")
    public void testSuccessResultErrorIsNull() {
        // Act
        ToolResult result = ToolResult.success("output");
        
        // Assert
        assertNull(result.getError());
    }
    
    @Test
    @DisplayName("错误结果的getOutput应该返回null")
    public void testErrorResultOutputIsNull() {
        // Act
        ToolResult result = ToolResult.error("error");
        
        // Assert
        assertNull(result.getOutput());
    }
    
    @Test
    @DisplayName("应该支持长输出内容")
    public void testSupportsLongOutput() {
        // Arrange
        String longOutput = "x".repeat(100000);
        
        // Act
        ToolResult result = ToolResult.success(longOutput);
        
        // Assert
        assertEquals(longOutput, result.getOutput());
    }
    
    @Test
    @DisplayName("应该支持长错误消息")
    public void testSupportsLongError() {
        // Arrange
        String longError = "Error: ".repeat(1000);
        
        // Act
        ToolResult result = ToolResult.error(longError);
        
        // Assert
        assertEquals(longError, result.getError());
    }
    
    @Test
    @DisplayName("应该支持多行输出")
    public void testSupportsMultilineOutput() {
        // Arrange
        String multilineOutput = "line1\nline2\nline3";
        
        // Act
        ToolResult result = ToolResult.success(multilineOutput);
        
        // Assert
        assertEquals(multilineOutput, result.getOutput());
    }
    
    @Test
    @DisplayName("应该正确生成成功结果的toString")
    public void testSuccessResultToString() {
        // Arrange
        ToolResult result = ToolResult.success("test output");
        
        // Act
        String toString = result.toString();
        
        // Assert
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("output"));
    }
    
    @Test
    @DisplayName("应该正确生成错误结果的toString")
    public void testErrorResultToString() {
        // Arrange
        ToolResult result = ToolResult.error("test error");
        
        // Act
        String toString = result.toString();
        
        // Assert
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains("error"));
    }
    
    @Test
    @DisplayName("应该允许空字符串输出")
    public void testAllowsEmptyOutput() {
        // Act
        ToolResult result = ToolResult.success("");
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals("", result.getOutput());
    }
    
    @Test
    @DisplayName("应该允许空字符串错误")
    public void testAllowsEmptyError() {
        // Act
        ToolResult result = ToolResult.error("");
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("", result.getError());
    }
}
