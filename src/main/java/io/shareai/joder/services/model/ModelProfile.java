package io.shareai.joder.services.model;

import java.util.Objects;

/**
 * 模型配置文件
 * 包含单个 AI 模型的完整配置信息
 */
public class ModelProfile {
    
    private String name;          // 模型名称（唯一标识）
    private String provider;      // 提供商（anthropic, openai, alibaba, etc.）
    private String model;         // 模型标识（如 claude-3-5-sonnet-20241022）
    private String apiKey;        // API 密钥
    private String baseUrl;       // API 基础URL（可选）
    private int maxTokens;        // 最大输出 token 数
    private double temperature;   // 温度参数
    private int contextLength;    // 上下文长度
    
    // 构造函数
    public ModelProfile() {
        this.maxTokens = 8096;
        this.temperature = 1.0;
        this.contextLength = 200000;
    }
    
    public ModelProfile(String name, String provider, String model) {
        this();
        this.name = name;
        this.provider = provider;
        this.model = model;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public int getContextLength() {
        return contextLength;
    }
    
    public void setContextLength(int contextLength) {
        this.contextLength = contextLength;
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
                && provider != null && !provider.trim().isEmpty()
                && model != null && !model.trim().isEmpty();
    }
    
    /**
     * 是否配置了 API Key
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelProfile that = (ModelProfile) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "ModelProfile{" +
                "name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", hasApiKey=" + hasApiKey() +
                ", maxTokens=" + maxTokens +
                '}';
    }
    
    /**
     * 创建默认的 Claude 配置
     */
    public static ModelProfile createClaudeDefault() {
        ModelProfile profile = new ModelProfile();
        profile.setName("claude-3-sonnet");
        profile.setProvider("anthropic");
        profile.setModel("claude-3-5-sonnet-20241022");
        profile.setMaxTokens(8096);
        profile.setTemperature(1.0);
        profile.setContextLength(200000);
        return profile;
    }
    
    /**
     * 创建默认的 OpenAI 配置
     */
    public static ModelProfile createOpenAIDefault() {
        ModelProfile profile = new ModelProfile();
        profile.setName("gpt-4");
        profile.setProvider("openai");
        profile.setModel("gpt-4-turbo");
        profile.setMaxTokens(4096);
        profile.setTemperature(1.0);
        profile.setContextLength(128000);
        return profile;
    }
}
