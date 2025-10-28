package io.shareai.joder.integration;

import io.shareai.joder.services.completion.CompletionService;
import io.shareai.joder.services.completion.CompletionSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 命令补全集成测试
 */
public class CompletionIntegrationTest {

    private CompletionService completionService;

    @BeforeEach
    public void setUp() {
        completionService = new CompletionService();
    }

    @Test
    public void testJoderCommandCompletion() {
        List<CompletionSuggestion> suggestions = completionService.getCompletions("/h", 5);
        
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getText().equals("/help")));
    }

    @Test
    public void testSystemCommandCompletion() {
        // 使用更容易匹配的输入
        List<CompletionSuggestion> suggestions = completionService.getCompletions("g", 10);
        
        assertFalse(suggestions.isEmpty());
        // 检查是否有 git 或其他 g 开头的命令
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getText().startsWith("g")));
    }

    @Test
    public void testFuzzyMatching() {
        // 测试模糊匹配：nde -> node
        List<CompletionSuggestion> suggestions = completionService.getCompletions("nde", 5);
        
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getText().equals("node")));
    }

    @Test
    public void testRecentCommands() {
        // 添加最近命令
        completionService.addRecentCommand("custom-command");
        
        List<CompletionSuggestion> suggestions = completionService.getCompletions("cus", 5);
        
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getText().equals("custom-command")));
    }

    @Test
    public void testCompletionStats() {
        CompletionService.CompletionStats stats = completionService.getStats();
        
        assertTrue(stats.getSystemCommandCount() > 0);
        assertTrue(stats.getJoderCommandCount() > 0);
        assertTrue(stats.getTotalCommandCount() > 0);
    }

    @Test
    public void testClearRecentCommands() {
        completionService.addRecentCommand("test1");
        completionService.addRecentCommand("test2");
        
        assertTrue(completionService.getStats().getRecentCommandCount() > 0);
        
        completionService.clearRecentCommands();
        
        assertEquals(0, completionService.getStats().getRecentCommandCount());
    }
}
