package io.shareai.joder.services.agents;

import io.shareai.joder.domain.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AGENTS.md 文件解析器
 * 支持 YAML frontmatter + Markdown 格式
 */
public class AgentsParser {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentsParser.class);
    
    // YAML frontmatter 的正则表达式
    private static final Pattern FRONTMATTER_PATTERN = 
            Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);
    
    /**
     * 解析 AGENTS.md 文件
     */
    public static AgentConfig parse(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("Agent file not found: " + filePath);
        }
        
        String content = Files.readString(filePath);
        return parseContent(content, filePath);
    }
    
    /**
     * 解析文件内容
     */
    public static AgentConfig parseContent(String content, Path filePath) {
        AgentConfig agent = new AgentConfig();
        agent.setFilePath(filePath != null ? filePath.toString() : null);
        
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content.trim());
        
        if (matcher.matches()) {
            // 包含 YAML frontmatter
            String frontmatter = matcher.group(1);
            String markdown = matcher.group(2);
            
            parseYamlFrontmatter(frontmatter, agent);
            agent.setSystemPrompt(markdown.trim());
        } else {
            // 纯 Markdown，作为系统提示词
            agent.setSystemPrompt(content.trim());
            
            // 从文件名推断代理名称
            if (filePath != null) {
                String fileName = filePath.getFileName().toString();
                agent.setName(fileName.replace(".md", "").replace("AGENTS", "").replace("CLAUDE", ""));
            }
        }
        
        return agent;
    }
    
    /**
     * 解析 YAML frontmatter
     */
    private static void parseYamlFrontmatter(String yaml, AgentConfig agent) {
        String[] lines = yaml.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            
            String key = parts[0].trim();
            String value = parts[1].trim();
            
            // 移除引号
            value = value.replaceAll("^['\"]|['\"]$", "");
            
            switch (key.toLowerCase()) {
                case "name":
                    agent.setName(value);
                    break;
                case "description":
                    agent.setDescription(value);
                    break;
                case "model":
                    agent.setModel(value);
                    break;
                case "color":
                    agent.setColor(value);
                    break;
                case "tools":
                    agent.setTools(parseArray(value));
                    break;
            }
        }
    }
    
    /**
     * 解析数组值
     */
    private static List<String> parseArray(String value) {
        // 支持 [tool1, tool2] 或 tool1, tool2 格式
        value = value.replaceAll("[\\[\\]]", "").trim();
        
        if (value.isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] items = value.split(",");
        List<String> result = new ArrayList<>();
        
        for (String item : items) {
            String cleaned = item.trim().replaceAll("^['\"]|['\"]$", "");
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        
        return result;
    }
    
    /**
     * 生成 AGENTS.md 内容
     */
    public static String generate(AgentConfig agent) {
        StringBuilder sb = new StringBuilder();
        
        // YAML frontmatter
        sb.append("---\n");
        sb.append("name: ").append(agent.getName()).append("\n");
        
        if (agent.getDescription() != null) {
            sb.append("description: ").append(agent.getDescription()).append("\n");
        }
        
        if (agent.getModel() != null) {
            sb.append("model: ").append(agent.getModel()).append("\n");
        }
        
        if (agent.getColor() != null) {
            sb.append("color: ").append(agent.getColor()).append("\n");
        }
        
        if (agent.getTools() != null && !agent.getTools().isEmpty()) {
            sb.append("tools: [");
            sb.append(String.join(", ", agent.getTools().stream()
                    .map(t -> "\"" + t + "\"")
                    .toArray(String[]::new)));
            sb.append("]\n");
        }
        
        sb.append("---\n\n");
        
        // System prompt
        if (agent.getSystemPrompt() != null) {
            sb.append(agent.getSystemPrompt());
        }
        
        return sb.toString();
    }
    
    /**
     * 保存 Agent 配置到文件
     */
    public static void save(AgentConfig agent, Path filePath) throws IOException {
        String content = generate(agent);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
        logger.info("Saved agent config to: {}", filePath);
    }
}
