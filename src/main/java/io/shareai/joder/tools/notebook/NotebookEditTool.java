package io.shareai.joder.tools.notebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.shareai.joder.WorkingDirectory;
import io.shareai.joder.tools.Tool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * NotebookEdit Tool - Jupyter Notebook 编辑工具
 * 
 * 编辑 Jupyter Notebook 单元格
 */
@Singleton
public class NotebookEditTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(NotebookEditTool.class);
    
    private final String workingDirectory;
    private final ObjectMapper objectMapper;
    
    @Inject
    public NotebookEditTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "NotebookEditCell";
    }
    
    @Override
    public String getDescription() {
        return "编辑 Jupyter Notebook (.ipynb) 文件的单元格。\n" +
               "- 替换单元格内容\n" +
               "- 插入新单元格\n" +
               "- 删除单元格\n" +
               "- 自动清除输出和执行计数";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具编辑 Jupyter Notebook 文件的单元格。\n\n" +
               "**参数**：\n" +
               "- notebook_path: Notebook 文件的绝对路径（必需）\n" +
               "- cell_number: 要编辑的单元格索引（从 0 开始，必需）\n" +
               "- new_source: 新的单元格内容（必需）\n" +
               "- cell_type: 单元格类型（code 或 markdown，可选）\n" +
               "- edit_mode: 编辑模式（replace、insert、delete，默认 replace）\n\n" +
               "**编辑模式**：\n" +
               "1. replace - 替换指定单元格的内容\n" +
               "2. insert - 在指定位置插入新单元格\n" +
               "3. delete - 删除指定单元格\n\n" +
               "**注意事项**：\n" +
               "- 文件必须是 .ipynb 格式\n" +
               "- cell_number 必须在有效范围内\n" +
               "- insert 模式需要指定 cell_type\n" +
               "- 修改后会自动清除单元格输出\n\n" +
               "**示例**：\n" +
               "```json\n" +
               "// 替换单元格\n" +
               "{\n" +
               "  \"notebook_path\": \"/path/to/notebook.ipynb\",\n" +
               "  \"cell_number\": 0,\n" +
               "  \"new_source\": \"import numpy as np\",\n" +
               "  \"edit_mode\": \"replace\"\n" +
               "}\n\n" +
               "// 插入新单元格\n" +
               "{\n" +
               "  \"notebook_path\": \"/path/to/notebook.ipynb\",\n" +
               "  \"cell_number\": 1,\n" +
               "  \"new_source\": \"# New Section\",\n" +
               "  \"cell_type\": \"markdown\",\n" +
               "  \"edit_mode\": \"insert\"\n" +
               "}\n" +
               "```";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return false; // 修改文件
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return false; // 文件操作不安全并发
    }
    
    @Override
    public boolean needsPermissions() {
        return true; // 需要写权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String notebookPath = (String) input.get("notebook_path");
        Integer cellNumber = input.get("cell_number") != null 
            ? ((Number) input.get("cell_number")).intValue() 
            : null;
        String newSource = (String) input.get("new_source");
        String cellType = (String) input.get("cell_type");
        String editMode = (String) input.getOrDefault("edit_mode", "replace");
        
        // 验证参数
        if (notebookPath == null || notebookPath.trim().isEmpty()) {
            return ToolResult.error("notebook_path 不能为空");
        }
        
        if (cellNumber == null) {
            return ToolResult.error("cell_number 不能为空");
        }
        
        if (cellNumber < 0) {
            return ToolResult.error("cell_number 必须非负");
        }
        
        if (!"delete".equals(editMode) && (newSource == null || newSource.isEmpty())) {
            return ToolResult.error("new_source 不能为空（delete 模式除外）");
        }
        
        if ("insert".equals(editMode) && (cellType == null || cellType.isEmpty())) {
            return ToolResult.error("insert 模式需要指定 cell_type");
        }
        
        try {
            // 解析路径
            Path path = Paths.get(notebookPath);
            if (!path.isAbsolute()) {
                path = Paths.get(workingDirectory).resolve(notebookPath).normalize();
            }
            
            // 验证文件
            if (!Files.exists(path)) {
                return ToolResult.error("Notebook 文件不存在: " + notebookPath);
            }
            
            if (!path.toString().endsWith(".ipynb")) {
                return ToolResult.error("文件必须是 .ipynb 格式");
            }
            
            // 读取 Notebook
            String content = Files.readString(path);
            JsonNode notebook = objectMapper.readTree(content);
            
            if (!notebook.isObject()) {
                return ToolResult.error("Notebook 格式无效");
            }
            
            ObjectNode notebookObj = (ObjectNode) notebook;
            ArrayNode cells = (ArrayNode) notebookObj.get("cells");
            
            if (cells == null) {
                return ToolResult.error("Notebook 缺少 cells 字段");
            }
            
            // 验证 cell_number 范围
            if ("insert".equals(editMode)) {
                if (cellNumber > cells.size()) {
                    return ToolResult.error(String.format(
                        "cell_number 超出范围。insert 模式最大值为 %d（在末尾追加）", 
                        cells.size()
                    ));
                }
            } else {
                if (cellNumber >= cells.size()) {
                    return ToolResult.error(String.format(
                        "cell_number 超出范围。Notebook 有 %d 个单元格", 
                        cells.size()
                    ));
                }
            }
            
            // 执行编辑操作
            String result = switch (editMode.toLowerCase()) {
                case "delete" -> deleteCell(cells, cellNumber);
                case "insert" -> insertCell(cells, cellNumber, newSource, cellType);
                case "replace" -> replaceCell(cells, cellNumber, newSource, cellType);
                default -> throw new IllegalArgumentException("无效的 edit_mode: " + editMode);
            };
            
            // 写回文件
            String updatedContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(notebookObj);
            Files.writeString(path, updatedContent);
            
            logger.info("Notebook 编辑成功: {} - {}", notebookPath, editMode);
            
            return ToolResult.success(result);
            
        } catch (IOException e) {
            logger.error("Notebook 编辑失败: {}", notebookPath, e);
            return ToolResult.error("编辑失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Notebook 编辑错误: {}", notebookPath, e);
            return ToolResult.error("错误: " + e.getMessage());
        }
    }
    
    /**
     * 删除单元格
     */
    private String deleteCell(ArrayNode cells, int cellNumber) {
        cells.remove(cellNumber);
        return String.format("✅ 已删除单元格 %d", cellNumber);
    }
    
    /**
     * 插入单元格
     */
    private String insertCell(ArrayNode cells, int cellNumber, String newSource, String cellType) {
        ObjectNode newCell = objectMapper.createObjectNode();
        newCell.put("cell_type", cellType);
        newCell.put("source", newSource);
        newCell.set("metadata", objectMapper.createObjectNode());
        
        if ("code".equals(cellType)) {
            newCell.set("outputs", objectMapper.createArrayNode());
            newCell.putNull("execution_count");
        }
        
        cells.insert(cellNumber, newCell);
        
        return String.format("✅ 已在位置 %d 插入 %s 单元格\n\n内容:\n%s", 
            cellNumber, cellType, newSource);
    }
    
    /**
     * 替换单元格
     */
    private String replaceCell(ArrayNode cells, int cellNumber, String newSource, String cellType) {
        ObjectNode targetCell = (ObjectNode) cells.get(cellNumber);
        
        // 更新内容
        targetCell.put("source", newSource);
        
        // 更新类型（如果指定）
        if (cellType != null && !cellType.isEmpty()) {
            targetCell.put("cell_type", cellType);
        }
        
        // 清除输出和执行计数
        if ("code".equals(targetCell.get("cell_type").asText())) {
            targetCell.set("outputs", objectMapper.createArrayNode());
            targetCell.putNull("execution_count");
        }
        
        return String.format("✅ 已更新单元格 %d\n\n新内容:\n%s", cellNumber, newSource);
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String notebookPath = (String) input.get("notebook_path");
        Integer cellNumber = input.get("cell_number") != null 
            ? ((Number) input.get("cell_number")).intValue() 
            : null;
        String editMode = (String) input.getOrDefault("edit_mode", "replace");
        
        return String.format("编辑 Notebook: %s, 单元格: %d, 模式: %s", 
            notebookPath, cellNumber, editMode);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ Notebook 编辑成功";
        } else {
            return "❌ Notebook 编辑失败: " + result.getError();
        }
    }
}
