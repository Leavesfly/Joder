package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.domain.AgentConfig;
import io.leavesfly.joder.services.agents.AgentsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Agents Command - 代理管理命令
 * 支持 AGENTS.md 和 CLAUDE.md 标准
 */
@Singleton
public class AgentsCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentsCommand.class);
    
    private final AgentsManager agentsManager;
    
    @Inject
    public AgentsCommand(AgentsManager agentsManager) {
        this.agentsManager = agentsManager;
    }
    
    @Override
    public String getDescription() {
        return "管理 AI 代理配置";
    }
    
    @Override
    public String getUsage() {
        return "/agents - 列出和管理代理配置";
    }
    
    @Override
    public CommandResult execute(String args) {
        args = args != null ? args.trim() : "";
        
        // 解析命令
        String[] parts = args.split("\\s+", 2);
        String subCommand = parts.length > 0 ? parts[0] : "list";
        String subArgs = parts.length > 1 ? parts[1] : "";
        
        try {
            return switch (subCommand.toLowerCase()) {
                case "list", "" -> listAgents();
                case "show" -> showAgent(subArgs);
                case "init" -> initAgents(subArgs);
                case "reload" -> reloadAgents();
                default -> CommandResult.error("未知子命令: " + subCommand + ". 使用 /agents help 查看帮助。");
            };
        } catch (Exception e) {
            logger.error("Failed to execute agents command", e);
            return CommandResult.error("执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 列出所有代理
     */
    private CommandResult listAgents() {
        StringBuilder output = new StringBuilder();
        output.append("🤖 AI 代理列表\n\n");
        
        Map<String, AgentConfig> allAgents = agentsManager.getAllAgents();
        
        if (allAgents.isEmpty()) {
            output.append("未找到任何代理配置。\n\n");
            output.append("使用 /agents init 初始化代理目录。\n");
            return CommandResult.success(output.toString());
        }
        
        // 按类型分组
        List<AgentConfig> builtinAgents = agentsManager.getAgentsByType(AgentConfig.AgentType.BUILTIN);
        List<AgentConfig> userAgents = agentsManager.getAgentsByType(AgentConfig.AgentType.USER);
        List<AgentConfig> projectAgents = agentsManager.getAgentsByType(AgentConfig.AgentType.PROJECT);
        
        // 显示内置代理
        if (!builtinAgents.isEmpty()) {
            output.append("📚 内置代理 (").append(builtinAgents.size()).append(")\n");
            for (AgentConfig agent : builtinAgents) {
                output.append(formatAgentLine(agent));
            }
            output.append("\n");
        }
        
        // 显示用户代理
        if (!userAgents.isEmpty()) {
            output.append("👤 用户代理 (").append(userAgents.size()).append(")\n");
            for (AgentConfig agent : userAgents) {
                output.append(formatAgentLine(agent));
            }
            output.append("\n");
        }
        
        // 显示项目代理
        if (!projectAgents.isEmpty()) {
            output.append("📁 项目代理 (").append(projectAgents.size()).append(")\n");
            for (AgentConfig agent : projectAgents) {
                output.append(formatAgentLine(agent));
            }
            output.append("\n");
        }
        
        output.append("💡 使用提示:\n");
        output.append("  /agents show <name>  - 查看代理详情\n");
        output.append("  /agents init         - 初始化代理目录\n");
        output.append("  /agents reload       - 重新加载代理配置\n");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 格式化代理信息行
     */
    private String formatAgentLine(AgentConfig agent) {
        return String.format("  • %-20s %s%s\n",
                agent.getName(),
                agent.getDescription() != null ? agent.getDescription() : "",
                agent.getModel() != null ? " (" + agent.getModel() + ")" : ""
        );
    }
    
    /**
     * 显示代理详情
     */
    private CommandResult showAgent(String name) {
        if (name == null || name.trim().isEmpty()) {
            return CommandResult.error("请指定代理名称");
        }
        
        return agentsManager.getAgent(name.trim())
                .map(this::formatAgentDetails)
                .map(CommandResult::success)
                .orElse(CommandResult.error("未找到代理: " + name));
    }
    
    /**
     * 格式化代理详细信息
     */
    private String formatAgentDetails(AgentConfig agent) {
        StringBuilder output = new StringBuilder();
        
        output.append("🤖 代理: ").append(agent.getName()).append("\n\n");
        
        if (agent.getDescription() != null) {
            output.append("📝 描述: ").append(agent.getDescription()).append("\n");
        }
        
        output.append("🏷️  类型: ").append(agent.getType().getDescription()).append("\n");
        
        if (agent.getModel() != null) {
            output.append("🤖 模型: ").append(agent.getModel()).append("\n");
        }
        
        if (agent.getColor() != null) {
            output.append("🎨 颜色: ").append(agent.getColor()).append("\n");
        }
        
        if (agent.getTools() != null && !agent.getTools().isEmpty()) {
            output.append("🛠️  工具: ").append(String.join(", ", agent.getTools())).append("\n");
        }
        
        if (agent.getFilePath() != null) {
            output.append("💾 文件: ").append(agent.getFilePath()).append("\n");
        }
        
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            output.append("\n💬 系统提示词:\n");
            output.append(agent.getSystemPrompt());
        }
        
        return output.toString();
    }
    
    /**
     * 初始化代理目录
     */
    private CommandResult initAgents(String scope) {
        boolean isProject = scope.equals("project") || scope.isEmpty();
        
        try {
            agentsManager.initializeAgentsDirectory(isProject);
            agentsManager.reload();
            
            String location = isProject ? ".joder/agents/" : "~/.config/joder/agents/";
            return CommandResult.success(
                    "✅ 初始化成功！\n\n" +
                    "代理目录: " + location + "\n" +
                    "示例代理: example.md\n\n" +
                    "你可以在该目录下创建更多 .md 文件来定义代理。"
            );
        } catch (Exception e) {
            logger.error("Failed to initialize agents directory", e);
            return CommandResult.error("初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新加载代理
     */
    private CommandResult reloadAgents() {
        try {
            agentsManager.reload();
            int count = agentsManager.getAllAgents().size();
            return CommandResult.success("✅ 重新加载成功，当前有 " + count + " 个代理。");
        } catch (Exception e) {
            logger.error("Failed to reload agents", e);
            return CommandResult.error("重新加载失败: " + e.getMessage());
        }
    }
}
