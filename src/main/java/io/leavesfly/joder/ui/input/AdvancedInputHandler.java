package io.leavesfly.joder.ui.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import io.leavesfly.joder.hooks.CommandHistoryHook;
import io.leavesfly.joder.hooks.TextInputHook;
import io.leavesfly.joder.hooks.UnifiedCompletionHook;
import io.leavesfly.joder.services.completion.CompletionManager;
import io.leavesfly.joder.services.completion.CompletionSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 高级输入处理器
 * 基于 Lanterna 实现按键级输入处理，支持：
 * - Tab 触发补全
 * - ↑↓ 历史导航
 * - 光标移动（←→ Home End）
 * - 按词编辑（Ctrl+W Ctrl+U）
 */
public class AdvancedInputHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedInputHandler.class);
    
    private final TextInputHook textInputHook;
    private final CommandHistoryHook commandHistoryHook;
    private final UnifiedCompletionHook completionHook;
    private final CompletionManager completionManager;
    
    private String currentInput;
    private int cursorPosition;
    private boolean completionActive;
    private int completionIndex;
    private List<CompletionSuggestion> currentSuggestions;
    
    @Inject
    public AdvancedInputHandler(
            TextInputHook textInputHook,
            CommandHistoryHook commandHistoryHook,
            UnifiedCompletionHook completionHook,
            CompletionManager completionManager) {
        this.textInputHook = textInputHook;
        this.commandHistoryHook = commandHistoryHook;
        this.completionHook = completionHook;
        this.completionManager = completionManager;
        this.currentInput = "";
        this.cursorPosition = 0;
        this.completionActive = false;
        this.completionIndex = 0;
        this.currentSuggestions = List.of();
    }
    
    /**
     * 处理按键输入
     * 
     * @param keyStroke Lanterna 按键事件
     * @param onUpdate 输入更新回调 (text, cursorPos)
     * @param onSubmit 提交回调
     * @return 是否处理了该按键
     */
    public boolean handleKeyStroke(
            KeyStroke keyStroke,
            BiConsumer<String, Integer> onUpdate,
            Consumer<String> onSubmit) {
        
        if (keyStroke == null) {
            return false;
        }
        
        KeyType keyType = keyStroke.getKeyType();
        
        switch (keyType) {
            case Character:
                return handleCharacter(keyStroke.getCharacter(), onUpdate);
                
            case Enter:
                return handleEnter(onSubmit);
                
            case Backspace:
                return handleBackspace(onUpdate);
                
            case Delete:
                return handleDelete(onUpdate);
                
            case Tab:
                return handleTab(onUpdate);
                
            case ArrowUp:
                return handleArrowUp(onUpdate);
                
            case ArrowDown:
                return handleArrowDown(onUpdate);
                
            case ArrowLeft:
                return handleArrowLeft(onUpdate);
                
            case ArrowRight:
                return handleArrowRight(onUpdate);
                
            case Home:
                return handleHome(onUpdate);
                
            case End:
                return handleEnd(onUpdate);
                
            default:
                // 检查是否为 Ctrl 组合键
                if (keyStroke.isCtrlDown()) {
                    return handleCtrlKey(keyStroke.getCharacter(), onUpdate);
                }
                return false;
        }
    }
    
    /**
     * 处理普通字符输入
     */
    private boolean handleCharacter(char c, BiConsumer<String, Integer> onUpdate) {
        // 取消补全
        cancelCompletion();
        
        // 插入字符
        currentInput = currentInput.substring(0, cursorPosition) + c + 
                      currentInput.substring(cursorPosition);
        cursorPosition++;
        
        onUpdate.accept(currentInput, cursorPosition);
        return true;
    }
    
    /**
     * 处理回车（提交）
     */
    private boolean handleEnter(Consumer<String> onSubmit) {
        // 如果补全激活，应用第一个建议
        if (completionActive && !currentSuggestions.isEmpty()) {
            applyCompletion(currentSuggestions.get(completionIndex));
            cancelCompletion();
            return true;
        }
        
        // 提交输入
        String input = currentInput;
        commandHistoryHook.addToHistory(input);
        
        // 重置状态
        currentInput = "";
        cursorPosition = 0;
        cancelCompletion();
        
        onSubmit.accept(input);
        return true;
    }
    
    /**
     * 处理退格键
     */
    private boolean handleBackspace(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition > 0) {
            currentInput = currentInput.substring(0, cursorPosition - 1) + 
                          currentInput.substring(cursorPosition);
            cursorPosition--;
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理删除键
     */
    private boolean handleDelete(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition < currentInput.length()) {
            currentInput = currentInput.substring(0, cursorPosition) + 
                          currentInput.substring(cursorPosition + 1);
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理 Tab 键（触发补全）
     */
    private boolean handleTab(BiConsumer<String, Integer> onUpdate) {
        if (!completionActive) {
            // 触发补全
            currentSuggestions = completionManager.getCompletions(
                currentInput, cursorPosition);
            
            if (!currentSuggestions.isEmpty()) {
                completionActive = true;
                completionIndex = 0;
                logger.debug("补全激活，建议数: {}", currentSuggestions.size());
                onUpdate.accept(currentInput, cursorPosition);
            }
        } else {
            // 循环到下一个建议
            completionIndex = (completionIndex + 1) % currentSuggestions.size();
            logger.debug("切换到建议 {}/{}", completionIndex + 1, currentSuggestions.size());
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理向上箭头（历史导航）
     */
    private boolean handleArrowUp(BiConsumer<String, Integer> onUpdate) {
        if (completionActive) {
            // 补全激活时，向上选择建议
            completionIndex--;
            if (completionIndex < 0) {
                completionIndex = currentSuggestions.size() - 1;
            }
            onUpdate.accept(currentInput, cursorPosition);
        } else {
            // 历史导航
            String prev = commandHistoryHook.navigateUp(currentInput);
            if (!prev.equals(currentInput)) {
                currentInput = prev;
                cursorPosition = currentInput.length();
                onUpdate.accept(currentInput, cursorPosition);
            }
        }
        return true;
    }
    
    /**
     * 处理向下箭头（历史导航）
     */
    private boolean handleArrowDown(BiConsumer<String, Integer> onUpdate) {
        if (completionActive) {
            // 补全激活时，向下选择建议
            completionIndex = (completionIndex + 1) % currentSuggestions.size();
            onUpdate.accept(currentInput, cursorPosition);
        } else {
            // 历史导航
            String next = commandHistoryHook.navigateDown(currentInput);
            if (!next.equals(currentInput)) {
                currentInput = next;
                cursorPosition = currentInput.length();
                onUpdate.accept(currentInput, cursorPosition);
            }
        }
        return true;
    }
    
    /**
     * 处理向左箭头（光标移动）
     */
    private boolean handleArrowLeft(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition > 0) {
            cursorPosition--;
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理向右箭头（光标移动）
     */
    private boolean handleArrowRight(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition < currentInput.length()) {
            cursorPosition++;
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理 Home 键（移动到行首）
     */
    private boolean handleHome(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition != 0) {
            cursorPosition = 0;
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理 End 键（移动到行尾）
     */
    private boolean handleEnd(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition != currentInput.length()) {
            cursorPosition = currentInput.length();
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 处理 Ctrl 组合键
     */
    private boolean handleCtrlKey(char c, BiConsumer<String, Integer> onUpdate) {
        switch (Character.toLowerCase(c)) {
            case 'w':  // Ctrl+W: 删除当前单词
                return handleCtrlW(onUpdate);
            case 'u':  // Ctrl+U: 删除到行首
                return handleCtrlU(onUpdate);
            case 'k':  // Ctrl+K: 删除到行尾
                return handleCtrlK(onUpdate);
            case 'a':  // Ctrl+A: 移动到行首
                return handleHome(onUpdate);
            case 'e':  // Ctrl+E: 移动到行尾
                return handleEnd(onUpdate);
            default:
                return false;
        }
    }
    
    /**
     * Ctrl+W: 删除当前单词
     */
    private boolean handleCtrlW(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition == 0) {
            return true;
        }
        
        int start = cursorPosition - 1;
        // 跳过空格
        while (start > 0 && currentInput.charAt(start) == ' ') {
            start--;
        }
        // 跳过非空格
        while (start > 0 && currentInput.charAt(start) != ' ') {
            start--;
        }
        if (currentInput.charAt(start) == ' ') {
            start++;
        }
        
        currentInput = currentInput.substring(0, start) + 
                      currentInput.substring(cursorPosition);
        cursorPosition = start;
        onUpdate.accept(currentInput, cursorPosition);
        return true;
    }
    
    /**
     * Ctrl+U: 删除到行首
     */
    private boolean handleCtrlU(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition > 0) {
            currentInput = currentInput.substring(cursorPosition);
            cursorPosition = 0;
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * Ctrl+K: 删除到行尾
     */
    private boolean handleCtrlK(BiConsumer<String, Integer> onUpdate) {
        cancelCompletion();
        
        if (cursorPosition < currentInput.length()) {
            currentInput = currentInput.substring(0, cursorPosition);
            onUpdate.accept(currentInput, cursorPosition);
        }
        return true;
    }
    
    /**
     * 应用补全建议
     */
    private void applyCompletion(CompletionSuggestion suggestion) {
        // 提取前缀
        String prefix = CompletionManager.extractPrefix(currentInput, cursorPosition);
        
        // 替换为补全文本
        int prefixStart = cursorPosition - prefix.length();
        currentInput = currentInput.substring(0, prefixStart) + 
                      suggestion.getText() + 
                      currentInput.substring(cursorPosition);
        cursorPosition = prefixStart + suggestion.getText().length();
        
        logger.debug("应用补全: {} -> {}", prefix, suggestion.getText());
    }
    
    /**
     * 取消补全
     */
    private void cancelCompletion() {
        if (completionActive) {
            completionActive = false;
            completionIndex = 0;
            currentSuggestions = List.of();
            logger.debug("补全已取消");
        }
    }
    
    /**
     * 获取当前输入
     */
    public String getCurrentInput() {
        return currentInput;
    }
    
    /**
     * 获取光标位置
     */
    public int getCursorPosition() {
        return cursorPosition;
    }
    
    /**
     * 是否补全激活
     */
    public boolean isCompletionActive() {
        return completionActive;
    }
    
    /**
     * 获取当前补全建议
     */
    public List<CompletionSuggestion> getCurrentSuggestions() {
        return currentSuggestions;
    }
    
    /**
     * 获取当前选中的补全索引
     */
    public int getCompletionIndex() {
        return completionIndex;
    }
    
    /**
     * 重置输入状态
     */
    public void reset() {
        currentInput = "";
        cursorPosition = 0;
        cancelCompletion();
    }
}
