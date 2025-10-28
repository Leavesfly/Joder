package io.leavesfly.joder.core;

import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.tools.ToolRegistry;
import io.leavesfly.joder.ui.components.MessageRenderer;
import io.leavesfly.joder.services.context.ContextCompressor;
import io.leavesfly.joder.services.memory.ProjectMemoryManager;
import io.leavesfly.joder.services.model.MockModelAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MainLoop 单元测试
 */
@DisplayName("MainLoop 主控制循环测试")
class MainLoopTest {
    
    @Mock
    private ToolRegistry toolRegistry;
    
    @Mock
    private MessageRenderer messageRenderer;
    
    @Mock
    private ProjectMemoryManager projectMemoryManager;
    
    @Mock
    private ContextCompressor contextCompressor;
    
    private MainLoop mainLoop;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mainLoop = new MainLoop(toolRegistry, messageRenderer, projectMemoryManager, contextCompressor);
        mainLoop.setCurrentModel(new MockModelAdapter());
    }
    
    @Test
    @DisplayName("应该正确处理用户输入")
    void testProcessUserInput() {
        // Given
        String userInput = "Hello, AI!";
        
        // When
        Message response = mainLoop.processUserInput(userInput);
        
        // Then
        assertNotNull(response, "Response should not be null");
        Assertions.assertEquals(MessageRole.ASSISTANT, response.getRole(), "Response role should be ASSISTANT");
        assertEquals(2, mainLoop.getHistorySize(), "History should contain 2 messages (user + assistant)");
    }
    
    @Test
    @DisplayName("应该正确管理消息历史")
    void testMessageHistoryManagement() {
        // When
        mainLoop.processUserInput("First message");
        mainLoop.processUserInput("Second message");
        
        // Then
        assertEquals(4, mainLoop.getHistorySize(), "History should contain 4 messages");
        
        var history = mainLoop.getMessageHistory();
        assertEquals(MessageRole.USER, history.get(0).getRole());
        assertEquals(MessageRole.ASSISTANT, history.get(1).getRole());
        assertEquals(MessageRole.USER, history.get(2).getRole());
        assertEquals(MessageRole.ASSISTANT, history.get(3).getRole());
    }
    
    @Test
    @DisplayName("应该能撤销最后一轮对话")
    void testUndoLastInteraction() {
        // Given
        mainLoop.processUserInput("Test message");
        assertEquals(2, mainLoop.getHistorySize());
        
        // When
        boolean undone = mainLoop.undoLastInteraction();
        
        // Then
        assertTrue(undone, "Undo should succeed");
        assertEquals(0, mainLoop.getHistorySize(), "History should be empty after undo");
    }
    
    @Test
    @DisplayName("历史不足时撤销应该失败")
    void testUndoWithInsufficientHistory() {
        // Given - empty history
        
        // When
        boolean undone = mainLoop.undoLastInteraction();
        
        // Then
        assertFalse(undone, "Undo should fail with empty history");
    }
    
    @Test
    @DisplayName("应该能清空历史记录")
    void testClearHistory() {
        // Given
        mainLoop.processUserInput("Test 1");
        mainLoop.processUserInput("Test 2");
        assertEquals(4, mainLoop.getHistorySize());
        
        // When
        mainLoop.clearHistory();
        
        // Then
        assertEquals(0, mainLoop.getHistorySize(), "History should be empty after clear");
    }
    
    @Test
    @DisplayName("应该能移除最后N条消息")
    void testRemoveLastMessages() {
        // Given
        mainLoop.processUserInput("Message 1");
        mainLoop.processUserInput("Message 2");
        mainLoop.processUserInput("Message 3");
        assertEquals(6, mainLoop.getHistorySize());
        
        // When
        mainLoop.removeLastMessages(2);
        
        // Then
        assertEquals(4, mainLoop.getHistorySize(), "Should have removed 2 messages");
    }
    
    @Test
    @DisplayName("应该能管理系统提示词")
    void testSystemPromptManagement() {
        // Given
        String prompt = "You are a helpful assistant.";
        
        // When
        mainLoop.setSystemPrompt(prompt);
        
        // Then
        assertEquals(prompt, mainLoop.getSystemPrompt());
        
        // When appending
        mainLoop.appendSystemPrompt("Additional instruction.");
        
        // Then
        assertTrue(mainLoop.getSystemPrompt().contains(prompt));
        assertTrue(mainLoop.getSystemPrompt().contains("Additional instruction."));
    }
    
    @Test
    @DisplayName("应该能加载项目记忆")
    void testLoadProjectMemory() {
        // Given
        when(projectMemoryManager.exists()).thenReturn(true);
        when(projectMemoryManager.load()).thenReturn("# Project Memory\n\nTest content");
        
        mainLoop.setSystemPrompt("Initial prompt");
        
        // When
        mainLoop.loadProjectMemory();
        
        // Then
        String systemPrompt = mainLoop.getSystemPrompt();
        assertTrue(systemPrompt.contains("<project_memory>"), "Should contain project memory tag");
        assertTrue(systemPrompt.contains("Test content"), "Should contain memory content");
        assertTrue(systemPrompt.contains("Initial prompt"), "Should preserve initial prompt");
        
        verify(projectMemoryManager).exists();
        verify(projectMemoryManager).load();
    }
    
    @Test
    @DisplayName("项目记忆不存在时不应加载")
    void testLoadProjectMemoryWhenNotExists() {
        // Given
        when(projectMemoryManager.exists()).thenReturn(false);
        
        // When
        mainLoop.loadProjectMemory();
        
        // Then
        verify(projectMemoryManager).exists();
        verify(projectMemoryManager, never()).load();
    }
    
    @Test
    @DisplayName("返回的消息历史应该是不可变的")
    void testMessageHistoryIsImmutable() {
        // Given
        mainLoop.processUserInput("Test");
        
        // When
        var history = mainLoop.getMessageHistory();
        
        // Then
        assertThrows(UnsupportedOperationException.class, () -> {
            history.add(new Message(MessageRole.USER, "Should fail"));
        }, "History from getMessageHistory() should be unmodifiable");
    }
}
