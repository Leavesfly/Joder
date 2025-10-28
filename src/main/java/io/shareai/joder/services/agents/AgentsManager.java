package io.shareai.joder.services.agents;

import io.shareai.joder.domain.AgentConfig;
import io.shareai.joder.domain.AgentConfig.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Agents 管理器
 * 负责加载、管理和维护所有 Agent 配置
 */
@Singleton
public class AgentsManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentsManager.class);
    
    private static final String AGENTS_DIR = ".joder/agents";
    private static final String GLOBAL_AGENTS_DIR = ".config/joder/agents";
    
    private final Map<String, AgentConfig> agents;
    private final Path projectAgentsPath;
    private final Path globalAgentsPath;
    
    @Inject
    public AgentsManager(@io.shareai.joder.WorkingDirectory String workingDir) {
        this.agents = new LinkedHashMap<>();
        this.projectAgentsPath = Paths.get(workingDir, AGENTS_DIR);
        this.globalAgentsPath = Paths.get(System.getProperty("user.home"), GLOBAL_AGENTS_DIR);
        
        loadAgents();
    }
    
    /**
     * 加载所有 Agent 配置
     */
    private void loadAgents() {
        // 1. 加载内置代理
        loadBuiltinAgents();
        
        // 2. 加载全局代理
        loadAgentsFromDirectory(globalAgentsPath, AgentType.USER);
        
        // 3. 加载项目代理
        loadAgentsFromDirectory(projectAgentsPath, AgentType.PROJECT);
        
        logger.info("Loaded {} agents", agents.size());
    }
    
    /**
     * 加载内置代理
     */
    private void loadBuiltinAgents() {
        // 通用代理
        AgentConfig generalPurpose = AgentConfig.createGeneralPurpose();
        agents.put(generalPurpose.getName(), generalPurpose);
        
        // 代码专家
        AgentConfig codeExpert = AgentConfig.createCodeExpert();
        agents.put(codeExpert.getName(), codeExpert);
        
        logger.debug("Loaded {} builtin agents", 2);
    }
    
    /**
     * 从目录加载代理
     */
    private void loadAgentsFromDirectory(Path directory, AgentType type) {
        if (!Files.exists(directory)) {
            return;
        }
        
        try (Stream<Path> paths = Files.walk(directory, 1)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md"))
                 .forEach(path -> {
                     try {
                         AgentConfig agent = AgentsParser.parse(path);
                         agent.setType(type);
                         
                         if (agent.isValid()) {
                             agents.put(agent.getName(), agent);
                             logger.debug("Loaded {} agent: {}", type, agent.getName());
                         } else {
                             logger.warn("Invalid agent config: {}", path);
                         }
                     } catch (IOException e) {
                         logger.error("Failed to load agent from: {}", path, e);
                     }
                 });
        } catch (IOException e) {
            logger.error("Failed to scan agents directory: {}", directory, e);
        }
    }
    
    /**
     * 获取所有代理
     */
    public Map<String, AgentConfig> getAllAgents() {
        return new LinkedHashMap<>(agents);
    }
    
    /**
     * 根据名称获取代理
     */
    public Optional<AgentConfig> getAgent(String name) {
        return Optional.ofNullable(agents.get(name));
    }
    
    /**
     * 根据类型获取代理列表
     */
    public List<AgentConfig> getAgentsByType(AgentType type) {
        List<AgentConfig> result = new ArrayList<>();
        for (AgentConfig agent : agents.values()) {
            if (agent.getType() == type) {
                result.add(agent);
            }
        }
        return result;
    }
    
    /**
     * 创建新代理
     */
    public void createAgent(AgentConfig agent, boolean isProject) throws IOException {
        if (!agent.isValid()) {
            throw new IllegalArgumentException("Invalid agent config");
        }
        
        Path targetDir = isProject ? projectAgentsPath : globalAgentsPath;
        Path filePath = targetDir.resolve(agent.getName() + ".md");
        
        if (Files.exists(filePath)) {
            throw new IOException("Agent already exists: " + agent.getName());
        }
        
        AgentsParser.save(agent, filePath);
        
        agent.setType(isProject ? AgentType.PROJECT : AgentType.USER);
        agent.setFilePath(filePath.toString());
        agents.put(agent.getName(), agent);
        
        logger.info("Created agent: {} at {}", agent.getName(), filePath);
    }
    
    /**
     * 更新代理
     */
    public void updateAgent(AgentConfig agent) throws IOException {
        if (!agent.isValid()) {
            throw new IllegalArgumentException("Invalid agent config");
        }
        
        AgentConfig existing = agents.get(agent.getName());
        if (existing == null) {
            throw new IOException("Agent not found: " + agent.getName());
        }
        
        if (existing.getType() == AgentType.BUILTIN) {
            throw new IOException("Cannot modify builtin agent");
        }
        
        Path filePath = Paths.get(existing.getFilePath());
        AgentsParser.save(agent, filePath);
        
        agent.setType(existing.getType());
        agent.setFilePath(existing.getFilePath());
        agents.put(agent.getName(), agent);
        
        logger.info("Updated agent: {}", agent.getName());
    }
    
    /**
     * 删除代理
     */
    public void deleteAgent(String name) throws IOException {
        AgentConfig agent = agents.get(name);
        if (agent == null) {
            throw new IOException("Agent not found: " + name);
        }
        
        if (agent.getType() == AgentType.BUILTIN) {
            throw new IOException("Cannot delete builtin agent");
        }
        
        Path filePath = Paths.get(agent.getFilePath());
        Files.deleteIfExists(filePath);
        
        agents.remove(name);
        logger.info("Deleted agent: {}", name);
    }
    
    /**
     * 重新加载所有代理
     */
    public void reload() {
        agents.clear();
        loadAgents();
    }
    
    /**
     * 检查代理是否存在
     */
    public boolean hasAgent(String name) {
        return agents.containsKey(name);
    }
    
    /**
     * 初始化 agents 目录
     */
    public void initializeAgentsDirectory(boolean isProject) throws IOException {
        Path targetDir = isProject ? projectAgentsPath : globalAgentsPath;
        
        if (Files.exists(targetDir)) {
            logger.info("Agents directory already exists: {}", targetDir);
            return;
        }
        
        Files.createDirectories(targetDir);
        
        // 创建示例 agent
        AgentConfig example = new AgentConfig();
        example.setName("example");
        example.setDescription("示例代理配置");
        example.setModel("claude-3-5-sonnet");
        example.setColor("purple");
        example.setTools(List.of("FileRead", "FileWrite"));
        example.setSystemPrompt("""
                你是一个示例 AI 代理。
                
                请根据你的专业知识帮助用户完成任务。
                """);
        
        Path examplePath = targetDir.resolve("example.md");
        AgentsParser.save(example, examplePath);
        
        logger.info("Initialized agents directory at: {}", targetDir);
    }
}
