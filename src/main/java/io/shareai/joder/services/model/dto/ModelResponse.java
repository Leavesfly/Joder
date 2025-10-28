package io.shareai.joder.services.model.dto;

/**
 * 模型响应数据
 */
public class ModelResponse {
    private final String content;
    private final String stopReason;
    private final int inputTokens;
    private final int outputTokens;
    
    private ModelResponse(Builder builder) {
        this.content = builder.content;
        this.stopReason = builder.stopReason;
        this.inputTokens = builder.inputTokens;
        this.outputTokens = builder.outputTokens;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getStopReason() {
        return stopReason;
    }
    
    public int getInputTokens() {
        return inputTokens;
    }
    
    public int getOutputTokens() {
        return outputTokens;
    }
    
    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String content;
        private String stopReason;
        private int inputTokens;
        private int outputTokens;
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder stopReason(String stopReason) {
            this.stopReason = stopReason;
            return this;
        }
        
        public Builder inputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }
        
        public Builder outputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }
        
        public ModelResponse build() {
            return new ModelResponse(this);
        }
    }
}
