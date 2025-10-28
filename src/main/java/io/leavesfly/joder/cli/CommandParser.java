package io.leavesfly.joder.cli;

import java.util.HashMap;
import java.util.Map;

/**
 * 命令解析器
 */
public class CommandParser {
    
    private final Map<String, Command> commands;
    
    public CommandParser() {
        this.commands = new HashMap<>();
    }
    
    /**
     * 注册命令
     */
    public void registerCommand(String name, Command command) {
        commands.put(name, command);
    }
    
    /**
     * 解析并执行命令
     */
    public CommandResult parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return CommandResult.empty();
        }
        
        String trimmed = input.trim();
        
        // 检查是否是命令（以 / 开头）
        if (!trimmed.startsWith("/")) {
            return CommandResult.userMessage(trimmed);
        }
        
        // 解析命令
        String[] parts = trimmed.substring(1).split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        // 查找命令
        Command command = commands.get(commandName);
        if (command == null) {
            return CommandResult.error("未知命令: /" + commandName + "\n使用 /help 查看可用命令");
        }
        
        // 执行命令
        try {
            return command.execute(args);
        } catch (Exception e) {
            return CommandResult.error("命令执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有注册的命令
     */
    public Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }
}
