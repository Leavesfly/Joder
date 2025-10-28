package io.leavesfly.joder.cli.commands;

import com.typesafe.config.Config;
import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * /modelstatus 命令 - 显示模型配置状态
 */
public class ModelStatusCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelStatusCommand.class);
    
    private final ConfigManager configManager;
    
    @Inject
    public ModelStatusCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public String getDescription() {
        return "显示所有模型的配置状态和模型指针设置";
    }
    
    @Override
    public String getUsage() {
        return "modelstatus [无参数]";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            Config config = configManager.getConfig();
            
            StringBuilder output = new StringBuilder();
            output.append("📊 模型配置状态\n\n");
            
            // 1. 显示默认模型
            String defaultModel = "未配置";
            if (config.hasPath("joder.model.default")) {
                defaultModel = config.getString("joder.model.default");
            }
            output.append(String.format("🎯 默认模型: %s\n\n", defaultModel));
            
            // 2. 显示模型指针
            output.append("🔗 模型指针:\n");
            String[] pointers = {"main", "task", "reasoning", "quick"};
            String[] pointerDescriptions = {
                    "主对话模型",
                    "子任务模型",
                    "复杂推理模型",
                    "快速响应模型"
            };
            
            for (int i = 0; i < pointers.length; i++) {
                String pointer = pointers[i];
                String desc = pointerDescriptions[i];
                String path = "joder.model.pointers." + pointer;
                
                String modelName = "未配置";
                if (config.hasPath(path)) {
                    modelName = config.getString(path);
                }
                
                String status = modelName.equals("未配置") ? "✗" : "✓";
                output.append(String.format("  %s %-10s → %-20s (%s)\n", 
                        status, pointer, modelName, desc));
            }
            
            // 3. 显示已配置的模型列表
            output.append("\n📋 已配置模型:\n");
            if (config.hasPath("joder.model.profiles")) {
                Config profiles = config.getConfig("joder.model.profiles");
                
                if (profiles.isEmpty()) {
                    output.append("  (无)\n");
                } else {
                    int count = 0;
                    for (String profileName : profiles.root().keySet()) {
                        try {
                            Config profile = profiles.getConfig(profileName);
                            
                            String provider = profile.hasPath("provider") 
                                    ? profile.getString("provider") : "unknown";
                            String model = profile.hasPath("model") 
                                    ? profile.getString("model") : "unknown";
                            boolean hasApiKey = profile.hasPath("apiKey");
                            
                            String apiKeyStatus = hasApiKey ? "✓ 已配置" : "✗ 未配置";
                            
                            output.append(String.format("  %d. %-20s (%s / %s) API Key: %s\n",
                                    ++count, profileName, provider, model, apiKeyStatus));
                            
                        } catch (Exception e) {
                            logger.warn("Failed to read profile: {}", profileName, e);
                        }
                    }
                }
            } else {
                output.append("  (无)\n");
            }
            
            // 4. 显示权限模式
            output.append("\n🔒 权限模式: ");
            String permissionMode = "default";
            if (config.hasPath("joder.permissions.mode")) {
                permissionMode = config.getString("joder.permissions.mode");
            }
            output.append(permissionMode).append("\n");
            
            // 5. 显示主题
            output.append("🎨 主题: ");
            String theme = "dark";
            if (config.hasPath("joder.theme")) {
                theme = config.getString("joder.theme");
            }
            output.append(theme).append("\n");
            
            // 6. 配置建议
            output.append("\n💡 配置提示:\n");
            
            boolean hasWarnings = false;
            
            if (defaultModel.equals("未配置")) {
                output.append("  ⚠️  未配置默认模型，请运行 /model 命令配置\n");
                hasWarnings = true;
            }
            
            if (config.hasPath("joder.model.profiles") && config.getConfig("joder.model.profiles").isEmpty()) {
                output.append("  ⚠️  未配置任何模型，请编辑 .joder/config.conf\n");
                hasWarnings = true;
            }
            
            if (!hasWarnings) {
                output.append("  ✅ 配置良好！\n");
            }
            
            return CommandResult.success(output.toString());
            
        } catch (Exception e) {
            logger.error("Failed to get model status", e);
            return CommandResult.error("获取模型状态失败: " + e.getMessage());
        }
    }
}
