package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.core.config.ConfigManager;

import javax.inject.Inject;

/**
 * 配置命令
 */
public class ConfigCommand implements Command {
    
    private final ConfigManager configManager;
    
    @Inject
    public ConfigCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public CommandResult execute(String args) {
        if (args == null || args.trim().isEmpty()) {
            return showConfig();
        }
        
        String[] parts = args.trim().split("\\s+", 2);
        String action = parts[0].toLowerCase();
        
        return switch (action) {
            case "show", "list" -> showConfig();
            case "get" -> getConfig(parts.length > 1 ? parts[1] : "");
            case "set" -> setConfig(parts.length > 1 ? parts[1] : "");
            default -> CommandResult.error("未知操作: " + action + "\n使用 /config show 查看配置");
        };
    }
    
    private CommandResult showConfig() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Joder 配置 ===\n\n");
        sb.append("主题: ").append(configManager.getString("joder.theme", "dark")).append("\n");
        sb.append("语言: ").append(configManager.getString("joder.language", "zh-CN")).append("\n");
        sb.append("默认模型: ").append(configManager.getString("joder.model.default", "N/A")).append("\n");
        sb.append("权限模式: ").append(configManager.getString("joder.permissions.mode", "default")).append("\n");
        sb.append("\n");
        return CommandResult.success(sb.toString());
    }
    
    private CommandResult getConfig(String key) {
        if (key.isEmpty()) {
            return CommandResult.error("请指定配置键，例如: /config get joder.theme");
        }
        
        try {
            if (!configManager.hasPath(key)) {
                return CommandResult.error("配置键不存在: " + key);
            }
            String value = configManager.getString(key);
            return CommandResult.success(key + " = " + value);
        } catch (Exception e) {
            return CommandResult.error("读取配置失败: " + e.getMessage());
        }
    }
    
    private CommandResult setConfig(String keyValue) {
        if (keyValue.isEmpty()) {
            return CommandResult.error("请指定配置键值，例如: /config set joder.theme dark");
        }
        
        String[] parts = keyValue.split("\\s+", 2);
        if (parts.length < 2) {
            return CommandResult.error("格式错误，使用: /config set <key> <value>");
        }
        
        // TODO: 实现配置设置功能
        return CommandResult.error("配置设置功能尚未实现");
    }
    
    @Override
    public String getDescription() {
        return "查看和修改配置";
    }
    
    @Override
    public String getUsage() {
        return "/config [show|get <key>|set <key> <value>]";
    }
}
