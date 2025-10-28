package io.leavesfly.joder.tools;

import io.leavesfly.joder.services.mcp.McpServerManager;
import io.leavesfly.joder.services.mcp.McpToolRegistry;
import io.leavesfly.joder.tools.mcp.McpToolAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 */
@Singleton
public class ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final Map<String, Tool> tools;
    private McpServerManager mcpServerManager;
    private McpToolRegistry mcpToolRegistry;
    
    public ToolRegistry() {
        this.tools = new ConcurrentHashMap<>();
    }
    
    /**
     * 设置 MCP 管理器（用于动态加载 MCP 工具）
     */
    public void setMcpManagers(McpServerManager serverManager, McpToolRegistry toolRegistry) {
        this.mcpServerManager = serverManager;
        this.mcpToolRegistry = toolRegistry;
    }
    
    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        String name = tool.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }
        
        tools.put(name, tool);
        logger.info("Registered tool: {}", name);
    }
    
    /**
     * 注册多个工具
     */
    public void registerTools(Tool... toolArray) {
        for (Tool tool : toolArray) {
            registerTool(tool);
        }
    }
    
    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * 获取所有工具名称
     */
    public Set<String> getToolNames() {
        return new HashSet<>(tools.keySet());
    }
    
    /**
     * 获取所有工具
     */
    public Collection<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * 获取已启用的工具
     */
    public List<Tool> getEnabledTools() {
        return tools.values().stream()
            .filter(Tool::isEnabled)
            .toList();
    }
    
    /**
     * 移除工具
     */
    public void removeTool(String name) {
        Tool removed = tools.remove(name);
        if (removed != null) {
            logger.info("Removed tool: {}", name);
        }
    }
    
    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
        logger.info("Cleared all tools");
    }
    
    /**
     * 从 MCP 服务器加载工具
     */
    public void loadMcpTools() {
        if (mcpServerManager == null || mcpToolRegistry == null) {
            logger.warn("MCP managers not configured, skipping MCP tool loading");
            return;
        }
        
        // 发现工具
        mcpToolRegistry.discoverTools();
        
        // 注册工具
        for (Map.Entry<String, McpToolRegistry.McpToolInfo> entry : mcpToolRegistry.getAllTools().entrySet()) {
            String toolId = entry.getKey();
            McpToolRegistry.McpToolInfo toolInfo = entry.getValue();
            
            McpToolAdapter adapter = new McpToolAdapter(toolId, toolInfo, mcpServerManager);
            registerTool(adapter);
        }
        
        logger.info("Loaded {} MCP tools", mcpToolRegistry.getAllTools().size());
    }
    
    /**
     * 获取工具数量
     */
    public int size() {
        return tools.size();
    }
}
