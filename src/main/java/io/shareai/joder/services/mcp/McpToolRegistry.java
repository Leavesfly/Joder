package io.shareai.joder.services.mcp;

import io.shareai.joder.services.mcp.protocol.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册表
 * 发现和管理来自 MCP 服务器的工具
 */
@Singleton
public class McpToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(McpToolRegistry.class);
    
    private final McpServerManager serverManager;
    private final Map<String, McpToolInfo> tools;
    
    @Inject
    public McpToolRegistry(McpServerManager serverManager) {
        this.serverManager = serverManager;
        this.tools = new ConcurrentHashMap<>();
    }
    
    /**
     * 从所有运行中的 MCP 服务器发现工具
     */
    public void discoverTools() {
        tools.clear();
        
        Map<String, McpClient> clients = serverManager.getRunningClients();
        for (Map.Entry<String, McpClient> entry : clients.entrySet()) {
            String serverName = entry.getKey();
            McpClient client = entry.getValue();
            
            try {
                List<McpTool> serverTools = client.listTools();
                for (McpTool tool : serverTools) {
                    String toolId = serverName + "." + tool.getName();
                    McpToolInfo toolInfo = new McpToolInfo(serverName, tool);
                    tools.put(toolId, toolInfo);
                    logger.debug("Discovered tool: {} from server: {}", tool.getName(), serverName);
                }
                logger.info("Discovered {} tools from server: {}", serverTools.size(), serverName);
            } catch (IOException e) {
                logger.error("Failed to discover tools from server: {}", serverName, e);
            }
        }
        
        logger.info("Total tools discovered: {}", tools.size());
    }
    
    /**
     * 从指定服务器发现工具
     */
    public void discoverToolsFromServer(String serverName) {
        McpClient client = serverManager.getClient(serverName);
        if (client == null) {
            logger.warn("MCP server not running: {}", serverName);
            return;
        }
        
        // 移除该服务器的旧工具
        tools.entrySet().removeIf(entry -> entry.getValue().getServerName().equals(serverName));
        
        try {
            List<McpTool> serverTools = client.listTools();
            for (McpTool tool : serverTools) {
                String toolId = serverName + "." + tool.getName();
                McpToolInfo toolInfo = new McpToolInfo(serverName, tool);
                tools.put(toolId, toolInfo);
            }
            logger.info("Discovered {} tools from server: {}", serverTools.size(), serverName);
        } catch (IOException e) {
            logger.error("Failed to discover tools from server: {}", serverName, e);
        }
    }
    
    /**
     * 获取所有工具
     */
    public Map<String, McpToolInfo> getAllTools() {
        return new HashMap<>(tools);
    }
    
    /**
     * 获取指定工具信息
     */
    public McpToolInfo getTool(String toolId) {
        return tools.get(toolId);
    }
    
    /**
     * 获取指定服务器的所有工具
     */
    public List<McpToolInfo> getToolsFromServer(String serverName) {
        List<McpToolInfo> result = new ArrayList<>();
        for (McpToolInfo toolInfo : tools.values()) {
            if (toolInfo.getServerName().equals(serverName)) {
                result.add(toolInfo);
            }
        }
        return result;
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolId) {
        return tools.containsKey(toolId);
    }
    
    /**
     * 清空注册表
     */
    public void clear() {
        tools.clear();
        logger.info("Cleared MCP tool registry");
    }
    
    /**
     * MCP 工具信息
     */
    public static class McpToolInfo {
        private final String serverName;
        private final McpTool tool;
        
        public McpToolInfo(String serverName, McpTool tool) {
            this.serverName = serverName;
            this.tool = tool;
        }
        
        public String getServerName() {
            return serverName;
        }
        
        public McpTool getTool() {
            return tool;
        }
        
        public String getFullName() {
            return serverName + "." + tool.getName();
        }
    }
}
