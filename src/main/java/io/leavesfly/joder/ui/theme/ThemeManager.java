package io.leavesfly.joder.ui.theme;

import io.leavesfly.joder.core.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 主题管理器
 */
@Singleton
public class ThemeManager {
    
    private final ConfigManager configManager;
    private Theme currentTheme;
    
    @Inject
    public ThemeManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.currentTheme = loadTheme();
    }
    
    /**
     * 加载主题
     */
    private Theme loadTheme() {
        String themeName = configManager.getString("joder.theme", "dark");
        return switch (themeName.toLowerCase()) {
            case "light" -> new LightTheme();
            case "dark" -> new DarkTheme();
            default -> new DarkTheme();
        };
    }
    
    /**
     * 获取当前主题
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * 设置主题
     */
    public void setTheme(String themeName) {
        currentTheme = switch (themeName.toLowerCase()) {
            case "light" -> new LightTheme();
            case "dark" -> new DarkTheme();
            default -> new DarkTheme();
        };
    }
}
