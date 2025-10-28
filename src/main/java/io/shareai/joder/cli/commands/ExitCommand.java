package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;

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
