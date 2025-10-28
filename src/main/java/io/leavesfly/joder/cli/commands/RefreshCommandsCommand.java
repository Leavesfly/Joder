package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.services.commands.CustomCommandService;

import javax.inject.Inject;

/**
 * /refresh-commands 命令 - 刷新自定义命令缓存
 * 
 * <p>强制重新扫描自定义命令目录，重新加载所有命令。
 * 用于在添加、修改或删除自定义命令后，无需重启应用即可生效。
 * 
 * <p>使用示例：
 * <pre>
 * /refresh-commands
 * </pre>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class RefreshCommandsCommand implements Command {
    
    private final CustomCommandService customCommandService;
    
    @Inject
    public RefreshCommandsCommand(CustomCommandService customCommandService) {
        this.customCommandService = customCommandService;
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            // 刷新命令缓存
            customCommandService.refreshCommands();
            
            // 统计命令数量
            int totalCommands = customCommandService.getAllCommands().size();
            int userCommands = customCommandService.getUserCommands().size();
            int projectCommands = customCommandService.getProjectCommands().size();
            
            StringBuilder output = new StringBuilder();
            output.append("✅ 自定义命令已刷新\n\n");
            output.append(String.format("总计: %d 个命令\n", totalCommands));
            output.append(String.format("  - 用户级: %d 个\n", userCommands));
            output.append(String.format("  - 项目级: %d 个\n", projectCommands));
            
            if (totalCommands > 0) {
                output.append("\n可用命令:\n");
                customCommandService.getVisibleCommands().forEach(cmd -> {
                    output.append(String.format("  %s - %s\n", 
                            cmd.getName(), 
                            cmd.getDescription()));
                });
            }
            
            return CommandResult.success(output.toString());
        } catch (Exception e) {
            return CommandResult.error("刷新失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getDescription() {
        return "刷新自定义命令缓存";
    }
    
    @Override
    public String getUsage() {
        return "/refresh-commands";
    }
}
