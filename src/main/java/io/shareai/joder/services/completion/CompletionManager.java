package io.shareai.joder.services.completion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 补全管理器
 * 协调多个补全提供者，提供统一的补全服务
 */
@Singleton
public class CompletionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CompletionManager.class);
    
    private final List<CompletionProvider> providers;
    
    @Inject
    public CompletionManager() {
        this.providers = new ArrayList<>();
    }
    
    /**
     * 注册补全提供者
     */
    public void registerProvider(CompletionProvider provider) {
        providers.add(provider);
        // 按优先级排序（高优先级在前）
        providers.sort(Comparator.comparingInt(CompletionProvider::getPriority).reversed());
        logger.info("Registered completion provider: {} (priority: {})", 
                provider.getName(), provider.getPriority());
    }
    
    /**
     * 获取补全建议
     * 
     * @param input 用户输入
     * @param cursorPosition 光标位置
     * @return 补全建议列表
     */
    public List<CompletionSuggestion> getCompletions(String input, int cursorPosition) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<CompletionSuggestion> allSuggestions = new ArrayList<>();
        
        // 收集所有支持的提供者的建议
        for (CompletionProvider provider : providers) {
            try {
                if (provider.supports(input, cursorPosition)) {
                    List<CompletionSuggestion> suggestions = 
                            provider.getCompletions(input, cursorPosition);
                    if (suggestions != null && !suggestions.isEmpty()) {
                        allSuggestions.addAll(suggestions);
                        logger.debug("Provider {} returned {} suggestions", 
                                provider.getName(), suggestions.size());
                    }
                }
            } catch (Exception e) {
                logger.error("Error getting completions from provider: {}", 
                        provider.getName(), e);
            }
        }
        
        // 按分数排序并去重
        return allSuggestions.stream()
                .sorted(Comparator.comparingInt(CompletionSuggestion::getScore).reversed())
                .distinct()
                .limit(10)  // 限制返回数量
                .collect(Collectors.toList());
    }
    
    /**
     * 获取补全建议（简化版，光标在末尾）
     */
    public List<CompletionSuggestion> getCompletions(String input) {
        return getCompletions(input, input != null ? input.length() : 0);
    }
    
    /**
     * 检查是否有可用的补全
     */
    public boolean hasCompletions(String input, int cursorPosition) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        for (CompletionProvider provider : providers) {
            if (provider.supports(input, cursorPosition)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取所有注册的提供者
     */
    public List<CompletionProvider> getProviders() {
        return new ArrayList<>(providers);
    }
    
    /**
     * 提取输入中的前缀（用于触发补全）
     * 
     * @param input 完整输入
     * @param cursorPosition 光标位置
     * @return 当前单词/前缀
     */
    public static String extractPrefix(String input, int cursorPosition) {
        if (input == null || cursorPosition <= 0) {
            return "";
        }
        
        int pos = Math.min(cursorPosition, input.length());
        int start = pos;
        
        // 向前查找到空格或特殊字符
        while (start > 0) {
            char c = input.charAt(start - 1);
            if (Character.isWhitespace(c)) {
                break;
            }
            start--;
        }
        
        return input.substring(start, pos);
    }
}
