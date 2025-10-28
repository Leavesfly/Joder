package io.shareai.joder.tools.edit;

import java.util.ArrayList;
import java.util.List;

/**
 * Diff生成器 - 生成文件编辑的差异预览
 * 使用简化的diff算法,提供清晰的变更预览
 */
public class DiffGenerator {
    
    private static final int CONTEXT_LINES = 3; // 上下文行数
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    
    /**
     * 生成unified diff格式的预览
     * 
     * @param originalContent 原始文件内容
     * @param oldString 要替换的字符串
     * @param newString 替换后的字符串
     * @param filePath 文件路径(用于显示)
     * @return diff预览字符串
     */
    public String generateDiff(String originalContent, String oldString, 
                               String newString, String filePath) {
        // 计算新内容
        String newContent = originalContent.replace(oldString, newString);
        
        // 分割为行
        String[] oldLines = originalContent.split("\r?\n", -1);
        String[] newLines = newContent.split("\r?\n", -1);
        
        // 查找变更位置
        int changeStartLine = findChangeStartLine(oldLines, newLines);
        if (changeStartLine == -1) {
            return "无变更";
        }
        
        int changeEndLineOld = findChangeEndLine(oldLines, newLines, changeStartLine);
        int changeEndLineNew = findChangeEndLineNew(oldLines, newLines, changeStartLine);
        
        // 计算上下文范围
        int contextStart = Math.max(0, changeStartLine - CONTEXT_LINES);
        int contextEndOld = Math.min(oldLines.length, changeEndLineOld + CONTEXT_LINES);
        int contextEndNew = Math.min(newLines.length, changeEndLineNew + CONTEXT_LINES);
        
        // 生成diff
        StringBuilder diff = new StringBuilder();
        
        // Diff头部
        diff.append(ANSI_CYAN).append("--- ").append(filePath).append("\n");
        diff.append("+++ ").append(filePath).append(ANSI_RESET).append("\n");
        
        // Hunk头部
        int oldHunkSize = contextEndOld - contextStart;
        int newHunkSize = contextEndNew - contextStart;
        diff.append(ANSI_CYAN)
            .append("@@ -").append(contextStart + 1).append(",").append(oldHunkSize)
            .append(" +").append(contextStart + 1).append(",").append(newHunkSize)
            .append(" @@").append(ANSI_RESET).append("\n");
        
        // 输出上下文和变更
        for (int i = contextStart; i < changeStartLine; i++) {
            diff.append(" ").append(oldLines[i]).append("\n");
        }
        
        // 输出删除的行
        for (int i = changeStartLine; i < changeEndLineOld; i++) {
            diff.append(ANSI_RED).append("-").append(oldLines[i])
                .append(ANSI_RESET).append("\n");
        }
        
        // 输出添加的行
        for (int i = changeStartLine; i < changeEndLineNew; i++) {
            diff.append(ANSI_GREEN).append("+").append(newLines[i])
                .append(ANSI_RESET).append("\n");
        }
        
        // 输出后续上下文
        int afterContextStart = Math.max(changeEndLineOld, changeEndLineNew);
        int afterContextEnd = Math.min(oldLines.length, 
                                       Math.max(contextEndOld, contextEndNew));
        for (int i = afterContextStart; i < afterContextEnd && i < oldLines.length; i++) {
            diff.append(" ").append(oldLines[i]).append("\n");
        }
        
        return diff.toString();
    }
    
    /**
     * 生成简化的并列对比格式
     * 
     * @param originalContent 原始内容
     * @param oldString 要替换的字符串
     * @param newString 替换后的字符串
     * @return 并列对比字符串
     */
    public String generateSideBySideDiff(String originalContent, 
                                         String oldString, 
                                         String newString) {
        String[] oldLines = oldString.split("\r?\n", -1);
        String[] newLines = newString.split("\r?\n", -1);
        
        StringBuilder diff = new StringBuilder();
        diff.append("\n").append(ANSI_CYAN).append("变更预览:").append(ANSI_RESET).append("\n");
        diff.append("─".repeat(60)).append("\n");
        
        int maxLines = Math.max(oldLines.length, newLines.length);
        
        for (int i = 0; i < maxLines; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : "";
            String newLine = i < newLines.length ? newLines[i] : "";
            
            if (!oldLine.equals(newLine)) {
                if (!oldLine.isEmpty()) {
                    diff.append(ANSI_RED).append("- ").append(oldLine)
                        .append(ANSI_RESET).append("\n");
                }
                if (!newLine.isEmpty()) {
                    diff.append(ANSI_GREEN).append("+ ").append(newLine)
                        .append(ANSI_RESET).append("\n");
                }
            }
        }
        
        diff.append("─".repeat(60)).append("\n");
        
        return diff.toString();
    }
    
    /**
     * 生成统计信息
     * 
     * @param oldString 原始字符串
     * @param newString 新字符串
     * @return 统计信息字符串
     */
    public String generateStats(String oldString, String newString) {
        int oldLines = oldString.isEmpty() ? 0 : oldString.split("\r?\n", -1).length;
        int newLines = newString.isEmpty() ? 0 : newString.split("\r?\n", -1).length;
        int added = newLines;
        int removed = oldLines;
        
        StringBuilder stats = new StringBuilder();
        stats.append(ANSI_CYAN).append("变更统计: ").append(ANSI_RESET);
        
        if (removed > 0) {
            stats.append(ANSI_RED).append("-").append(removed).append(" 行 ")
                .append(ANSI_RESET);
        }
        if (added > 0) {
            stats.append(ANSI_GREEN).append("+").append(added).append(" 行")
                .append(ANSI_RESET);
        }
        
        return stats.toString();
    }
    
    /**
     * 查找变更开始行
     */
    private int findChangeStartLine(String[] oldLines, String[] newLines) {
        int minLength = Math.min(oldLines.length, newLines.length);
        
        for (int i = 0; i < minLength; i++) {
            if (!oldLines[i].equals(newLines[i])) {
                return i;
            }
        }
        
        // 如果所有行都相同,检查长度
        if (oldLines.length != newLines.length) {
            return minLength;
        }
        
        return -1; // 无变更
    }
    
    /**
     * 查找原始文件中变更结束行
     */
    private int findChangeEndLine(String[] oldLines, String[] newLines, int changeStart) {
        // 从后向前比较,找到第一个不同的位置
        int oldIdx = oldLines.length - 1;
        int newIdx = newLines.length - 1;
        
        while (oldIdx > changeStart && newIdx >= 0) {
            if (!oldLines[oldIdx].equals(newLines[newIdx])) {
                return oldIdx + 1;
            }
            oldIdx--;
            newIdx--;
        }
        
        return oldIdx + 1;
    }
    
    /**
     * 查找新文件中变更结束行
     */
    private int findChangeEndLineNew(String[] oldLines, String[] newLines, int changeStart) {
        int oldIdx = oldLines.length - 1;
        int newIdx = newLines.length - 1;
        
        while (oldIdx >= 0 && newIdx > changeStart) {
            if (!oldLines[oldIdx].equals(newLines[newIdx])) {
                return newIdx + 1;
            }
            oldIdx--;
            newIdx--;
        }
        
        return newIdx + 1;
    }
}
