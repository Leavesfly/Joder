package io.leavesfly.joder.services.model;

import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.services.adapters.ClaudeAdapter;
import io.leavesfly.joder.services.adapters.DeepSeekAdapter;
import io.leavesfly.joder.services.adapters.OpenAIAdapter;
import io.leavesfly.joder.services.adapters.QwenAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 模型适配器工厂
 * 根据配置创建合适的模型适配器
 */
@Singleton
public class ModelAdapterFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelAdapterFactory.class);
    
    private final ConfigManager configManager;
    
    @Inject
    public ModelAdapterFactory(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * 创建默认模型适配器
     */
    public ModelAdapter createDefaultAdapter() {
        String defaultModel = configManager.getString("joder.model.default", "mock");
        return createAdapter(defaultModel);
    }
    
    /**
     * 根据模型名称创建适配器
     */
    public ModelAdapter createAdapter(String modelName) {
        logger.info("Creating adapter for model: {}", modelName);
        
        // 检查是否有配置该模型
        String profilePath = "joder.model.profiles." + modelName;
        if (!configManager.hasPath(profilePath)) {
            logger.warn("Model profile not found: {}, using mock adapter", modelName);
            return new MockModelAdapter();
        }
        
        String provider = configManager.getString(profilePath + ".provider", "mock");
        
        // 根据提供商创建适配器
        return switch (provider.toLowerCase()) {
            case "anthropic" -> createClaudeAdapter(modelName);
            case "openai" -> createOpenAiAdapter(modelName);
            case "qwen" -> createQwenAdapter(modelName);
            case "deepseek" -> createDeepSeekAdapter(modelName);
            default -> {
                logger.warn("Unknown provider: {}, using mock adapter", provider);
                yield new MockModelAdapter();
            }
        };
    }
    
    /**
     * 根据模型指针创建适配器
     * <p>
     * 模型指针在配置中定义 (joder.model.pointers),
     * 可以将逻辑角色映射到具体模型
     * </p>
     * 
     * @param pointer 指针名称 (main/task/quick/reasoning)
     * @return 模型适配器
     */
    public ModelAdapter createAdapterByPointer(String pointer) {
        String pointerPath = "joder.model.pointers." + pointer;
        
        if (!configManager.hasPath(pointerPath)) {
            logger.warn("Model pointer not found: {}, using default model", pointer);
            return createDefaultAdapter();
        }
        
        String modelName = configManager.getString(pointerPath);
        logger.debug("Resolving pointer {} to model {}", pointer, modelName);
        
        return createAdapter(modelName);
    }
    
    private ModelAdapter createClaudeAdapter(String modelName) {
        String profilePath = "joder.model.profiles." + modelName;
        logger.info("Creating Claude adapter for model: {}", modelName);
        return new ClaudeAdapter(configManager, modelName, profilePath);
    }
    
    private ModelAdapter createOpenAiAdapter(String modelName) {
        String profilePath = "joder.model.profiles." + modelName;
        logger.info("Creating OpenAI adapter for model: {}", modelName);
        return new OpenAIAdapter(configManager, modelName, profilePath);
    }
    
    private ModelAdapter createQwenAdapter(String modelName) {
        String profilePath = "joder.model.profiles." + modelName;
        logger.info("Creating Qwen adapter for model: {}", modelName);
        return new QwenAdapter(configManager, modelName, profilePath);
    }
    
    private ModelAdapter createDeepSeekAdapter(String modelName) {
        String profilePath = "joder.model.profiles." + modelName;
        logger.info("Creating DeepSeek adapter for model: {}", modelName);
        return new DeepSeekAdapter(configManager, modelName, profilePath);
    }
}
