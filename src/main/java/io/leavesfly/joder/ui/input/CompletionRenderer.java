package io.leavesfly.joder.ui.input;

import io.leavesfly.joder.services.completion.CompletionSuggestion;
import io.leavesfly.joder.ui.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * 补全建议渲染器
 * 负责在终端中美观地显示补全建议列表
 */
public class CompletionRenderer {
    
    private static final Logger logger = LoggerFactory.getLogger(CompletionRenderer.class);
    private static final int MAX_VISIBLE_SUGGESTIONS = 5;
    
    private final Theme theme;
    
    @Inject
    public CompletionRenderer(Theme theme) {
        this.theme = theme;
    }
    
    /**
     * 渲染补全建议列表
     * 
     * @param suggestions 补全建议列表
     * @param selectedIndex 当前选中的索引
     * @param terminalWidth 终端宽度
     * @return 渲染后的字符串（多行）
     */
    public String render(List<CompletionSuggestion> suggestions, int selectedIndex, int terminalWidth) {
        if (suggestions == null || suggestions.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 计算可见建议范围
        int visibleStart = Math.max(0, selectedIndex - MAX_VISIBLE_SUGGESTIONS / 2);
        int visibleEnd = Math.min(suggestions.size(), visibleStart + MAX_VISIBLE_SUGGESTIONS);
        
        // 如果选中项在末尾，调整起始位置
        if (visibleEnd - visibleStart < MAX_VISIBLE_SUGGESTIONS) {
            visibleStart = Math.max(0, visibleEnd - MAX_VISIBLE_SUGGESTIONS);
        }
        
        // 渲染标题
        sb.append("\n");
        sb.append(formatTitle(suggestions.size(), selectedIndex));
        sb.append("\n");
        
        // 渲染可见建议
        for (int i = visibleStart; i < visibleEnd; i++) {
            CompletionSuggestion suggestion = suggestions.get(i);
            boolean isSelected = (i == selectedIndex);
            
            sb.append(formatSuggestion(suggestion, isSelected, i + 1, terminalWidth));
            sb.append("\n");
        }
        
        // 如果有更多建议，显示提示
        if (visibleEnd < suggestions.size()) {
            sb.append(formatMoreHint(suggestions.size() - visibleEnd));
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 渲染简化的补全提示（非交互式）
     */
    public String renderSimple(List<CompletionSuggestion> suggestions, int maxCount) {
        if (suggestions == null || suggestions.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n💡 建议命令:\n");
        
        int count = Math.min(maxCount, suggestions.size());
        for (int i = 0; i < count; i++) {
            CompletionSuggestion suggestion = suggestions.get(i);
            sb.append(String.format("   /%s", suggestion.getText()));
            
            if (suggestion.getDescription() != null && !suggestion.getDescription().isEmpty()) {
                sb.append(" - ").append(suggestion.getDescription());
            }
            sb.append("\n");
        }
        
        if (suggestions.size() > maxCount) {
            sb.append(String.format("   ... 还有 %d 个建议\n", suggestions.size() - maxCount));
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化标题
     */
    private String formatTitle(int total, int selectedIndex) {
        return String.format("╭─ 补全建议 (%d/%d) ─────────────────────", 
            selectedIndex + 1, total);
    }
    
    /**
     * 格式化单个建议
     */
    private String formatSuggestion(
            CompletionSuggestion suggestion, 
            boolean isSelected, 
            int number,
            int terminalWidth) {
        
        StringBuilder sb = new StringBuilder();
        
        // 选中标记
        if (isSelected) {
            sb.append("│ ▶ ");
        } else {
            sb.append("│   ");
        }
        
        // 序号
        sb.append(String.format("%2d. ", number));
        
        // 补全文本（加粗显示）
        if (isSelected) {
            sb.append("\033[1m").append(suggestion.getText()).append("\033[0m");
        } else {
            sb.append(suggestion.getText());
        }
        
        // 描述（如果有）
        if (suggestion.getDescription() != null && !suggestion.getDescription().isEmpty()) {
            sb.append(" - ");
            
            // 计算剩余宽度
            int usedWidth = sb.length() + suggestion.getDescription().length();
            int maxDescWidth = terminalWidth - 20; // 留一些边距
            
            String description = suggestion.getDescription();
            if (usedWidth > maxDescWidth && description.length() > 30) {
                description = description.substring(0, 30) + "...";
            }
            
            // 描述用灰色显示
            sb.append("\033[90m").append(description).append("\033[0m");
        }
        
        // 类型标签（如果需要）
        if (suggestion.getType() != null) {
            sb.append(" ");
            sb.append(formatTypeTag(suggestion.getType()));
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化类型标签
     */
    private String formatTypeTag(CompletionSuggestion.CompletionType type) {
        switch (type) {
            case COMMAND:
                return "\033[36m[CMD]\033[0m";  // 青色
            case FILE:
                return "\033[33m[FILE]\033[0m";  // 黄色
            case MODEL:
                return "\033[35m[MODEL]\033[0m";  // 紫色
            default:
                return "";
        }
    }
    
    /**
     * 格式化"更多建议"提示
     */
    private String formatMoreHint(int moreCount) {
        return String.format("│ ⋮  ... 还有 %d 个建议（Tab 继续浏览）", moreCount);
    }
    
    /**
     * 渲染单行补全预览（在输入行右侧）
     */
    public String renderInlinePreview(CompletionSuggestion suggestion, String currentInput) {
        if (suggestion == null) {
            return "";
        }
        
        String prefix = currentInput;
        String completion = suggestion.getText();
        
        // 如果补全以输入为前缀，显示剩余部分
        if (completion.startsWith(prefix)) {
            String remaining = completion.substring(prefix.length());
            return "\033[90m" + remaining + "\033[0m";  // 灰色显示
        }
        
        return "";
    }
    
    /**
     * 清除补全显示（返回清屏序列）
     */
    public String clearCompletion(int lineCount) {
        StringBuilder sb = new StringBuilder();
        
        // 移动光标到上方并清除行
        for (int i = 0; i < lineCount; i++) {
            sb.append("\033[F");  // 上移一行
            sb.append("\033[2K");  // 清除整行
        }
        
        return sb.toString();
    }
}
