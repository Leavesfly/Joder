package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.services.model.ModelAdapterFactory;

import java.util.function.Consumer;

/**
 * 模型管理命令
 */
public class ModelCommand implements Command {
    
    private final ModelAdapterFactory modelAdapterFactory;
    private final Consumer<ModelAdapter> modelSwitcher;
    private ModelAdapter currentModel;
    
    public ModelCommand(ModelAdapterFactory modelAdapterFactory, 
                       ModelAdapter currentModel,
                       Consumer<ModelAdapter> modelSwitcher) {
        this.modelAdapterFactory = modelAdapterFactory;
        this.currentModel = currentModel;
        this.modelSwitcher = modelSwitcher;
    }
    
    public void setCurrentModel(ModelAdapter model) {
        this.currentModel = model;
    }
    
    @Override
    public CommandResult execute(String args) {
        if (args == null || args.trim().isEmpty()) {
            // 显示当前模型信息
            String info = String.format("""
                当前模型信息:
                  模型名称: %s
                  提供商: %s
                  状态: %s
                
                使用 /model <模型名> 切换模型
                可用模型: claude-3-sonnet, gpt-4o, qwen-max, deepseek-chat 等
                """,
                currentModel.getModelName(),
                currentModel.getProviderName(),
                currentModel.isConfigured() ? "✅ 已配置" : "❌ 未配置 API Key"
            );
            return CommandResult.success(info);
        } else {
            // 切换模型
            String modelName = args.trim();
            try {
                ModelAdapter newModel = modelAdapterFactory.createAdapter(modelName);
                modelSwitcher.accept(newModel);
                setCurrentModel(newModel);
                String successMsg = String.format("已切换到模型: %s (%s)", 
                    newModel.getModelName(), newModel.getProviderName());
                return CommandResult.success(successMsg);
            } catch (Exception e) {
                return CommandResult.error("切换模型失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "查看或切换当前使用的 AI 模型";
    }
    
    @Override
    public String getUsage() {
        return "/model [模型名称]";
    }
}
