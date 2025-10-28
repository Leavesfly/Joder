package io.leavesfly.joder.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.util.exceptions.ConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 配置管理工具类
 * 对应 Kode 的 config.ts
 */
public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 项目配置
     */
    public static class ProjectConfig {
        private List<String> allowedTools = new ArrayList<>();
        private Map<String, String> context = new HashMap<>();
        private List<String> history = new ArrayList<>();
        private boolean dontCrawlDirectory = false;
        private boolean enableArchitectTool = false;
        private List<String> mcpContextUris = new ArrayList<>();
        private Map<String, Object> mcpServers = new HashMap<>();

        public List<String> getAllowedTools() { return allowedTools; }
        public void setAllowedTools(List<String> tools) { this.allowedTools = tools; }

        public Map<String, String> getContext() { return context; }
        public void setContext(Map<String, String> ctx) { this.context = ctx; }

        public List<String> getHistory() { return history; }
        public void addToHistory(String entry) { this.history.add(entry); }

        public boolean isDontCrawlDirectory() { return dontCrawlDirectory; }
        public void setDontCrawlDirectory(boolean value) { this.dontCrawlDirectory = value; }

        public boolean isEnableArchitectTool() { return enableArchitectTool; }
        public void setEnableArchitectTool(boolean value) { this.enableArchitectTool = value; }

        public List<String> getMcpContextUris() { return mcpContextUris; }
        public Map<String, Object> getMcpServers() { return mcpServers; }
    }

    /**
     * 全局配置
     */
    public static class GlobalConfig {
        private int numStartups = 0;
        private String theme = "dark";
        private boolean verbose = false;
        private String primaryProvider = "anthropic";
        private String userID;
        private Map<String, ProjectConfig> projects = new HashMap<>();

        public int getNumStartups() { return numStartups; }
        public void setNumStartups(int count) { this.numStartups = count; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public boolean isVerbose() { return verbose; }
        public void setVerbose(boolean verbose) { this.verbose = verbose; }

        public String getPrimaryProvider() { return primaryProvider; }
        public void setPrimaryProvider(String provider) { this.primaryProvider = provider; }

        public String getUserID() { return userID; }
        public void setUserID(String id) { this.userID = id; }

        public Map<String, ProjectConfig> getProjects() { return projects; }
        public ProjectConfig getProject(String path) {
            return projects.getOrDefault(path, new ProjectConfig());
        }
    }

    /**
     * 从文件加载配置
     */
    public static GlobalConfig loadGlobalConfig(String configFilePath) throws ConfigParseException {
        Path path = Paths.get(configFilePath);

        if (!Files.exists(path)) {
            logger.warn("配置文件不存在: {}", configFilePath);
            return new GlobalConfig();
        }

        try {
            String content = new String(Files.readAllBytes(path));
            JsonNode json = objectMapper.readTree(content);

            GlobalConfig config = new GlobalConfig();
            if (json.has("numStartups")) {
                config.setNumStartups(json.get("numStartups").asInt());
            }
            if (json.has("theme")) {
                config.setTheme(json.get("theme").asText());
            }
            if (json.has("verbose")) {
                config.setVerbose(json.get("verbose").asBoolean());
            }
            if (json.has("userID")) {
                config.setUserID(json.get("userID").asText());
            }

            logger.info("配置文件加载成功: {}", configFilePath);
            return config;

        } catch (Exception e) {
            throw new ConfigParseException(
                "配置文件解析失败: " + e.getMessage(),
                configFilePath,
                new GlobalConfig()
            );
        }
    }

    /**
     * 保存全局配置到文件
     */
    public static void saveGlobalConfig(GlobalConfig config, String configFilePath) throws Exception {
        Path path = Paths.get(configFilePath);

        // 确保目录存在
        Files.createDirectories(path.getParent());

        // 转换为 JSON 并保存
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        Files.write(path, jsonContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        logger.info("配置文件已保存: {}", configFilePath);
    }

    /**
     * 加载项目配置
     */
    public static ProjectConfig loadProjectConfig(String projectPath) {
        String configFile = getProjectConfigFile(projectPath);
        try {
            if (Files.exists(Paths.get(configFile))) {
                String content = new String(Files.readAllBytes(Paths.get(configFile)));
                JsonNode json = objectMapper.readTree(content);

                ProjectConfig config = new ProjectConfig();
                if (json.has("allowedTools") && json.get("allowedTools").isArray()) {
                    json.get("allowedTools").forEach(tool ->
                        config.allowedTools.add(tool.asText())
                    );
                }
                return config;
            }
        } catch (Exception e) {
            logger.warn("加载项目配置失败: {}", projectPath, e);
        }

        return new ProjectConfig();
    }

    /**
     * 获取项目配置文件路径
     */
    public static String getProjectConfigFile(String projectPath) {
        String userHome = System.getProperty("user.home");
        return userHome + "/.kode/projects/" + projectPath.replace("/", "_") + ".json";
    }

    /**
     * 获取全局配置文件路径
     */
    public static String getGlobalConfigFile() {
        String userHome = System.getProperty("user.home");
        return userHome + "/.kode/config.json";
    }

    /**
     * 合并配置对象
     */
    public static <T> T mergeConfigs(T defaultConfig, T userConfig) {
        // 简化实现，实际应使用更复杂的合并逻辑
        return userConfig != null ? userConfig : defaultConfig;
    }

    /**
     * 验证配置有效性
     */
    public static boolean isValidConfig(GlobalConfig config) {
        // 基本验证
        if (config.getTheme() == null || config.getTheme().isEmpty()) {
            return false;
        }
        if (!config.getTheme().matches("^(dark|light)$")) {
            return false;
        }
        return true;
    }

    /**
     * 获取配置值（支持默认值）
     */
    public static String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * 获取配置布尔值
     */
    public static boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * 获取配置整数值
     */
    public static int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
