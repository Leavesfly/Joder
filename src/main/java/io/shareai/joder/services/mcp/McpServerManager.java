package io.shareai.joder.services.mcp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import io.shareai.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 服务器管理器
 * 负责加载、启动、停止和管理 MCP 服务器
 */
@Singleton
public class McpServerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(McpServerManager.class);
    
    private final ConfigManager configManager;
    private final Map<String, McpClient> clients;
    private final Map<String, McpServerConfig> configs;
    
    @Inject
    public McpServerManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.clients = new ConcurrentHashMap<>();
        this.configs = new ConcurrentHashMap<>();
        
        loadConfigs();
    }
    
    /**
     * 从配置文件加载 MCP 服务器配置
     */
    private void loadConfigs() {
        try {
            if (!configManager.hasPath("joder.mcp.servers")) {
                logger.info("No MCP servers configured");
                return;
            }
            
            ConfigObject serversConfig = configManager.getConfig().getObject("joder.mcp.servers");
            
            for (String serverName : serversConfig.keySet()) {
                try {
                    Config serverConfig = serversConfig.toConfig().getConfig(serverName);
                    
                    McpServerConfig config = new McpServerConfig();
                    config.setName(serverName);
                    config.setCommand(serverConfig.getString("command"));
                    
                    if (serverConfig.hasPath("args")) {
                        config.setArgs(serverConfig.getStringList("args"));
                    }
                    
                    if (serverConfig.hasPath("env")) {
                        Map<String, String> env = new HashMap<>();
                        Config envConfig = serverConfig.getConfig("env");
                        envConfig.entrySet().forEach(entry -> 
                            env.put(entry.getKey(), entry.getValue().unwrapped().toString())
                        );
                        config.setEnv(env);
                    }
                    
                    if (serverConfig.hasPath("enabled")) {
                        config.setEnabled(serverConfig.getBoolean("enabled"));
                    }
                    
                    configs.put(serverName, config);
                    logger.info("Loaded MCP server config: {}", serverName);
                    
                } catch (Exception e) {
                    logger.error("Failed to load MCP server config: {}", serverName, e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to load MCP server configs", e);
        }
    }
    
    /**
     * 启动指定的 MCP 服务器
     */
    public void startServer(String serverName) throws IOException {
        McpServerConfig config = configs.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("MCP server not found: " + serverName);
        }
        
        if (!config.isEnabled()) {
            throw new IllegalStateException("MCP server is disabled: " + serverName);
        }
        
        if (clients.containsKey(serverName)) {
            McpClient existingClient = clients.get(serverName);
            if (existingClient.isRunning()) {
                logger.warn("MCP server already running: {}", serverName);
                return;
            }
        }
        
        McpClient client = new McpClient(config);
        client.start();
        clients.put(serverName, client);
        
        logger.info("Started MCP server: {}", serverName);
    }
    
    /**
     * 停止指定的 MCP 服务器
     */
    public void stopServer(String serverName) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            logger.warn("MCP server not found: {}", serverName);
            return;
        }
        
        client.stop();
        clients.remove(serverName);
        
        logger.info("Stopped MCP server: {}", serverName);
    }
    
    /**
     * 启动所有已启用的 MCP 服务器
     */
    public void startAllServers() {
        configs.values().stream()
            .filter(McpServerConfig::isEnabled)
            .forEach(config -> {
                try {
                    startServer(config.getName());
                } catch (Exception e) {
                    logger.error("Failed to start MCP server: {}", config.getName(), e);
                }
            });
    }
    
    /**
     * 停止所有运行中的 MCP 服务器
     */
    public void stopAllServers() {
        new ArrayList<>(clients.keySet()).forEach(this::stopServer);
    }
    
    /**
     * 获取指定的 MCP 客户端
     */
    public McpClient getClient(String serverName) {
        return clients.get(serverName);
    }
    
    /**
     * 获取所有运行中的客户端
     */
    public Map<String, McpClient> getRunningClients() {
        return new HashMap<>(clients);
    }
    
    /**
     * 获取所有服务器配置
     */
    public Map<String, McpServerConfig> getConfigs() {
        return new HashMap<>(configs);
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isServerRunning(String serverName) {
        McpClient client = clients.get(serverName);
        return client != null && client.isRunning();
    }
    
    /**
     * 启用服务器
     */
    public void enableServer(String serverName) {
        McpServerConfig config = configs.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("MCP server not found: " + serverName);
        }
        config.setEnabled(true);
        logger.info("Enabled MCP server: {}", serverName);
    }
    
    /**
     * 禁用服务器
     */
    public void disableServer(String serverName) {
        McpServerConfig config = configs.get(serverName);
        if (config == null) {
            throw new IllegalArgumentException("MCP server not found: " + serverName);
        }
        
        // 如果正在运行，先停止
        if (isServerRunning(serverName)) {
            stopServer(serverName);
        }
        
        config.setEnabled(false);
        logger.info("Disabled MCP server: {}", serverName);
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfigs() {
        stopAllServers();
        configs.clear();
        loadConfigs();
        logger.info("Reloaded MCP server configs");
    }
}
