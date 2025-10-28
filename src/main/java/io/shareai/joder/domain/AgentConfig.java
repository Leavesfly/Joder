package io.shareai.joder.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agent 配置
 * 表示一个 AI 代理的完整配置信息
 */
public class AgentConfig {
    
    private String name;              // 代理名称
    private String description;       // 代理描述
    private String systemPrompt;      // 系统提示词
    private List<String> tools;       // 允许的工具列表
    private String model;             // 使用的模型
    private String color;             // UI 主题颜色
    private AgentType type;           // 代理类型
    private String filePath;          // 配置文件路径
    
    public enum AgentType {
        BUILTIN("built-in", "内置代理"),
        USER("user", "用户代理"),
        PROJECT("project", "项目代理");
        
        private final String key;
        private final String description;
        
        AgentType(String key, String description) {
            this.key = key;
            this.description = description;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public AgentConfig() {
        this.tools = new ArrayList<>();
        this.type = AgentType.USER;
    }
    
    public AgentConfig(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public List<String> getTools() {
        return tools;
    }
    
    public void setTools(List<String> tools) {
        this.tools = tools != null ? tools : new ArrayList<>();
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public AgentType getType() {
        return type;
    }
    
    public void setType(AgentType type) {
        this.type = type;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
                && systemPrompt != null && !systemPrompt.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentConfig that = (AgentConfig) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
    
    @Override
    public String toString() {
        return "AgentConfig{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", model='" + model + '\'' +
                ", tools=" + tools.size() +
                '}';
    }
    
    /**
     * 创建默认的通用代理
     */
    public static AgentConfig createGeneralPurpose() {
        AgentConfig agent = new AgentConfig();
        agent.setName("general-purpose");
        agent.setDescription("通用 AI 助手");
        agent.setSystemPrompt("你是一个通用的 AI 助手，可以帮助用户完成各种任务。");
        agent.setModel("claude-3-5-sonnet");
        agent.setColor("blue");
        agent.setType(AgentType.BUILTIN);
        agent.setTools(List.of("*"));  // 所有工具
        return agent;
    }
    
    /**
     * 创建代码专家代理
     */
    public static AgentConfig createCodeExpert() {
        AgentConfig agent = new AgentConfig();
        agent.setName("code-expert");
        agent.setDescription("代码审查和优化专家");
        agent.setSystemPrompt("""
                你是一个资深的代码专家，擅长：
                1. 代码审查和质量分析
                2. 性能优化建议
                3. 架构设计评审
                4. 最佳实践指导
                
                请提供专业、详细且可操作的建议。
                """);
        agent.setModel("claude-3-5-sonnet");
        agent.setColor("green");
        agent.setType(AgentType.BUILTIN);
        agent.setTools(List.of("FileRead", "FileWrite", "FileEdit", "Grep", "Bash"));
        return agent;
    }
}
