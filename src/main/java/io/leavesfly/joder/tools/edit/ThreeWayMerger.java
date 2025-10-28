package io.leavesfly.joder.tools.edit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 三方合并工具
 * 尝试自动合并基础版本、当前版本和编辑版本
 */
public class ThreeWayMerger {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreeWayMerger.class);
    
    /**
     * 执行三方合并
     * 
     * @param baseContent 基础版本内容（读取时的内容）
     * @param currentContent 当前文件内容（可能已被外部修改）
     * @param oldString 要替换的字符串
     * @param newString 替换后的字符串
     * @return 合并结果
     */
    public MergeResult merge(String baseContent, String currentContent, 
                            String oldString, String newString) {
        
        logger.debug("开始三方合并");
        
        // 如果当前内容与基础版本相同，直接应用编辑
        if (baseContent.equals(currentContent)) {
            String mergedContent = baseContent.replace(oldString, newString);
            return new MergeResult(true, mergedContent, "无冲突，直接应用编辑");
        }
        
        // 检查要替换的字符串是否还在当前版本中
        if (!currentContent.contains(oldString)) {
            return new MergeResult(false, null, 
                "冲突：要替换的内容在当前版本中不存在，可能已被外部修改");
        }
        
        // 计算基础版本到当前版本的变更
        List<String> baseLines = splitLines(baseContent);
        List<String> currentLines = splitLines(currentContent);
        
        // 定位 oldString 在基础版本中的位置
        int oldStringLineStart = findStringLineIndex(baseLines, oldString);
        if (oldStringLineStart == -1) {
            return new MergeResult(false, null, "无法定位要替换的内容在基础版本中的位置");
        }
        
        // 计算 oldString 跨越的行数
        int oldStringLineCount = splitLines(oldString).size();
        int oldStringLineEnd = oldStringLineStart + oldStringLineCount;
        
        // 检查 oldString 所在的行是否被外部修改
        boolean conflictDetected = false;
        for (int i = oldStringLineStart; i < oldStringLineEnd && i < currentLines.size(); i++) {
            if (i >= baseLines.size() || !baseLines.get(i).equals(currentLines.get(i))) {
                conflictDetected = true;
                break;
            }
        }
        
        if (conflictDetected) {
            return new MergeResult(false, null, 
                "冲突：要替换的内容所在区域已被外部修改");
        }
        
        // 尝试应用编辑
        try {
            String mergedContent = currentContent.replace(oldString, newString);
            
            // 验证替换成功
            if (mergedContent.equals(currentContent)) {
                return new MergeResult(false, null, "替换失败：内容未发生变化");
            }
            
            return new MergeResult(true, mergedContent, 
                "成功合并：外部修改与当前编辑不冲突");
            
        } catch (Exception e) {
            logger.error("合并失败", e);
            return new MergeResult(false, null, "合并失败: " + e.getMessage());
        }
    }
    
    /**
     * 分割内容为行
     */
    private List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        String[] parts = content.split("\r?\n", -1);
        for (String part : parts) {
            lines.add(part);
        }
        return lines;
    }
    
    /**
     * 查找字符串在行列表中的起始行索引
     */
    private int findStringLineIndex(List<String> lines, String searchString) {
        String combinedLines = String.join("\n", lines);
        int charIndex = combinedLines.indexOf(searchString);
        
        if (charIndex == -1) {
            return -1;
        }
        
        // 计算字符索引对应的行号
        int lineIndex = 0;
        int currentCharIndex = 0;
        
        for (int i = 0; i < lines.size(); i++) {
            int lineLength = lines.get(i).length() + 1; // +1 for newline
            if (currentCharIndex + lineLength > charIndex) {
                lineIndex = i;
                break;
            }
            currentCharIndex += lineLength;
        }
        
        return lineIndex;
    }
    
    /**
     * 合并结果
     */
    public static class MergeResult {
        private final boolean success;
        private final String mergedContent;
        private final String message;
        
        public MergeResult(boolean success, String mergedContent, String message) {
            this.success = success;
            this.mergedContent = mergedContent;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMergedContent() {
            return mergedContent;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("MergeResult{success=%s, message='%s'}", 
                success, message);
        }
    }
}
