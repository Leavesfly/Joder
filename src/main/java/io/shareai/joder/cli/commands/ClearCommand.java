package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;

/**
 * 清空命令
 */
public class ClearCommand implements Command {
    
    @Override
    public CommandResult execute(String args) {
        // 清屏ANSI转义序列
        System.out.print("\033[H\033[2J");
        System.out.flush();
        return CommandResult.success("屏幕已清空");
    }
    
    @Override
    public String getDescription() {
        return "清空屏幕";
    }
    
    @Override
    public String getUsage() {
        return "/clear";
    }
}
