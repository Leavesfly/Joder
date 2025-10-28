package io.leavesfly.joder.hooks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandHistoryHook 测试
 */
public class CommandHistoryHookTest {
    
    @Test
    public void testAddToHistory() {
        CommandHistoryHook hook = new CommandHistoryHook();
        
        hook.addToHistory("ls");
        hook.addToHistory("pwd");
        hook.addToHistory("cd /tmp");
        
        assertEquals(3, hook.getHistorySize());
    }
    
    @Test
    public void testNavigateUp() {
        CommandHistoryHook hook = new CommandHistoryHook();
        
        hook.addToHistory("command1");
        hook.addToHistory("command2");
        hook.addToHistory("command3");
        
        String current = "";
        
        // 向上导航应该返回最后一条命令
        String prev1 = hook.navigateUp(current);
        assertEquals("command3", prev1);
        
        // 再次向上应该返回倒数第二条
        String prev2 = hook.navigateUp(prev1);
        assertEquals("command2", prev2);
        
        // 再次向上应该返回第一条
        String prev3 = hook.navigateUp(prev2);
        assertEquals("command1", prev3);
    }
    
    @Test
    public void testNavigateDown() {
        CommandHistoryHook hook = new CommandHistoryHook();
        
        hook.addToHistory("command1");
        hook.addToHistory("command2");
        
        // 先向上两次
        String prev1 = hook.navigateUp("");
        String prev2 = hook.navigateUp(prev1);
        
        // 然后向下一次
        String next = hook.navigateDown(prev2);
        assertEquals("command2", next);
    }
    
    @Test
    public void testAvoidDuplicates() {
        CommandHistoryHook hook = new CommandHistoryHook();
        
        hook.addToHistory("ls");
        hook.addToHistory("ls");  // 重复，不应该添加
        hook.addToHistory("pwd");
        
        assertEquals(2, hook.getHistorySize());
    }
    
    @Test
    public void testClearHistory() {
        CommandHistoryHook hook = new CommandHistoryHook();
        
        hook.addToHistory("command1");
        hook.addToHistory("command2");
        
        hook.clearHistory();
        
        assertEquals(0, hook.getHistorySize());
    }
    
    @Test
    public void testMaxHistorySize() {
        CommandHistoryHook hook = new CommandHistoryHook();
        
        // 添加超过最大限制的命令
        for (int i = 0; i < 1100; i++) {
            hook.addToHistory("command" + i);
        }
        
        // 应该只保留最后 1000 条
        assertEquals(1000, hook.getHistorySize());
    }
}
