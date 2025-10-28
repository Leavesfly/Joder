package io.leavesfly.joder.services.context;

import io.leavesfly.joder.domain.Message;

import java.util.List;

/**
 * 上下文压缩结果
 */
public class CompressionResult {
    
    private final List<Message> compressedMessages;
    private final int originalMessageCount;
    private final int compressedMessageCount;
    private final int originalTokens;
    private final int compressedTokens;
    private final long durationMs;
    
    public CompressionResult(
            List<Message> compressedMessages,
            int originalMessageCount,
            int compressedMessageCount,
            int originalTokens,
            int compressedTokens,
            long durationMs) {
        this.compressedMessages = compressedMessages;
        this.originalMessageCount = originalMessageCount;
        this.compressedMessageCount = compressedMessageCount;
        this.originalTokens = originalTokens;
        this.compressedTokens = compressedTokens;
        this.durationMs = durationMs;
    }
    
    public List<Message> getCompressedMessages() {
        return compressedMessages;
    }
    
    public int getOriginalMessageCount() {
        return originalMessageCount;
    }
    
    public int getCompressedMessageCount() {
        return compressedMessageCount;
    }
    
    public int getOriginalTokens() {
        return originalTokens;
    }
    
    public int getCompressedTokens() {
        return compressedTokens;
    }
    
    public int getSavedTokens() {
        return originalTokens - compressedTokens;
    }
    
    public double getCompressionRatio() {
        if (originalTokens == 0) {
            return 0.0;
        }
        return (double) compressedTokens / originalTokens;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    @Override
    public String toString() {
        return String.format(
            "CompressionResult{messages: %d->%d, tokens: %d->%d (%.1f%% saved), duration: %dms}",
            originalMessageCount, compressedMessageCount,
            originalTokens, compressedTokens,
            (1.0 - getCompressionRatio()) * 100,
            durationMs
        );
    }
}
