package io.shareai.joder.services.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 错误
 */
public class JsonRpcError {
    
    @JsonProperty("code")
    private int code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    public JsonRpcError() {
    }
    
    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public JsonRpcError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}
