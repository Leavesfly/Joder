package io.shareai.joder.tools;

/**
 * 工具执行结果
 */
public class ToolResult {
    
    private final boolean success;
    private final String output;
    private final String error;
    
    private ToolResult(boolean success, String output, String error) {
        this.success = success;
        this.output = output;
        this.error = error;
    }
    
    /**
     * 创建成功结果
     */
    public static ToolResult success(String output) {
        return new ToolResult(true, output, null);
    }
    
    /**
     * 创建错误结果
     */
    public static ToolResult error(String error) {
        return new ToolResult(false, null, error);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getOutput() {
        return output;
    }
    
    public String getError() {
        return error;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "ToolResult{success=true, output='" + output + "'}";
        } else {
            return "ToolResult{success=false, error='" + error + "'}";
        }
    }
}
