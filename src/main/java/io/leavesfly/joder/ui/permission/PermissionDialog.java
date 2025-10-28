package io.leavesfly.joder.ui.permission;

import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.ui.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * 权限对话框
 * 提供美观的交互式权限请求 UI
 */
public class PermissionDialog {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionDialog.class);
    
    private final Theme theme;
    private final BufferedReader reader;
    
    @Inject
    public PermissionDialog(Theme theme) {
        this.theme = theme;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    /**
     * 请求工具权限
     * 
     * @param tool 要执行的工具
     * @param input 工具参数
     * @return 权限决策
     */
    public PermissionDecision requestPermission(Tool tool, Map<String, Object> input) {
        renderPermissionDialog(tool, input);
        
        try {
            System.out.print("\n选择操作 [A/D/P/R]: ");
            System.out.flush();
            
            String response = reader.readLine();
            if (response == null || response.trim().isEmpty()) {
                return PermissionDecision.DENY;
            }
            
            char choice = response.trim().toUpperCase().charAt(0);
            
            switch (choice) {
                case 'A':
                    logger.info("用户批准工具执行（一次）: {}", tool.getName());
                    return PermissionDecision.ALLOW_ONCE;
                    
                case 'P':
                    logger.info("用户永久批准工具: {}", tool.getName());
                    return PermissionDecision.ALLOW_PERMANENT;
                    
                case 'D':
                case 'R':
                    logger.info("用户拒绝工具执行: {}", tool.getName());
                    return PermissionDecision.DENY;
                    
                default:
                    logger.warn("无效的选择: {}, 默认拒绝", choice);
                    return PermissionDecision.DENY;
            }
            
        } catch (Exception e) {
            logger.error("读取用户输入失败", e);
            return PermissionDecision.DENY;
        }
    }
    
    /**
     * 渲染权限对话框
     */
    private void renderPermissionDialog(Tool tool, Map<String, Object> input) {
        int width = 60;
        
        System.out.println();
        System.out.println("╭" + "─".repeat(width - 2) + "╮");
        System.out.println("│ 🔐 " + centerText("权限请求", width - 6) + " │");
        System.out.println("├" + "─".repeat(width - 2) + "┤");
        
        // 工具信息
        System.out.println("│ " + padRight("工具名称: " + tool.getName(), width - 4) + " │");
        System.out.println("│ " + padRight("功能描述: " + truncate(tool.getDescription(), 40), width - 4) + " │");
        System.out.println("│ " + padRight("只读工具: " + (tool.isReadOnly() ? "✓ 是" : "✗ 否"), width - 4) + " │");
        
        // 参数预览
        if (input != null && !input.isEmpty()) {
            System.out.println("├" + "─".repeat(width - 2) + "┤");
            System.out.println("│ " + padRight("参数预览:", width - 4) + " │");
            
            int count = 0;
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                if (count >= 3) { // 最多显示 3 个参数
                    System.out.println("│ " + padRight("  ... (" + (input.size() - 3) + " 个参数省略)", width - 4) + " │");
                    break;
                }
                
                String value = String.valueOf(entry.getValue());
                if (value.length() > 30) {
                    value = value.substring(0, 27) + "...";
                }
                
                System.out.println("│ " + padRight("  " + entry.getKey() + ": " + value, width - 4) + " │");
                count++;
            }
        }
        
        // 操作选项
        System.out.println("├" + "─".repeat(width - 2) + "┤");
        System.out.println("│ " + padRight("可用操作:", width - 4) + " │");
        System.out.println("│ " + padRight("  [A] 批准（仅本次）", width - 4) + " │");
        System.out.println("│ " + padRight("  [P] 永久批准此工具", width - 4) + " │");
        System.out.println("│ " + padRight("  [D] 拒绝执行", width - 4) + " │");
        System.out.println("│ " + padRight("  [R] 拒绝并退出", width - 4) + " │");
        System.out.println("╰" + "─".repeat(width - 2) + "╯");
    }
    
    /**
     * 文本居中
     */
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        
        int leftPadding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - leftPadding;
        
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
    
    /**
     * 右侧填充空格
     */
    private String padRight(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        
        return text + " ".repeat(width - text.length());
    }
    
    /**
     * 截断文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 权限决策
     */
    public enum PermissionDecision {
        ALLOW_ONCE,       // 批准一次
        ALLOW_PERMANENT,  // 永久批准
        DENY              // 拒绝
    }
}
