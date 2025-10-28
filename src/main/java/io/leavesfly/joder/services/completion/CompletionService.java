package io.leavesfly.joder.services.completion;

import io.leavesfly.joder.util.FuzzyMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 命令补全服务
 * 使用 FuzzyMatcher 提供智能命令补全功能
 */
@Singleton
public class CompletionService {

    private static final Logger logger = LoggerFactory.getLogger(CompletionService.class);
    
    private final FuzzyMatcher fuzzyMatcher;
    private final Set<String> systemCommands;
    private final Set<String> joderCommands;
    private final Set<String> recentCommands;
    private final int maxRecentCommands = 100;

    public CompletionService() {
        this.fuzzyMatcher = new FuzzyMatcher();
        this.systemCommands = initializeSystemCommands();
        this.joderCommands = initializeJoderCommands();
        this.recentCommands = new LinkedHashSet<>();
    }

    /**
     * 初始化系统命令列表
     */
    private Set<String> initializeSystemCommands() {
        return new HashSet<>(Arrays.asList(
            // 常用系统命令
            "ls", "cd", "pwd", "cat", "grep", "find", "cp", "mv", "rm", "mkdir",
            "touch", "chmod", "chown", "ps", "kill", "top", "df", "du", "tar",
            "gzip", "gunzip", "zip", "unzip", "wget", "curl", "ssh", "scp",
            
            // Git 命令
            "git", "git-add", "git-commit", "git-push", "git-pull", "git-status",
            "git-log", "git-diff", "git-branch", "git-checkout", "git-merge",
            
            // 开发工具
            "node", "npm", "npx", "yarn", "python", "python3", "pip", "pip3",
            "java", "javac", "mvn", "gradle", "docker", "docker-compose",
            "kubectl", "helm", "terraform", "ansible",
            
            // 编辑器
            "vim", "vi", "nano", "emacs", "code", "idea"
        ));
    }

    /**
     * 初始化 Joder 内部命令列表
     */
    private Set<String> initializeJoderCommands() {
        return new HashSet<>(Arrays.asList(
            "/help", "/model", "/config", "/cost", "/clear", "/init",
            "/doctor", "/login", "/logout", "/agents", "/mcp"
        ));
    }

    /**
     * 获取命令补全建议
     * 
     * @param input 用户输入
     * @param limit 返回结果数量限制
     * @return 补全建议列表
     */
    public List<CompletionSuggestion> getCompletions(String input, int limit) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String trimmedInput = input.trim();
        
        // 如果是 Joder 命令（以 / 开头）
        if (trimmedInput.startsWith("/")) {
            return getJoderCommandCompletions(trimmedInput, limit);
        }
        
        // 系统命令补全
        return getSystemCommandCompletions(trimmedInput, limit);
    }

    /**
     * Joder 内部命令补全
     */
    private List<CompletionSuggestion> getJoderCommandCompletions(String input, int limit) {
        List<String> allCommands = new ArrayList<>(joderCommands);
        
        return fuzzyMatcher.matchMany(allCommands, input).stream()
            .limit(limit)
            .map(match -> new CompletionSuggestion(
                match.candidate,
                match.candidate,
                getJoderCommandDescription(match.candidate),
                CompletionSuggestion.CompletionType.COMMAND,
                (int) match.result.getScore()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 系统命令补全
     */
    private List<CompletionSuggestion> getSystemCommandCompletions(String input, int limit) {
        // 合并所有可能的命令源
        List<String> allCommands = new ArrayList<>();
        allCommands.addAll(recentCommands);      // 最近使用的命令优先
        allCommands.addAll(systemCommands);       // 系统命令
        
        // 去重
        Set<String> uniqueCommands = new LinkedHashSet<>(allCommands);
        
        return fuzzyMatcher.matchMany(new ArrayList<>(uniqueCommands), input).stream()
            .limit(limit)
            .map(match -> new CompletionSuggestion(
                match.candidate,
                match.candidate,
                getSystemCommandDescription(match.candidate),
                CompletionSuggestion.CompletionType.COMMAND,
                (int) match.result.getScore()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 添加到最近命令列表
     */
    public void addRecentCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        String trimmedCommand = command.trim();
        
        // 移除旧的相同命令
        recentCommands.remove(trimmedCommand);
        
        // 添加到开头
        recentCommands.add(trimmedCommand);
        
        // 限制大小
        if (recentCommands.size() > maxRecentCommands) {
            Iterator<String> iterator = recentCommands.iterator();
            iterator.next();
            iterator.remove();
        }

        logger.debug("添加最近命令: {}", trimmedCommand);
    }

    /**
     * 检查是否为最近使用的命令
     */
    private boolean isRecentCommand(String command) {
        return recentCommands.contains(command);
    }

    /**
     * 获取 Joder 命令描述
     */
    private String getJoderCommandDescription(String command) {
        switch (command) {
            case "/help":
                return "显示帮助信息";
            case "/model":
                return "模型管理";
            case "/config":
                return "配置管理";
            case "/cost":
                return "查看成本";
            case "/clear":
                return "清空会话";
            case "/init":
                return "初始化项目";
            case "/doctor":
                return "系统诊断";
            case "/login":
                return "登录";
            case "/logout":
                return "登出";
            case "/agents":
                return "代理管理";
            case "/mcp":
                return "MCP 管理";
            default:
                return "";
        }
    }

    /**
     * 获取系统命令描述
     */
    private String getSystemCommandDescription(String command) {
        // 简单的命令描述映射
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ls", "列出文件和目录");
        descriptions.put("cd", "切换目录");
        descriptions.put("pwd", "显示当前目录");
        descriptions.put("cat", "查看文件内容");
        descriptions.put("grep", "搜索文本");
        descriptions.put("git", "版本控制");
        descriptions.put("node", "运行 Node.js");
        descriptions.put("npm", "Node 包管理器");
        descriptions.put("python", "运行 Python");
        descriptions.put("docker", "容器管理");
        descriptions.put("vim", "文本编辑器");
        
        return descriptions.getOrDefault(command, "");
    }

    /**
     * 清空最近命令
     */
    public void clearRecentCommands() {
        recentCommands.clear();
        logger.info("已清空最近命令历史");
    }

    /**
     * 获取最近命令列表
     */
    public List<String> getRecentCommands(int limit) {
        return recentCommands.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取统计信息
     */
    public CompletionStats getStats() {
        return new CompletionStats(
            systemCommands.size(),
            joderCommands.size(),
            recentCommands.size()
        );
    }

    /**
     * 统计信息类
     */
    public static class CompletionStats {
        private final int systemCommandCount;
        private final int joderCommandCount;
        private final int recentCommandCount;

        public CompletionStats(int systemCommandCount, int joderCommandCount, int recentCommandCount) {
            this.systemCommandCount = systemCommandCount;
            this.joderCommandCount = joderCommandCount;
            this.recentCommandCount = recentCommandCount;
        }

        public int getSystemCommandCount() { return systemCommandCount; }
        public int getJoderCommandCount() { return joderCommandCount; }
        public int getRecentCommandCount() { return recentCommandCount; }
        public int getTotalCommandCount() { 
            return systemCommandCount + joderCommandCount + recentCommandCount; 
        }

        @Override
        public String toString() {
            return String.format("CompletionStats{system=%d, joder=%d, recent=%d, total=%d}",
                systemCommandCount, joderCommandCount, recentCommandCount, getTotalCommandCount());
        }
    }
}
