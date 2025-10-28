package io.leavesfly.joder.ui.theme;

import com.googlecode.lanterna.TextColor;

/**
 * 深色主题
 */
public class DarkTheme implements Theme {
    
    @Override
    public TextColor getBackgroundColor() {
        return new TextColor.RGB(30, 30, 30);
    }
    
    @Override
    public TextColor getForegroundColor() {
        return new TextColor.RGB(212, 212, 212);
    }
    
    @Override
    public TextColor getUserMessageColor() {
        return new TextColor.RGB(78, 201, 176);
    }
    
    @Override
    public TextColor getAssistantMessageColor() {
        return new TextColor.RGB(156, 220, 254);
    }
    
    @Override
    public TextColor getToolCallColor() {
        return new TextColor.RGB(220, 220, 170);
    }
    
    @Override
    public TextColor getErrorColor() {
        return new TextColor.RGB(244, 135, 113);
    }
    
    @Override
    public TextColor getSuccessColor() {
        return new TextColor.RGB(181, 206, 168);
    }
    
    @Override
    public TextColor getWarningColor() {
        return new TextColor.RGB(255, 193, 7);
    }
    
    @Override
    public TextColor getPromptColor() {
        return new TextColor.RGB(86, 156, 214);
    }
}
