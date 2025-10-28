package io.shareai.joder.services.model.dto;

import io.shareai.joder.domain.Message;

import java.util.List;

/**
 * 模型请求数据
 */
public class ModelRequest {
    private final List<Message> messages;
    private final String systemPrompt;
    private final Double temperature;
    private final Integer maxTokens;
    private final boolean stream;
    
    private ModelRequest(Builder builder) {
        this.messages = builder.messages;
        this.systemPrompt = builder.systemPrompt;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.stream = builder.stream;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<Message> messages;
        private String systemPrompt;
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
        private boolean stream = false;
        
        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }
        
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }
        
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public ModelRequest build() {
            return new ModelRequest(this);
        }
    }
}
