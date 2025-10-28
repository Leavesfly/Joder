package io.leavesfly.joder.ui.input;

import io.leavesfly.joder.hooks.CancelRequestHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CancellationHandler 单元测试
 * 注意：不测试双击 Ctrl+C 的 System.exit() 逻辑，以避免测试崩溃
 */
@DisplayName("CancellationHandler 取消处理器测试")
class CancellationHandlerTest {
    
    private CancelRequestHook cancelHook;
    private CancellationHandler handler;
    
    @BeforeEach
    void setUp() {
        cancelHook = new CancelRequestHook();
        handler = new CancellationHandler(cancelHook);
    }
    
    @Test
    @DisplayName("应该正确处理单次取消请求")
    void testHandleSingleCancellation() {
        assertFalse(handler.isCancelled());
        
        handler.handleCancellation();
        
        assertTrue(handler.isCancelled());
    }
    
    @Test
    @DisplayName("应该支持重置取消状态")
    void testResetCancellation() {
        handler.handleCancellation();
        assertTrue(handler.isCancelled());
        
        handler.reset();
        
        assertFalse(handler.isCancelled());
    }
    
    @Test
    @DisplayName("应该正确处理 ESC 键取消")
    void testHandleEscapeKey() {
        assertFalse(handler.isCancelled());
        
        handler.handleEscapeKey();
        
        assertTrue(handler.isCancelled());
    }
    
    @Test
    @DisplayName("应该支持取消回调")
    void testCancelCallback() {
        final boolean[] callbackInvoked = {false};
        
        handler.setOnCancel(() -> callbackInvoked[0] = true);
        handler.handleCancellation();
        
        assertTrue(callbackInvoked[0]);
    }
    
    @Test
    @DisplayName("取消回调应该只执行一次")
    void testCancelCallbackOnce() {
        final int[] callbackCount = {0};
        
        handler.setOnCancel(() -> callbackCount[0]++);
        
        handler.handleCancellation();
        handler.handleCancellation(); // 第二次不应触发
        
        assertEquals(1, callbackCount[0]);
    }
}
