package io.leavesfly.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MCP 工具列表响应
 */
public class McpListToolsResult {
    
    @JsonProperty("tools")
    private List<McpTool> tools;
    
    public McpListToolsResult() {
    }
    
    public McpListToolsResult(List<McpTool> tools) {
        this.tools = tools;
    }
    
    public List<McpTool> getTools() {
        return tools;
    }
    
    public void setTools(List<McpTool> tools) {
        this.tools = tools;
    }
}
