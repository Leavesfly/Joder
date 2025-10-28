package io.leavesfly.joder.tools.edit;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import io.leavesfly.joder.ui.renderer.DiffRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BatchEdit Tool - 批量文件编辑工具
 * 支持在一次操作中编辑多个文件,提高效率
 */
public class BatchEditTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchEditTool.class);
    private static final int MAX_BATCH_SIZE = 10; // 最大批量操作数
    
    private final String workingDirectory;
    private final FileEditTool fileEditTool;
    private final DiffRenderer diffRenderer;
    
    private boolean transactional = false; // 是否启用事务性执行
    private boolean showUnifiedDiff = true; // 是否显示统一Diff预览
    
    @Inject
    public BatchEditTool(@WorkingDirectory String workingDirectory,
                         FileEditTool fileEditTool,
                         DiffRenderer diffRenderer) {
        this.workingDirectory = workingDirectory;
        this.fileEditTool = fileEditTool;
        this.diffRenderer = diffRenderer;
    }
    
    @Override
    public String getName() {
        return "BatchEdit";
    }
    
    @Override
    public String getDescription() {
        return "批量编辑多个文件的工具。适用于需要对多个文件执行相同或不同操作的场景。";
    }
    
    @Override
    public String getPrompt() {
        return "此工具用于批量编辑多个文件,提高多文件修改的效率。\n\n" +
               "使用场景:\n" +
               "1. 需要对多个文件进行相似修改\n" +
               "2. 跨文件重构(如重命名类或方法)\n" +
               "3. 批量更新导入语句或依赖\n\n" +
               "输入参数:\n" +
               "1. edits: 编辑操作列表,每个操作包含:\n" +
               "   - file_path: 文件路径(必须是绝对路径)\n" +
               "   - old_string: 要替换的文本\n" +
               "   - new_string: 替换后的文本\n" +
               "2. transactional: (可选) 是否事务性执行,全部成功或全部回滚 (默认: false)\n" +
               "3. show_preview: (可选) 是否显示统一Diff预览 (默认: true)\n\n" +
               "关键要求:\n" +
               "1. 每个文件必须先通过 FileRead 工具读取\n" +
               "2. 每个 old_string 必须在对应文件中唯一\n" +
               "3. 最多支持 " + MAX_BATCH_SIZE + " 个文件的批量操作\n" +
               "4. 事务性模式:全部成功或全部回滚,确保数据一致性\n" +
               "5. 非事务性模式:按顺序执行,某个失败后继续执行\n\n" +
               "返回结果:\n" +
               "- 成功编辑的文件列表\n" +
               "- 失败编辑的详细信息\n" +
               "- 总体成功率统计\n" +
               "- 统一Diff预览(如启用)\n\n" +
               "警告:\n" +
               "- 非事务性模式下,部分成功/部分失败是可能的\n" +
               "- 建议在重要操作前备份文件或使用版本控制\n" +
               "- 事务性模式会创建临时备份,但仍建议使用版本控制";
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
        return false; // 批量编辑不支持并发
    }
    
    @Override
    public boolean needsPermissions() {
        return true; // 批量编辑需要权限检查
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ToolResult call(Map<String, Object> input) {
        List<Map<String, Object>> edits = (List<Map<String, Object>>) input.get("edits");
        
        if (edits == null || edits.isEmpty()) {
            return ToolResult.error("编辑列表不能为空");
        }
        
        if (edits.size() > MAX_BATCH_SIZE) {
            return ToolResult.error(String.format(
                "批量操作数量超过限制(%d),当前提供: %d", 
                MAX_BATCH_SIZE, edits.size()));
        }
        
        // 读取参数
        boolean isTransactional = Boolean.TRUE.equals(input.get("transactional"));
        boolean showPreview = input.get("show_preview") != null 
            ? Boolean.TRUE.equals(input.get("show_preview")) 
            : this.showUnifiedDiff;
        
        BatchEditResult result;
        
        if (isTransactional) {
            // 事务性执行
            result = executeTransactionalBatchEdits(edits);
        } else {
            // 非事务性执行
            result = executeBatchEdits(edits);
        }
        
        // 生成统一Diff预览
        if (showPreview && !result.getSuccesses().isEmpty()) {
            String unifiedDiff = generateUnifiedDiff(result);
            result.setUnifiedDiff(unifiedDiff);
        }
        
        // 生成结果报告
        return generateReport(result);
    }
    
    /**
     * 执行批量编辑（非事务性）
     */
    private BatchEditResult executeBatchEdits(List<Map<String, Object>> edits) {
        BatchEditResult result = new BatchEditResult();
        
        for (int i = 0; i < edits.size(); i++) {
            Map<String, Object> edit = edits.get(i);
            String filePath = (String) edit.get("file_path");
            
            try {
                // 执行单个文件编辑
                ToolResult editResult = fileEditTool.call(edit);
                
                if (editResult.isSuccess()) {
                    result.addSuccess(filePath, editResult.getOutput());
                    logger.info("批量编辑成功 [{}/{}]: {}", i + 1, edits.size(), filePath);
                } else {
                    result.addFailure(filePath, editResult.getError());
                    logger.warn("批量编辑失败 [{}/{}]: {} - {}", 
                        i + 1, edits.size(), filePath, editResult.getError());
                }
                
            } catch (Exception e) {
                result.addFailure(filePath, "执行编辑时发生异常: " + e.getMessage());
                logger.error("批量编辑异常 [{}/{}]: {}", i + 1, edits.size(), filePath, e);
            }
        }
        
        return result;
    }
    
    /**
     * 执行事务性批量编辑（全部成功或全部回滚）
     */
    private BatchEditResult executeTransactionalBatchEdits(List<Map<String, Object>> edits) {
        BatchEditResult result = new BatchEditResult();
        Map<String, Path> backups = new HashMap<>();
        
        try {
            // 第一阶段：创建所有文件的备份
            for (Map<String, Object> edit : edits) {
                String filePath = (String) edit.get("file_path");
                Path path = resolvePath(filePath);
                
                if (Files.exists(path)) {
                    Path backup = createBackup(path);
                    backups.put(filePath, backup);
                    logger.debug("已创建备份: {} -> {}", filePath, backup);
                }
            }
            
            // 第二阶段：执行所有编辑
            for (int i = 0; i < edits.size(); i++) {
                Map<String, Object> edit = edits.get(i);
                String filePath = (String) edit.get("file_path");
                
                try {
                    ToolResult editResult = fileEditTool.call(edit);
                    
                    if (editResult.isSuccess()) {
                        result.addSuccess(filePath, editResult.getOutput());
                        logger.info("事务性编辑成功 [{}/{}]: {}", i + 1, edits.size(), filePath);
                    } else {
                        // 编辑失败，触发回滚
                        throw new RuntimeException("编辑失败: " + editResult.getError());
                    }
                    
                } catch (Exception e) {
                    // 发生异常，触发回滚
                    logger.error("事务性编辑异常 [{}/{}]: {}", i + 1, edits.size(), filePath, e);
                    rollbackAllEdits(backups);
                    result.addFailure(filePath, "编辑失败，已回滚所有修改: " + e.getMessage());
                    result.setTransactionRolledBack(true);
                    return result;
                }
            }
            
            // 第三阶段：所有编辑成功，删除备份
            for (Path backup : backups.values()) {
                Files.deleteIfExists(backup);
            }
            
            logger.info("事务性批量编辑全部成功");
            
        } catch (IOException e) {
            logger.error("事务性批量编辑失败", e);
            rollbackAllEdits(backups);
            result.addFailure("transaction", "事务失败，已回滚: " + e.getMessage());
            result.setTransactionRolledBack(true);
        }
        
        return result;
    }
    
    /**
     * 创建文件备份
     */
    private Path createBackup(Path original) throws IOException {
        String fileName = original.getFileName().toString();
        Path parent = original.getParent();
        Path backup = parent.resolve("." + fileName + ".backup." + System.currentTimeMillis());
        Files.copy(original, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }
    
    /**
     * 回滚所有编辑
     */
    private void rollbackAllEdits(Map<String, Path> backups) {
        logger.info("开始回滚所有编辑...");
        
        for (Map.Entry<String, Path> entry : backups.entrySet()) {
            String filePath = entry.getKey();
            Path backup = entry.getValue();
            
            try {
                Path original = resolvePath(filePath);
                Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(backup);
                logger.info("已回滚: {}", filePath);
            } catch (IOException e) {
                logger.error("回滚文件失败: {}", filePath, e);
            }
        }
    }
    
    /**
     * 解析文件路径
     */
    private Path resolvePath(String filePath) {
        Path path = Paths.get(filePath);
        if (!path.isAbsolute()) {
            path = Paths.get(workingDirectory).resolve(filePath).normalize();
        }
        return path;
    }
    
    /**
     * 生成统一Diff预览
     */
    private String generateUnifiedDiff(BatchEditResult result) {
        StringBuilder diff = new StringBuilder();
        diff.append("\n\ud83d\udccb 统一Diff预览:\n");
        diff.append("=".repeat(60)).append("\n\n");
        
        // 为每个成功的文件生成diff
        for (String filePath : result.getSuccesses().keySet()) {
            diff.append("📄 ").append(filePath).append("\n");
            diff.append("-".repeat(60)).append("\n");
            
            // 这里简化处理，实际应该从编辑结果中提取diff
            String message = result.getSuccesses().get(filePath);
            if (message.contains("\n")) {
                String[] lines = message.split("\n", 3);
                if (lines.length > 2) {
                    diff.append(lines[2]).append("\n"); // 提取diff部分
                }
            }
            
            diff.append("\n");
        }
        
        return diff.toString();
    }
    
    /**
     * 生成执行报告
     */
    private ToolResult generateReport(BatchEditResult result) {
        StringBuilder report = new StringBuilder();
        
        // 总体统计
        report.append("📊 批量编辑完成\n\n");
        report.append(String.format("总计: %d 个文件\n", result.getTotalCount()));
        report.append(String.format("✅ 成功: %d 个\n", result.getSuccessCount()));
        report.append(String.format("❌ 失败: %d 个\n", result.getFailureCount()));
        report.append(String.format("成功率: %.1f%%\n\n", result.getSuccessRate()));
        
        // 成功列表
        if (!result.getSuccesses().isEmpty()) {
            report.append("成功编辑的文件:\n");
            result.getSuccesses().forEach((file, message) -> {
                report.append(String.format("  ✓ %s\n", file));
            });
            report.append("\n");
        }
        
        // 失败列表
        if (!result.getFailures().isEmpty()) {
            report.append("失败的文件:\n");
            result.getFailures().forEach((file, error) -> {
                report.append(String.format("  ✗ %s\n", file));
                report.append(String.format("    原因: %s\n", error));
            });
        }
        
        // 根据成功率判断整体结果
        if (result.getFailureCount() == 0) {
            return ToolResult.success(report.toString());
        } else if (result.getSuccessCount() > 0) {
            // 部分成功
            return ToolResult.success(report.toString());
        } else {
            // 全部失败
            return ToolResult.error(report.toString());
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edits = (List<Map<String, Object>>) input.get("edits");
        
        if (edits == null || edits.isEmpty()) {
            return "批量编辑文件: 0 个文件";
        }
        
        Set<String> files = edits.stream()
            .map(edit -> (String) edit.get("file_path"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        return String.format("批量编辑文件: %d 个文件", files.size());
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            String output = result.getOutput();
            // 提取第一行(总体统计)
            String[] lines = output.split("\n");
            if (lines.length > 0) {
                return lines[0];
            }
            return "✅ 批量编辑完成";
        } else {
            return "❌ 批量编辑失败";
        }
    }
    
    /**
     * 批量编辑结果
     */
    private static class BatchEditResult {
        private final Map<String, String> successes = new LinkedHashMap<>();
        private final Map<String, String> failures = new LinkedHashMap<>();
        private String unifiedDiff;
        private boolean transactionRolledBack = false;
        
        public void addSuccess(String filePath, String message) {
            successes.put(filePath, message);
        }
        
        public void addFailure(String filePath, String error) {
            failures.put(filePath, error);
        }
        
        public Map<String, String> getSuccesses() {
            return successes;
        }
        
        public Map<String, String> getFailures() {
            return failures;
        }
        
        public int getTotalCount() {
            return successes.size() + failures.size();
        }
        
        public int getSuccessCount() {
            return successes.size();
        }
        
        public int getFailureCount() {
            return failures.size();
        }
        
        public double getSuccessRate() {
            if (getTotalCount() == 0) {
                return 0.0;
            }
            return (double) getSuccessCount() / getTotalCount() * 100.0;
        }
        
        public void setUnifiedDiff(String diff) {
            this.unifiedDiff = diff;
        }
        
        public String getUnifiedDiff() {
            return unifiedDiff;
        }
        
        public boolean isTransactionRolledBack() {
            return transactionRolledBack;
        }
        
        public void setTransactionRolledBack(boolean rolledBack) {
            this.transactionRolledBack = rolledBack;
        }
    }
}
