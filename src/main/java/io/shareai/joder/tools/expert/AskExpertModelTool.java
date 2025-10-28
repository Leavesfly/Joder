package io.shareai.joder.tools.expert;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import io.shareai.joder.services.model.ModelAdapter;
import io.shareai.joder.services.model.ModelAdapterFactory;
import io.shareai.joder.services.model.ModelPointerManager;
import io.shareai.joder.services.model.ModelProfile;
import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * 专家模型咨询工具
 * 允许主模型咨询其他专业模型的意见
 */
public class AskExpertModelTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(AskExpertModelTool.class);
    
    private final ModelPointerManager modelPointerManager;
    private final ModelAdapterFactory modelAdapterFactory;
    private final ObjectMapper objectMapper;
    
    @Inject
    public AskExpertModelTool(
            ModelPointerManager modelPointerManager,
            ModelAdapterFactory modelAdapterFactory,
            ObjectMapper objectMapper) {
        this.modelPointerManager = modelPointerManager;
        this.modelAdapterFactory = modelAdapterFactory;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "ask_expert_model";
    }
    
    @Override
    public String getDescription() {
        return "咨询专家模型获取专业意见。支持指定模型指针（reasoning/quick）或具体模型名称。";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;  // 只读工具，不修改系统状态
    }
    
    @Override
    public ToolResult call(Map<String, Object> params) {
        try {
            // 获取参数
            String expertType = getString(params, "expert_type");
            String modelName = getString(params, "model_name", null);
            String question = getString(params, "question");
            String context = getString(params, "context", "");
            
            // 确定使用的模型
            ModelProfile expertModel = resolveExpertModel(expertType, modelName);
            if (expertModel == null) {
                return ToolResult.error("无法找到指定的专家模型: " + 
                        (modelName != null ? modelName : expertType));
            }
            
            logger.info("Consulting expert model: {} ({})", 
                    expertModel.getName(), expertModel.getModel());
            
            // 构建专家咨询消息
            List<Message> messages = buildExpertMessages(question, context);
            
            // 调用专家模型
            ModelAdapter adapter = modelAdapterFactory.createAdapter(expertModel.getName());
            String response = adapter.sendMessage(messages, "");
            
            // 返回专家意见
            Map<String, Object> result = new HashMap<>();
            result.put("expert_model", expertModel.getName());
            result.put("expert_provider", expertModel.getProvider());
            result.put("response", response);
            result.put("question", question);
            
            return ToolResult.success(formatExpertResponse(result));
            
        } catch (Exception e) {
            logger.error("Failed to consult expert model", e);
            return ToolResult.error("专家模型咨询失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析专家模型
     */
    private ModelProfile resolveExpertModel(String expertType, String modelName) {
        // 1. 如果指定了具体模型名称，直接使用
        if (modelName != null && !modelName.trim().isEmpty()) {
            Optional<ModelProfile> profile = modelPointerManager.getProfile(modelName);
            if (profile.isPresent()) {
                return profile.get();
            }
        }
        
        // 2. 根据专家类型解析模型指针
        if (expertType != null) {
            Optional<ModelPointerManager.PointerType> pointerType = 
                    ModelPointerManager.PointerType.fromKey(expertType);
            
            if (pointerType.isPresent()) {
                Optional<ModelProfile> profile = 
                        modelPointerManager.getModelForPointer(pointerType.get());
                if (profile.isPresent()) {
                    return profile.get();
                }
            }
        }
        
        // 3. 使用默认模型
        return modelPointerManager.getDefaultModel().orElse(null);
    }
    
    /**
     * 构建专家咨询消息
     */
    private List<Message> buildExpertMessages(String question, String context) {
        List<Message> messages = new ArrayList<>();
        
        // 系统提示
        String systemPrompt = """
                你是一位专业的 AI 助手，负责回答其他 AI 模型提出的专业问题。
                请提供准确、简洁且有深度的回答。
                """;
        messages.add(new Message(MessageRole.SYSTEM, systemPrompt));
        
        // 用户问题
        StringBuilder userMessage = new StringBuilder();
        if (context != null && !context.trim().isEmpty()) {
            userMessage.append("**上下文**:\n").append(context).append("\n\n");
        }
        userMessage.append("**问题**:\n").append(question);
        
        messages.add(new Message(MessageRole.USER, userMessage.toString()));
        
        return messages;
    }
    
    /**
     * 格式化专家响应
     */
    private String formatExpertResponse(Map<String, Object> result) {
        StringBuilder output = new StringBuilder();
        
        output.append("🤖 专家模型: ").append(result.get("expert_model"))
              .append(" (").append(result.get("expert_provider")).append(")\n\n");
        
        output.append("📝 问题:\n").append(result.get("question")).append("\n\n");
        
        output.append("💡 回答:\n").append(result.get("response"));
        
        return output.toString();
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            // 专家意见已包含在结果中
            return result.getOutput();
        } else {
            return "❌ " + result.getError();
        }
    }
}
