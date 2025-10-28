package io.shareai.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP 工具调用参数
 */
public class McpCallToolParams {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("arguments")
    private Map<String, Object> arguments;
    
    public McpCallToolParams() {
    }
    
    public McpCallToolParams(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
    
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
