package io.shareai.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 终端工具类
 * 对应 Kode 的 terminal.ts
 */
public class TerminalUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(TerminalUtils.class);
    
    /**
     * 设置终端标题
     * 
     * @param title 标题内容
     */
    public static void setTerminalTitle(String title) {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows: 直接设置 console title 不太可靠，这里仅做标记
            if (title != null && !title.isEmpty()) {
                System.out.printf("\u001B]0;✳ %s\u0007", title);
            }
        } else {
            // Unix/Linux/macOS: 使用 ANSI escape sequences
            if (title != null && !title.isEmpty()) {
                System.out.printf("\u001B]0;✳ %s\u0007", title);
            } else {
                System.out.print("\u001B]0;\u0007");
            }
            System.out.flush();
        }
    }
    
    /**
     * 清空终端屏幕
     */
    public static void clearTerminal() {
        try {
            // ANSI escape codes for clearing screen
            // \x1b[2J - 清除整个屏幕
            // \x1b[3J - 清除回滚缓冲区
            // \x1b[H  - 将光标移动到左上角
            System.out.print("\u001B[2J\u001B[3J\u001B[H");
            System.out.flush();
        } catch (Exception e) {
            logger.error("清空终端失败", e);
        }
    }
    
    /**
     * 获取终端宽度
     * 
     * @return 终端宽度（列数）
     */
    public static int getTerminalWidth() {
        String cols = System.getenv("COLUMNS");
        if (cols != null) {
            try {
                return Integer.parseInt(cols);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        return 80; // 默认宽度
    }
    
    /**
     * 获取终端高度
     * 
     * @return 终端高度（行数）
     */
    public static int getTerminalHeight() {
        String lines = System.getenv("LINES");
        if (lines != null) {
            try {
                return Integer.parseInt(lines);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        return 24; // 默认高度
    }
    
    /**
     * 移动光标到指定位置
     * 
     * @param row 行（从1开始）
     * @param col 列（从1开始）
     */
    public static void moveCursor(int row, int col) {
        System.out.printf("\u001B[%d;%dH", row, col);
        System.out.flush();
    }
    
    /**
     * 隐藏光标
     */
    public static void hideCursor() {
        System.out.print("\u001B[?25l");
        System.out.flush();
    }
    
    /**
     * 显示光标
     */
    public static void showCursor() {
        System.out.print("\u001B[?25h");
        System.out.flush();
    }
    
    /**
     * 保存光标位置
     */
    public static void saveCursorPosition() {
        System.out.print("\u001B7");
        System.out.flush();
    }
    
    /**
     * 恢复光标位置
     */
    public static void restoreCursorPosition() {
        System.out.print("\u001B8");
        System.out.flush();
    }
}
