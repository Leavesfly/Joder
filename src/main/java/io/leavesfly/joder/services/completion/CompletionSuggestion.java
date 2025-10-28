package io.leavesfly.joder.services.completion;

/**
 * 补全建议
 */
public class CompletionSuggestion {
    
    private final String text;              // 补全文本
    private final String displayText;       // 显示文本
    private final String description;       // 描述信息
    private final CompletionType type;      // 补全类型
    private final int score;                // 相关性分数 (0-100)
    
    public enum CompletionType {
        MODEL("模型"),
        FILE("文件"),
        COMMAND("命令"),
        TOOL("工具"),
        VARIABLE("变量");
        
        private final String description;
        
        CompletionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public CompletionSuggestion(String text, String displayText, String description, 
                               CompletionType type, int score) {
        this.text = text;
        this.displayText = displayText;
        this.description = description;
        this.type = type;
        this.score = Math.max(0, Math.min(100, score));
    }
    
    public String getText() {
        return text;
    }
    
    public String getDisplayText() {
        return displayText;
    }
    
    public String getDescription() {
        return description;
    }
    
    public CompletionType getType() {
        return type;
    }
    
    public int getScore() {
        return score;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s (score: %d)", 
                type.getDescription(), displayText, description, score);
    }
    
    /**
     * 创建模型补全建议
     */
    public static CompletionSuggestion model(String modelName, String description, int score) {
        return new CompletionSuggestion(
                "@ask-model:" + modelName,
                "@ask-model:" + modelName,
                description,
                CompletionType.MODEL,
                score
        );
    }
    
    /**
     * 创建文件补全建议
     */
    public static CompletionSuggestion file(String filePath, String description, int score) {
        return new CompletionSuggestion(
                "@file:" + filePath,
                "@file:" + filePath,
                description,
                CompletionType.FILE,
                score
        );
    }
    
    /**
     * 创建命令补全建议
     */
    public static CompletionSuggestion command(String command, String description, int score) {
        return new CompletionSuggestion(
                "/" + command,
                "/" + command,
                description,
                CompletionType.COMMAND,
                score
        );
    }
    
    /**
     * 创建工具补全建议
     */
    public static CompletionSuggestion tool(String toolName, String description, int score) {
        return new CompletionSuggestion(
                toolName,
                toolName,
                description,
                CompletionType.TOOL,
                score
        );
    }
}
