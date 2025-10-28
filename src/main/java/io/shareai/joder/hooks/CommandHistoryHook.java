package io.shareai.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令历史钩子
 * 对应 Kode 的 useArrowKeyHistory hook
 * 提供上下箭头导航历史命令的功能
 */
@Singleton
public class CommandHistoryHook {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandHistoryHook.class);
    private static final int MAX_HISTORY_SIZE = 1000;
    
    private final List<String> history;
    private int historyIndex;
    private String lastTypedInput;
    
    public CommandHistoryHook() {
        this.history = new ArrayList<>();
        this.historyIndex = 0;
        this.lastTypedInput = "";
    }
    
    /**
     * 添加命令到历史
     */
    public void addToHistory(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        
        // 避免连续重复的命令
        if (!history.isEmpty() && history.get(history.size() - 1).equals(command)) {
            return;
        }
        
        history.add(command);
        
        // 限制历史大小
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        
        // 重置索引
        resetIndex();
    }
    
    /**
     * 向上导航（获取上一条命令）
     */
    public String navigateUp(String currentInput) {
        if (history.isEmpty()) {
            return currentInput;
        }
        
        if (historyIndex == 0 && !currentInput.trim().isEmpty()) {
            lastTypedInput = currentInput;
        }
        
        if (historyIndex < history.size()) {
            historyIndex++;
            return getHistoryAt(historyIndex - 1);
        }
        
        return currentInput;
    }
    
    /**
     * 向下导航（获取下一条命令）
     */
    public String navigateDown(String currentInput) {
        if (historyIndex > 1) {
            historyIndex--;
            return getHistoryAt(historyIndex - 1);
        } else if (historyIndex == 1) {
            historyIndex = 0;
            return lastTypedInput;
        }
        
        return currentInput;
    }
    
    /**
     * 重置历史索引
     */
    public void resetIndex() {
        historyIndex = 0;
        lastTypedInput = "";
    }
    
    /**
     * 获取指定位置的历史命令
     */
    private String getHistoryAt(int index) {
        if (index < 0 || index >= history.size()) {
            return "";
        }
        return history.get(history.size() - 1 - index);
    }
    
    /**
     * 获取所有历史命令
     */
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * 清空历史
     */
    public void clearHistory() {
        history.clear();
        resetIndex();
    }
    
    /**
     * 获取历史大小
     */
    public int getHistorySize() {
        return history.size();
    }
    
    /**
     * 获取当前索引
     */
    public int getHistoryIndex() {
        return historyIndex;
    }
}
