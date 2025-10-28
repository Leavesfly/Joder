package io.leavesfly.joder.services.mention;

import io.leavesfly.joder.domain.Mention;
import io.leavesfly.joder.domain.MentionType;
import io.leavesfly.joder.util.FuzzyMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mention 处理器
 * 
 * <p>解析、验证和展开用户输入中的 @ 引用。
 * 
 * <p>支持的 mention 格式：
 * <ul>
 *   <li>@file:path/to/file.txt - 文件引用</li>
 *   <li>@agent:agent-name - Agent 引用</li>
 *   <li>@model:model-name - 模型引用</li>
 *   <li>@run-agent:agent-name - 运行 Agent</li>
 *   <li>@ask-model:model-name - 询问模型</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
@Singleton
public class MentionProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MentionProcessor.class);
    
    /**
     * Mention 匹配模式
     * 匹配 @type:value 格式，value 可以包含字母数字、连字符、下划线、点、斜杠
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "@([a-z-]+):([a-zA-Z0-9_./\\-]+)",
            Pattern.MULTILINE
    );
    
    private final FuzzyMatcher fuzzyMatcher;
    
    public MentionProcessor() {
        this.fuzzyMatcher = new FuzzyMatcher();
    }
    
    /**
     * 解析输入文本中的所有 mention
     * 
     * @param input 输入文本
     * @return Mention 列表
     */
    public List<Mention> parseMentions(String input) {
        List<Mention> mentions = new ArrayList<>();
        
        if (input == null || input.isEmpty()) {
            return mentions;
        }
        
        Matcher matcher = MENTION_PATTERN.matcher(input);
        
        while (matcher.find()) {
            String rawText = matcher.group(0);
            String typeStr = "@" + matcher.group(1);
            String value = matcher.group(2);
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            
            MentionType type = MentionType.fromString(typeStr);
            
            if (type != null) {
                Mention mention = new Mention(type, value, rawText, startIndex, endIndex);
                mentions.add(mention);
                
                logger.debug("Parsed mention: {} -> {}", rawText, type);
            } else {
                logger.debug("Unknown mention type: {}", typeStr);
            }
        }
        
        return mentions;
    }
    
    /**
     * 验证 mention 的有效性
     * 
     * @param mention Mention 对象
     * @param workingDirectory 工作目录（用于验证文件路径）
     */
    public void validateMention(Mention mention, String workingDirectory) {
        if (mention == null) {
            return;
        }
        
        switch (mention.getType()) {
            case FILE:
                validateFileMention(mention, workingDirectory);
                break;
                
            case AGENT:
            case RUN_AGENT:
                validateAgentMention(mention);
                break;
                
            case MODEL:
            case ASK_MODEL:
                validateModelMention(mention);
                break;
                
            default:
                mention.setValid(true);
        }
    }
    
    /**
     * 验证文件引用
     */
    private void validateFileMention(Mention mention, String workingDirectory) {
        String filePath = mention.getValue();
        
        // 尝试多种路径解析方式
        Path path = null;
        
        // 1. 绝对路径
        if (filePath.startsWith("/") || filePath.matches("[A-Za-z]:.*")) {
            path = Paths.get(filePath);
        }
        // 2. 相对于工作目录
        else if (workingDirectory != null) {
            path = Paths.get(workingDirectory, filePath);
        }
        // 3. 相对于当前目录
        else {
            path = Paths.get(filePath);
        }
        
        if (path != null && Files.exists(path)) {
            mention.setValid(true);
            logger.debug("File mention valid: {}", path);
        } else {
            mention.setValid(false);
            mention.setValidationMessage("File not found: " + filePath);
            logger.warn("File mention invalid: {} (resolved to: {})", filePath, path);
        }
    }
    
    /**
     * 验证 Agent 引用
     */
    private void validateAgentMention(Mention mention) {
        String agentName = mention.getValue();
        
        // 检查 agent 名称格式
        if (agentName == null || agentName.isEmpty()) {
            mention.setValid(false);
            mention.setValidationMessage("Agent name is empty");
            return;
        }
        
        // 基本格式检查（允许字母、数字、连字符、下划线）
        if (!agentName.matches("[a-zA-Z0-9_-]+")) {
            mention.setValid(false);
            mention.setValidationMessage("Invalid agent name format: " + agentName);
            return;
        }
        
        // 假设 agent 存在（实际需要查询 agent 注册表）
        mention.setValid(true);
        logger.debug("Agent mention valid: {}", agentName);
    }
    
    /**
     * 验证模型引用
     */
    private void validateModelMention(Mention mention) {
        String modelName = mention.getValue();
        
        // 检查模型名称格式
        if (modelName == null || modelName.isEmpty()) {
            mention.setValid(false);
            mention.setValidationMessage("Model name is empty");
            return;
        }
        
        // 基本格式检查
        if (!modelName.matches("[a-zA-Z0-9_.-]+")) {
            mention.setValid(false);
            mention.setValidationMessage("Invalid model name format: " + modelName);
            return;
        }
        
        // 假设模型存在（实际需要查询模型注册表）
        mention.setValid(true);
        logger.debug("Model mention valid: {}", modelName);
    }
    
    /**
     * 模糊匹配文件路径
     * 
     * @param partialPath 部分路径
     * @param candidates 候选文件列表
     * @param maxResults 最大结果数
     * @return 最佳匹配结果
     */
    public List<String> fuzzyMatchFile(String partialPath, List<String> candidates, int maxResults) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<MatchScore> scores = new ArrayList<>();
        
        for (String candidate : candidates) {
            FuzzyMatcher.MatchResult result = fuzzyMatcher.match(candidate, partialPath);
            if (result.getScore() > 0) {
                scores.add(new MatchScore(candidate, result.getScore()));
            }
        }
        
        // 按分数降序排序
        scores.sort((a, b) -> Double.compare(b.score, a.score));
        
        return scores.stream()
                .limit(maxResults)
                .map(s -> s.candidate)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 匹配分数内部类
     */
    private static class MatchScore {
        final String candidate;
        final double score;
        
        MatchScore(String candidate, double score) {
            this.candidate = candidate;
            this.score = score;
        }
    }
    
    /**
     * 展开 mention 为实际内容
     * 
     * @param mention Mention 对象
     * @param workingDirectory 工作目录
     * @return 展开后的内容，如果无法展开返回原始文本
     */
    public String expandMention(Mention mention, String workingDirectory) {
        if (!mention.isValid()) {
            return mention.getRawText();
        }
        
        switch (mention.getType()) {
            case FILE:
                return expandFileMention(mention, workingDirectory);
                
            case AGENT:
            case RUN_AGENT:
                return expandAgentMention(mention);
                
            case MODEL:
            case ASK_MODEL:
                return expandModelMention(mention);
                
            default:
                return mention.getRawText();
        }
    }
    
    /**
     * 展开文件引用为文件内容
     */
    private String expandFileMention(Mention mention, String workingDirectory) {
        String filePath = mention.getValue();
        
        try {
            Path path;
            if (filePath.startsWith("/") || filePath.matches("[A-Za-z]:.*")) {
                path = Paths.get(filePath);
            } else if (workingDirectory != null) {
                path = Paths.get(workingDirectory, filePath);
            } else {
                path = Paths.get(filePath);
            }
            
            if (Files.exists(path) && Files.isRegularFile(path)) {
                String content = Files.readString(path);
                return String.format("```\nFile: %s\n\n%s\n```", filePath, content);
            }
        } catch (Exception e) {
            logger.error("Failed to expand file mention: {}", filePath, e);
        }
        
        return mention.getRawText();
    }
    
    /**
     * 展开 Agent 引用
     */
    private String expandAgentMention(Mention mention) {
        // 实际实现需要调用 Agent 系统
        return String.format("[Agent: %s]", mention.getValue());
    }
    
    /**
     * 展开模型引用
     */
    private String expandModelMention(Mention mention) {
        // 实际实现需要调用模型系统
        return String.format("[Model: %s]", mention.getValue());
    }
    
    /**
     * 处理输入文本，解析、验证并展开所有 mention
     * 
     * @param input 输入文本
     * @param workingDirectory 工作目录
     * @return 处理后的文本
     */
    public String processInput(String input, String workingDirectory) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        List<Mention> mentions = parseMentions(input);
        
        if (mentions.isEmpty()) {
            return input;
        }
        
        // 验证所有 mention
        for (Mention mention : mentions) {
            validateMention(mention, workingDirectory);
        }
        
        // 从后向前替换（避免索引错位）
        StringBuilder result = new StringBuilder(input);
        for (int i = mentions.size() - 1; i >= 0; i--) {
            Mention mention = mentions.get(i);
            String expanded = expandMention(mention, workingDirectory);
            result.replace(mention.getStartIndex(), mention.getEndIndex(), expanded);
        }
        
        return result.toString();
    }
    
    /**
     * 提取输入中的无效 mention
     * 
     * @param input 输入文本
     * @param workingDirectory 工作目录
     * @return 无效 mention 列表
     */
    public List<Mention> getInvalidMentions(String input, String workingDirectory) {
        List<Mention> mentions = parseMentions(input);
        List<Mention> invalid = new ArrayList<>();
        
        for (Mention mention : mentions) {
            validateMention(mention, workingDirectory);
            if (!mention.isValid()) {
                invalid.add(mention);
            }
        }
        
        return invalid;
    }
}
