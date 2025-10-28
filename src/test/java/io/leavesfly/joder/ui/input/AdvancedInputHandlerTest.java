package io.leavesfly.joder.ui.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import io.leavesfly.joder.hooks.CommandHistoryHook;
import io.leavesfly.joder.hooks.TextInputHook;
import io.leavesfly.joder.hooks.UnifiedCompletionHook;
import io.leavesfly.joder.services.completion.CompletionManager;
import io.leavesfly.joder.services.completion.CompletionSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdvancedInputHandler 单元测试
 */
@DisplayName("AdvancedInputHandler 高级输入处理器测试")
class AdvancedInputHandlerTest {
    
    @Mock
    private TextInputHook textInputHook;
    
    @Mock
    private CommandHistoryHook commandHistoryHook;
    
    @Mock
    private UnifiedCompletionHook completionHook;
    
    @Mock
    private CompletionManager completionManager;
    
    private AdvancedInputHandler handler;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AdvancedInputHandler(
            textInputHook,
            commandHistoryHook,
            completionHook,
            completionManager
        );
    }
    
    @Test
    @DisplayName("应该正确处理普通字符输入")
    void testHandleCharacterInput() {
        AtomicReference<String> updatedText = new AtomicReference<>();
        AtomicReference<Integer> updatedCursor = new AtomicReference<>();
        
        KeyStroke keyStroke = new KeyStroke('a', false, false);
        
        boolean handled = handler.handleKeyStroke(
            keyStroke,
            (text, cursor) -> {
                updatedText.set(text);
                updatedCursor.set(cursor);
            },
            null
        );
        
        assertTrue(handled);
        assertEquals("a", updatedText.get());
        assertEquals(1, updatedCursor.get());
    }
    
    @Test
    @DisplayName("应该正确处理退格键")
    void testHandleBackspace() {
        // 先输入一些字符
        handler.handleKeyStroke(new KeyStroke('a', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('b', false, false), (t, c) -> {}, null);
        
        AtomicReference<String> updatedText = new AtomicReference<>();
        
        // 退格
        KeyStroke backspace = new KeyStroke(KeyType.Backspace);
        handler.handleKeyStroke(
            backspace,
            (text, cursor) -> updatedText.set(text),
            null
        );
        
        assertEquals("a", updatedText.get());
    }
    
    @Test
    @DisplayName("应该在 Tab 键时触发补全")
    void testHandleTabTriggersCompletion() {
        // 模拟补全建议
        List<CompletionSuggestion> suggestions = Arrays.asList(
            CompletionSuggestion.command("help", "显示帮助", 100),
            CompletionSuggestion.command("history", "历史记录", 90)
        );
        
        when(completionManager.getCompletions(anyString(), anyInt()))
            .thenReturn(suggestions);
        
        // 输入 "/he"
        handler.handleKeyStroke(new KeyStroke('/', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('h', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('e', false, false), (t, c) -> {}, null);
        
        // 按 Tab
        KeyStroke tab = new KeyStroke(KeyType.Tab);
        boolean handled = handler.handleKeyStroke(tab, (t, c) -> {}, null);
        
        assertTrue(handled);
        assertTrue(handler.isCompletionActive());
        assertEquals(2, handler.getCurrentSuggestions().size());
    }
    
    @Test
    @DisplayName("应该支持历史导航（向上箭头）")
    void testHandleArrowUpForHistory() {
        when(commandHistoryHook.navigateUp(anyString()))
            .thenReturn("/help");
        
        AtomicReference<String> updatedText = new AtomicReference<>();
        
        KeyStroke arrowUp = new KeyStroke(KeyType.ArrowUp);
        handler.handleKeyStroke(
            arrowUp,
            (text, cursor) -> updatedText.set(text),
            null
        );
        
        assertEquals("/help", updatedText.get());
        verify(commandHistoryHook).navigateUp(anyString());
    }
    
    @Test
    @DisplayName("应该支持回车提交输入")
    void testHandleEnterSubmit() {
        // 输入一些文本
        handler.handleKeyStroke(new KeyStroke('t', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('e', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('s', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('t', false, false), (t, c) -> {}, null);
        
        AtomicReference<String> submittedText = new AtomicReference<>();
        
        KeyStroke enter = new KeyStroke(KeyType.Enter);
        handler.handleKeyStroke(
            enter,
            (t, c) -> {},
            text -> submittedText.set(text)
        );
        
        assertEquals("test", submittedText.get());
        verify(commandHistoryHook).addToHistory("test");
        
        // 提交后应该重置输入
        assertEquals("", handler.getCurrentInput());
    }
    
    @Test
    @DisplayName("应该支持光标移动（左右箭头）")
    void testHandleArrowLeftRight() {
        // 输入 "abc"
        handler.handleKeyStroke(new KeyStroke('a', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('b', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('c', false, false), (t, c) -> {}, null);
        
        // 光标应该在位置 3
        assertEquals(3, handler.getCursorPosition());
        
        // 向左移动
        handler.handleKeyStroke(new KeyStroke(KeyType.ArrowLeft), (t, c) -> {}, null);
        assertEquals(2, handler.getCursorPosition());
        
        // 再向左移动
        handler.handleKeyStroke(new KeyStroke(KeyType.ArrowLeft), (t, c) -> {}, null);
        assertEquals(1, handler.getCursorPosition());
        
        // 向右移动
        handler.handleKeyStroke(new KeyStroke(KeyType.ArrowRight), (t, c) -> {}, null);
        assertEquals(2, handler.getCursorPosition());
    }
    
    @Test
    @DisplayName("应该支持 Home/End 键")
    void testHandleHomeEnd() {
        // 输入 "test"
        handler.handleKeyStroke(new KeyStroke('t', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('e', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('s', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('t', false, false), (t, c) -> {}, null);
        
        // Home 键移动到行首
        handler.handleKeyStroke(new KeyStroke(KeyType.Home), (t, c) -> {}, null);
        assertEquals(0, handler.getCursorPosition());
        
        // End 键移动到行尾
        handler.handleKeyStroke(new KeyStroke(KeyType.End), (t, c) -> {}, null);
        assertEquals(4, handler.getCursorPosition());
    }
    
    @Test
    @DisplayName("应该支持 Delete 键")
    void testHandleDelete() {
        // 输入 "abc"
        handler.handleKeyStroke(new KeyStroke('a', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('b', false, false), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke('c', false, false), (t, c) -> {}, null);
        
        // 移动光标到位置 1（'a' 和 'b' 之间）
        handler.handleKeyStroke(new KeyStroke(KeyType.Home), (t, c) -> {}, null);
        handler.handleKeyStroke(new KeyStroke(KeyType.ArrowRight), (t, c) -> {}, null);
        
        AtomicReference<String> updatedText = new AtomicReference<>();
        
        // Delete 删除光标后的字符 'b'
        handler.handleKeyStroke(
            new KeyStroke(KeyType.Delete),
            (text, cursor) -> updatedText.set(text),
            null
        );
        
        assertEquals("ac", updatedText.get());
    }
    
    @Test
    @DisplayName("补全激活后字符输入应取消补全")
    void testCharacterInputCancelsCompletion() {
        // 激活补全
        List<CompletionSuggestion> suggestions = Arrays.asList(
            CompletionSuggestion.command("help", "帮助", 100)
        );
        when(completionManager.getCompletions(anyString(), anyInt()))
            .thenReturn(suggestions);
        
        handler.handleKeyStroke(new KeyStroke(KeyType.Tab), (t, c) -> {}, null);
        assertTrue(handler.isCompletionActive());
        
        // 输入字符应取消补全
        handler.handleKeyStroke(new KeyStroke('x', false, false), (t, c) -> {}, null);
        assertFalse(handler.isCompletionActive());
    }
}
