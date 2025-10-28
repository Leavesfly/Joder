package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;

/**
 * 退出命令
 */
public class ExitCommand implements Command {
    
    @Override
    public CommandResult execute(String args) {
        return CommandResult.exit();
    }
    
    @Override
    public String getDescription() {
        return "退出 Joder";
    }
    
    @Override
    public String getUsage() {
        return "/exit";
    }
}
