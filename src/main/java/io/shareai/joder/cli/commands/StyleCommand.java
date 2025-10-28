package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.ui.style.OutputStyle;
import io.shareai.joder.ui.style.OutputStyleManager;

import javax.inject.Inject;

/**
 * Style命令 - 切换输出样式
 */
public class StyleCommand implements Command {
    
    private final OutputStyleManager outputStyleManager;
    
    @Inject
    public StyleCommand(OutputStyleManager outputStyleManager) {
        this.outputStyleManager = outputStyleManager;
    }
    
    @Override
    public String getDescription() {
        return "切换AI输出的详细程度和样式";
    }
    
    @Override
    public String getUsage() {
        return "用法: /style [concise|balanced|detailed|thinking]\n\n" +
               "不带参数时显示当前样式和可用选项\n\n" +
               "样式说明:\n" +
               "  concise  - 简洁模式,只显示核心结果\n" +
               "  balanced - 平衡模式(默认),显示重要信息\n" +
               "  detailed - 详细模式,显示完整信息和步骤\n" +
               "  thinking - 思考模式,显示AI推理过程";
    }
    
    @Override
    public CommandResult execute(String args) {
        args = args != null ? args.trim() : "";
        
        // 没有参数时显示当前样式
        if (args.isEmpty()) {
            String description = outputStyleManager.getStyleDescription();
            return CommandResult.success(description);
        }
        
        // 切换样式
        boolean success = outputStyleManager.setStyle(args);
        
        if (success) {
            OutputStyle newStyle = outputStyleManager.getCurrentStyle();
            return CommandResult.success(String.format(
                "✓ 输出样式已切换为: %s (%s)\n\n%s",
                newStyle.getKey(),
                newStyle.getDisplayName(),
                newStyle.getDescription()
            ));
        } else {
            return CommandResult.error(String.format(
                "无效的样式: %s\n\n%s",
                args,
                OutputStyle.getAllStylesDescription()
            ));
        }
    }
}
