package io.shareai.joder.services.mcp;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务器配置
 */
public class McpServerConfig {
    
    private String name;
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private boolean enabled;
    
    public McpServerConfig() {
        this.enabled = true;
    }
    
    public McpServerConfig(String name, String command, List<String> args) {
        this.name = name;
        this.command = command;
        this.args = args;
        this.enabled = true;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public List<String> getArgs() {
        return args;
    }
    
    public void setArgs(List<String> args) {
        this.args = args;
    }
    
    public Map<String, String> getEnv() {
        return env;
    }
    
    public void setEnv(Map<String, String> env) {
        this.env = env;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
