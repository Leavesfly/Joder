package io.leavesfly.joder.services.completion;

import java.util.List;

/**
 * 补全提供者接口
 * 负责根据输入提供智能补全建议
 */
public interface CompletionProvider {
    
    /**
     * 获取补全建议
     * 
     * @param input 用户输入
     * @param cursorPosition 光标位置
     * @return 补全建议列表（按相关性排序）
     */
    List<CompletionSuggestion> getCompletions(String input, int cursorPosition);
    
    /**
     * 检查是否支持该输入的补全
     * 
     * @param input 用户输入
     * @param cursorPosition 光标位置
     * @return 是否支持补全
     */
    boolean supports(String input, int cursorPosition);
    
    /**
     * 获取提供者名称
     */
    String getName();
    
    /**
     * 获取提供者优先级（数字越大优先级越高）
     */
    default int getPriority() {
        return 0;
    }
}
