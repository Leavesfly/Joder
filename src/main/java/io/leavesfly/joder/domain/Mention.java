package io.leavesfly.joder.domain;

/**
 * Mention 引用数据模型
 * 
 * <p>表示用户输入中的一个 @ 引用。
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class Mention {
    
    /** Mention 类型 */
    private MentionType type;
    
    /** 引用值（文件路径、agent 名称等） */
    private String value;
    
    /** 原始文本 */
    private String rawText;
    
    /** 在输入文本中的起始位置 */
    private int startIndex;
    
    /** 在输入文本中的结束位置 */
    private int endIndex;
    
    /** 是否有效（验证结果） */
    private boolean valid;
    
    /** 验证消息（如果无效） */
    private String validationMessage;
    
    // 构造函数
    
    public Mention() {}
    
    public Mention(MentionType type, String value, String rawText, int startIndex, int endIndex) {
        this.type = type;
        this.value = value;
        this.rawText = rawText;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.valid = true;
    }
    
    // Getters and Setters
    
    public MentionType getType() {
        return type;
    }
    
    public void setType(MentionType type) {
        this.type = type;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getRawText() {
        return rawText;
    }
    
    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getValidationMessage() {
        return validationMessage;
    }
    
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
    
    @Override
    public String toString() {
        return "Mention{" +
                "type=" + type +
                ", value='" + value + '\'' +
                ", rawText='" + rawText + '\'' +
                ", valid=" + valid +
                '}';
    }
}
