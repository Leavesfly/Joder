package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.core.InteractionMode;
import io.leavesfly.joder.core.MainLoop;

import javax.inject.Inject;

/**
 * /mode 命令 - 切换交互模式
 */
public class ModeCommand implements Command {
    
    private final MainLoop mainLoop;
    
    @Inject
    public ModeCommand(MainLoop mainLoop) {
        this.mainLoop = mainLoop;
    }
    
    @Override
    public String getDescription() {
        return "切换交互模式 (default/auto/plan)";
    }
    
    @Override
    public String getUsage() {
        return "mode [模式名称]";
    }
    
    @Override
    public CommandResult execute(String args) {
        args = args.trim();
        
        // 无参数 - 显示当前模式
        if (args.isEmpty()) {
            return showCurrentMode();
        }
        
        // 特殊命令 - 循环切换
        if (args.equalsIgnoreCase("next") || args.equalsIgnoreCase("toggle")) {
            return toggleMode();
        }
        
        // 设置指定模式
        try {
            InteractionMode mode = InteractionMode.fromKey(args);
            mainLoop.setInteractionMode(mode);
            return CommandResult.success(formatModeInfo(mode, true));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("未知的交互模式: " + args + "\n" + 
                                     "可用模式: default, auto, plan");
        }
    }
    
    /**
     * 显示当前模式
     */
    private CommandResult showCurrentMode() {
        InteractionMode current = mainLoop.getInteractionMode();
        StringBuilder output = new StringBuilder();
        output.append("🎯 当前交互模式\n\n");
        output.append(formatModeInfo(current, false));
        output.append("\n可用模式:\n");
        
        for (InteractionMode mode : InteractionMode.values()) {
            String marker = (mode == current) ? "✓ " : "  ";
            output.append(marker)
                  .append(mode.getKey())
                  .append(" - ")
                  .append(mode.getDisplayName())
                  .append("\n");
        }
        
        output.append("\n提示: 使用 /mode <模式名称> 切换模式");
        output.append("\n      使用 Shift+Tab 快速切换");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 循环切换模式
     */
    private CommandResult toggleMode() {
        InteractionMode newMode = mainLoop.switchToNextMode();
        return CommandResult.success(formatModeInfo(newMode, true));
    }
    
    /**
     * 格式化模式信息
     */
    private String formatModeInfo(InteractionMode mode, boolean isChange) {
        StringBuilder output = new StringBuilder();
        
        if (isChange) {
            output.append("✓ 已切换到: ");
        }
        
        output.append("**").append(mode.getDisplayName()).append("**\n\n");
        output.append("📝 说明: ").append(mode.getDescription()).append("\n\n");
        
        // 模式特性说明
        switch (mode) {
            case DEFAULT -> {
                output.append("特性:\n");
                output.append("  • AI 提出建议后等待确认\n");
                output.append("  • 所有写操作需要用户批准\n");
                output.append("  • 适合日常开发和学习\n");
            }
            case AUTO -> {
                output.append("特性:\n");
                output.append("  • AI 自主执行大部分操作\n");
                output.append("  • 危险操作仍需确认\n");
                output.append("  • 适合信任的批量任务\n");
                output.append("  ⚠️  请谨慎使用,确保了解AI的操作\n");
            }
            case PLAN -> {
                output.append("特性:\n");
                output.append("  • AI 只分析和规划,不执行写操作\n");
                output.append("  • 使用扩展思考能力\n");
                output.append("  • 适合架构设计和问题分析\n");
                output.append("  • 生成详细的执行计划和任务分解\n");
            }
        }
        
        return output.toString();
    }
}
