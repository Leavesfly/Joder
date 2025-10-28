package io.leavesfly.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.function.Consumer;

/**
 * 文本输入钩子
 * 对应 Kode 的 useTextInput hook
 * 处理光标、编辑、历史导航等功能
 */
@Singleton
public class TextInputHook {
    
    private static final Logger logger = LoggerFactory.getLogger(TextInputHook.class);
    
    private String value;
    private int cursorOffset;
    private final CommandHistoryHook historyHook;
    
    public TextInputHook(CommandHistoryHook historyHook) {
        this.value = "";
        this.cursorOffset = 0;
        this.historyHook = historyHook;
    }
    
    /**
     * 处理输入
     */
    public void handleInput(
            char input,
            Consumer<String> onChange,
            Consumer<String> onSubmit) {
        
        switch (input) {
            case '\n', '\r' -> {
                // 回车：提交
                if (onSubmit != null) {
                    onSubmit.accept(value);
                    historyHook.addToHistory(value);
                }
                value = "";
                cursorOffset = 0;
            }
            case '\b', 127 -> {
                // 退格
                if (cursorOffset > 0) {
                    value = value.substring(0, cursorOffset - 1) + value.substring(cursorOffset);
                    cursorOffset--;
                    if (onChange != null) {
                        onChange.accept(value);
                    }
                }
            }
            case 27 -> {
                // ESC：清空（需要双击确认）
                // 这里简化处理
            }
            default -> {
                // 普通字符：插入
                if (input >= 32 && input < 127) {
                    value = value.substring(0, cursorOffset) + input + value.substring(cursorOffset);
                    cursorOffset++;
                    if (onChange != null) {
                        onChange.accept(value);
                    }
                }
            }
        }
    }
    
    /**
     * 处理特殊键
     */
    public void handleSpecialKey(
            SpecialKey key,
            Consumer<String> onChange) {
        
        switch (key) {
            case LEFT_ARROW -> {
                if (cursorOffset > 0) {
                    cursorOffset--;
                }
            }
            case RIGHT_ARROW -> {
                if (cursorOffset < value.length()) {
                    cursorOffset++;
                }
            }
            case UP_ARROW -> {
                value = historyHook.navigateUp(value);
                cursorOffset = value.length();
                if (onChange != null) {
                    onChange.accept(value);
                }
            }
            case DOWN_ARROW -> {
                value = historyHook.navigateDown(value);
                cursorOffset = value.length();
                if (onChange != null) {
                    onChange.accept(value);
                }
            }
            case HOME -> {
                cursorOffset = 0;
            }
            case END -> {
                cursorOffset = value.length();
            }
            case DELETE -> {
                if (cursorOffset < value.length()) {
                    value = value.substring(0, cursorOffset) + value.substring(cursorOffset + 1);
                    if (onChange != null) {
                        onChange.accept(value);
                    }
                }
            }
        }
    }
    
    /**
     * 删除当前单词
     */
    public void deleteWord(Consumer<String> onChange) {
        if (cursorOffset == 0) {
            return;
        }
        
        int start = cursorOffset - 1;
        while (start > 0 && value.charAt(start) == ' ') {
            start--;
        }
        while (start > 0 && value.charAt(start) != ' ') {
            start--;
        }
        if (value.charAt(start) == ' ') {
            start++;
        }
        
        value = value.substring(0, start) + value.substring(cursorOffset);
        cursorOffset = start;
        
        if (onChange != null) {
            onChange.accept(value);
        }
    }
    
    /**
     * 删除到行首
     */
    public void deleteToLineStart(Consumer<String> onChange) {
        value = value.substring(cursorOffset);
        cursorOffset = 0;
        if (onChange != null) {
            onChange.accept(value);
        }
    }
    
    /**
     * 删除到行尾
     */
    public void deleteToLineEnd(Consumer<String> onChange) {
        value = value.substring(0, cursorOffset);
        if (onChange != null) {
            onChange.accept(value);
        }
    }
    
    /**
     * 移动到行首
     */
    public void moveToLineStart() {
        cursorOffset = 0;
    }
    
    /**
     * 移动到行尾
     */
    public void moveToLineEnd() {
        cursorOffset = value.length();
    }
    
    /**
     * 移动到上一个单词
     */
    public void moveToPrevWord() {
        if (cursorOffset == 0) {
            return;
        }
        
        int pos = cursorOffset - 1;
        while (pos > 0 && value.charAt(pos) == ' ') {
            pos--;
        }
        while (pos > 0 && value.charAt(pos) != ' ') {
            pos--;
        }
        if (value.charAt(pos) == ' ') {
            pos++;
        }
        
        cursorOffset = pos;
    }
    
    /**
     * 移动到下一个单词
     */
    public void moveToNextWord() {
        if (cursorOffset >= value.length()) {
            return;
        }
        
        int pos = cursorOffset;
        while (pos < value.length() && value.charAt(pos) != ' ') {
            pos++;
        }
        while (pos < value.length() && value.charAt(pos) == ' ') {
            pos++;
        }
        
        cursorOffset = pos;
    }
    
    // Getters and Setters
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
        this.cursorOffset = Math.min(cursorOffset, value.length());
    }
    
    public int getCursorOffset() {
        return cursorOffset;
    }
    
    public void setCursorOffset(int offset) {
        this.cursorOffset = Math.max(0, Math.min(offset, value.length()));
    }
    
    /**
     * 特殊键枚举
     */
    public enum SpecialKey {
        LEFT_ARROW,
        RIGHT_ARROW,
        UP_ARROW,
        DOWN_ARROW,
        HOME,
        END,
        DELETE
    }
}
