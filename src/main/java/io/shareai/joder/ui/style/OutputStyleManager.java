package io.shareai.joder.ui.style;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * 输出样式管理器
 * 管理全局输出样式设置
 */
@Singleton
public class OutputStyleManager {
    
    private static final Logger logger = LoggerFactory.getLogger(OutputStyleManager.class);
    
    private OutputStyle currentStyle = OutputStyle.BALANCED; // 默认平衡模式
    
    /**
     * 设置输出样式
     */
    public void setStyle(OutputStyle style) {
        OutputStyle oldStyle = this.currentStyle;
        this.currentStyle = style;
        logger.info("输出样式已切换: {} -> {}", oldStyle.getDisplayName(), style.getDisplayName());
    }
    
    /**
     * 设置输出样式(通过key)
     */
    public boolean setStyle(String key) {
        OutputStyle style = OutputStyle.fromKey(key);
        if (style != null) {
            setStyle(style);
            return true;
        }
        return false;
    }
    
    /**
     * 获取当前输出样式
     */
    public OutputStyle getCurrentStyle() {
        return currentStyle;
    }
    
    /**
     * 是否显示工具调用详情
     */
    public boolean shouldShowToolDetails() {
        return currentStyle.showToolDetails();
    }
    
    /**
     * 是否显示思考过程
     */
    public boolean shouldShowThinking() {
        return currentStyle.showThinking();
    }
    
    /**
     * 是否显示进度信息
     */
    public boolean shouldShowProgress() {
        return currentStyle.showProgress();
    }
    
    /**
     * 是否显示统计信息
     */
    public boolean shouldShowStats() {
        return currentStyle.showStats();
    }
    
    /**
     * 格式化消息(根据当前样式)
     */
    public String formatMessage(String fullMessage, String summary) {
        switch (currentStyle) {
            case CONCISE:
                // 简洁模式:只返回summary
                return summary != null ? summary : extractSummary(fullMessage);
                
            case BALANCED:
                // 平衡模式:返回适度简化的消息
                return simplifyMessage(fullMessage);
                
            case DETAILED:
            case THINKING:
            default:
                // 详细和思考模式:返回完整消息
                return fullMessage;
        }
    }
    
    /**
     * 提取摘要(从完整消息中)
     */
    private String extractSummary(String fullMessage) {
        if (fullMessage == null || fullMessage.isEmpty()) {
            return fullMessage;
        }
        
        // 提取第一行或第一段作为摘要
        String[] lines = fullMessage.split("\n");
        if (lines.length > 0) {
            return lines[0];
        }
        
        return fullMessage;
    }
    
    /**
     * 简化消息(去除不必要的详情)
     */
    private String simplifyMessage(String fullMessage) {
        if (fullMessage == null || fullMessage.isEmpty()) {
            return fullMessage;
        }
        
        // 简单实现:保留前几行和关键信息
        StringBuilder simplified = new StringBuilder();
        String[] lines = fullMessage.split("\n");
        
        int lineCount = 0;
        for (String line : lines) {
            // 跳过空行
            if (line.trim().isEmpty()) {
                continue;
            }
            
            // 保留关键信息行
            if (isImportantLine(line)) {
                simplified.append(line).append("\n");
                lineCount++;
            }
            
            // 限制总行数
            if (lineCount >= 10) {
                simplified.append("...(更多详情请使用detailed模式查看)\n");
                break;
            }
        }
        
        return simplified.toString();
    }
    
    /**
     * 判断是否为重要信息行
     */
    private boolean isImportantLine(String line) {
        // 包含关键标记的行
        return line.contains("✅") || 
               line.contains("❌") || 
               line.contains("⚠") ||
               line.contains("成功") ||
               line.contains("失败") ||
               line.contains("错误") ||
               line.contains("Warning") ||
               line.contains("Error") ||
               line.contains("Success") ||
               !line.trim().startsWith(" "); // 顶级行
    }
    
    /**
     * 重置为默认样式
     */
    public void reset() {
        setStyle(OutputStyle.BALANCED);
    }
    
    /**
     * 获取样式描述
     */
    public String getStyleDescription() {
        return String.format("当前输出样式: %s (%s)\n\n%s", 
            currentStyle.getKey(),
            currentStyle.getDisplayName(),
            OutputStyle.getAllStylesDescription());
    }
}
