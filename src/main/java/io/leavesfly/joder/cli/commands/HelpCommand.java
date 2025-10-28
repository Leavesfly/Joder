package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandParser;
import io.leavesfly.joder.cli.CommandResult;

import javax.inject.Inject;
import java.util.Map;

/**
 * 帮助命令
 */
public class HelpCommand implements Command {
    
    private final CommandParser commandParser;
    
    @Inject
    public HelpCommand(CommandParser commandParser) {
        this.commandParser = commandParser;
    }
    
    @Override
    public CommandResult execute(String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Joder 可用命令 ===\n\n");
        
        Map<String, Command> commands = commandParser.getCommands();
        
        // 列出所有命令
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            String name = entry.getKey();
            Command cmd = entry.getValue();
            sb.append(String.format("  /%s - %s\n", name, cmd.getDescription()));
            sb.append(String.format("    用法: %s\n\n", cmd.getUsage()));
        }
        
        sb.append("提示：\n");
        sb.append("  - 直接输入消息与 AI 对话\n");
        sb.append("  - 使用 / 开头的命令执行系统功能\n");
        sb.append("  - 使用 Ctrl+C 或 /exit 退出应用\n");
        sb.append("\n");
        
        return CommandResult.success(sb.toString());
    }
    
    @Override
    public String getDescription() {
        return "显示帮助信息";
    }
    
    @Override
    public String getUsage() {
        return "/help";
    }
}
