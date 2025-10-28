package io.leavesfly.joder.services.model;

import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.services.model.dto.StreamHandler;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * 模型适配器抽象基类
 * 提供通用的配置和 HTTP 客户端支持
 */
public abstract class AbstractModelAdapter implements ModelAdapter {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractModelAdapter.class);
    
    protected final ConfigManager configManager;
    protected final OkHttpClient httpClient;
    protected final String modelName;
    protected final String apiKey;
    protected final String baseUrl;
    
    protected AbstractModelAdapter(ConfigManager configManager, String modelName, String profilePath) {
        this.configManager = configManager;
        this.modelName = modelName;
        
        // 读取模型配置
        this.apiKey = resolveApiKey(profilePath);
        this.baseUrl = configManager.getString(profilePath + ".baseUrl", getDefaultBaseUrl());
        
        // 创建 HTTP 客户端
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(120))
            .writeTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    /**
     * 解析 API Key
     * 优先从环境变量读取，其次从配置文件读取
     */
    private String resolveApiKey(String profilePath) {
        // 尝试从配置中获取环境变量名
        String envVarName = configManager.getString(profilePath + ".apiKeyEnv", null);
        if (envVarName != null) {
            String envValue = System.getenv(envVarName);
            if (envValue != null && !envValue.isEmpty()) {
                return envValue;
            }
        }
        
        // 从配置文件读取
        return configManager.getString(profilePath + ".apiKey", "");
    }
    
    /**
     * 获取默认 Base URL
     */
    protected abstract String getDefaultBaseUrl();
    
    /**
     * 流式发送消息（子类可选实现）
     */
    public void sendMessageStream(List<Message> messages, String systemPrompt, StreamHandler handler) {
        throw new UnsupportedOperationException("Stream mode not supported by " + getProviderName());
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    /**
     * 检查配置并抛出异常
     */
    protected void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                String.format("API key not configured for %s. Please set environment variable or configure in settings.",
                    getProviderName())
            );
        }
    }
}
