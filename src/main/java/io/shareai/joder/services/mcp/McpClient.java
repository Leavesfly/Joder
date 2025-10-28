package io.shareai.joder.services.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.services.mcp.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 客户端
 * 实现 JSON-RPC over stdio 通信
 */
public class McpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(McpClient.class);
    
    private final McpServerConfig config;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdCounter;
    private final Map<Object, CompletableFuture<JsonRpcResponse>> pendingRequests;
    private final ExecutorService executorService;
    
    private Process process;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private Thread outputReaderThread;
    private volatile boolean running;
    
    public McpClient(McpServerConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.requestIdCounter = new AtomicLong(1);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    /**
     * 启动 MCP 服务器进程
     */
    public void start() throws IOException {
        if (running) {
            logger.warn("MCP client already running: {}", config.getName());
            return;
        }
        
        logger.info("Starting MCP server: {}", config.getName());
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(config.getCommand());
        if (config.getArgs() != null) {
            pb.command().addAll(config.getArgs());
        }
        
        if (config.getEnv() != null) {
            pb.environment().putAll(config.getEnv());
        }
        
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        
        process = pb.start();
        processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        running = true;
        
        // 启动输出读取线程
        startOutputReader();
        
        // 初始化连接
        initialize();
        
        logger.info("MCP server started: {}", config.getName());
    }
    
    /**
     * 停止 MCP 服务器
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        logger.info("Stopping MCP server: {}", config.getName());
        
        running = false;
        
        // 关闭所有待处理的请求
        pendingRequests.values().forEach(future -> 
            future.completeExceptionally(new IOException("Client stopped"))
        );
        pendingRequests.clear();
        
        // 关闭输入输出流
        try {
            if (processInput != null) {
                processInput.close();
            }
            if (processOutput != null) {
                processOutput.close();
            }
        } catch (IOException e) {
            logger.error("Error closing streams", e);
        }
        
        // 销毁进程
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        
        logger.info("MCP server stopped: {}", config.getName());
    }
    
    /**
     * 初始化连接
     */
    private void initialize() throws IOException {
        JsonRpcRequest request = new JsonRpcRequest(
            requestIdCounter.getAndIncrement(),
            "initialize",
            Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                    "name", "joder",
                    "version", "1.0.0"
                )
            )
        );
        
        JsonRpcResponse response = sendRequest(request).join();
        if (response.hasError()) {
            throw new IOException("Initialize failed: " + response.getError().getMessage());
        }
        
        logger.debug("MCP client initialized: {}", config.getName());
    }
    
    /**
     * 列出可用工具
     */
    public List<McpTool> listTools() throws IOException {
        JsonRpcRequest request = new JsonRpcRequest(
            requestIdCounter.getAndIncrement(),
            "tools/list",
            Map.of()
        );
        
        JsonRpcResponse response = sendRequest(request).join();
        if (response.hasError()) {
            throw new IOException("List tools failed: " + response.getError().getMessage());
        }
        
        McpListToolsResult result = objectMapper.convertValue(
            response.getResult(),
            McpListToolsResult.class
        );
        
        return result.getTools();
    }
    
    /**
     * 调用工具
     */
    public McpCallToolResult callTool(String toolName, Map<String, Object> arguments) throws IOException {
        McpCallToolParams params = new McpCallToolParams(toolName, arguments);
        
        JsonRpcRequest request = new JsonRpcRequest(
            requestIdCounter.getAndIncrement(),
            "tools/call",
            params
        );
        
        JsonRpcResponse response = sendRequest(request).join();
        if (response.hasError()) {
            throw new IOException("Call tool failed: " + response.getError().getMessage());
        }
        
        return objectMapper.convertValue(
            response.getResult(),
            McpCallToolResult.class
        );
    }
    
    /**
     * 发送请求
     */
    private CompletableFuture<JsonRpcResponse> sendRequest(JsonRpcRequest request) {
        CompletableFuture<JsonRpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getId(), future);
        
        try {
            String json = objectMapper.writeValueAsString(request);
            synchronized (processInput) {
                processInput.write(json);
                processInput.newLine();
                processInput.flush();
            }
            logger.debug("Sent request: {}", json);
        } catch (IOException e) {
            future.completeExceptionally(e);
            pendingRequests.remove(request.getId());
        }
        
        return future;
    }
    
    /**
     * 启动输出读取线程
     */
    private void startOutputReader() {
        outputReaderThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = processOutput.readLine()) != null) {
                    logger.debug("Received: {}", line);
                    handleResponse(line);
                }
            } catch (IOException e) {
                if (running) {
                    logger.error("Error reading output from MCP server", e);
                }
            }
        }, "MCP-Output-Reader-" + config.getName());
        
        outputReaderThread.setDaemon(true);
        outputReaderThread.start();
    }
    
    /**
     * 处理响应
     */
    private void handleResponse(String line) {
        try {
            JsonRpcResponse response = objectMapper.readValue(line, JsonRpcResponse.class);
            CompletableFuture<JsonRpcResponse> future = pendingRequests.remove(response.getId());
            if (future != null) {
                future.complete(response);
            }
        } catch (Exception e) {
            logger.error("Error handling response", e);
        }
    }
    
    public boolean isRunning() {
        return running && process != null && process.isAlive();
    }
    
    public McpServerConfig getConfig() {
        return config;
    }
}
