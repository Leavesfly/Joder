package io.shareai.joder.ui.theme;

import com.googlecode.lanterna.TextColor;

/**
 * 主题接口
 */
public interface Theme {
    /**
     * 获取背景色
     */
    TextColor getBackgroundColor();
    
    /**
     * 获取前景色
     */
    TextColor getForegroundColor();
    
    /**
     * 获取用户消息颜色
     */
    TextColor getUserMessageColor();
    
    /**
     * 获取助手消息颜色
     */
    TextColor getAssistantMessageColor();
    
    /**
     * 获取工具调用颜色
     */
    TextColor getToolCallColor();
    
    /**
     * 获取错误信息颜色
     */
    TextColor getErrorColor();
    
    /**
     * 获取成功信息颜色
     */
    TextColor getSuccessColor();
    
    /**
     * 获取警告信息颜色
     */
    TextColor getWarningColor();
    
    /**
     * 获取提示符颜色
     */
    TextColor getPromptColor();
}
