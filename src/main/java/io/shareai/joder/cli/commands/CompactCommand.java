package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * Compact Command - 压缩输出命令
 * 启用紧凑模式以节省屏幕空间
 */
@Singleton
public class CompactCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(CompactCommand.class);
    
    private static boolean compactMode = false;
    
    @Override
    public String getDescription() {
        return "切换紧凑输出模式";
    }
    
    @Override
    public String getUsage() {
        return "compact [on|off|toggle] - 控制输出模式";
    }
    
    @Override
    public CommandResult execute(String args) {
        String action = args != null ? args.trim().toLowerCase() : "toggle";
        
        return switch (action) {
            case "on" -> enableCompact();
            case "off" -> disableCompact();
            case "toggle", "" -> toggleCompact();
            case "status" -> showStatus();
            default -> CommandResult.error("未知参数: " + action);
        };
    }
    
    /**
     * 启用紧凑模式
     */
    private CommandResult enableCompact() {
        compactMode = true;
        System.setProperty("joder.compact", "true");
        
        logger.info("Compact mode enabled");
        
        return CommandResult.success(
                "✅ 紧凑模式已启用\n\n" +
                "紧凑模式特性:\n" +
                "  • 减少空行\n" +
                "  • 简化分隔符\n" +
                "  • 缩短提示信息\n" +
                "  • 隐藏调试输出\n\n" +
                "提示: 使用 /compact off 恢复正常模式"
        );
    }
    
    /**
     * 禁用紧凑模式
     */
    private CommandResult disableCompact() {
        compactMode = false;
        System.clearProperty("joder.compact");
        
        logger.info("Compact mode disabled");
        
        return CommandResult.success("✅ 已恢复正常输出模式");
    }
    
    /**
     * 切换模式
     */
    private CommandResult toggleCompact() {
        if (compactMode) {
            return disableCompact();
        } else {
            return enableCompact();
        }
    }
    
    /**
     * 显示状态
     */
    private CommandResult showStatus() {
        String status = compactMode ? "启用" : "禁用";
        String emoji = compactMode ? "✓" : "✗";
        
        return CommandResult.success(
                String.format("紧凑模式: %s %s\n\n", status, emoji) +
                "可用命令:\n" +
                "  /compact on      - 启用紧凑模式\n" +
                "  /compact off     - 禁用紧凑模式\n" +
                "  /compact toggle  - 切换模式\n" +
                "  /compact status  - 查看状态"
        );
    }
    
    /**
     * 检查是否为紧凑模式
     */
    public static boolean isCompactMode() {
        return compactMode;
    }
    
    /**
     * 获取分隔符
     */
    public static String getSeparator() {
        return compactMode ? "─────\n" : "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
    }
    
    /**
     * 获取换行
     */
    public static String getNewline() {
        return compactMode ? "\n" : "\n\n";
    }
}
