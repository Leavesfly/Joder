package io.leavesfly.joder.hooks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * StartupTimeHook 测试
 */
public class StartupTimeHookTest {
    
    @Test
    public void testGetUptimeMs() throws InterruptedException {
        StartupTimeHook hook = new StartupTimeHook();
        
        long uptime1 = hook.getUptimeMs();
        assertTrue(uptime1 >= 0);
        
        // 等待一小段时间
        Thread.sleep(100);
        
        long uptime2 = hook.getUptimeMs();
        assertTrue(uptime2 > uptime1);
    }
    
    @Test
    public void testGetUptimeSeconds() throws InterruptedException {
        StartupTimeHook hook = new StartupTimeHook();
        
        Thread.sleep(1100);
        
        long uptimeSeconds = hook.getUptimeSeconds();
        assertTrue(uptimeSeconds >= 1);
    }
    
    @Test
    public void testGetFormattedUptime() throws InterruptedException {
        StartupTimeHook hook = new StartupTimeHook();
        
        String formatted = hook.getFormattedUptime();
        assertNotNull(formatted);
        assertTrue(formatted.endsWith("s") || formatted.contains("m") || formatted.contains("h"));
    }
    
    @Test
    public void testLogStartupTime() {
        StartupTimeHook hook = new StartupTimeHook();
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> hook.logStartupTime());
    }
}
