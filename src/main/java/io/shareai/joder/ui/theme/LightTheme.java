package io.shareai.joder.ui.theme;

import com.googlecode.lanterna.TextColor;

/**
 * 浅色主题
 */
public class LightTheme implements Theme {
    
    @Override
    public TextColor getBackgroundColor() {
        return TextColor.ANSI.WHITE;
    }
    
    @Override
    public TextColor getForegroundColor() {
        return TextColor.ANSI.BLACK;
    }
    
    @Override
    public TextColor getUserMessageColor() {
        return new TextColor.RGB(0, 128, 128);
    }
    
    @Override
    public TextColor getAssistantMessageColor() {
        return new TextColor.RGB(0, 0, 255);
    }
    
    @Override
    public TextColor getToolCallColor() {
        return new TextColor.RGB(121, 94, 38);
    }
    
    @Override
    public TextColor getErrorColor() {
        return new TextColor.RGB(163, 21, 21);
    }
    
    @Override
    public TextColor getSuccessColor() {
        return new TextColor.RGB(0, 128, 0);
    }
    
    @Override
    public TextColor getWarningColor() {
        return new TextColor.RGB(255, 140, 0);
    }
    
    @Override
    public TextColor getPromptColor() {
        return new TextColor.RGB(0, 0, 139);
    }
}
