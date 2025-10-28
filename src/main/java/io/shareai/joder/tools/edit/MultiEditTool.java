package io.shareai.joder.tools.edit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.WorkingDirectory;
import io.shareai.joder.tools.Tool;
import io.shareai.joder.tools.ToolResult;
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
 * MultiEdit Tool - 批量编辑工具
 * 在单个操作中对一个文件进行多次编辑
 */
public class MultiEditTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiEditTool.class);
    private static final int N_LINES_SNIPPET = 4;
    
    private final String workingDirectory;
    private final FileEditTool fileEditTool;
    private final ObjectMapper objectMapper;
    
    @Inject
    public MultiEditTool(@WorkingDirectory String workingDirectory, FileEditTool fileEditTool) {
        this.workingDirectory = workingDirectory;
        this.fileEditTool = fileEditTool;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "MultiEdit";
    }
    
    @Override
    public String getDescription() {
        return "用于在一个操作中对单个文件进行多次编辑的工具。基于 Edit 工具构建，允许高效执行多个查找替换操作。需要对同一文件进行多次编辑时，优先使用此工具而非 Edit 工具。";
    }
    
    @Override
    public String getPrompt() {
        return "此工具用于在一个操作中对单个文件进行多次编辑。基于 Edit 工具构建，允许高效执行多个查找替换操作。\n\n" +
               "使用此工具前：\n" +
               "1. 使用 FileRead 工具了解文件的内容和上下文\n" +
               "2. 验证目录路径正确\n\n" +
               "进行多次文件编辑需要提供：\n" +
               "1. file_path: 要修改的文件的绝对路径（必须是绝对路径）\n" +
               "2. edits: 要执行的编辑操作数组，每个编辑包含：\n" +
               "   - old_string: 要替换的文本（必须与文件内容完全匹配，包括所有空格和缩进）\n" +
               "   - new_string: 用于替换 old_string 的编辑后文本\n" +
               "   - replace_all: 替换所有 old_string 出现（可选，默认 false）\n\n" +
               "重要说明：\n" +
               "- 所有编辑按提供的顺序依次应用\n" +
               "- 每次编辑在前一次编辑的结果上操作\n" +
               "- 所有编辑都必须有效才能成功 - 如果任何编辑失败，则不会应用任何编辑\n" +
               "- 此工具适用于需要对同一文件的不同部分进行多次更改的情况\n\n" +
               "关键要求：\n" +
               "1. 所有编辑遵循与单个 Edit 工具相同的要求\n" +
               "2. 编辑是原子的 - 要么全部成功，要么全部不应用\n" +
               "3. 仔细规划编辑，避免顺序操作之间的冲突\n\n" +
               "警告：\n" +
               "- 如果 edits.old_string 不完全匹配文件内容（包括空格），工具将失败\n" +
               "- 如果 edits.old_string 和 edits.new_string 相同，工具将失败\n" +
               "- 由于编辑按顺序应用，确保早期编辑不影响后续编辑要查找的文本\n\n" +
               "进行编辑时：\n" +
               "- 确保所有编辑产生符合习惯的正确代码\n" +
               "- 不要使代码处于破损状态\n" +
               "- 始终使用绝对文件路径（以 / 开头）\n" +
               "- 使用 replace_all 在整个文件中替换和重命名字符串。如果想重命名变量，此参数很有用\n\n" +
               "创建新文件时：\n" +
               "- 使用新文件路径（如需要包含目录名）\n" +
               "- 第一次编辑：空的 old_string 和新文件的内容作为 new_string\n" +
               "- 后续编辑：对创建的内容进行正常编辑操作";
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
        return false; // 批量编辑修改文件，不支持并发执行
    }
    
    @Override
    public boolean needsPermissions() {
        return true; // 批量编辑需要权限检查
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String filePathStr = (String) input.get("file_path");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> editsRaw = (List<Map<String, Object>>) input.get("edits");
        
        if (filePathStr == null || filePathStr.trim().isEmpty()) {
            return ToolResult.error("file_path 不能为空");
        }
        
        if (editsRaw == null || editsRaw.isEmpty()) {
            return ToolResult.error("edits 不能为空");
        }
        
        // 解析编辑操作
        List<EditOperation> edits = new ArrayList<>();
        for (Map<String, Object> editRaw : editsRaw) {
            EditOperation edit = new EditOperation();
            edit.oldString = (String) editRaw.get("old_string");
            edit.newString = (String) editRaw.get("new_string");
            edit.replaceAll = Boolean.TRUE.equals(editRaw.get("replace_all"));
            
            if (edit.oldString == null) edit.oldString = "";
            if (edit.newString == null) edit.newString = "";
            
            edits.add(edit);
        }
        
        Path filePath = Paths.get(filePathStr);
        if (!filePath.isAbsolute()) {
            filePath = Paths.get(workingDirectory).resolve(filePathStr).normalize();
        }
        
        try {
            return applyMultipleEdits(filePath, edits);
        } catch (Exception e) {
            logger.error("批量编辑失败: {}", filePath, e);
            return ToolResult.error("批量编辑失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用多次编辑
     */
    private ToolResult applyMultipleEdits(Path filePath, List<EditOperation> edits) throws IOException {
        boolean isNewFile = !Files.exists(filePath);
        
        // 验证新文件的第一次编辑
        if (isNewFile) {
            if (edits.isEmpty() || !edits.get(0).oldString.isEmpty()) {
                return ToolResult.error("对于新文件，第一次编辑必须有空的 old_string 以创建文件内容");
            }
        }
        
        // 读取原始内容（或创建空内容）
        String currentContent;
        Charset encoding = StandardCharsets.UTF_8;
        
        if (!isNewFile) {
            encoding = detectEncoding(filePath);
            currentContent = Files.readString(filePath, encoding);
            
            // 记录文件读取（让 FileEditTool 知道）
            fileEditTool.recordFileRead(filePath.toString());
        } else {
            currentContent = "";
            
            // 确保父目录存在
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        }
        
        // 逐个应用编辑
        StringBuilder changeLog = new StringBuilder();
        int editCount = 0;
        
        for (int i = 0; i < edits.size(); i++) {
            EditOperation edit = edits.get(i);
            
            // 验证编辑
            if (edit.oldString.equals(edit.newString)) {
                return ToolResult.error(String.format("编辑 #%d: old_string 和 new_string 相同，无需更改", i + 1));
            }
            
            // 应用编辑
            try {
                if (edit.replaceAll) {
                    // 替换所有出现
                    String escapedOld = Pattern.quote(edit.oldString);
                    int occurrences = currentContent.split(escapedOld, -1).length - 1;
                    currentContent = currentContent.replace(edit.oldString, edit.newString);
                    changeLog.append(String.format("  编辑 #%d: 替换了 %d 处\n", i + 1, occurrences));
                    editCount++;
                } else {
                    // 只替换一次
                    if (!currentContent.contains(edit.oldString)) {
                        return ToolResult.error(String.format("编辑 #%d: 文件中未找到要替换的字符串", i + 1));
                    }
                    
                    int occurrences = countOccurrences(currentContent, edit.oldString);
                    if (occurrences > 1) {
                        return ToolResult.error(String.format("编辑 #%d: 找到 %d 处匹配。为安全起见，请设置 replace_all=true 或添加更多上下文以唯一标识", i + 1, occurrences));
                    }
                    
                    currentContent = currentContent.replace(edit.oldString, edit.newString);
                    changeLog.append(String.format("  编辑 #%d: 替换了 1 处\n", i + 1));
                    editCount++;
                }
            } catch (Exception e) {
                return ToolResult.error(String.format("编辑 #%d 失败: %s", i + 1, e.getMessage()));
            }
        }
        
        // 写入文件
        Files.writeString(filePath, currentContent, encoding);
        
        // 更新时间戳
        fileEditTool.recordFileRead(filePath.toString());
        
        logger.info("批量编辑完成: {} ({} 次编辑)", filePath, editCount);
        
        // 生成代码片段（基于第一次编辑）
        String snippet = "";
        if (!edits.isEmpty() && !isNewFile) {
            snippet = getSnippetForFirstEdit(currentContent, edits.get(0));
        }
        
        String result = String.format("✅ 文件已更新: %s\n" +
                                    "共应用 %d 次编辑：\n%s\n%s",
                                    filePath, editCount, changeLog.toString(),
                                    snippet.isEmpty() ? "" : "\n修改后的代码片段：\n" + snippet);
        
        return ToolResult.success(result);
    }
    
    /**
     * 获取第一次编辑的代码片段
     */
    private String getSnippetForFirstEdit(String finalContent, EditOperation firstEdit) {
        String[] lines = finalContent.split("\r?\n");
        
        // 找到新字符串所在的行
        int lineNumber = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(firstEdit.newString)) {
                lineNumber = i;
                break;
            }
        }
        
        if (lineNumber == -1) {
            return "（无法生成片段）";
        }
        
        // 提取周围的行
        int startLine = Math.max(0, lineNumber - N_LINES_SNIPPET);
        int endLine = Math.min(lines.length, lineNumber + N_LINES_SNIPPET + 1);
        
        StringBuilder snippet = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            snippet.append(String.format("%4d | %s\n", i + 1, lines[i]));
        }
        
        return snippet.toString();
    }
    
    /**
     * 计算字符串出现次数
     */
    private int countOccurrences(String text, String substring) {
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
        return StandardCharsets.UTF_8;
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String filePath = (String) input.get("file_path");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> editsRaw = (List<Map<String, Object>>) input.get("edits");
        
        Path path = Paths.get(filePath);
        if (!path.isAbsolute()) {
            path = Paths.get(workingDirectory).resolve(filePath);
        }
        
        Path cwd = Paths.get(workingDirectory);
        String relativePath = cwd.relativize(path).toString();
        
        int editCount = editsRaw != null ? editsRaw.size() : 0;
        
        return String.format("批量编辑 %s（%d 次编辑）", relativePath, editCount);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            String output = result.getOutput();
            String[] lines = output.split("\n");
            return lines[0]; // 只显示第一行
        } else {
            return "❌ 批量编辑失败: " + result.getError();
        }
    }
    
    /**
     * 编辑操作类
     */
    private static class EditOperation {
        String oldString;
        String newString;
        boolean replaceAll;
    }
}
