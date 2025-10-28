package io.leavesfly.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 内容块
 */
public class McpContent {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("text")
    private String text;
    
    public McpContent() {
    }
    
    public McpContent(String type, String text) {
        this.type = type;
        this.text = text;
    }
    
    public static McpContent text(String text) {
        return new McpContent("text", text);
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}
