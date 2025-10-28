package io.shareai.joder.services.completion;

import io.shareai.joder.services.model.ModelPointerManager;
import io.shareai.joder.services.model.ModelProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型补全提供者
 * 提供 @ask-model: 语义引用的补全
 */
public class ModelCompletionProvider implements CompletionProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelCompletionProvider.class);
    private static final String PREFIX = "@ask-model";
    
    private final ModelPointerManager modelPointerManager;
    
    @Inject
    public ModelCompletionProvider(ModelPointerManager modelPointerManager) {
        this.modelPointerManager = modelPointerManager;
    }
    
    @Override
    public String getName() {
        return "ModelCompletionProvider";
    }
    
    @Override
    public int getPriority() {
        return 100;  // 高优先级
    }
    
    @Override
    public boolean supports(String input, int cursorPosition) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String prefix = CompletionManager.extractPrefix(input, cursorPosition);
        return prefix.startsWith("@ask") || prefix.startsWith("@model");
    }
    
    @Override
    public List<CompletionSuggestion> getCompletions(String input, int cursorPosition) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        
        String prefix = CompletionManager.extractPrefix(input, cursorPosition).toLowerCase();
        
        // 添加模型指针补全
        addPointerCompletions(suggestions, prefix);
        
        // 添加具体模型补全
        addModelProfileCompletions(suggestions, prefix);
        
        return suggestions;
    }
    
    /**
     * 添加模型指针补全
     */
    private void addPointerCompletions(List<CompletionSuggestion> suggestions, String prefix) {
        for (ModelPointerManager.PointerType type : ModelPointerManager.PointerType.values()) {
            String pointerName = type.getKey();
            
            if (matches(prefix, pointerName)) {
                int score = calculateScore(prefix, pointerName);
                
                // 获取指针指向的模型
                String modelInfo = modelPointerManager.getModelForPointer(type)
                        .map(p -> " → " + p.getName())
                        .orElse(" (未配置)");
                
                suggestions.add(CompletionSuggestion.model(
                        pointerName,
                        type.getDescription() + modelInfo,
                        score
                ));
            }
        }
    }
    
    /**
     * 添加具体模型配置补全
     */
    private void addModelProfileCompletions(List<CompletionSuggestion> suggestions, String prefix) {
        Map<String, ModelProfile> profiles = modelPointerManager.getAllProfiles();
        
        for (Map.Entry<String, ModelProfile> entry : profiles.entrySet()) {
            String profileName = entry.getKey();
            ModelProfile profile = entry.getValue();
            
            if (matches(prefix, profileName)) {
                int score = calculateScore(prefix, profileName);
                
                String description = String.format("%s / %s%s",
                        profile.getProvider(),
                        profile.getModel(),
                        profile.hasApiKey() ? "" : " (无 API Key)"
                );
                
                suggestions.add(CompletionSuggestion.model(
                        profileName,
                        description,
                        score - 10  // 具体模型分数略低于指针
                ));
            }
        }
    }
    
    /**
     * 检查是否匹配
     */
    private boolean matches(String prefix, String candidate) {
        if (prefix.isEmpty()) {
            return true;
        }
        
        String normalizedPrefix = prefix.toLowerCase()
                .replace("@ask-model:", "")
                .replace("@ask-model", "")
                .replace("@model:", "")
                .replace("@model", "")
                .replace(":", "")
                .trim();
        
        if (normalizedPrefix.isEmpty()) {
            return true;
        }
        
        return candidate.toLowerCase().contains(normalizedPrefix);
    }
    
    /**
     * 计算相关性分数
     */
    private int calculateScore(String prefix, String candidate) {
        String normalizedPrefix = prefix.toLowerCase()
                .replace("@ask-model:", "")
                .replace("@ask-model", "")
                .replace("@model:", "")
                .replace("@model", "")
                .replace(":", "")
                .trim();
        
        String lowerCandidate = candidate.toLowerCase();
        
        // 完全匹配
        if (lowerCandidate.equals(normalizedPrefix)) {
            return 100;
        }
        
        // 前缀匹配
        if (lowerCandidate.startsWith(normalizedPrefix)) {
            return 90;
        }
        
        // 包含匹配
        if (lowerCandidate.contains(normalizedPrefix)) {
            return 70;
        }
        
        // 模糊匹配（字符都存在）
        if (fuzzyMatch(normalizedPrefix, lowerCandidate)) {
            return 50;
        }
        
        return 30;
    }
    
    /**
     * 模糊匹配
     */
    private boolean fuzzyMatch(String query, String target) {
        int queryIndex = 0;
        for (int i = 0; i < target.length() && queryIndex < query.length(); i++) {
            if (target.charAt(i) == query.charAt(queryIndex)) {
                queryIndex++;
            }
        }
        return queryIndex == query.length();
    }
}
