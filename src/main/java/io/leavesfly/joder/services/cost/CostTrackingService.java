package io.leavesfly.joder.services.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 成本追踪服务
 * 支持按模型、工具、会话和项目级别的成本统计
 */
@Singleton
public class CostTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(CostTrackingService.class);
    
    // 会话开始时间
    private final long sessionStartTime = System.currentTimeMillis();
    private final String sessionId = generateSessionId();
    
    // 总体统计
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    private final AtomicLong totalApiCalls = new AtomicLong(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    
    // 按模型统计
    private final Map<String, ModelCost> modelCosts = new ConcurrentHashMap<>();
    
    // 按工具统计
    private final Map<String, ToolUsage> toolUsages = new ConcurrentHashMap<>();
    
    // API调用历史
    private final List<ApiCallRecord> callHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * 记录模型API调用
     */
    public void recordModelCall(String modelName, int inputTokens, int outputTokens, long durationMs) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        totalApiCalls.incrementAndGet();
        totalDurationMs.addAndGet(durationMs);
        
        // 按模型统计
        modelCosts.computeIfAbsent(modelName, k -> new ModelCost())
                 .record(inputTokens, outputTokens, durationMs);
        
        // 记录历史
        addToHistory(new ApiCallRecord(modelName, inputTokens, outputTokens, durationMs));
        
        logger.debug("记录模型调用: {} - {} input, {} output, {} ms",
            modelName, inputTokens, outputTokens, durationMs);
    }
    
    /**
     * 记录工具使用
     */
    public void recordToolUsage(String toolName, long durationMs, boolean success) {
        toolUsages.computeIfAbsent(toolName, k -> new ToolUsage())
                 .record(durationMs, success);
        
        logger.debug("记录工具使用: {} - {} ms, success={}",
            toolName, durationMs, success);
    }
    
    /**
     * 获取会话统计
     */
    public SessionStats getSessionStats() {
        return new SessionStats(
            sessionId,
            sessionStartTime,
            totalInputTokens.get(),
            totalOutputTokens.get(),
            totalApiCalls.get(),
            totalDurationMs.get(),
            new HashMap<>(modelCosts),
            new HashMap<>(toolUsages)
        );
    }
    
    /**
     * 获取成本摘要
     */
    public String getCostSummary() {
        SessionStats stats = getSessionStats();
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════╗\n");
        sb.append("║          会话成本统计                      ║\n");
        sb.append("╠════════════════════════════════════════════╣\n");
        sb.append(String.format("║ 会话ID: %-34s ║\n", sessionId.substring(0, Math.min(34, sessionId.length()))));
        sb.append(String.format("║ 会话时长: %,8d 秒                    ║\n", 
            (System.currentTimeMillis() - sessionStartTime) / 1000));
        sb.append("╠════════════════════════════════════════════╣\n");
        sb.append(String.format("║ API调用次数: %,10d                   ║\n", stats.totalApiCalls));
        sb.append(String.format("║ 输入Token:   %,10d                   ║\n", stats.totalInputTokens));
        sb.append(String.format("║ 输出Token:   %,10d                   ║\n", stats.totalOutputTokens));
        sb.append(String.format("║ 总计Token:   %,10d                   ║\n", 
            stats.totalInputTokens + stats.totalOutputTokens));
        sb.append("╠════════════════════════════════════════════╣\n");
        
        // 计算总成本(使用简化的平均定价)
        double inputCost = (stats.totalInputTokens / 1_000_000.0) * 3.0;
        double outputCost = (stats.totalOutputTokens / 1_000_000.0) * 15.0;
        double totalCost = inputCost + outputCost;
        
        sb.append(String.format("║ 输入成本:    $%,.4f                    ║\n", inputCost));
        sb.append(String.format("║ 输出成本:    $%,.4f                    ║\n", outputCost));
        sb.append(String.format("║ 总计成本:    $%,.4f                    ║\n", totalCost));
        sb.append("╚════════════════════════════════════════════╝\n");
        
        // 按模型统计
        if (!stats.modelCosts.isEmpty()) {
            sb.append("\n按模型统计:\n");
            stats.modelCosts.forEach((model, cost) -> {
                sb.append(String.format("  %s: %,d calls, %,d tokens\n",
                    model, cost.calls, cost.inputTokens + cost.outputTokens));
            });
        }
        
        // 按工具统计
        if (!stats.toolUsages.isEmpty()) {
            sb.append("\n按工具统计:\n");
            stats.toolUsages.forEach((tool, usage) -> {
                sb.append(String.format("  %s: %,d uses, %.1f%% success\n",
                    tool, usage.totalCalls, usage.getSuccessRate() * 100));
            });
        }
        
        return sb.toString();
    }
    
    /**
     * 重置统计
     */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalApiCalls.set(0);
        totalDurationMs.set(0);
        modelCosts.clear();
        toolUsages.clear();
        callHistory.clear();
        
        logger.info("成本追踪已重置");
    }
    
    /**
     * 添加到历史记录
     */
    private void addToHistory(ApiCallRecord record) {
        synchronized (callHistory) {
            callHistory.add(record);
            if (callHistory.size() > MAX_HISTORY_SIZE) {
                callHistory.remove(0);
            }
        }
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
               "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 模型成本统计
     */
    public static class ModelCost {
        long calls = 0;
        long inputTokens = 0;
        long outputTokens = 0;
        long totalDurationMs = 0;
        
        void record(int input, int output, long duration) {
            calls++;
            inputTokens += input;
            outputTokens += output;
            totalDurationMs += duration;
        }
    }
    
    /**
     * 工具使用统计
     */
    public static class ToolUsage {
        long totalCalls = 0;
        long successCalls = 0;
        long totalDurationMs = 0;
        
        void record(long duration, boolean success) {
            totalCalls++;
            if (success) {
                successCalls++;
            }
            totalDurationMs += duration;
        }
        
        double getSuccessRate() {
            return totalCalls > 0 ? (double) successCalls / totalCalls : 0.0;
        }
    }
    
    /**
     * API调用记录
     */
    private static class ApiCallRecord {
        final String modelName;
        final int inputTokens;
        final int outputTokens;
        final long durationMs;
        final long timestamp;
        
        ApiCallRecord(String modelName, int inputTokens, int outputTokens, long durationMs) {
            this.modelName = modelName;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.durationMs = durationMs;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 会话统计信息
     */
    public static class SessionStats {
        public final String sessionId;
        public final long sessionStartTime;
        public final long totalInputTokens;
        public final long totalOutputTokens;
        public final long totalApiCalls;
        public final long totalDurationMs;
        public final Map<String, ModelCost> modelCosts;
        public final Map<String, ToolUsage> toolUsages;
        
        SessionStats(String sessionId, long sessionStartTime,
                    long totalInputTokens, long totalOutputTokens,
                    long totalApiCalls, long totalDurationMs,
                    Map<String, ModelCost> modelCosts,
                    Map<String, ToolUsage> toolUsages) {
            this.sessionId = sessionId;
            this.sessionStartTime = sessionStartTime;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.totalApiCalls = totalApiCalls;
            this.totalDurationMs = totalDurationMs;
            this.modelCosts = modelCosts;
            this.toolUsages = toolUsages;
        }
    }
}
