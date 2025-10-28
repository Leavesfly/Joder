package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.core.MainLoop;

import javax.inject.Inject;

/**
 * /undo 命令 - 撤销最后一轮对话
 */
public class UndoCommand implements Command {
    
    private final MainLoop mainLoop;
    
    @Inject
    public UndoCommand(MainLoop mainLoop) {
        this.mainLoop = mainLoop;
    }
    
    @Override
    public String getDescription() {
        return "撤销最后一轮对话(移除用户消息和AI响应)";
    }
    
    @Override
    public String getUsage() {
        return "undo [无参数]";
    }
    
    @Override
    public CommandResult execute(String args) {
        if (mainLoop.getHistorySize() == 0) {
            return CommandResult.error("没有可撤销的对话历史");
        }
        
        boolean success = mainLoop.undoLastInteraction();
        
        if (success) {
            return CommandResult.success(String.format(
                "✓ 已撤销最后一轮对话\n当前对话历史: %d 条消息",
                mainLoop.getHistorySize()
            ));
        } else {
            return CommandResult.error("撤销失败: 对话历史格式不正确");
        }
    }
}
