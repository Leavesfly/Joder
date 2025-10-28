package io.leavesfly.joder.services.commands;

import io.leavesfly.joder.domain.CustomCommand;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frontmatter 解析器
 * 
 * <p>从 Markdown 文件中提取和解析 YAML frontmatter，
 * 支持 Jekyll 风格的 --- 分隔符。
 * 
 * <p>支持的 YAML 特性：
 * <ul>
 *   <li>字符串值（带或不带引号）</li>
 *   <li>布尔值（true/false）</li>
 *   <li>数组（行内格式 [a, b] 和多行格式）</li>
 *   <li>注释（# 开头的行）</li>
 * </ul>
 * 
 * <p>示例：
 * <pre>
 * ---
 * name: my-command
 * description: "This is a command"
 * enabled: true
 * aliases: [cmd, c]
 * allowed-tools:
 *   - BashTool
 *   - FileRead
 * ---
 * Command content here...
 * </pre>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class FrontmatterParser {
    
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n?",
        Pattern.MULTILINE
    );
    
    private final Yaml yaml;
    
    public FrontmatterParser() {
        this.yaml = new Yaml();
    }
    
    /**
     * 解析结果类
     */
    public static class ParseResult {
        private final Map<String, Object> frontmatter;
        private final String content;
        
        public ParseResult(Map<String, Object> frontmatter, String content) {
            this.frontmatter = frontmatter;
            this.content = content;
        }
        
        public Map<String, Object> getFrontmatter() {
            return frontmatter;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    /**
     * 从 Markdown 内容中解析 frontmatter
     * 
     * @param markdownContent Markdown 文件的完整内容
     * @return 解析结果，包含 frontmatter 和正文内容
     */
    public ParseResult parse(String markdownContent) {
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            return new ParseResult(Collections.emptyMap(), markdownContent);
        }
        
        Matcher matcher = FRONTMATTER_PATTERN.matcher(markdownContent);
        
        if (!matcher.find()) {
            // 没有 frontmatter
            return new ParseResult(Collections.emptyMap(), markdownContent);
        }
        
        String yamlContent = matcher.group(1);
        String remainingContent = markdownContent.substring(matcher.end());
        
        try {
            Map<String, Object> frontmatter = parseYaml(yamlContent);
            return new ParseResult(frontmatter, remainingContent);
        } catch (Exception e) {
            // YAML 解析失败，返回空 frontmatter
            System.err.println("Warning: Failed to parse frontmatter: " + e.getMessage());
            return new ParseResult(Collections.emptyMap(), markdownContent);
        }
    }
    
    /**
     * 解析 YAML 内容
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            Object parsed = yaml.load(yamlContent);
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从 frontmatter 提取字符串值
     */
    public static String getString(Map<String, Object> frontmatter, String key) {
        Object value = frontmatter.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 从 frontmatter 提取布尔值
     */
    public static boolean getBoolean(Map<String, Object> frontmatter, String key, boolean defaultValue) {
        Object value = frontmatter.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * 从 frontmatter 提取字符串列表
     */
    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String, Object> frontmatter, String key) {
        Object value = frontmatter.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        
        if (value instanceof String) {
            // 处理逗号分隔的字符串
            String str = (String) value;
            if (str.trim().isEmpty()) {
                return Collections.emptyList();
            }
            String[] parts = str.split(",");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                result.add(part.trim());
            }
            return result;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 将 frontmatter 应用到 CustomCommand 对象
     */
    public static void applyToCustomCommand(Map<String, Object> frontmatter, CustomCommand command) {
        String name = getString(frontmatter, "name");
        if (name != null) {
            command.setName(name);
        }
        
        String description = getString(frontmatter, "description");
        if (description != null) {
            command.setDescription(description);
        }
        
        List<String> aliases = getStringList(frontmatter, "aliases");
        if (!aliases.isEmpty()) {
            command.setAliases(aliases);
        }
        
        command.setEnabled(getBoolean(frontmatter, "enabled", true));
        command.setHidden(getBoolean(frontmatter, "hidden", false));
        
        String progressMessage = getString(frontmatter, "progressMessage");
        if (progressMessage != null) {
            command.setProgressMessage(progressMessage);
        }
        
        List<String> argNames = getStringList(frontmatter, "argNames");
        if (!argNames.isEmpty()) {
            command.setArgNames(argNames);
        }
        
        List<String> allowedTools = getStringList(frontmatter, "allowed-tools");
        if (!allowedTools.isEmpty()) {
            command.setAllowedTools(allowedTools);
        }
    }
}
