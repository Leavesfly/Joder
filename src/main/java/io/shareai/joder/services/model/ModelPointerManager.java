package io.shareai.joder.services.model;

import com.typesafe.config.Config;
import io.shareai.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 模型指针管理器
 * 管理多模型协作中的模型指针配置和切换
 * 
 * 支持 4 种模型指针：
 * - main: 主对话模型
 * - task: 子任务处理模型
 * - reasoning: 复杂推理模型
 * - quick: 快速响应模型
 */
@Singleton
public class ModelPointerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelPointerManager.class);
    
    public enum PointerType {
        MAIN("main", "主对话模型"),
        TASK("task", "子任务处理模型"),
        REASONING("reasoning", "复杂推理模型"),
        QUICK("quick", "快速响应模型");
        
        private final String key;
        private final String description;
        
        PointerType(String key, String description) {
            this.key = key;
            this.description = description;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static Optional<PointerType> fromKey(String key) {
            for (PointerType type : values()) {
                if (type.key.equals(key)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }
    
    private final ConfigManager configManager;
    private final Map<PointerType, String> pointers;
    private final Map<String, ModelProfile> profiles;
    
    @Inject
    public ModelPointerManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.pointers = new HashMap<>();
        this.profiles = new HashMap<>();
        loadConfiguration();
    }
    
    /**
     * 加载配置
     */
    private void loadConfiguration() {
        Config config = configManager.getConfig();
        
        // 加载模型指针配置
        loadPointers(config);
        
        // 加载模型配置文件
        loadProfiles(config);
        
        logger.info("Loaded {} model pointers and {} model profiles", 
                pointers.size(), profiles.size());
    }
    
    /**
     * 加载模型指针
     */
    private void loadPointers(Config config) {
        for (PointerType type : PointerType.values()) {
            String path = "joder.model.pointers." + type.getKey();
            if (config.hasPath(path)) {
                String modelName = config.getString(path);
                pointers.put(type, modelName);
                logger.debug("Loaded pointer: {} -> {}", type.getKey(), modelName);
            }
        }
    }
    
    /**
     * 加载模型配置文件
     */
    private void loadProfiles(Config config) {
        if (!config.hasPath("joder.model.profiles")) {
            logger.warn("No model profiles configured");
            return;
        }
        
        Config profilesConfig = config.getConfig("joder.model.profiles");
        
        for (String profileName : profilesConfig.root().keySet()) {
            try {
                Config profileConfig = profilesConfig.getConfig(profileName);
                
                ModelProfile profile = new ModelProfile();
                profile.setName(profileName);
                
                if (profileConfig.hasPath("provider")) {
                    profile.setProvider(profileConfig.getString("provider"));
                }
                if (profileConfig.hasPath("model")) {
                    profile.setModel(profileConfig.getString("model"));
                }
                if (profileConfig.hasPath("apiKey")) {
                    profile.setApiKey(profileConfig.getString("apiKey"));
                }
                if (profileConfig.hasPath("baseUrl")) {
                    profile.setBaseUrl(profileConfig.getString("baseUrl"));
                }
                if (profileConfig.hasPath("maxTokens")) {
                    profile.setMaxTokens(profileConfig.getInt("maxTokens"));
                }
                if (profileConfig.hasPath("temperature")) {
                    profile.setTemperature(profileConfig.getDouble("temperature"));
                }
                if (profileConfig.hasPath("contextLength")) {
                    profile.setContextLength(profileConfig.getInt("contextLength"));
                }
                
                if (profile.isValid()) {
                    profiles.put(profileName, profile);
                    logger.debug("Loaded profile: {}", profile);
                } else {
                    logger.warn("Invalid profile configuration: {}", profileName);
                }
                
            } catch (Exception e) {
                logger.error("Failed to load profile: {}", profileName, e);
            }
        }
    }
    
    /**
     * 获取模型指针指向的模型配置
     */
    public Optional<ModelProfile> getModelForPointer(PointerType pointerType) {
        String modelName = pointers.get(pointerType);
        if (modelName == null) {
            logger.debug("Pointer {} not configured", pointerType.getKey());
            return Optional.empty();
        }
        
        ModelProfile profile = profiles.get(modelName);
        if (profile == null) {
            logger.warn("Pointer {} points to non-existent model: {}", 
                    pointerType.getKey(), modelName);
            return Optional.empty();
        }
        
        return Optional.of(profile);
    }
    
    /**
     * 获取主对话模型
     */
    public Optional<ModelProfile> getMainModel() {
        return getModelForPointer(PointerType.MAIN);
    }
    
    /**
     * 获取子任务模型
     */
    public Optional<ModelProfile> getTaskModel() {
        return getModelForPointer(PointerType.TASK);
    }
    
    /**
     * 获取推理模型
     */
    public Optional<ModelProfile> getReasoningModel() {
        return getModelForPointer(PointerType.REASONING);
    }
    
    /**
     * 获取快速响应模型
     */
    public Optional<ModelProfile> getQuickModel() {
        return getModelForPointer(PointerType.QUICK);
    }
    
    /**
     * 通过名称获取模型配置
     */
    public Optional<ModelProfile> getProfile(String name) {
        return Optional.ofNullable(profiles.get(name));
    }
    
    /**
     * 获取所有模型配置
     */
    public Map<String, ModelProfile> getAllProfiles() {
        return new HashMap<>(profiles);
    }
    
    /**
     * 获取所有模型指针配置
     */
    public Map<PointerType, String> getAllPointers() {
        return new HashMap<>(pointers);
    }
    
    /**
     * 检查指针是否已配置
     */
    public boolean isPointerConfigured(PointerType pointerType) {
        return pointers.containsKey(pointerType) 
                && profiles.containsKey(pointers.get(pointerType));
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        pointers.clear();
        profiles.clear();
        loadConfiguration();
    }
    
    /**
     * 获取默认模型（优先使用 main 指针）
     */
    public Optional<ModelProfile> getDefaultModel() {
        // 1. 尝试获取 main 指针
        Optional<ModelProfile> mainModel = getMainModel();
        if (mainModel.isPresent()) {
            return mainModel;
        }
        
        // 2. 尝试从配置读取默认模型
        Config config = configManager.getConfig();
        if (config.hasPath("joder.model.default")) {
            String defaultModelName = config.getString("joder.model.default");
            Optional<ModelProfile> defaultProfile = getProfile(defaultModelName);
            if (defaultProfile.isPresent()) {
                return defaultProfile;
            }
        }
        
        // 3. 返回第一个可用的模型
        if (!profiles.isEmpty()) {
            return Optional.of(profiles.values().iterator().next());
        }
        
        logger.warn("No default model available");
        return Optional.empty();
    }
}
