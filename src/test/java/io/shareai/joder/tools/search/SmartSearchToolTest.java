package io.shareai.joder.tools.search;

import io.shareai.joder.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SmartSearchTool 单元测试
 */
class SmartSearchToolTest {
    
    @TempDir
    Path tempDir;
    
    private SearchStrategyAnalyzer mockStrategyAnalyzer;
    private SearchExecutor mockSearchExecutor;
    private SmartSearchTool smartSearchTool;
    
    @BeforeEach
    void setUp() {
        mockStrategyAnalyzer = mock(SearchStrategyAnalyzer.class);
        mockSearchExecutor = mock(SearchExecutor.class);
        
        smartSearchTool = new SmartSearchTool(
            tempDir.toString(),
            mockStrategyAnalyzer,
            mockSearchExecutor
        );
    }
    
    @Test
    void testSuccessfulSearch() throws IOException {
        // 准备测试数据
        String query = "查找用户认证代码";
        
        // 模拟策略分析
        SearchStrategy strategy = new SearchStrategy();
        strategy.setRegex("(auth|login|authenticate)");
        strategy.setSearchPath(".");
        when(mockStrategyAnalyzer.analyze(anyString(), anyString())).thenReturn(strategy);
        
        // 模拟搜索执行
        SearchMatch match = new SearchMatch();
        match.setFilePath(tempDir.resolve("AuthService.java"));
        match.setLastModified(System.currentTimeMillis());
        match.addMatchedLine(new SearchMatch.MatchedLine(10, "public class AuthService {", 13, 4));
        
        when(mockSearchExecutor.execute(any(SearchStrategy.class), any(Path.class)))
            .thenReturn(List.of(match));
        
        // 执行搜索
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        
        ToolResult result = smartSearchTool.call(input);
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().contains("AuthService.java"));
        assertTrue(result.getOutput().contains("找到 1 个匹配"));
        
        // 验证调用
        verify(mockStrategyAnalyzer, times(1)).analyze(eq(query), anyString());
        verify(mockSearchExecutor, times(1)).execute(any(SearchStrategy.class), any(Path.class));
    }
    
    @Test
    void testEmptyQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", "");
        
        ToolResult result = smartSearchTool.call(input);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("不能为空"));
    }
    
    @Test
    void testSearchWithContext() throws IOException {
        String query = "查找配置文件";
        String context = "这是一个Java Maven项目";
        
        SearchStrategy strategy = new SearchStrategy();
        strategy.setRegex(".*\\.properties|.*\\.yaml|.*\\.yml");
        when(mockStrategyAnalyzer.analyze(anyString(), anyString())).thenReturn(strategy);
        when(mockSearchExecutor.execute(any(SearchStrategy.class), any(Path.class)))
            .thenReturn(List.of());
        
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        input.put("context", context);
        
        ToolResult result = smartSearchTool.call(input);
        
        assertTrue(result.isSuccess());
        verify(mockStrategyAnalyzer, times(1)).analyze(eq(query), eq(context));
    }
    
    @Test
    void testFilesOnlyMode() throws IOException {
        String query = "查找Java文件";
        
        SearchStrategy strategy = new SearchStrategy();
        strategy.setRegex(".*\\.java");
        when(mockStrategyAnalyzer.analyze(anyString(), anyString())).thenReturn(strategy);
        
        SearchMatch match = new SearchMatch();
        match.setFilePath(tempDir.resolve("Test.java"));
        match.setLastModified(System.currentTimeMillis());
        when(mockSearchExecutor.execute(any(SearchStrategy.class), any(Path.class)))
            .thenReturn(List.of(match));
        
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        input.put("filesOnly", true);
        
        ToolResult result = smartSearchTool.call(input);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Test.java"));
    }
    
    @Test
    void testMaxResults() throws IOException {
        String query = "查找所有文件";
        
        SearchStrategy strategy = new SearchStrategy();
        strategy.setRegex(".*");
        when(mockStrategyAnalyzer.analyze(anyString(), anyString())).thenReturn(strategy);
        when(mockSearchExecutor.execute(any(SearchStrategy.class), any(Path.class)))
            .thenReturn(List.of());
        
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        input.put("maxResults", 50);
        
        ToolResult result = smartSearchTool.call(input);
        
        assertTrue(result.isSuccess());
        verify(mockSearchExecutor, times(1)).execute(
            argThat(s -> s.getMaxResults() == 50), any(Path.class));
    }
    
    @Test
    void testAnalyzerFailure() {
        String query = "测试查询";
        
        when(mockStrategyAnalyzer.analyze(anyString(), anyString()))
            .thenThrow(new RuntimeException("分析失败"));
        
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        
        ToolResult result = smartSearchTool.call(input);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("搜索失败"));
    }
    
    @Test
    void testExecutorFailure() throws IOException {
        String query = "测试查询";
        
        SearchStrategy strategy = new SearchStrategy();
        strategy.setRegex("test");
        when(mockStrategyAnalyzer.analyze(anyString(), anyString())).thenReturn(strategy);
        when(mockSearchExecutor.execute(any(SearchStrategy.class), any(Path.class)))
            .thenThrow(new IOException("搜索执行失败"));
        
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        
        ToolResult result = smartSearchTool.call(input);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("搜索失败"));
    }
    
    @Test
    void testNoResults() throws IOException {
        String query = "查找不存在的内容";
        
        SearchStrategy strategy = new SearchStrategy();
        strategy.setRegex("nonexistent");
        when(mockStrategyAnalyzer.analyze(anyString(), anyString())).thenReturn(strategy);
        when(mockSearchExecutor.execute(any(SearchStrategy.class), any(Path.class)))
            .thenReturn(List.of());
        
        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        
        ToolResult result = smartSearchTool.call(input);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("未找到匹配"));
    }
}
