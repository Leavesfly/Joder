package io.shareai.joder.hooks;

import io.shareai.joder.services.completion.CompletionManager;
import io.shareai.joder.services.completion.CompletionSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 统一补全钩子
 * 对应 Kode 的 useUnifiedCompletion hook
 * 提供命令、文件、代理等的统一补全功能
 */
@Singleton
public class UnifiedCompletionHook {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedCompletionHook.class);
    
    private final CompletionManager completionManager;
    private List<CompletionSuggestion> suggestions;
    private int selectedIndex;
    private boolean isActive;
    
    @Inject
    public UnifiedCompletionHook(CompletionManager completionManager) {
        this.completionManager = completionManager;
        this.suggestions = List.of();
        this.selectedIndex = 0;
        this.isActive = false;
    }
    
    /**
     * 触发补全
     */
    public void triggerCompletion(String input, int cursorOffset) {
        try {
            suggestions = completionManager.getCompletions(input, cursorOffset);
            
            if (!suggestions.isEmpty()) {
                selectedIndex = 0;
                isActive = true;
                logger.debug("补全激活，建议数: {}", suggestions.size());
            } else {
                reset();
            }
            
        } catch (Exception e) {
            logger.error("补全触发失败", e);
            reset();
        }
    }
    
    /**
     * 选择下一个建议
     */
    public void selectNext() {
        if (!isActive || suggestions.isEmpty()) {
            return;
        }
        
        selectedIndex = (selectedIndex + 1) % suggestions.size();
    }
    
    /**
     * 选择上一个建议
     */
    public void selectPrevious() {
        if (!isActive || suggestions.isEmpty()) {
            return;
        }
        
        selectedIndex--;
        if (selectedIndex < 0) {
            selectedIndex = suggestions.size() - 1;
        }
    }
    
    /**
     * 应用当前选中的建议
     */
    public String applySelected() {
        if (!isActive || suggestions.isEmpty()) {
            return null;
        }
        
        CompletionSuggestion selected = suggestions.get(selectedIndex);
        reset();
        
        return selected.getText();
    }
    
    /**
     * 获取当前选中的建议
     */
    public CompletionSuggestion getSelected() {
        if (!isActive || suggestions.isEmpty() || selectedIndex >= suggestions.size()) {
            return null;
        }
        
        return suggestions.get(selectedIndex);
    }
    
    /**
     * 重置补全状态
     */
    public void reset() {
        suggestions = List.of();
        selectedIndex = 0;
        isActive = false;
    }
    
    /**
     * 处理 Tab 键
     */
    public boolean handleTab(
            String currentInput,
            int cursorOffset,
            BiConsumer<String, Integer> updateInput) {
        
        if (!isActive) {
            // 触发补全
            triggerCompletion(currentInput, cursorOffset);
            return !suggestions.isEmpty();
        } else {
            // 应用补全
            String completed = applySelected();
            if (completed != null) {
                updateInput.accept(completed, completed.length());
                return true;
            }
            return false;
        }
    }
    
    /**
     * 获取所有建议
     */
    public List<CompletionSuggestion> getSuggestions() {
        return suggestions;
    }
    
    /**
     * 获取选中索引
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * 是否激活
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 获取建议数量
     */
    public int getSuggestionCount() {
        return suggestions.size();
    }
}
