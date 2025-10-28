package io.leavesfly.joder.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义命令数据模型
 * 
 * <p>表示从 Markdown 文件加载的自定义命令，支持 YAML frontmatter 配置。
 * 兼容 Claude Desktop 和 Kode 的自定义命令格式。
 * 
 * <p>示例 Markdown 文件：
 * <pre>
 * ---
 * name: test-runner
 * description: Run project tests
 * aliases: [test, t]
 * enabled: true
 * allowed-tools: [BashTool, FileRead]
 * ---
 * 
 * Run tests for the current project:
 * !`npm test`
 * 
 * Recent test results from @test-results.log
 * </pre>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class CustomCommand {
    
    /**
     * 命令名称（必需）
     * <p>格式：user:namespace:command 或 project:namespace:command
     */
    private String name;
    
    /**
     * 命令描述
     */
    private String description;
    
    /**
     * 命令别名列表
     */
    private List<String> aliases;
    
    /**
     * 是否启用（默认 true）
     */
    private boolean enabled;
    
    /**
     * 是否隐藏（默认 false）
     */
    private boolean hidden;
    
    /**
     * 进度消息
     */
    private String progressMessage;
    
    /**
     * 参数名称列表（用于 {arg} 占位符替换）
     */
    private List<String> argNames;
    
    /**
     * 允许使用的工具列表（可选）
     */
    private List<String> allowedTools;
    
    /**
     * 命令内容（Markdown 正文，不含 frontmatter）
     */
    private String content;
    
    /**
     * 源文件路径
     */
    private String filePath;
    
    /**
     * 命令作用域（user 或 project）
     */
    private CommandScope scope;
    
    /**
     * 命令作用域枚举
     */
    public enum CommandScope {
        /** 用户级命令（~/.claude/commands 或 ~/.kode/commands） */
        USER,
        /** 项目级命令（{project}/.claude/commands 或 {project}/.kode/commands） */
        PROJECT
    }
    
    // 构造函数
    
    public CustomCommand() {
        this.aliases = new ArrayList<>();
        this.argNames = new ArrayList<>();
        this.allowedTools = new ArrayList<>();
        this.enabled = true;
        this.hidden = false;
        this.scope = CommandScope.PROJECT;
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
    
    public List<String> getAliases() {
        return aliases;
    }
    
    public void setAliases(List<String> aliases) {
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public String getProgressMessage() {
        return progressMessage;
    }
    
    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }
    
    public List<String> getArgNames() {
        return argNames;
    }
    
    public void setArgNames(List<String> argNames) {
        this.argNames = argNames != null ? argNames : new ArrayList<>();
    }
    
    public List<String> getAllowedTools() {
        return allowedTools;
    }
    
    public void setAllowedTools(List<String> allowedTools) {
        this.allowedTools = allowedTools != null ? allowedTools : new ArrayList<>();
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public CommandScope getScope() {
        return scope;
    }
    
    public void setScope(CommandScope scope) {
        this.scope = scope;
    }
    
    // 辅助方法
    
    /**
     * 获取用户可见的命令名称
     */
    public String getUserFacingName() {
        return name;
    }
    
    /**
     * 检查命令是否匹配给定的名称或别名
     */
    public boolean matches(String commandName) {
        if (commandName == null) {
            return false;
        }
        
        if (name != null && name.equals(commandName)) {
            return true;
        }
        
        return aliases != null && aliases.contains(commandName);
    }
    
    /**
     * 生成命令的前缀（user: 或 project:）
     */
    public String getPrefix() {
        return scope == CommandScope.USER ? "user:" : "project:";
    }
    
    @Override
    public String toString() {
        return "CustomCommand{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", scope=" + scope +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
