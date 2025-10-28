package io.shareai.joder.tools.file;

import io.shareai.joder.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件读取工具单元测试
 */
@DisplayName("FileReadTool 文件读取工具测试")
public class FileReadToolTest {
    
    @TempDir
    Path tempDir;
    
    private FileReadTool fileReadTool;
    
    @BeforeEach
    public void setUp() {
        fileReadTool = new FileReadTool();
    }
    
    @Test
    @DisplayName("应该返回正确的工具名称")
    public void testGetName() {
        // Act
        String name = fileReadTool.getName();
        
        // Assert
        assertEquals("FileReadTool", name);
    }
    
    @Test
    @DisplayName("应该返回有效的描述")
    public void testGetDescription() {
        // Act
        String description = fileReadTool.getDescription();
        
        // Assert
        assertNotNull(description);
        assertFalse(description.isEmpty());
    }
    
    @Test
    @DisplayName("应该是只读工具")
    public void testIsReadOnly() {
        // Act
        boolean readOnly = fileReadTool.isReadOnly();
        
        // Assert
        assertTrue(readOnly);
    }
    
    @Test
    @DisplayName("应该能读取简单文本文件")
    public void testReadSimpleTextFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Hello, World!".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Hello, World!"));
    }
    
    @Test
    @DisplayName("应该能读取多行文件")
    public void testReadMultilineFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("multiline.txt");
        Files.write(testFile, "Line 1\nLine 2\nLine 3".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Line 1"));
        assertTrue(result.getOutput().contains("Line 2"));
        assertTrue(result.getOutput().contains("Line 3"));
    }
    
    @Test
    @DisplayName("读取不存在的文件应该返回错误")
    public void testReadNonexistentFile() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("path", "/nonexistent/file.txt");
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("不存在"));
    }
    
    @Test
    @DisplayName("缺少必需参数应该返回错误")
    public void testMissingPathParameter() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("缺少") || result.getError().contains("参数"));
    }
    
    @Test
    @DisplayName("应该能读取指定行范围的文件")
    public void testReadFileWithLineRange() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("range.txt");
        Files.write(testFile, "Line 1\nLine 2\nLine 3\nLine 4".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        input.put("startLine", 1);
        input.put("endLine", 3);
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
        String output = result.getOutput();
        assertTrue(output.contains("Line 2"));
        assertTrue(output.contains("Line 3"));
    }
    
    @Test
    @DisplayName("应该在读取目录时返回错误")
    public void testReadDirectory() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("path", tempDir.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("不是一个文件"));
    }
    
    @Test
    @DisplayName("应该能读取空文件")
    public void testReadEmptyFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("empty.txt");
        Files.write(testFile, "".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
    }
    
    @Test
    @DisplayName("应该在起始行超出范围时返回错误")
    public void testStartLineOutOfRange() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Line 1\nLine 2".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        input.put("startLine", 100);
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("超出文件范围"));
    }
    
    @Test
    @DisplayName("应该能读取包含特殊字符的文件")
    public void testReadFileWithSpecialCharacters() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("special.txt");
        Files.write(testFile, "特殊字符: !@#$%^&*()".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("特殊字符"));
    }
    
    @Test
    @DisplayName("应该能读取包含长行的文件")
    public void testReadFileWithLongLines() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("long.txt");
        String longLine = "x".repeat(1000);
        Files.write(testFile, longLine.getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
    }
    
    @Test
    @DisplayName("应该显示正确的行号")
    public void testDisplayCorrectLineNumbers() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("numbered.txt");
        Files.write(testFile, "First\nSecond\nThird".getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
        String output = result.getOutput();
        assertTrue(output.contains("1:") || output.contains("1 "));
    }
    
    @Test
    @DisplayName("应该生成正确的工具使用消息")
    public void testRenderToolUseMessage() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("path", "/path/to/file.txt");
        
        // Act
        String message = fileReadTool.renderToolUseMessage(input);
        
        // Assert
        assertNotNull(message);
        assertTrue(message.contains("file.txt"));
    }
    
    @Test
    @DisplayName("应该生成包含行范围的工具使用消息")
    public void testRenderToolUseMessageWithRange() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("path", "/path/to/file.txt");
        input.put("startLine", 1);
        input.put("endLine", 10);
        
        // Act
        String message = fileReadTool.renderToolUseMessage(input);
        
        // Assert
        assertNotNull(message);
        assertTrue(message.contains("file.txt"));
    }
    
    @Test
    @DisplayName("应该能读取非常大的文件")
    public void testReadLargeFile() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("large.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(testFile, content.toString().getBytes());
        
        Map<String, Object> input = new HashMap<>();
        input.put("path", testFile.toString());
        
        // Act
        ToolResult result = fileReadTool.call(input);
        
        // Assert
        assertTrue(result.isSuccess());
    }
}
