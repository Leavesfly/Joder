package io.leavesfly.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Diff 工具类
 * 对应 Kode 的 diff.ts
 * 
 * 简化实现，不依赖外部库
 * TODO: 可以后续添加 java-diff-utils 依赖以获得更完整的功能
 */
public class DiffUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(DiffUtils.class);
    private static final int DEFAULT_CONTEXT_LINES = 3;
    
    /**
     * Diff 块（Hunk）
     */
    public static class DiffHunk {
        private final int oldStart;
        private final int oldLines;
        private final int newStart;
        private final int newLines;
        private final List<String> lines;
        
        public DiffHunk(int oldStart, int oldLines, int newStart, int newLines, List<String> lines) {
            this.oldStart = oldStart;
            this.oldLines = oldLines;
            this.newStart = newStart;
            this.newLines = newLines;
            this.lines = lines;
        }
        
        public int getOldStart() {
            return oldStart;
        }
        
        public int getOldLines() {
            return oldLines;
        }
        
        public int getNewStart() {
            return newStart;
        }
        
        public int getNewLines() {
            return newLines;
        }
        
        public List<String> getLines() {
            return lines;
        }
        
        @Override
        public String toString() {
            return String.format("@@ -%d,%d +%d,%d @@\n%s",
                oldStart, oldLines, newStart, newLines, String.join("\n", lines));
        }
    }
    
    /**
     * 获取补丁（Patch）
     * 
     * 简化实现：只返回修改的位置和内容
     * 
     * @param filePath 文件路径
     * @param fileContents 原始文件内容
     * @param oldStr 要替换的旧字符串
     * @param newStr 新字符串
     * @return Diff 块列表
     */
    public static List<DiffHunk> getPatch(String filePath, String fileContents, 
                                          String oldStr, String newStr) {
        return getPatch(filePath, fileContents, oldStr, newStr, DEFAULT_CONTEXT_LINES);
    }
    
    /**
     * 获取补丁（Patch）
     * 
     * 简化实现：直接查找替换位置
     * 
     * @param filePath 文件路径
     * @param fileContents 原始文件内容
     * @param oldStr 要替换的旧字符串
     * @param newStr 新字符串
     * @param contextLines 上下文行数
     * @return Diff 块列表
     */
    public static List<DiffHunk> getPatch(String filePath, String fileContents,
                                          String oldStr, String newStr, int contextLines) {
        List<DiffHunk> hunks = new ArrayList<>();
        
        try {
            // 查找 oldStr 在文件中的位置
            int index = fileContents.indexOf(oldStr);
            if (index == -1) {
                return hunks; // 未找到，返回空列表
            }
            
            // 计算行号
            String beforeChange = fileContents.substring(0, index);
            int lineNumber = beforeChange.split("\n", -1).length;
            
            // 分割成行
            List<String> originalLines = Arrays.asList(fileContents.split("\n", -1));
            List<String> oldStrLines = Arrays.asList(oldStr.split("\n", -1));
            List<String> newStrLines = Arrays.asList(newStr.split("\n", -1));
            
            // 构建 diff 行
            List<String> diffLines = new ArrayList<>();
            
            // 添加上下文（之前的行）
            int contextStart = Math.max(0, lineNumber - 1 - contextLines);
            for (int i = contextStart; i < lineNumber - 1; i++) {
                diffLines.add(" " + originalLines.get(i));
            }
            
            // 添加删除的行
            for (String line : oldStrLines) {
                diffLines.add("-" + line);
            }
            
            // 添加新增的行
            for (String line : newStrLines) {
                diffLines.add("+" + line);
            }
            
            // 添加上下文（之后的行）
            int contextEnd = Math.min(originalLines.size(), lineNumber + oldStrLines.size() - 1 + contextLines);
            for (int i = lineNumber + oldStrLines.size() - 1; i < contextEnd; i++) {
                if (i < originalLines.size()) {
                    diffLines.add(" " + originalLines.get(i));
                }
            }
            
            hunks.add(new DiffHunk(
                lineNumber,
                oldStrLines.size(),
                lineNumber,
                newStrLines.size(),
                diffLines
            ));
            
        } catch (Exception e) {
            logger.error("生成补丁失败", e);
        }
        
        return hunks;
    }
    
    /**
     * 生成统一格式的 diff 字符串
     * 
     * @param filePath 文件路径
     * @param fileContents 原始文件内容
     * @param oldStr 要替换的旧字符串
     * @param newStr 新字符串
     * @return 统一格式的 diff 字符串
     */
    public static String getUnifiedDiff(String filePath, String fileContents,
                                       String oldStr, String newStr) {
        List<DiffHunk> hunks = getPatch(filePath, fileContents, oldStr, newStr);
        
        if (hunks.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(filePath).append("\n");
        sb.append("+++ ").append(filePath).append("\n");
        
        for (DiffHunk hunk : hunks) {
            sb.append(hunk.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 比较两个文件内容
     * 
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 是否相同
     */
    public static boolean contentEquals(String oldContent, String newContent) {
        if (oldContent == null && newContent == null) {
            return true;
        }
        if (oldContent == null || newContent == null) {
            return false;
        }
        return oldContent.equals(newContent);
    }
    
    /**
     * 计算两个文本的相似度（0-1之间）
     * 
     * 简化算法：基于编辑距离
     * 
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度（0-1）
     */
    public static double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        if (text1.equals(text2)) {
            return 1.0;
        }
        
        // 使用 Levenshtein 距离计算相似度
        int maxLen = Math.max(text1.length(), text2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(text1, text2);
        return 1.0 - ((double) distance / maxLen);
    }
    
    /**
     * 计算 Levenshtein 距离（编辑距离）
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
}
