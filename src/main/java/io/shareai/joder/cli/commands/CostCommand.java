package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.services.cost.CostTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Cost 命令 - 显示当前会话的总成本和持续时间
 */
@Singleton
public class CostCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(CostCommand.class);
    
    private final CostTrackingService costTrackingService;
    
    @Inject
    public CostCommand(CostTrackingService costTrackingService) {
        this.costTrackingService = costTrackingService;
    }
    
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
        return CommandResult.success(costTrackingService.getCostSummary());
    }
    
    /**
     * 记录 API 调用成本（供其他组件调用）
     */
    public void recordApiCall(String modelName, int inputTokens, int outputTokens, long durationMs) {
        costTrackingService.recordModelCall(modelName, inputTokens, outputTokens, durationMs);
        logger.debug("记录 API 成本: {} - {} 输入 tokens, {} 输出 tokens, {} ms", 
            modelName, inputTokens, outputTokens, durationMs);
    }
    
    /**
     * 重置成本追踪
     */
    public void reset() {
        costTrackingService.reset();
    }
}
