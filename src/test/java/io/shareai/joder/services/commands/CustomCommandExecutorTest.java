package io.shareai.joder.services.commands;

import io.shareai.joder.domain.CustomCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomCommandExecutor 单元测试
 */
class CustomCommandExecutorTest {
    
    @TempDir
    Path tempDir;
    
    private CustomCommandExecutor executor;
    
    @BeforeEach
    void setUp() {
        executor = new CustomCommandExecutor(tempDir.toString());
    }
    
    @Test
    void testSimpleExecution() throws IOException {
        CustomCommand command = new CustomCommand();
        command.setContent("Hello World");
        
        String result = executor.execute(command, null);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    void testArgumentsPlaceholder() throws IOException {
        CustomCommand command = new CustomCommand();
        command.setContent("Run with $ARGUMENTS");
        
        String result = executor.execute(command, "arg1 arg2");
        
        assertEquals("Run with arg1 arg2", result);
    }
    
    @Test
    void testNamedArguments() throws IOException {
        CustomCommand command = new CustomCommand();
        command.setContent("File: {filename}, Mode: {mode}");
        command.setArgNames(Arrays.asList("filename", "mode"));
        
        String result = executor.execute(command, "test.txt debug");
        
        assertEquals("File: test.txt, Mode: debug", result);
    }
    
    @Test
    void testBashCommandExecution() throws IOException {
        CustomCommand command = new CustomCommand();
        command.setContent("Current directory: !`pwd`");
        
        String result = executor.execute(command, null);
        
        assertTrue(result.startsWith("Current directory:"));
        assertTrue(result.contains(tempDir.toString()));
    }
    
    @Test
    void testFileReference() throws IOException {
        // 创建测试文件
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "File content");
        
        CustomCommand command = new CustomCommand();
        command.setContent("File content: @test.txt");
        
        String result = executor.execute(command, null);
        
        assertTrue(result.contains("File content"));
        assertTrue(result.contains("## File: test.txt"));
    }
    
    @Test
    void testToolRestrictions() throws IOException {
        CustomCommand command = new CustomCommand();
        command.setContent("Do something");
        command.setAllowedTools(Arrays.asList("BashTool", "FileRead"));
        
        String result = executor.execute(command, null);
        
        assertTrue(result.contains("restricted to using only these tools"));
        assertTrue(result.contains("BashTool, FileRead"));
    }
}
