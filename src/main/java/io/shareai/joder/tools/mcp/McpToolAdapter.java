package io.shareai.joder.tools.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.services.mcp.McpClient;
import io.shareai.joder.services.mcp.McpServerManager;
import io.shareai.joder.services.mcp.McpToolRegistry;
import io.shareai.joder.services.mcp.protocol.McpCallToolResult;
import io.shareai.joder.services.mcp.protocol.McpContent;
import io.shareai.joder.tools.Tool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 工具适配器
 * 将 MCP 工具包装为 Joder 工具
 */
public class McpToolAdapter implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(McpToolAdapter.class);
    
    private final String toolId;
    private final McpToolRegistry.McpToolInfo toolInfo;
    private final McpServerManager serverManager;
    private final ObjectMapper objectMapper;
    
    public McpToolAdapter(String toolId, 
                         McpToolRegistry.McpToolInfo toolInfo,
                         McpServerManager serverManager) {
        this.toolId = toolId;
        this.toolInfo = toolInfo;
        this.serverManager = serverManager;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return toolId;
    }
    
    @Override
    public String getDescription() {
        return String.format("[MCP:%s] %s", 
            toolInfo.getServerName(), 
            toolInfo.getTool().getDescription());
    }
    
    @Override
    public String getPrompt() {
        return String.format("MCP 工具 %s 来自服务器 %s", 
            toolInfo.getTool().getName(),
            toolInfo.getServerName());
    }
    
    @Override
    public boolean isEnabled() {
        McpClient client = serverManager.getClient(toolInfo.getServerName());
        return client != null && client.isRunning();
    }
    
    @Override
    public boolean isReadOnly() {
        // MCP 工具默认不为只读，因为可能有副作用
        return false;
    }
    
    @Override
    public boolean isConcurrencySafe() {
        // MCP 工具默认不支持并发，需要逐个调用
        return false;
    }
    
    @Override
    public boolean needsPermissions() {
        // MCP 工具默认需要权限
        return true;
    }
    
    @Override
    public ToolResult call(Map<String, Object> parameters) {
        try {
            McpClient client = serverManager.getClient(toolInfo.getServerName());
            if (client == null) {
                return ToolResult.error("MCP server not running: " + toolInfo.getServerName());
            }
            
            if (!client.isRunning()) {
                return ToolResult.error("MCP server is not active: " + toolInfo.getServerName());
            }
            
            // 调用 MCP 工具
            McpCallToolResult result = client.callTool(
                toolInfo.getTool().getName(),
                parameters
            );
            
            // 转换结果
            if (Boolean.TRUE.equals(result.getIsError())) {
                String errorMessage = extractTextFromContent(result.getContent());
                return ToolResult.error(errorMessage);
            }
            
            String output = extractTextFromContent(result.getContent());
            return ToolResult.success(output);
            
        } catch (IOException e) {
            logger.error("Failed to execute MCP tool: {}", toolId, e);
            return ToolResult.error("MCP tool execution failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error executing MCP tool: {}", toolId, e);
            return ToolResult.error("Unexpected error: " + e.getMessage());
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        return String.format("🔧 [MCP:%s] 调用工具: %s", 
            toolInfo.getServerName(),
            toolInfo.getTool().getName());
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return String.format("✅ [MCP:%s] 工具执行成功: %s", 
                toolInfo.getServerName(),
                toolInfo.getTool().getName());
        } else {
            return String.format("❌ [MCP:%s] 工具执行失败: %s - %s", 
                toolInfo.getServerName(),
                toolInfo.getTool().getName(),
                result.getError());
        }
    }
    
    /**
     * 从 MCP 内容列表中提取文本
     */
    private String extractTextFromContent(java.util.List<McpContent> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        return content.stream()
            .filter(c -> "text".equals(c.getType()))
            .map(McpContent::getText)
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * 获取工具的输入模式（JSON Schema）
     */
    public Map<String, Object> getInputSchema() {
        return toolInfo.getTool().getInputSchema();
    }
    
    /**
     * 获取服务器名称
     */
    public String getServerName() {
        return toolInfo.getServerName();
    }
    
    /**
     * 获取原始工具名称
     */
    public String getOriginalToolName() {
        return toolInfo.getTool().getName();
    }
}
