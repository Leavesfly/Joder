package io.shareai.joder.screens;

import io.shareai.joder.cli.CommandParser;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.cli.commands.*;
import io.shareai.joder.core.config.ConfigManager;
import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import io.shareai.joder.services.mcp.McpServerManager;
import io.shareai.joder.services.mcp.McpToolRegistry;
import io.shareai.joder.services.model.ModelAdapter;
import io.shareai.joder.services.model.ModelAdapterFactory;
import io.shareai.joder.tools.ToolRegistry;
import io.shareai.joder.ui.components.MessageRenderer;
import io.shareai.joder.ui.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * REPL 交互界面
 */
@Singleton
public class ReplScreen {
    
    private static final Logger logger = LoggerFactory.getLogger(ReplScreen.class);
    
    private final ConfigManager configManager;
    private final ThemeManager themeManager;
    private final MessageRenderer messageRenderer;
    private final CommandParser commandParser;
    private final ModelAdapterFactory modelAdapterFactory;
    private final McpServerManager mcpServerManager;
    private final McpToolRegistry mcpToolRegistry;
    private final ToolRegistry toolRegistry;
    private final CostCommand costCommand;
    private final DoctorCommand doctorCommand;
    private final AgentsCommand agentsCommand;
    private final ResumeCommand resumeCommand;
    private final LoginCommand loginCommand;
    private final List<Message> messageHistory;
    private final BufferedReader reader;
    private final ModelCommand modelCommand;
    
    private boolean running;
    private ModelAdapter currentModel;
    
    @Inject
    public ReplScreen(
            ConfigManager configManager,
            ThemeManager themeManager,
            MessageRenderer messageRenderer,
            ModelAdapterFactory modelAdapterFactory,
            McpServerManager mcpServerManager,
            McpToolRegistry mcpToolRegistry,
            ToolRegistry toolRegistry,
            CostCommand costCommand,
            DoctorCommand doctorCommand,
            AgentsCommand agentsCommand,
            ResumeCommand resumeCommand,
            LoginCommand loginCommand) {
        this.configManager = configManager;
        this.themeManager = themeManager;
        this.messageRenderer = messageRenderer;
        this.modelAdapterFactory = modelAdapterFactory;
        this.mcpServerManager = mcpServerManager;
        this.mcpToolRegistry = mcpToolRegistry;
        this.toolRegistry = toolRegistry;
        this.costCommand = costCommand;
        this.doctorCommand = doctorCommand;
        this.agentsCommand = agentsCommand;
        this.resumeCommand = resumeCommand;
        this.loginCommand = loginCommand;
        this.commandParser = new CommandParser();
        this.messageHistory = new ArrayList<>();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.running = false;
        
        // 初始化模型
        this.currentModel = modelAdapterFactory.createDefaultAdapter();
        
        // 创建模型命令
        this.modelCommand = new ModelCommand(
            modelAdapterFactory, 
            currentModel, 
            model -> this.currentModel = model
        );
        
        // 注册命令
        registerCommands();
    }
    
    /**
     * 注册所有命令
     */
    private void registerCommands() {
        commandParser.registerCommand("help", new HelpCommand(commandParser));
        commandParser.registerCommand("clear", new ClearCommand());
        commandParser.registerCommand("config", new ConfigCommand(configManager));
        commandParser.registerCommand("model", modelCommand);
        commandParser.registerCommand("mcp", new McpCommand(mcpServerManager, mcpToolRegistry));
        commandParser.registerCommand("cost", costCommand);
        commandParser.registerCommand("doctor", doctorCommand);
        commandParser.registerCommand("agents", agentsCommand);
        commandParser.registerCommand("resume", resumeCommand);
        commandParser.registerCommand("login", loginCommand);
        commandParser.registerCommand("exit", new ExitCommand());
        commandParser.registerCommand("quit", new ExitCommand());
    }
    
    /**
     * 启动 REPL 循环
     */
    public void start() {
        running = true;
        
        // 显示欢迎信息
        displayWelcome();
        
        // 主循环
        while (running) {
            try {
                // 显示提示符
                System.out.print("\n> ");
                System.out.flush();
                
                // 读取用户输入
                String input = reader.readLine();
                
                if (input == null) {
                    // EOF (Ctrl+D)
                    break;
                }
                
                // 处理输入
                handleInput(input);
                
            } catch (IOException e) {
                logger.error("读取输入失败", e);
                System.err.println(messageRenderer.renderError("读取输入失败: " + e.getMessage()));
            } catch (Exception e) {
                logger.error("处理输入时发生错误", e);
                System.err.println(messageRenderer.renderError("发生错误: " + e.getMessage()));
            }
        }
        
        // 显示退出信息
        System.out.println("\n感谢使用 Joder！再见！\n");
    }
    
    /**
     * 处理用户输入
     */
    private void handleInput(String input) {
        // 解析命令
        CommandResult result = commandParser.parse(input);
        
        // 处理结果
        switch (result.getType()) {
            case SUCCESS -> {
                if (!result.getMessage().isEmpty()) {
                    System.out.println(messageRenderer.renderSuccess(result.getMessage()));
                }
                if (result.shouldExit()) {
                    running = false;
                }
            }
            case ERROR -> System.out.println(messageRenderer.renderError(result.getMessage()));
            case USER_MESSAGE -> handleUserMessage(result.getMessage());
            case EMPTY -> {
                // 空输入，什么都不做
            }
        }
    }
    
    /**
     * 处理用户消息
     */
    private void handleUserMessage(String content) {
        // 添加到消息历史
        Message userMessage = new Message(MessageRole.USER, content);
        messageHistory.add(userMessage);
        
        // 渲染用户消息
        System.out.println(messageRenderer.render(userMessage));
        
        try {
            // 调用 AI 模型处理消息
            System.out.print("⚙️  AI 思考中...");
            System.out.flush();
            
            String aiResponse = currentModel.sendMessage(messageHistory, "");
            
            // 清除思考提示
            System.out.print("\r                    \r");
            
            // 创建 AI 响应消息
            Message assistantMessage = new Message(MessageRole.ASSISTANT, aiResponse);
            messageHistory.add(assistantMessage);
            
            // 渲染 AI 响应
            System.out.println(messageRenderer.render(assistantMessage));
            
        } catch (Exception e) {
            logger.error("AI 响应失败", e);
            System.out.println(messageRenderer.renderError("AI 响应失败: " + e.getMessage()));
        }
    }
    
    /**
     * 显示欢迎信息
     */
    private void displayWelcome() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  🚀 欢迎使用 Joder AI 助手");
        System.out.println("  版本: 1.0.0");
        System.out.println("  当前模型: " + currentModel.getModelName() + " (" + currentModel.getProviderName() + ")");
        System.out.println("  配置状态: " + (currentModel.isConfigured() ? "✅ 就绪" : "⚠️ 未配置 API Key"));
        System.out.println("  主题: " + configManager.getString("joder.theme", "dark"));
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("\n💡 提示:");
        System.out.println("  • 输入 /help 查看可用命令");
        System.out.println("  • 输入 /model 查看或切换模型");
        System.out.println("  • 直接输入消息开始与 AI 对话");
        System.out.println("  • 按 Ctrl+C 或输入 /exit 退出\n");
    }
    
    /**
     * 停止 REPL
     */
    public void stop() {
        running = false;
    }
    
    /**
     * 获取消息历史
     */
    public List<Message> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
}
