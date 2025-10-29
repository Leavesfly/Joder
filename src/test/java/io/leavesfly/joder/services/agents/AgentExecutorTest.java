package io.leavesfly.joder.services.agents;

import io.leavesfly.joder.domain.AgentConfig;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.services.model.ModelAdapterFactory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentExecutorTest {
    
    @Mock
    private AgentsManager agentsManager;
    
    @Mock
    private ModelAdapterFactory modelAdapterFactory;
    
    @Mock
    private ToolRegistry toolRegistry;
    
    @Mock
    private ModelAdapter modelAdapter;
    
    private AgentExecutor agentExecutor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        agentExecutor = new AgentExecutor(agentsManager, modelAdapterFactory, toolRegistry);
    }
    
    @Test
    void testExecuteAgentWithValidConfig() {
        // Given
        String agentName = "test-agent";
        String userInput = "测试任务";
        
        AgentConfig config = new AgentConfig();
        config.setName(agentName);
        config.setDescription("测试代理");
        config.setSystemPrompt("你是一个测试代理");
        config.setModel("claude-3-5-sonnet");
        config.setTools(List.of("FileRead", "FileWrite"));
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.of(config));
        when(modelAdapterFactory.createAdapter("claude-3-5-sonnet")).thenReturn(modelAdapter);
        when(modelAdapter.sendMessage(anyList(), anyString())).thenReturn("测试响应");
        when(toolRegistry.getAllTools()).thenReturn(Collections.emptyList());
        
        // When
        Message result = agentExecutor.execute(agentName, userInput);
        
        // Then
        assertNotNull(result);
        assertEquals(MessageRole.ASSISTANT, result.getRole());
        assertEquals("测试响应", result.getContent());
        
        verify(agentsManager).getAgent(agentName);
        verify(modelAdapterFactory).createAdapter("claude-3-5-sonnet");
        verify(modelAdapter).sendMessage(anyList(), anyString());
    }
    
    @Test
    void testExecuteAgentNotFound() {
        // Given
        String agentName = "non-existent";
        String userInput = "任务";
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            agentExecutor.execute(agentName, userInput);
        });
    }
    
    @Test
    void testExecuteAgentWithDefaultModel() {
        // Given
        String agentName = "test-agent";
        String userInput = "测试任务";
        
        AgentConfig config = new AgentConfig();
        config.setName(agentName);
        config.setSystemPrompt("你是一个测试代理");
        // 不设置 model，应使用默认模型
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.of(config));
        when(modelAdapterFactory.createDefaultAdapter()).thenReturn(modelAdapter);
        when(modelAdapter.sendMessage(anyList(), anyString())).thenReturn("默认模型响应");
        when(toolRegistry.getAllTools()).thenReturn(Collections.emptyList());
        
        // When
        Message result = agentExecutor.execute(agentName, userInput);
        
        // Then
        assertNotNull(result);
        assertEquals("默认模型响应", result.getContent());
        verify(modelAdapterFactory).createDefaultAdapter();
    }
    
    @Test
    void testExecuteAgentWithToolFiltering() {
        // Given
        String agentName = "test-agent";
        String userInput = "测试任务";
        
        AgentConfig config = new AgentConfig();
        config.setName(agentName);
        config.setSystemPrompt("你是一个测试代理");
        config.setTools(List.of("FileRead", "FileWrite"));
        
        Tool mockTool1 = mock(Tool.class);
        when(mockTool1.getName()).thenReturn("FileRead");
        when(mockTool1.getDescription()).thenReturn("读取文件");
        
        Tool mockTool2 = mock(Tool.class);
        when(mockTool2.getName()).thenReturn("Bash");
        when(mockTool2.getDescription()).thenReturn("执行命令");
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.of(config));
        when(modelAdapterFactory.createDefaultAdapter()).thenReturn(modelAdapter);
        when(modelAdapter.sendMessage(anyList(), anyString())).thenReturn("响应");
        when(toolRegistry.getAllTools()).thenReturn(List.of(mockTool1, mockTool2));
        
        // When
        Message result = agentExecutor.execute(agentName, userInput);
        
        // Then
        assertNotNull(result);
        // 验证系统提示词包含工具限制说明
        verify(modelAdapter).sendMessage(anyList(), contains("FileRead"));
    }
    
    @Test
    void testExecuteAgentWithAllTools() {
        // Given
        String agentName = "test-agent";
        String userInput = "测试任务";
        
        AgentConfig config = new AgentConfig();
        config.setName(agentName);
        config.setSystemPrompt("你是一个测试代理");
        config.setTools(List.of("*")); // 通配符，所有工具
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.of(config));
        when(modelAdapterFactory.createDefaultAdapter()).thenReturn(modelAdapter);
        when(modelAdapter.sendMessage(anyList(), anyString())).thenReturn("响应");
        when(toolRegistry.getAllTools()).thenReturn(Collections.emptyList());
        
        // When
        Message result = agentExecutor.execute(agentName, userInput);
        
        // Then
        assertNotNull(result);
    }
    
    @Test
    void testHasAgent() {
        // Given
        String agentName = "test-agent";
        when(agentsManager.hasAgent(agentName)).thenReturn(true);
        
        // When
        boolean result = agentExecutor.hasAgent(agentName);
        
        // Then
        assertTrue(result);
        verify(agentsManager).hasAgent(agentName);
    }
    
    @Test
    void testGetAgentConfig() {
        // Given
        String agentName = "test-agent";
        AgentConfig config = new AgentConfig();
        config.setName(agentName);
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.of(config));
        
        // When
        AgentConfig result = agentExecutor.getAgentConfig(agentName);
        
        // Then
        assertNotNull(result);
        assertEquals(agentName, result.getName());
    }
    
    @Test
    void testExecuteWithInvalidConfig() {
        // Given
        String agentName = "invalid-agent";
        String userInput = "任务";
        
        AgentConfig config = new AgentConfig();
        config.setName(agentName);
        // 缺少 systemPrompt，配置无效
        
        when(agentsManager.getAgent(agentName)).thenReturn(Optional.of(config));
        
        // When & Then - 应该抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            agentExecutor.execute(agentName, userInput);
        });
    }
}
