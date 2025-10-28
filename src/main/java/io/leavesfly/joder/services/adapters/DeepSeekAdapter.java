package io.leavesfly.joder.services.adapters;

import io.leavesfly.joder.core.config.ConfigManager;

/**
 * DeepSeek API 适配器
 * 兼容 OpenAI 格式的 API
 */
public class DeepSeekAdapter extends OpenAIAdapter {
    
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    
    public DeepSeekAdapter(ConfigManager configManager, String modelName, String profilePath) {
        super(configManager, modelName, profilePath);
    }
    
    @Override
    protected String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }
    
    @Override
    public String getProviderName() {
        return "deepseek";
    }
}
