package io.leavesfly.joder.screens;

import io.leavesfly.joder.cli.CommandParser;
import io.leavesfly.joder.cli.CommandResult;
import io.leavesfly.joder.cli.commands.*;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.tools.ToolRegistry;
import io.leavesfly.joder.core.MainLoop;
import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.services.mcp.McpServerManager;
import io.leavesfly.joder.services.mcp.McpToolRegistry;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.services.model.ModelAdapterFactory;
import io.leavesfly.joder.ui.components.MessageRenderer;
import io.leavesfly.joder.ui.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private final MainLoop mainLoop;  // 新增:主控制循环
    private final ModeCommand modeCommand;  // 新增:模式切换命令
    private final UndoCommand undoCommand;  // 新增
    private final RethinkCommand rethinkCommand;  // 新增
    private final StyleCommand styleCommand;  // 新增
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
            LoginCommand loginCommand,
            MainLoop mainLoop,
            ModeCommand modeCommand,
            UndoCommand undoCommand,
            RethinkCommand rethinkCommand,
            StyleCommand styleCommand) {  // 新增
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
        this.mainLoop = mainLoop;  // 新增
        this.modeCommand = modeCommand;  // 新增
        this.undoCommand = undoCommand;  // 新增
        this.rethinkCommand = rethinkCommand;  // 新增
        this.styleCommand = styleCommand;  // 新增
        this.commandParser = new CommandParser();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.running = false;
        
        // 初始化模型
        this.currentModel = modelAdapterFactory.createDefaultAdapter();
        
        // 设置主循环的当前模型
        this.mainLoop.setCurrentModel(this.currentModel);
        
        // 创建模型命令
        this.modelCommand = new ModelCommand(
            modelAdapterFactory, 
            currentModel, 
            model -> {
                this.currentModel = model;
                this.mainLoop.setCurrentModel(model);  // 同步到主循环
            }
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
        commandParser.registerCommand("mode", modeCommand);  // 新增
        commandParser.registerCommand("mcp", new McpCommand(mcpServerManager, mcpToolRegistry));
        commandParser.registerCommand("cost", costCommand);
        commandParser.registerCommand("doctor", doctorCommand);
        commandParser.registerCommand("agents", agentsCommand);
        commandParser.registerCommand("resume", resumeCommand);
        commandParser.registerCommand("login", loginCommand);
        commandParser.registerCommand("undo", undoCommand);  // 新增
        commandParser.registerCommand("rethink", rethinkCommand);  // 新增
        commandParser.registerCommand("style", styleCommand);  // 新增
        commandParser.registerCommand("exit", new ExitCommand());
        commandParser.registerCommand("quit", new ExitCommand());
    }
    
    /**
     * 启动 REPL 循环
     */
    public void start() {
        running = true;
        
        // 加载项目记忆到主循环
        mainLoop.loadProjectMemory();
        
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
        // 渲染用户消息(在添加到历史前显示)
        Message userMessage = new Message(MessageRole.USER, content);
        System.out.println(messageRenderer.render(userMessage));
        
        try {
            // 调用主循环处理消息(自动管理消息历史)
            System.out.print("⚙️  AI 思考中...");
            System.out.flush();
            
            Message assistantMessage = mainLoop.processUserInput(content);
            
            // 清除思考提示
            System.out.print("\r                    \r");
            
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
        return mainLoop.getMessageHistoryCopy();
    }
}
