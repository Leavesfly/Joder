package io.leavesfly.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 消息基类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class JsonRpcMessage {
    
    @JsonProperty("jsonrpc")
    private final String jsonrpc = "2.0";
    
    public String getJsonrpc() {
        return jsonrpc;
    }
}
