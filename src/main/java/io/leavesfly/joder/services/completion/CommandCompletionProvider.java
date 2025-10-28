package io.leavesfly.joder.services.completion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令补全提供者
 * 提供 / 命令的补全
 */
public class CommandCompletionProvider implements CompletionProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandCompletionProvider.class);
    
    // 命令定义 <命令名, 描述>
    private static final Map<String, String> COMMANDS = new LinkedHashMap<>();
    
    static {
        // 基础命令
        COMMANDS.put("help", "显示帮助信息");
        COMMANDS.put("clear", "清除屏幕");
        COMMANDS.put("config", "配置管理");
        COMMANDS.put("model", "模型配置");
        COMMANDS.put("mcp", "MCP 服务器管理");
        COMMANDS.put("exit", "退出程序");
        
        // 项目管理命令
        COMMANDS.put("init", "初始化项目配置");
        COMMANDS.put("modelstatus", "查看模型状态");
        
        // 诊断命令
        COMMANDS.put("cost", "查看 API 成本");
        COMMANDS.put("doctor", "系统诊断");
        
        // 代理命令
        COMMANDS.put("agents", "代理管理");
        
        // 会话管理
        COMMANDS.put("resume", "恢复会话");
        COMMANDS.put("login", "登录");
        COMMANDS.put("logout", "登出");
        COMMANDS.put("history", "会话历史");
        COMMANDS.put("export", "导出会话");
        
        // 开发工具
        COMMANDS.put("review", "代码审查");
        COMMANDS.put("listen", "文件监听");
        COMMANDS.put("benchmark", "性能基准测试");
        COMMANDS.put("debug", "调试工具");
        COMMANDS.put("test", "运行测试");
        
        // 其他辅助命令
        COMMANDS.put("compact", "紧凑输出模式");
        COMMANDS.put("bug", "Bug 报告");
        COMMANDS.put("release-notes", "发布说明");
    }
    
    @Inject
    public CommandCompletionProvider() {
    }
    
    @Override
    public String getName() {
        return "CommandCompletionProvider";
    }
    
    @Override
    public int getPriority() {
        return 80;  // 中高优先级
    }
    
    @Override
    public boolean supports(String input, int cursorPosition) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String trimmed = input.trim();
        return trimmed.startsWith("/");
    }
    
    @Override
    public List<CompletionSuggestion> getCompletions(String input, int cursorPosition) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        
        String query = extractCommandQuery(input);
        
        for (Map.Entry<String, String> entry : COMMANDS.entrySet()) {
            String command = entry.getKey();
            String description = entry.getValue();
            
            if (matches(query, command)) {
                int score = calculateScore(query, command);
                
                suggestions.add(CompletionSuggestion.command(
                        command,
                        description,
                        score
                ));
            }
        }
        
        return suggestions;
    }
    
    /**
     * 提取命令查询部分
     */
    private String extractCommandQuery(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 检查是否匹配
     */
    private boolean matches(String query, String command) {
        if (query.isEmpty()) {
            return true;
        }
        
        String lowerCommand = command.toLowerCase();
        
        // 前缀匹配
        if (lowerCommand.startsWith(query)) {
            return true;
        }
        
        // 包含匹配
        if (lowerCommand.contains(query)) {
            return true;
        }
        
        // 模糊匹配
        return fuzzyMatch(query, lowerCommand);
    }
    
    /**
     * 计算相关性分数
     */
    private int calculateScore(String query, String command) {
        if (query.isEmpty()) {
            return 60;  // 默认分数
        }
        
        String lowerCommand = command.toLowerCase();
        
        // 完全匹配
        if (lowerCommand.equals(query)) {
            return 100;
        }
        
        // 前缀匹配
        if (lowerCommand.startsWith(query)) {
            return 95;
        }
        
        // 包含匹配
        if (lowerCommand.contains(query)) {
            int index = lowerCommand.indexOf(query);
            return 85 - index;  // 越靠前分数越高
        }
        
        // 模糊匹配
        if (fuzzyMatch(query, lowerCommand)) {
            return 70;
        }
        
        return 50;
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
    
    /**
     * 注册新命令（用于动态扩展）
     */
    public static void registerCommand(String command, String description) {
        COMMANDS.put(command, description);
    }
}
