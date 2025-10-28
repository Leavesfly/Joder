package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cost 命令 - 显示当前会话的总成本和持续时间
 */
@Singleton
public class CostCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(CostCommand.class);
    
    // 成本追踪（简化实现）
    private final AtomicInteger totalInputTokens = new AtomicInteger(0);
    private final AtomicInteger totalOutputTokens = new AtomicInteger(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    private final long sessionStartTime = System.currentTimeMillis();
    
    @Override
    public String getDescription() {
        return "显示当前会话的总成本和持续时间";
    }
    
    @Override
    public String getUsage() {
        return "/cost - 显示 API 调用成本统计";
    }
    
    @Override
    public CommandResult execute(String args) {
        int inputTokens = totalInputTokens.get();
        int outputTokens = totalOutputTokens.get();
        int totalTokens = inputTokens + outputTokens;
        long durationMs = totalDurationMs.get();
        long sessionDurationMs = System.currentTimeMillis() - sessionStartTime;
        
        // 简化的成本估算（基于 Claude 3.5 Sonnet 定价）
        // 输入: $3 / MTok, 输出: $15 / MTok
        double inputCost = (inputTokens / 1_000_000.0) * 3.0;
        double outputCost = (outputTokens / 1_000_000.0) * 15.0;
        double totalCost = inputCost + outputCost;
        
        // 格式化输出
        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("┌─────────────────────────────────┐\n");
        result.append("│     会话成本统计                │\n");
        result.append("├─────────────────────────────────┤\n");
        result.append(String.format("│ 输入 Token:     %,10d    │\n", inputTokens));
        result.append(String.format("│ 输出 Token:     %,10d    │\n", outputTokens));
        result.append(String.format("│ 总计 Token:     %,10d    │\n", totalTokens));
        result.append("├─────────────────────────────────┤\n");
        result.append(String.format("│ 输入成本:        $%,.4f     │\n", inputCost));
        result.append(String.format("│ 输出成本:        $%,.4f     │\n", outputCost));
        result.append(String.format("│ 总计成本:        $%,.4f     │\n", totalCost));
        result.append("├─────────────────────────────────┤\n");
        result.append(String.format("│ API 时间:        %,6d ms   │\n", durationMs));
        result.append(String.format("│ 会话时间:        %,6d s    │\n", sessionDurationMs / 1000));
        result.append("└─────────────────────────────────┘\n");
        
        return CommandResult.success(result.toString());
    }
    
    /**
     * 记录 API 调用成本（供其他组件调用）
     */
    public void recordApiCall(int inputTokens, int outputTokens, long durationMs) {
        this.totalInputTokens.addAndGet(inputTokens);
        this.totalOutputTokens.addAndGet(outputTokens);
        this.totalDurationMs.addAndGet(durationMs);
        
        logger.debug("记录 API 成本: {} 输入 tokens, {} 输出 tokens, {} ms", 
            inputTokens, outputTokens, durationMs);
    }
    
    /**
     * 重置成本追踪
     */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalDurationMs.set(0);
        logger.info("成本追踪已重置");
    }
}
