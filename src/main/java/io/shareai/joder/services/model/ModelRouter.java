package io.shareai.joder.services.model;

import io.shareai.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能模型路由器
 * <p>
 * 核心优化策略:
 * - 对辅助任务使用轻量模型 (Haiku/Turbo)
 * - 对核心任务使用重量级模型 (Sonnet/GPT-4)
 * - 预计可节省 70-80% 的成本
 * </p>
 */
@Singleton
public class ModelRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);
    
    private final ConfigManager configManager;
    private final ModelAdapterFactory modelAdapterFactory;
    
    /**
     * 任务类型到模型配置的映射
     */
    private final Map<TaskType, String> taskModelMapping;
    
    /**
     * 是否启用智能路由
     */
    private boolean enableSmartRouting;
    
    @Inject
    public ModelRouter(
            ConfigManager configManager,
            ModelAdapterFactory modelAdapterFactory) {
        this.configManager = configManager;
        this.modelAdapterFactory = modelAdapterFactory;
        this.taskModelMapping = new HashMap<>();
        this.enableSmartRouting = true;
        
        initializeDefaultMapping();
    }
    
    /**
     * 初始化默认的任务-模型映射
     */
    private void initializeDefaultMapping() {
        // 从配置读取,如果没有则使用默认值
        
        // 核心推理使用重量级模型
        taskModelMapping.put(TaskType.CORE_REASONING, 
            configManager.getString("joder.model.routing.core", "claude-3-sonnet"));
        
        // 代码生成使用标准模型
        taskModelMapping.put(TaskType.CODE_GENERATION, 
            configManager.getString("joder.model.routing.code", "claude-3-sonnet"));
        
        // 轻量任务使用轻量模型
        String lightweightModel = configManager.getString("joder.model.routing.lightweight", "claude-3-haiku");
        taskModelMapping.put(TaskType.CONTENT_PARSING, lightweightModel);
        taskModelMapping.put(TaskType.STRUCTURE_ANALYSIS, lightweightModel);
        taskModelMapping.put(TaskType.SEARCH_OPTIMIZATION, lightweightModel);
        taskModelMapping.put(TaskType.SUMMARIZATION, lightweightModel);
        
        // 是否启用智能路由
        enableSmartRouting = configManager.getBoolean("joder.model.routing.enabled", true);
        
        logger.info("Model routing initialized. Smart routing: {}", enableSmartRouting);
    }
    
    /**
     * 根据任务类型选择合适的模型
     * 
     * @param taskType 任务类型
     * @return 模型适配器
     */
    public ModelAdapter routeModel(TaskType taskType) {
        if (!enableSmartRouting) {
            // 如果未启用智能路由,返回默认模型
            return modelAdapterFactory.createDefaultAdapter();
        }
        
        String modelName = taskModelMapping.get(taskType);
        
        if (modelName == null) {
            logger.warn("No model mapping found for task type: {}, using default", taskType);
            return modelAdapterFactory.createDefaultAdapter();
        }
        
        try {
            ModelAdapter adapter = modelAdapterFactory.createAdapter(modelName);
            logger.debug("Routed task {} to model {}", taskType, modelName);
            return adapter;
        } catch (Exception e) {
            logger.error("Failed to create adapter for model: {}, falling back to default", modelName, e);
            return modelAdapterFactory.createDefaultAdapter();
        }
    }
    
    /**
     * 根据任务类型选择合适的模型(使用模型指针)
     * 
     * @param taskType 任务类型
     * @return 模型适配器
     */
    public ModelAdapter routeModelByPointer(TaskType taskType) {
        if (!enableSmartRouting) {
            return modelAdapterFactory.createDefaultAdapter();
        }
        
        // 根据任务类型选择模型指针
        String pointer = switch (taskType) {
            case CORE_REASONING -> "main";
            case CODE_GENERATION -> "task";
            case CONTENT_PARSING, STRUCTURE_ANALYSIS, 
                 SEARCH_OPTIMIZATION, SUMMARIZATION -> "quick";
        };
        
        try {
            ModelAdapter adapter = modelAdapterFactory.createAdapterByPointer(pointer);
            logger.debug("Routed task {} to pointer {} ({})", 
                taskType, pointer, adapter.getModelName());
            return adapter;
        } catch (Exception e) {
            logger.error("Failed to route by pointer: {}, falling back to default", pointer, e);
            return modelAdapterFactory.createDefaultAdapter();
        }
    }
    
    /**
     * 设置任务类型的模型映射
     * 
     * @param taskType 任务类型
     * @param modelName 模型名称
     */
    public void setTaskModel(TaskType taskType, String modelName) {
        taskModelMapping.put(taskType, modelName);
        logger.info("Updated task model mapping: {} -> {}", taskType, modelName);
    }
    
    /**
     * 启用或禁用智能路由
     * 
     * @param enabled 是否启用
     */
    public void setSmartRoutingEnabled(boolean enabled) {
        this.enableSmartRouting = enabled;
        logger.info("Smart routing {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * 检查智能路由是否启用
     */
    public boolean isSmartRoutingEnabled() {
        return enableSmartRouting;
    }
    
    /**
     * 获取任务类型的模型名称
     */
    public String getTaskModelName(TaskType taskType) {
        return taskModelMapping.get(taskType);
    }
    
    /**
     * 估算使用轻量模型的成本节省
     * 
     * @param taskType 任务类型
     * @return 成本比例 (例如 0.2 表示只需要 20% 的成本)
     */
    public double estimateCostRatio(TaskType taskType) {
        return switch (taskType) {
            case CORE_REASONING, CODE_GENERATION -> 1.0;  // 100% 成本
            case CONTENT_PARSING, STRUCTURE_ANALYSIS, 
                 SEARCH_OPTIMIZATION, SUMMARIZATION -> 0.2;  // 20% 成本 (节省 80%)
        };
    }
}
