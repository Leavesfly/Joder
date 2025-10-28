package io.shareai.joder.hooks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DoublePressHook 测试
 */
public class DoublePressHookTest {
    
    @Test
    public void testSinglePress() throws InterruptedException {
        DoublePressHook hook = new DoublePressHook();
        
        final boolean[] firstPressed = {false};
        final boolean[] doublePressed = {false};
        final boolean[] pending = {false};
        
        hook.handlePress(
            () -> firstPressed[0] = true,
            () -> doublePressed[0] = true,
            p -> pending[0] = p
        );
        
        // 第一次按键应该触发 firstPress
        assertTrue(firstPressed[0]);
        assertFalse(doublePressed[0]);
        assertTrue(pending[0]);
        
        // 等待超时
        Thread.sleep(2100);
        
        // 超时后应该不再是 pending 状态
        assertFalse(hook.isPending());
    }
    
    @Test
    public void testDoublePress() throws InterruptedException {
        DoublePressHook hook = new DoublePressHook();
        
        final boolean[] doublePressed = {false};
        final boolean[] pending = {false};
        
        // 第一次按键
        hook.handlePress(
            null,
            () -> doublePressed[0] = true,
            p -> pending[0] = p
        );
        
        assertTrue(pending[0]);
        assertFalse(doublePressed[0]);
        
        // 在 2 秒内第二次按键
        Thread.sleep(500);
        hook.handlePress(
            null,
            () -> doublePressed[0] = true,
            p -> pending[0] = p
        );
        
        // 应该触发双击
        assertTrue(doublePressed[0]);
        assertFalse(pending[0]);
    }
    
    @Test
    public void testReset() {
        DoublePressHook hook = new DoublePressHook();
        
        final boolean[] pending = {false};
        
        hook.handlePress(
            null,
            () -> {},
            p -> pending[0] = p
        );
        
        assertTrue(pending[0]);
        
        hook.reset();
        
        assertFalse(hook.isPending());
    }
}
