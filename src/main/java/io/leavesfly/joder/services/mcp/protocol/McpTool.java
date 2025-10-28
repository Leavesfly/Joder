package io.leavesfly.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP 工具定义
 */
public class McpTool {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;
    
    public McpTool() {
    }
    
    public McpTool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }
    
    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema;
    }
}
