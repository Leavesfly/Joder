package io.shareai.joder.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 配置管理器
 * 支持多层级配置：命令行参数 > 项目配置 > 全局配置 > 默认配置
 */
@Singleton
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private static final String GLOBAL_CONFIG_DIR = ".config/joder";
    private static final String PROJECT_CONFIG_DIR = ".joder";
    private static final String CONFIG_FILE_NAME = "config.conf";
    
    private final Config config;
    private final Path globalConfigPath;
    private final Path projectConfigPath;
    
    public ConfigManager() {
        this(System.getProperty("user.dir"));
    }
    
    public ConfigManager(String workingDirectory) {
        this.globalConfigPath = getGlobalConfigPath();
        this.projectConfigPath = getProjectConfigPath(workingDirectory);
        this.config = loadConfiguration();
        
        logger.info("Configuration loaded successfully");
        logger.debug("Global config: {}", globalConfigPath);
        logger.debug("Project config: {}", projectConfigPath);
    }
    
    /**
     * 加载配置，按优先级合并
     */
    private Config loadConfiguration() {
        Config result = ConfigFactory.defaultReference();
        
        // 加载全局配置
        if (Files.exists(globalConfigPath)) {
            try {
                Config globalConfig = ConfigFactory.parseFile(globalConfigPath.toFile());
                result = globalConfig.withFallback(result);
                logger.debug("Loaded global config from {}", globalConfigPath);
            } catch (Exception e) {
                logger.warn("Failed to load global config: {}", e.getMessage());
            }
        }
        
        // 加载项目配置
        if (Files.exists(projectConfigPath)) {
            try {
                Config projectConfig = ConfigFactory.parseFile(projectConfigPath.toFile());
                result = projectConfig.withFallback(result);
                logger.debug("Loaded project config from {}", projectConfigPath);
            } catch (Exception e) {
                logger.warn("Failed to load project config: {}", e.getMessage());
            }
        }
        
        // 加载默认配置（从资源文件）
        Config defaultConfig = ConfigFactory.load();
        result = result.withFallback(defaultConfig);
        
        return result.resolve();
    }
    
    /**
     * 获取全局配置文件路径
     */
    private Path getGlobalConfigPath() {
        String homeDir = System.getProperty("user.home");
        return Paths.get(homeDir, GLOBAL_CONFIG_DIR, CONFIG_FILE_NAME);
    }
    
    /**
     * 获取项目配置文件路径
     */
    private Path getProjectConfigPath(String workingDirectory) {
        return Paths.get(workingDirectory, PROJECT_CONFIG_DIR, CONFIG_FILE_NAME);
    }
    
    /**
     * 获取完整配置对象
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * 获取字符串配置
     */
    public String getString(String path) {
        return config.getString(path);
    }
    
    /**
     * 获取字符串配置（带默认值）
     */
    public String getString(String path, String defaultValue) {
        return config.hasPath(path) ? config.getString(path) : defaultValue;
    }
    
    /**
     * 获取整数配置
     */
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    /**
     * 获取整数配置（带默认值）
     */
    public int getInt(String path, int defaultValue) {
        return config.hasPath(path) ? config.getInt(path) : defaultValue;
    }
    
    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
    
    /**
     * 获取布尔配置（带默认值）
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
    }
    
    /**
     * 获取双精度浮点配置
     */
    public double getDouble(String path) {
        return config.getDouble(path);
    }
    
    /**
     * 获取双精度浮点配置（带默认值）
     */
    public double getDouble(String path, double defaultValue) {
        return config.hasPath(path) ? config.getDouble(path) : defaultValue;
    }
    
    /**
     * 获取字符串列表配置
     */
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    /**
     * 获取字符串列表配置（带默认值）
     */
    public List<String> getStringList(String path, List<String> defaultValue) {
        return config.hasPath(path) ? config.getStringList(path) : defaultValue;
    }
    
    /**
     * 检查配置路径是否存在
     */
    public boolean hasPath(String path) {
        return config.hasPath(path);
    }
    
    /**
     * 获取配置对象
     */
    public Config getConfig(String path) {
        return config.getConfig(path);
    }
    
    /**
     * 保存全局配置
     */
    public void saveGlobalConfig(Map<String, Object> settings) {
        // TODO: 实现配置保存逻辑
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    /**
     * 保存项目配置
     */
    public void saveProjectConfig(Map<String, Object> settings) {
        // TODO: 实现配置保存逻辑
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    /**
     * 初始化配置目录
     */
    public void initializeConfigDirectories() {
        try {
            Files.createDirectories(globalConfigPath.getParent());
            Files.createDirectories(projectConfigPath.getParent());
            logger.info("Configuration directories initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize config directories", e);
        }
    }
}
