package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Console;
import java.util.HashMap;
import java.util.Map;

/**
 * Login Command - 登录认证命令
 * 
 * 管理用户认证和 API 密钥配置
 */
@Singleton
public class LoginCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);
    
    private final ConfigManager configManager;
    
    @Inject
    public LoginCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public String getDescription() {
        return "管理 API 密钥配置和用户认证";
    }
    
    @Override
    public String getUsage() {
        return "/login [service] - 配置 API 密钥\n" +
               "支持的服务: openai, anthropic, qwen, deepseek";
    }
    
    @Override
    public CommandResult execute(String args) {
        StringBuilder output = new StringBuilder();
        
        output.append("🔐 登录配置\n\n");
        output.append("─".repeat(60)).append("\n\n");
        
        if (args == null || args.trim().isEmpty()) {
            // 显示所有已配置的服务
            return showStatus(output);
        }
        
        String service = args.trim().toLowerCase();
        
        // 验证服务名称
        if (!isValidService(service)) {
            output.append("❌ 不支持的服务: ").append(service).append("\n\n");
            output.append("支持的服务:\n");
            output.append("  - openai\n");
            output.append("  - anthropic\n");
            output.append("  - qwen\n");
            output.append("  - deepseek\n");
            return CommandResult.success(output.toString());
        }
        
        // 配置服务
        return configureService(service, output);
    }
    
    /**
     * 显示配置状态
     */
    private CommandResult showStatus(StringBuilder output) {
        output.append("📊 当前配置状态\n\n");
        
        Map<String, String> services = new HashMap<>();
        services.put("OpenAI", "OPENAI_API_KEY");
        services.put("Anthropic (Claude)", "ANTHROPIC_API_KEY");
        services.put("通义千问", "DASHSCOPE_API_KEY");
        services.put("DeepSeek", "DEEPSEEK_API_KEY");
        
        for (Map.Entry<String, String> entry : services.entrySet()) {
            String name = entry.getKey();
            String envKey = entry.getValue();
            boolean configured = System.getenv(envKey) != null && 
                               !System.getenv(envKey).isEmpty();
            
            output.append(String.format("%-20s : %s\n", 
                name, 
                configured ? "✅ 已配置" : "❌ 未配置"
            ));
        }
        
        output.append("\n");
        output.append("💡 提示:\n");
        output.append("  - 使用 /login <service> 配置特定服务\n");
        output.append("  - 或在环境变量中设置 API 密钥\n");
        output.append("  - 示例: export OPENAI_API_KEY=sk-xxx\n");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 配置服务
     */
    private CommandResult configureService(String service, StringBuilder output) {
        output.append("🔧 配置 ").append(getServiceName(service)).append("\n\n");
        
        // 检查环境变量
        String envKey = getEnvKey(service);
        String existingKey = System.getenv(envKey);
        
        if (existingKey != null && !existingKey.isEmpty()) {
            output.append("✅ 已从环境变量读取 API 密钥\n");
            output.append(String.format("   环境变量: %s\n", envKey));
            output.append(String.format("   密钥前缀: %s***\n", maskApiKey(existingKey)));
            output.append("\n");
            output.append("💡 如需更新，请设置新的环境变量或重启应用\n");
            return CommandResult.success(output.toString());
        }
        
        // 提示设置环境变量
        output.append("❌ 未检测到 API 密钥\n\n");
        output.append("请按以下方式配置:\n\n");
        output.append("**方式 1: 设置环境变量（推荐）**\n");
        output.append(String.format("```bash\n"));
        output.append(String.format("export %s=your-api-key-here\n", envKey));
        output.append("```\n\n");
        
        output.append("**方式 2: 在启动命令中设置**\n");
        output.append(String.format("```bash\n"));
        output.append(String.format("%s=your-api-key-here java -jar joder.jar\n", envKey));
        output.append("```\n\n");
        
        output.append("**方式 3: 在 ~/.bashrc 或 ~/.zshrc 中添加**\n");
        output.append(String.format("```bash\n"));
        output.append(String.format("export %s=your-api-key-here\n", envKey));
        output.append("```\n\n");
        
        output.append(String.format("📌 获取 API 密钥:\n"));
        output.append(getApiKeyUrl(service)).append("\n");
        
        return CommandResult.success(output.toString());
    }
    
    /**
     * 验证服务名称
     */
    private boolean isValidService(String service) {
        return switch (service) {
            case "openai", "anthropic", "qwen", "deepseek" -> true;
            default -> false;
        };
    }
    
    /**
     * 获取服务名称
     */
    private String getServiceName(String service) {
        return switch (service) {
            case "openai" -> "OpenAI";
            case "anthropic" -> "Anthropic (Claude)";
            case "qwen" -> "通义千问";
            case "deepseek" -> "DeepSeek";
            default -> service;
        };
    }
    
    /**
     * 获取环境变量名
     */
    private String getEnvKey(String service) {
        return switch (service) {
            case "openai" -> "OPENAI_API_KEY";
            case "anthropic" -> "ANTHROPIC_API_KEY";
            case "qwen" -> "DASHSCOPE_API_KEY";
            case "deepseek" -> "DEEPSEEK_API_KEY";
            default -> "";
        };
    }
    
    /**
     * 获取 API 密钥获取 URL
     */
    private String getApiKeyUrl(String service) {
        return switch (service) {
            case "openai" -> "   https://platform.openai.com/api-keys";
            case "anthropic" -> "   https://console.anthropic.com/settings/keys";
            case "qwen" -> "   https://dashscope.console.aliyun.com/apiKey";
            case "deepseek" -> "   https://platform.deepseek.com/api_keys";
            default -> "";
        };
    }
    
    /**
     * 掩码 API 密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 7);
    }
}
