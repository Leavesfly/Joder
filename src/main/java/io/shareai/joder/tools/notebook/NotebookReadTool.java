package io.shareai.joder.tools.notebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * NotebookRead Tool - Jupyter Notebook 读取工具
 * 
 * 读取并展示 Jupyter Notebook 内容
 */
@Singleton
public class NotebookReadTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(NotebookReadTool.class);
    
    private final String workingDirectory;
    private final ObjectMapper objectMapper;
    
    @Inject
    public NotebookReadTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "NotebookRead";
    }
    
    @Override
    public String getDescription() {
        return "读取 Jupyter Notebook (.ipynb) 文件并展示内容。\n" +
               "- 显示所有单元格或指定单元格\n" +
               "- 显示单元格类型和内容\n" +
               "- 显示执行计数和输出\n" +
               "- 显示 Notebook 元信息";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具读取 Jupyter Notebook 文件。\n\n" +
               "**参数**：\n" +
               "- notebook_path: Notebook 文件的绝对路径（必需）\n" +
               "- cell_number: 要读取的单元格索引（可选，不指定则读取所有）\n" +
               "- show_outputs: 是否显示单元格输出（默认 false）\n\n" +
               "**输出格式**：\n" +
               "- 单元格索引和类型\n" +
               "- 单元格源代码\n" +
               "- 执行计数（如果有）\n" +
               "- 输出内容（如果 show_outputs=true）\n\n" +
               "**适用场景**：\n" +
               "- 查看 Notebook 结构\n" +
               "- 审查代码单元格\n" +
               "- 分析执行结果\n" +
               "- 提取 Notebook 内容\n\n" +
               "**示例**：\n" +
               "```json\n" +
               "// 读取整个 Notebook\n" +
               "{\n" +
               "  \"notebook_path\": \"/path/to/notebook.ipynb\",\n" +
               "  \"show_outputs\": true\n" +
               "}\n\n" +
               "// 读取指定单元格\n" +
               "{\n" +
               "  \"notebook_path\": \"/path/to/notebook.ipynb\",\n" +
               "  \"cell_number\": 2\n" +
               "}\n" +
               "```";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true; // 只读操作
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false; // 只读不需要特殊权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String notebookPath = (String) input.get("notebook_path");
        Integer cellNumber = input.get("cell_number") != null 
            ? ((Number) input.get("cell_number")).intValue() 
            : null;
        Boolean showOutputs = (Boolean) input.getOrDefault("show_outputs", false);
        
        // 验证参数
        if (notebookPath == null || notebookPath.trim().isEmpty()) {
            return ToolResult.error("notebook_path 不能为空");
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
            
            ArrayNode cells = (ArrayNode) notebook.get("cells");
            if (cells == null) {
                return ToolResult.error("Notebook 缺少 cells 字段");
            }
            
            // 获取元信息
            JsonNode metadata = notebook.get("metadata");
            String kernelName = "Unknown";
            String language = "python";
            
            if (metadata != null && metadata.has("kernelspec")) {
                JsonNode kernelspec = metadata.get("kernelspec");
                kernelName = kernelspec.has("display_name") 
                    ? kernelspec.get("display_name").asText() 
                    : "Unknown";
            }
            
            if (metadata != null && metadata.has("language_info")) {
                JsonNode langInfo = metadata.get("language_info");
                language = langInfo.has("name") 
                    ? langInfo.get("name").asText() 
                    : "python";
            }
            
            // 构建输出
            StringBuilder output = new StringBuilder();
            output.append("📓 Jupyter Notebook\n\n");
            output.append("═".repeat(60)).append("\n\n");
            
            // 元信息
            output.append("📋 元信息\n");
            output.append("─".repeat(60)).append("\n");
            output.append(String.format("文件:         %s\n", path.getFileName()));
            output.append(String.format("内核:         %s\n", kernelName));
            output.append(String.format("语言:         %s\n", language));
            output.append(String.format("单元格数量:   %d\n\n", cells.size()));
            
            // 读取单元格
            if (cellNumber != null) {
                // 读取单个单元格
                if (cellNumber < 0 || cellNumber >= cells.size()) {
                    return ToolResult.error(String.format(
                        "cell_number 超出范围。Notebook 有 %d 个单元格", 
                        cells.size()
                    ));
                }
                
                output.append(renderCell(cells.get(cellNumber), cellNumber, showOutputs));
            } else {
                // 读取所有单元格
                output.append("📝 单元格内容\n");
                output.append("─".repeat(60)).append("\n\n");
                
                for (int i = 0; i < cells.size(); i++) {
                    output.append(renderCell(cells.get(i), i, showOutputs));
                    if (i < cells.size() - 1) {
                        output.append("\n").append("─".repeat(60)).append("\n\n");
                    }
                }
            }
            
            output.append("\n").append("═".repeat(60)).append("\n");
            
            return ToolResult.success(output.toString());
            
        } catch (IOException e) {
            logger.error("读取 Notebook 失败: {}", notebookPath, e);
            return ToolResult.error("读取失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Notebook 解析错误: {}", notebookPath, e);
            return ToolResult.error("解析错误: " + e.getMessage());
        }
    }
    
    /**
     * 渲染单个单元格
     */
    private String renderCell(JsonNode cell, int index, boolean showOutputs) {
        StringBuilder sb = new StringBuilder();
        
        String cellType = cell.has("cell_type") ? cell.get("cell_type").asText() : "unknown";
        String source = cell.has("source") ? cell.get("source").asText() : "";
        
        // 单元格头部
        String icon = "code".equals(cellType) ? "💻" : "📝";
        sb.append(String.format("%s 单元格 [%d] - %s\n", icon, index, cellType));
        
        // 执行计数
        if (cell.has("execution_count") && !cell.get("execution_count").isNull()) {
            sb.append(String.format("执行次数: In [%s]\n", cell.get("execution_count").asText()));
        }
        
        sb.append("\n");
        
        // 源代码
        sb.append(source);
        
        // 输出
        if (showOutputs && cell.has("outputs") && cell.get("outputs").isArray()) {
            ArrayNode outputs = (ArrayNode) cell.get("outputs");
            if (outputs.size() > 0) {
                sb.append("\n\n📤 输出:\n");
                for (int i = 0; i < outputs.size(); i++) {
                    JsonNode output = outputs.get(i);
                    String outputType = output.has("output_type") 
                        ? output.get("output_type").asText() 
                        : "unknown";
                    
                    switch (outputType) {
                        case "stream":
                            if (output.has("text")) {
                                sb.append(output.get("text").asText());
                            }
                            break;
                        case "execute_result":
                        case "display_data":
                            if (output.has("data") && output.get("data").has("text/plain")) {
                                sb.append(output.get("data").get("text/plain").asText());
                            }
                            break;
                        case "error":
                            sb.append("❌ 错误:\n");
                            if (output.has("ename")) {
                                sb.append(output.get("ename").asText()).append(": ");
                            }
                            if (output.has("evalue")) {
                                sb.append(output.get("evalue").asText());
                            }
                            break;
                    }
                    
                    if (i < outputs.size() - 1) {
                        sb.append("\n");
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String notebookPath = (String) input.get("notebook_path");
        Integer cellNumber = input.get("cell_number") != null 
            ? ((Number) input.get("cell_number")).intValue() 
            : null;
        
        if (cellNumber != null) {
            return String.format("读取 Notebook: %s, 单元格: %d", notebookPath, cellNumber);
        } else {
            return String.format("读取 Notebook: %s（全部）", notebookPath);
        }
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ Notebook 读取成功";
        } else {
            return "❌ Notebook 读取失败: " + result.getError();
        }
    }
}
