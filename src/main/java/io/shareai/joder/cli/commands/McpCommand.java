package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.services.mcp.McpClient;
import io.shareai.joder.services.mcp.McpServerConfig;
import io.shareai.joder.services.mcp.McpServerManager;
import io.shareai.joder.services.mcp.McpToolRegistry;

import java.util.Map;

/**
 * MCP 服务器管理命令
 */
public class McpCommand implements Command {
    
    private final McpServerManager serverManager;
    private final McpToolRegistry toolRegistry;
    
    public McpCommand(McpServerManager serverManager, McpToolRegistry toolRegistry) {
        this.serverManager = serverManager;
        this.toolRegistry = toolRegistry;
    }
    
    @Override
    public CommandResult execute(String args) {
        String[] parts = args == null ? new String[0] : args.trim().split("\\s+", 2);
        
        if (parts.length == 0 || parts[0].isEmpty()) {
            return listServers();
        }
        
        String subCommand = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";
        
        return switch (subCommand) {
            case "list" -> listServers();
            case "start" -> startServer(argument);
            case "stop" -> stopServer(argument);
            case "enable" -> enableServer(argument);
            case "disable" -> disableServer(argument);
            case "tools" -> listTools(argument);
            case "reload" -> reloadServers();
            default -> CommandResult.error("未知的子命令: " + subCommand + "\n使用 /mcp 查看帮助");
        };
    }
    
    private CommandResult listServers() {
        Map<String, McpServerConfig> configs = serverManager.getConfigs();
        Map<String, McpClient> running = serverManager.getRunningClients();
        
        if (configs.isEmpty()) {
            return CommandResult.success("未配置任何 MCP 服务器");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📋 MCP 服务器列表:\n\n");
        
        for (Map.Entry<String, McpServerConfig> entry : configs.entrySet()) {
            String name = entry.getKey();
            McpServerConfig config = entry.getValue();
            boolean isRunning = running.containsKey(name) && running.get(name).isRunning();
            
            sb.append(String.format("  %s %s %s\n",
                isRunning ? "🟢" : "⚪",
                name,
                config.isEnabled() ? "" : "(已禁用)"));
            sb.append(String.format("    命令: %s\n", config.getCommand()));
            if (isRunning) {
                sb.append("    状态: 运行中\n");
            }
        }
        
        sb.append("\n💡 可用命令:\n");
        sb.append("  /mcp list           - 列出所有服务器\n");
        sb.append("  /mcp start <name>   - 启动服务器\n");
        sb.append("  /mcp stop <name>    - 停止服务器\n");
        sb.append("  /mcp enable <name>  - 启用服务器\n");
        sb.append("  /mcp disable <name> - 禁用服务器\n");
        sb.append("  /mcp tools [name]   - 列出工具\n");
        sb.append("  /mcp reload         - 重新加载配置\n");
        
        return CommandResult.success(sb.toString());
    }
    
    private CommandResult startServer(String serverName) {
        if (serverName.isEmpty()) {
            return CommandResult.error("请指定服务器名称");
        }
        
        try {
            serverManager.startServer(serverName);
            toolRegistry.discoverToolsFromServer(serverName);
            return CommandResult.success("✅ 已启动 MCP 服务器: " + serverName);
        } catch (Exception e) {
            return CommandResult.error("❌ 启动失败: " + e.getMessage());
        }
    }
    
    private CommandResult stopServer(String serverName) {
        if (serverName.isEmpty()) {
            return CommandResult.error("请指定服务器名称");
        }
        
        try {
            serverManager.stopServer(serverName);
            return CommandResult.success("✅ 已停止 MCP 服务器: " + serverName);
        } catch (Exception e) {
            return CommandResult.error("❌ 停止失败: " + e.getMessage());
        }
    }
    
    private CommandResult enableServer(String serverName) {
        if (serverName.isEmpty()) {
            return CommandResult.error("请指定服务器名称");
        }
        
        try {
            serverManager.enableServer(serverName);
            return CommandResult.success("✅ 已启用 MCP 服务器: " + serverName);
        } catch (Exception e) {
            return CommandResult.error("❌ 启用失败: " + e.getMessage());
        }
    }
    
    private CommandResult disableServer(String serverName) {
        if (serverName.isEmpty()) {
            return CommandResult.error("请指定服务器名称");
        }
        
        try {
            serverManager.disableServer(serverName);
            return CommandResult.success("✅ 已禁用 MCP 服务器: " + serverName);
        } catch (Exception e) {
            return CommandResult.error("❌ 禁用失败: " + e.getMessage());
        }
    }
    
    private CommandResult listTools(String serverName) {
        Map<String, McpToolRegistry.McpToolInfo> tools;
        
        if (serverName.isEmpty()) {
            tools = toolRegistry.getAllTools();
        } else {
            tools = Map.of();
            for (McpToolRegistry.McpToolInfo toolInfo : toolRegistry.getToolsFromServer(serverName)) {
                tools.put(toolInfo.getFullName(), toolInfo);
            }
        }
        
        if (tools.isEmpty()) {
            String message = serverName.isEmpty() 
                ? "未发现任何工具。请先启动 MCP 服务器。"
                : "服务器 " + serverName + " 未提供任何工具";
            return CommandResult.success(message);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔧 MCP 工具列表:\n\n");
        
        for (Map.Entry<String, McpToolRegistry.McpToolInfo> entry : tools.entrySet()) {
            McpToolRegistry.McpToolInfo toolInfo = entry.getValue();
            sb.append(String.format("  • %s\n", toolInfo.getFullName()));
            sb.append(String.format("    %s\n", toolInfo.getTool().getDescription()));
        }
        
        return CommandResult.success(sb.toString());
    }
    
    private CommandResult reloadServers() {
        try {
            serverManager.reloadConfigs();
            toolRegistry.clear();
            return CommandResult.success("✅ 已重新加载 MCP 配置");
        } catch (Exception e) {
            return CommandResult.error("❌ 重新加载失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getDescription() {
        return "管理 MCP (Model Context Protocol) 服务器";
    }
    
    @Override
    public String getUsage() {
        return "/mcp [list|start|stop|enable|disable|tools|reload] [服务器名]";
    }
}
