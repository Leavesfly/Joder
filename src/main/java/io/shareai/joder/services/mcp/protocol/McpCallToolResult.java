package io.shareai.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MCP 工具调用结果
 */
public class McpCallToolResult {
    
    @JsonProperty("content")
    private List<McpContent> content;
    
    @JsonProperty("isError")
    private Boolean isError;
    
    public McpCallToolResult() {
    }
    
    public McpCallToolResult(List<McpContent> content) {
        this.content = content;
        this.isError = false;
    }
    
    public McpCallToolResult(List<McpContent> content, Boolean isError) {
        this.content = content;
        this.isError = isError;
    }
    
    public List<McpContent> getContent() {
        return content;
    }
    
    public void setContent(List<McpContent> content) {
        this.content = content;
    }
    
    public Boolean getIsError() {
        return isError;
    }
    
    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
}
