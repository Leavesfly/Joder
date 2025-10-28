package io.leavesfly.joder.tools.edit;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * FileEdit Tool - 文件编辑工具
 * 通过搜索替换方式编辑文件内容
 */
public class FileEditTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(FileEditTool.class);
    private static final int N_LINES_SNIPPET = 4;
    
    private final String workingDirectory;
    private final Map<String, Long> readFileTimestamps = new HashMap<>();
    private final DiffGenerator diffGenerator = new DiffGenerator();
    private final ConflictDetector conflictDetector = new ConflictDetector();
    private final ThreeWayMerger threeWayMerger = new ThreeWayMerger();
    private final LargeFileOptimizer largeFileOptimizer = new LargeFileOptimizer();
    private boolean showDiff = true; // 是否显示diff预览
    private boolean enableConflictDetection = true; // 是否启用冲突检测
    private boolean enableThreeWayMerge = true; // 是否启用三方合并
    
    @Inject
    public FileEditTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public String getName() {
        return "Edit";
    }
    
    @Override
    public String getDescription() {
        return "用于编辑文件的工具。用于移动或重命名文件时，应使用 Bash 工具的 'mv' 命令。对于大规模编辑，使用 Write 工具覆盖文件。";
    }
    
    @Override
    public String getPrompt() {
        return "此工具用于编辑文件。移动或重命名文件请使用 Bash 工具的 'mv' 命令，大规模编辑请使用 Write 工具覆盖文件。\n\n" +
               "使用此工具前：\n" +
               "1. 使用 FileRead 工具了解文件的内容和上下文\n" +
               "2. 验证目录路径正确（仅在创建新文件时）- 使用 LS 工具验证父目录存在且位置正确\n\n" +
               "进行文件编辑需要提供：\n" +
               "1. file_path: 要修改的文件的绝对路径（必须是绝对路径）\n" +
               "2. old_string: 要替换的文本（必须在文件中唯一，且必须与文件内容完全匹配，包括所有空格和缩进）\n" +
               "3. new_string: 用于替换 old_string 的编辑后文本\n\n" +
               "工具将在指定文件中用 new_string 替换 old_string 的一次出现。\n\n" +
               "关键要求：\n" +
               "1. 唯一性：old_string 必须唯一标识要更改的特定实例\n" +
               "   - 在更改点之前至少包含 3-5 行上下文\n" +
               "   - 在更改点之后至少包含 3-5 行上下文\n" +
               "   - 包含所有空格、缩进和周围代码，完全按文件中的样子\n\n" +
               "2. 单实例：此工具一次只能更改一个实例\n" +
               "   - 如需更改多个实例，为每个实例单独调用此工具\n" +
               "   - 每次调用必须使用大量上下文唯一标识其特定实例\n\n" +
               "3. 验证：使用此工具前\n" +
               "   - 检查目标文本在文件中存在多少个实例\n" +
               "   - 如存在多个实例，收集足够的上下文以唯一标识每个实例\n" +
               "   - 为每个实例计划单独的工具调用\n\n" +
               "警告：\n" +
               "- 如果 old_string 匹配多个位置，工具将失败\n" +
               "- 如果 old_string 不完全匹配（包括空格），工具将失败\n" +
               "- 如果不包含足够的上下文，可能会更改错误的实例\n\n" +
               "创建新文件时：\n" +
               "- 使用新文件路径（如需要包含目录名）\n" +
               "- old_string 为空\n" +
               "- new_string 为新文件的内容";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return false; // 文件编辑修改文件，不支持并发执行
    }
    
    @Override
    public boolean needsPermissions() {
        return true; // 文件编辑需要权限检查
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String filePathStr = (String) input.get("file_path");
        String oldString = (String) input.get("old_string");
        String newString = (String) input.get("new_string");
        
        if (filePathStr == null || filePathStr.trim().isEmpty()) {
            return ToolResult.error("file_path 不能为空");
        }
        
        if (oldString == null) {
            oldString = "";
        }
        
        if (newString == null) {
            newString = "";
        }
        
        // 验证输入
        String validationError = validateInput(filePathStr, oldString, newString);
        if (validationError != null) {
            return ToolResult.error(validationError);
        }
        
        Path filePath = Paths.get(filePathStr);
        if (!filePath.isAbsolute()) {
            filePath = Paths.get(workingDirectory).resolve(filePathStr).normalize();
        }
        
        try {
            // 创建新文件
            if (!Files.exists(filePath) && oldString.isEmpty()) {
                return createNewFile(filePath, newString);
            }
            
            // 编辑现有文件
            return editExistingFile(filePath, oldString, newString);
            
        } catch (IOException e) {
            logger.error("文件编辑失败: {}", filePath, e);
            return ToolResult.error("文件编辑失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证输入参数
     */
    private String validateInput(String filePathStr, String oldString, String newString) {
        if (oldString.equals(newString)) {
            return "无需更改：old_string 和 new_string 完全相同";
        }
        
        Path filePath = Paths.get(filePathStr);
        if (!filePath.isAbsolute()) {
            filePath = Paths.get(workingDirectory).resolve(filePathStr).normalize();
        }
        
        // 检查文件是否已存在（创建新文件时）
        if (Files.exists(filePath) && oldString.isEmpty()) {
            return "无法创建新文件 - 文件已存在";
        }
        
        // 如果是新文件创建，跳过其他验证
        if (!Files.exists(filePath) && oldString.isEmpty()) {
            return null;
        }
        
        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            return "文件不存在: " + filePath;
        }
        
        // 检查是否为 Jupyter Notebook
        if (filePathStr.endsWith(".ipynb")) {
            return "文件是 Jupyter Notebook，请使用 NotebookEdit 工具编辑此文件";
        }
        
        // 检查文件是否已读取
        String absolutePath = filePath.toString();
        if (!readFileTimestamps.containsKey(absolutePath)) {
            return "文件尚未读取。写入前请先读取它。";
        }
        
        try {
            // 检查文件自读取后是否被修改
            FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            long lastWriteTime = lastModifiedTime.toMillis();
            long readTime = readFileTimestamps.get(absolutePath);
            
            if (lastWriteTime > readTime) {
                return "文件自读取后已被修改（用户或格式化工具）。请在尝试写入前重新读取它。";
            }
            
            // 读取文件内容并检查 old_string
            String content = Files.readString(filePath, detectEncoding(filePath));
            
            if (!content.contains(oldString)) {
                return "文件中未找到要替换的字符串";
            }
            
            // 计算匹配次数
            int matches = countMatches(content, oldString);
            if (matches > 1) {
                return String.format("找到 %d 处要替换的字符串匹配。为安全起见，此工具一次仅支持替换一处。请在编辑中添加更多上下文行，然后重试。", matches);
            }
            
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
        
        return null; // 验证通过
    }
    
    /**
     * 创建新文件
     */
    private ToolResult createNewFile(Path filePath, String content) throws IOException {
        // 确保父目录存在
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // 写入文件
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        
        // 更新时间戳
        readFileTimestamps.put(filePath.toString(), System.currentTimeMillis());
        
        logger.info("创建新文件: {}", filePath);
        
        return ToolResult.success(String.format("✅ 文件已创建: %s (%d 字节)", 
            filePath, content.length()));
    }
    
    /**
     * 编辑现有文件（增强版）
     */
    private ToolResult editExistingFile(Path filePath, String oldString, String newString) throws IOException {
        // 检测编码
        Charset encoding = detectEncoding(filePath);
        
        // 读取原始内容
        String originalContent = Files.readString(filePath, encoding);
        
        // 冲突检测
        if (enableConflictDetection) {
            ConflictDetector.ConflictResult conflictResult = conflictDetector.detectConflict(filePath);
            
            if (conflictResult.hasConflict()) {
                logger.warn("检测到文件冲突: {}", conflictResult.getMessage());
                
                // 尝试三方合并
                if (enableThreeWayMerge) {
                    String currentContent = Files.readString(filePath, encoding);
                    ThreeWayMerger.MergeResult mergeResult = threeWayMerger.merge(
                        originalContent, currentContent, oldString, newString);
                    
                    if (mergeResult.isSuccess()) {
                        logger.info("三方合并成功: {}", mergeResult.getMessage());
                        // 使用合并后的内容
                        Files.writeString(filePath, mergeResult.getMergedContent(), encoding);
                        
                        // 更新时间戳和快照
                        FileTime newModifiedTime = Files.getLastModifiedTime(filePath);
                        readFileTimestamps.put(filePath.toString(), newModifiedTime.toMillis());
                        conflictDetector.updateSnapshot(filePath);
                        
                        logger.info("文件已编辑（通过三方合并）: {}", filePath);
                        
                        String preview = generatePreview(filePath.toString(), originalContent, oldString, newString);
                        return ToolResult.success(String.format("✅ 文件已更新（三方合并）: %s\n合并信息: %s\n\n%s", 
                            filePath, mergeResult.getMessage(), preview));
                    } else {
                        return ToolResult.error("文件冲突且无法自动合并: " + mergeResult.getMessage() + 
                            "\n请先重新读取文件，或手动解决冲突后重试。");
                    }
                } else {
                    return ToolResult.error("文件冲突: " + conflictResult.getMessage() + 
                        "\n请先重新读取文件后重试。");
                }
            }
        }
        
        // 大文件优化
        if (largeFileOptimizer.isLargeFile(filePath)) {
            logger.info("检测到大文件，使用优化处理: {}", filePath);
            LargeFileOptimizer.EditResult editResult = largeFileOptimizer.editLargeFile(
                filePath, oldString, newString, encoding);
            
            if (editResult.isSuccess()) {
                // 更新时间戳和快照
                FileTime newModifiedTime = Files.getLastModifiedTime(filePath);
                readFileTimestamps.put(filePath.toString(), newModifiedTime.toMillis());
                conflictDetector.updateSnapshot(filePath);
                
                logger.info("大文件已编辑: {}", filePath);
                
                String preview = generatePreview(filePath.toString(), originalContent, oldString, newString);
                return ToolResult.success(String.format("✅ 大文件已更新: %s\n大小变化: %d -> %d 字节 (%+d)\n\n%s", 
                    filePath, editResult.getOldSize(), editResult.getNewSize(), 
                    editResult.getSizeDiff(), preview));
            } else {
                return ToolResult.error("大文件编辑失败: " + editResult.getMessage());
            }
        }
        
        // 标准编辑流程
        String updatedContent = originalContent.replace(oldString, newString);
        Files.writeString(filePath, updatedContent, encoding);
        
        // 更新时间戳和快照
        FileTime newModifiedTime = Files.getLastModifiedTime(filePath);
        readFileTimestamps.put(filePath.toString(), newModifiedTime.toMillis());
        conflictDetector.updateSnapshot(filePath);
        
        logger.info("文件已编辑: {}", filePath);
        
        // 生成预览信息
        String preview = generatePreview(filePath.toString(), originalContent, oldString, newString);
        
        return ToolResult.success(String.format("✅ 文件已更新: %s\n\n%s", 
            filePath, preview));
    }
    
    /**
     * 生成编辑预览信息
     */
    private String generatePreview(String filePath, String originalContent, 
                                   String oldString, String newString) {
        StringBuilder preview = new StringBuilder();
        
        // 添加diff预览
        if (showDiff) {
            String sideBySideDiff = diffGenerator.generateSideBySideDiff(
                originalContent, oldString, newString);
            preview.append(sideBySideDiff);
            
            // 添加统计信息
            String stats = diffGenerator.generateStats(oldString, newString);
            preview.append("\n").append(stats).append("\n");
        }
        
        // 添加传统的代码片段
        String snippet = getSnippet(originalContent, oldString, newString);
        preview.append("\n修改后的代码片段：\n").append(snippet);
        
        return preview.toString();
    }
    
    /**
     * 获取修改后的代码片段
     */
    private String getSnippet(String originalText, String oldStr, String newStr) {
        String[] parts = originalText.split(Pattern.quote(oldStr), 2);
        if (parts.length < 2) {
            return "（无法生成片段）";
        }
        
        String before = parts[0];
        int replacementLine = before.split("\r?\n").length - 1;
        
        String newContent = originalText.replace(oldStr, newStr);
        String[] newLines = newContent.split("\r?\n");
        
        // 计算片段的起始和结束行
        int startLine = Math.max(0, replacementLine - N_LINES_SNIPPET);
        int endLine = Math.min(newLines.length, 
            replacementLine + N_LINES_SNIPPET + newStr.split("\r?\n").length);
        
        // 提取片段
        StringBuilder snippet = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            snippet.append(String.format("%4d | %s\n", i + 1, newLines[i]));
        }
        
        return snippet.toString();
    }
    
    /**
     * 计算字符串出现次数
     */
    private int countMatches(String text, String substring) {
        if (substring.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * 检测文件编码
     */
    private Charset detectEncoding(Path filePath) {
        // 简单实现：默认使用 UTF-8
        // 可以扩展为更复杂的编码检测
        return StandardCharsets.UTF_8;
    }
    
    /**
     * 记录文件读取时间戳（供 FileReadTool 调用）
     */
    public void recordFileRead(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                FileTime modifiedTime = Files.getLastModifiedTime(path);
                readFileTimestamps.put(filePath, modifiedTime.toMillis());
                // 同时记录冲突检测快照
                conflictDetector.recordSnapshot(path);
            }
        } catch (IOException e) {
            logger.warn("记录文件读取时间失败: {}", filePath, e);
        }
    }
    
    /**
     * 设置是否显示diff预览
     */
    public void setShowDiff(boolean showDiff) {
        this.showDiff = showDiff;
    }
    
    /**
     * 获取当前diff显示设置
     */
    public boolean isShowDiff() {
        return this.showDiff;
    }
    
    /**
     * 设置是否启用冲突检测
     */
    public void setEnableConflictDetection(boolean enable) {
        this.enableConflictDetection = enable;
    }
    
    /**
     * 设置是否启用三方合并
     */
    public void setEnableThreeWayMerge(boolean enable) {
        this.enableThreeWayMerge = enable;
    }
    
    /**
     * 获取冲突检测器（用于测试）
     */
    ConflictDetector getConflictDetector() {
        return conflictDetector;
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String filePath = (String) input.get("file_path");
        Path path = Paths.get(filePath);
        if (!path.isAbsolute()) {
            path = Paths.get(workingDirectory).resolve(filePath);
        }
        
        Path cwd = Paths.get(workingDirectory);
        String relativePath = cwd.relativize(path).toString();
        
        String oldString = (String) input.get("old_string");
        String operation = (oldString == null || oldString.isEmpty()) ? "创建" : "编辑";
        
        return String.format("%s文件: %s", operation, relativePath);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return result.getOutput().split("\n")[0]; // 只显示第一行
        } else {
            return "❌ 编辑失败: " + result.getError();
        }
    }
}
