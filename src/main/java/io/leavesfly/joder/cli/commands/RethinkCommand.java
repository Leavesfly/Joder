package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.core.MainLoop;

import javax.inject.Inject;

/**
 * /rethink 命令 - 让 AI 重新思考当前方案
 * <p>
 * 在不修改对话历史的情况下,要求 AI 重新审视和改进其方案
 * </p>
 */
public class RethinkCommand implements Command {
    
    private final MainLoop mainLoop;
    
    @Inject
    public RethinkCommand(MainLoop mainLoop) {
        this.mainLoop = mainLoop;
    }
    
    @Override
    public String getDescription() {
        return "要求AI重新思考当前方案,寻找更好的解决方法";
    }
    
    @Override
    public String getUsage() {
        return "rethink [可选:具体要重新考虑的方面]";
    }
    
    @Override
    public CommandResult execute(String args) {
        if (mainLoop.getHistorySize() == 0) {
            return CommandResult.error("没有对话历史可供重新思考");
        }
        
        // 构建重新思考的提示
        StringBuilder prompt = new StringBuilder();
        prompt.append("请重新审视你刚才的方案,思考以下方面:\n");
        prompt.append("1. 是否有更简单的实现方式?\n");
        prompt.append("2. 是否考虑了所有边界情况?\n");
        prompt.append("3. 是否有潜在的性能或安全问题?\n");
        prompt.append("4. 是否符合最佳实践?\n");
        
        if (!args.trim().isEmpty()) {
            prompt.append("\n特别关注: ").append(args.trim()).append("\n");
        }
        
        prompt.append("\n如果发现可以改进的地方,请提出新的方案。");
        
        // 将提示作为用户消息添加到历史
        // 这里实际上会触发新一轮对话
        // 在实际使用中,ReplScreen 会捕获这个命令并特殊处理
        
        return CommandResult.userMessage(prompt.toString());
    }
}
