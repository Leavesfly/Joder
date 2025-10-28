package io.shareai.joder.ui.renderer;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.ChangeDelta;
import com.github.difflib.patch.DeleteDelta;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件 Diff 可视化渲染器
 * 
 * <p>支持显示文件变更对比，使用 ANSI 颜色高亮显示添加、删除和修改的行。
 * 
 * <p>支持两种显示模式：
 * <ul>
 *   <li>统一视图（Unified）：类似 git diff 的输出</li>
 *   <li>并排视图（Side-by-side）：左右对比显示</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class DiffRenderer {
    
    private static final Logger logger = LoggerFactory.getLogger(DiffRenderer.class);
    
    /** 上下文行数（显示变更前后的行数） */
    private int contextLines = 3;
    
    /** 是否显示行号 */
    private boolean showLineNumbers = true;
    
    /** 是否启用颜色 */
    private boolean enableColors = true;
    
    /** 颜色方案 */
    private final ColorScheme colorScheme;
    
    /**
     * Diff 颜色方案
     */
    public static class ColorScheme {
        public final Ansi.Color added;
        public final Ansi.Color deleted;
        public final Ansi.Color modified;
        public final Ansi.Color context;
        public final Ansi.Color lineNumber;
        public final Ansi.Color header;
        
        public ColorScheme(Ansi.Color added, Ansi.Color deleted, Ansi.Color modified,
                          Ansi.Color context, Ansi.Color lineNumber, Ansi.Color header) {
            this.added = added;
            this.deleted = deleted;
            this.modified = modified;
            this.context = context;
            this.lineNumber = lineNumber;
            this.header = header;
        }
        
        /** 默认颜色方案 */
        public static ColorScheme defaultScheme() {
            return new ColorScheme(
                Ansi.Color.GREEN,   // added
                Ansi.Color.RED,     // deleted
                Ansi.Color.YELLOW,  // modified
                Ansi.Color.WHITE,   // context
                Ansi.Color.CYAN,    // line number
                Ansi.Color.BLUE     // header
            );
        }
    }
    
    public DiffRenderer() {
        this(ColorScheme.defaultScheme());
    }
    
    public DiffRenderer(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }
    
    /**
     * 渲染两个文件的 diff
     * 
     * @param oldFile 原文件路径
     * @param newFile 新文件路径
     * @return diff 输出
     * @throws IOException 文件读取错误
     */
    public String renderFileDiff(Path oldFile, Path newFile) throws IOException {
        List<String> oldLines = Files.readAllLines(oldFile);
        List<String> newLines = Files.readAllLines(newFile);
        
        return renderDiff(
            oldLines,
            newLines,
            oldFile.getFileName().toString(),
            newFile.getFileName().toString()
        );
    }
    
    /**
     * 渲染两个字符串的 diff
     * 
     * @param oldContent 原内容
     * @param newContent 新内容
     * @return diff 输出
     */
    public String renderStringDiff(String oldContent, String newContent) {
        List<String> oldLines = Arrays.asList(oldContent.split("\n"));
        List<String> newLines = Arrays.asList(newContent.split("\n"));
        
        return renderDiff(oldLines, newLines, "original", "modified");
    }
    
    /**
     * 渲染 diff（核心方法）
     * 
     * @param oldLines 原内容行列表
     * @param newLines 新内容行列表
     * @param oldLabel 原文件标签
     * @param newLabel 新文件标签
     * @return diff 输出
     */
    public String renderDiff(List<String> oldLines, List<String> newLines,
                            String oldLabel, String newLabel) {
        try {
            // 生成 diff patch
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            
            if (patch.getDeltas().isEmpty()) {
                return colored("No changes detected", colorScheme.context);
            }
            
            StringBuilder output = new StringBuilder();
            
            // 文件头
            output.append(renderHeader(oldLabel, newLabel));
            output.append("\n");
            
            // 渲染每个变更块
            for (AbstractDelta<String> delta : patch.getDeltas()) {
                output.append(renderDelta(delta, oldLines, newLines));
                output.append("\n");
            }
            
            return output.toString();
            
        } catch (Exception e) {
            logger.error("Failed to render diff", e);
            return "Error rendering diff: " + e.getMessage();
        }
    }
    
    /**
     * 渲染文件头
     */
    private String renderHeader(String oldLabel, String newLabel) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(colored("--- " + oldLabel, colorScheme.header)).append("\n");
        sb.append(colored("+++ " + newLabel, colorScheme.header));
        
        return sb.toString();
    }
    
    /**
     * 渲染单个变更块
     */
    private String renderDelta(AbstractDelta<String> delta, 
                              List<String> oldLines, 
                              List<String> newLines) {
        StringBuilder sb = new StringBuilder();
        
        int oldStart = delta.getSource().getPosition();
        int oldSize = delta.getSource().size();
        int newStart = delta.getTarget().getPosition();
        int newSize = delta.getTarget().size();
        
        // 变更块头（类似 @@ -1,3 +1,4 @@）
        String chunkHeader = String.format("@@ -%d,%d +%d,%d @@",
            oldStart + 1, oldSize, newStart + 1, newSize);
        sb.append(colored(chunkHeader, colorScheme.header)).append("\n");
        
        // 添加上下文（变更前）
        int contextStart = Math.max(0, oldStart - contextLines);
        for (int i = contextStart; i < oldStart; i++) {
            sb.append(renderLine(" ", i + 1, oldLines.get(i), colorScheme.context));
            sb.append("\n");
        }
        
        // 渲染变更内容
        if (delta instanceof DeleteDelta) {
            // 删除
            for (String line : delta.getSource().getLines()) {
                sb.append(renderLine("-", oldStart + 1, line, colorScheme.deleted));
                sb.append("\n");
                oldStart++;
            }
        } else if (delta instanceof InsertDelta) {
            // 添加
            for (String line : delta.getTarget().getLines()) {
                sb.append(renderLine("+", newStart + 1, line, colorScheme.added));
                sb.append("\n");
                newStart++;
            }
        } else if (delta instanceof ChangeDelta) {
            // 修改（删除 + 添加）
            for (String line : delta.getSource().getLines()) {
                sb.append(renderLine("-", oldStart + 1, line, colorScheme.deleted));
                sb.append("\n");
                oldStart++;
            }
            for (String line : delta.getTarget().getLines()) {
                sb.append(renderLine("+", newStart + 1, line, colorScheme.added));
                sb.append("\n");
                newStart++;
            }
        }
        
        // 添加上下文（变更后）
        int sourceEnd = delta.getSource().getPosition() + delta.getSource().size();
        int contextEnd = Math.min(oldLines.size(), sourceEnd + contextLines);
        for (int i = sourceEnd; i < contextEnd; i++) {
            sb.append(renderLine(" ", i + 1, oldLines.get(i), colorScheme.context));
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 渲染单行
     */
    private String renderLine(String prefix, int lineNum, String content, Ansi.Color color) {
        StringBuilder sb = new StringBuilder();
        
        // 行号
        if (showLineNumbers) {
            String lineNumStr = String.format("%4d", lineNum);
            sb.append(colored(lineNumStr, colorScheme.lineNumber));
            sb.append(" ");
        }
        
        // 前缀和内容
        sb.append(colored(prefix, color));
        sb.append(" ");
        sb.append(colored(content, color));
        
        return sb.toString();
    }
    
    /**
     * 渲染简洁的 diff 摘要
     * 
     * @param oldContent 原内容
     * @param newContent 新内容
     * @return diff 摘要
     */
    public String renderDiffSummary(String oldContent, String newContent) {
        List<String> oldLines = Arrays.asList(oldContent.split("\n"));
        List<String> newLines = Arrays.asList(newContent.split("\n"));
        
        try {
            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            
            if (patch.getDeltas().isEmpty()) {
                return colored("✓ No changes", Ansi.Color.GREEN);
            }
            
            int additions = 0;
            int deletions = 0;
            
            for (AbstractDelta<String> delta : patch.getDeltas()) {
                if (delta instanceof DeleteDelta) {
                    deletions += delta.getSource().size();
                } else if (delta instanceof InsertDelta) {
                    additions += delta.getTarget().size();
                } else if (delta instanceof ChangeDelta) {
                    deletions += delta.getSource().size();
                    additions += delta.getTarget().size();
                }
            }
            
            return colored("+" + additions, Ansi.Color.GREEN) + " " +
                   colored("-" + deletions, Ansi.Color.RED) + " " +
                   "(" + patch.getDeltas().size() + " changes)";
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * 并排渲染 diff（实验性）
     * 
     * @param oldContent 原内容
     * @param newContent 新内容
     * @param width 总宽度
     * @return 并排 diff 输出
     */
    public String renderSideBySide(String oldContent, String newContent, int width) {
        List<String> oldLines = Arrays.asList(oldContent.split("\n"));
        List<String> newLines = Arrays.asList(newContent.split("\n"));
        
        int halfWidth = (width - 3) / 2; // 留出中间分隔符的空间
        StringBuilder output = new StringBuilder();
        
        // 头部
        output.append(padRight("ORIGINAL", halfWidth)).append(" | ");
        output.append(padRight("MODIFIED", halfWidth)).append("\n");
        output.append("-".repeat(width)).append("\n");
        
        // 内容（简单对齐）
        int maxLines = Math.max(oldLines.size(), newLines.size());
        for (int i = 0; i < maxLines; i++) {
            String oldLine = i < oldLines.size() ? oldLines.get(i) : "";
            String newLine = i < newLines.size() ? newLines.get(i) : "";
            
            String oldPart = truncate(oldLine, halfWidth);
            String newPart = truncate(newLine, halfWidth);
            
            Ansi.Color color = oldLine.equals(newLine) ? colorScheme.context : colorScheme.modified;
            
            output.append(colored(padRight(oldPart, halfWidth), color));
            output.append(" | ");
            output.append(colored(padRight(newPart, halfWidth), color));
            output.append("\n");
        }
        
        return output.toString();
    }
    
    /**
     * 应用颜色
     */
    private String colored(String text, Ansi.Color color) {
        if (!enableColors) {
            return text;
        }
        return Ansi.ansi().fg(color).a(text).reset().toString();
    }
    
    /**
     * 右侧填充
     */
    private String padRight(String text, int length) {
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }
    
    /**
     * 截断字符串
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    // Getters and Setters
    
    public void setContextLines(int contextLines) {
        this.contextLines = contextLines;
    }
    
    public void setShowLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
    }
    
    public void setEnableColors(boolean enableColors) {
        this.enableColors = enableColors;
    }
    
    public int getContextLines() {
        return contextLines;
    }
    
    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }
    
    public boolean isEnableColors() {
        return enableColors;
    }
}
