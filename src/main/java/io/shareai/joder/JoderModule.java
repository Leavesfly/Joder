package io.shareai.joder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.shareai.joder.core.config.ConfigManager;
import io.shareai.joder.screens.ReplScreen;
import io.shareai.joder.ui.components.MessageRenderer;
import io.shareai.joder.ui.theme.ThemeManager;
import io.shareai.joder.tools.*;
import io.shareai.joder.tools.bash.BashTool;
import io.shareai.joder.tools.file.FileReadTool;
import io.shareai.joder.tools.file.FileWriteTool;
import io.shareai.joder.tools.ls.LSTool;
import io.shareai.joder.tools.glob.GlobTool;
import io.shareai.joder.tools.grep.GrepTool;
import io.shareai.joder.tools.edit.FileEditTool;
import io.shareai.joder.tools.edit.MultiEditTool;
import io.shareai.joder.tools.think.ThinkTool;
import io.shareai.joder.tools.todo.TodoWriteTool;
import io.shareai.joder.tools.task.TaskTool;
import io.shareai.joder.tools.web.WebSearchTool;
import io.shareai.joder.tools.web.URLFetcherTool;
import io.shareai.joder.tools.expert.AskExpertModelTool;
import io.shareai.joder.tools.completion.AttemptCompletionTool;
import io.shareai.joder.tools.filetree.FileTreeTool;
import io.shareai.joder.tools.web.InspectSiteTool;
import io.shareai.joder.tools.memory.MemoryTool;
import io.shareai.joder.tools.memory.MemoryReadTool;
import io.shareai.joder.tools.memory.MemoryWriteTool;
import io.shareai.joder.tools.notebook.NotebookEditTool;
import io.shareai.joder.tools.notebook.NotebookReadTool;
import io.shareai.joder.tools.architect.ArchitectTool;

import javax.inject.Singleton;

/**
 * Joder 应用的 Guice 依赖注入模块
 */
public class JoderModule extends AbstractModule {
    
    private final String workingDirectory;
    
    public JoderModule() {
        this(System.getProperty("user.dir"));
    }
    
    public JoderModule(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    protected void configure() {
        // 基础配置
        bind(String.class).annotatedWith(WorkingDirectory.class).toInstance(workingDirectory);
        
        // UI 组件
        bind(ThemeManager.class);
        bind(MessageRenderer.class);
        bind(ReplScreen.class);
        
        // 权限管理
        bind(io.shareai.joder.core.permission.PermissionManager.class);
        
        // 命令系统
        bind(io.shareai.joder.cli.commands.CostCommand.class);
        bind(io.shareai.joder.cli.commands.DoctorCommand.class);
        bind(io.shareai.joder.cli.commands.AgentsCommand.class);
        bind(io.shareai.joder.cli.commands.ResumeCommand.class);
        bind(io.shareai.joder.cli.commands.LoginCommand.class);
        bind(io.shareai.joder.cli.commands.InitCommand.class);
        bind(io.shareai.joder.cli.commands.ModelStatusCommand.class);
        bind(io.shareai.joder.cli.commands.LogoutCommand.class);
        bind(io.shareai.joder.cli.commands.ReviewCommand.class);
        bind(io.shareai.joder.cli.commands.HistoryCommand.class);
        bind(io.shareai.joder.cli.commands.ExportCommand.class);
        // Phase 7: 开发工具和其他命令
        bind(io.shareai.joder.cli.commands.ListenCommand.class);
        bind(io.shareai.joder.cli.commands.BenchmarkCommand.class);
        bind(io.shareai.joder.cli.commands.DebugCommand.class);
        bind(io.shareai.joder.cli.commands.TestCommand.class);
        bind(io.shareai.joder.cli.commands.CompactCommand.class);
        bind(io.shareai.joder.cli.commands.BugCommand.class);
        bind(io.shareai.joder.cli.commands.ReleaseNotesCommand.class);
        bind(io.shareai.joder.cli.commands.RefreshCommandsCommand.class);
        
        // MCP 系统
        bind(io.shareai.joder.services.mcp.McpServerManager.class);
        bind(io.shareai.joder.services.mcp.McpToolRegistry.class);
        
        // 模型系统
        bind(io.shareai.joder.services.model.ModelAdapterFactory.class);
        bind(io.shareai.joder.services.model.ModelPointerManager.class);
        
        // 补全系统
        bind(io.shareai.joder.services.completion.CompletionManager.class);
        bind(io.shareai.joder.services.completion.ModelCompletionProvider.class);
        bind(io.shareai.joder.services.completion.FileCompletionProvider.class);
        bind(io.shareai.joder.services.completion.CommandCompletionProvider.class);
        bind(io.shareai.joder.services.completion.CompletionService.class).in(Singleton.class);
        
        // 维护系统
        bind(io.shareai.joder.services.maintenance.MaintenanceScheduler.class).in(Singleton.class);
        
        // 自定义命令系统
        bind(io.shareai.joder.services.commands.CustomCommandService.class).in(Singleton.class);
        
        // OAuth 认证系统
        bind(io.shareai.joder.services.oauth.OAuthConfig.class).in(Singleton.class);
        bind(io.shareai.joder.services.oauth.OAuthService.class).in(Singleton.class);
        bind(io.shareai.joder.services.oauth.TokenManager.class).in(Singleton.class);
        
        // 文件新鲜度追踪系统
        bind(io.shareai.joder.services.freshness.FileFreshnessService.class).in(Singleton.class);
        
        // Mention 处理系统
        bind(io.shareai.joder.services.mention.MentionProcessor.class).in(Singleton.class);
        
        // 系统提醒服务
        bind(io.shareai.joder.services.reminder.SystemReminderService.class).in(Singleton.class);
        
        // UI 渲染器
        bind(io.shareai.joder.ui.renderer.SyntaxHighlighter.class).in(Singleton.class);
        bind(io.shareai.joder.ui.renderer.DiffRenderer.class).in(Singleton.class);
        
        // Agents 系统
        bind(io.shareai.joder.services.agents.AgentsManager.class);
        
        // Hooks 系统
        bind(io.shareai.joder.hooks.ApiKeyVerificationHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.CommandHistoryHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.ToolPermissionHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.DoublePressHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.MessageLogHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.StartupTimeHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.NotifyAfterTimeoutHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.TerminalSizeHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.UnifiedCompletionHook.class).in(Singleton.class);
        bind(io.shareai.joder.hooks.CancelRequestHook.class).in(Singleton.class);
        
        // 工具系统 - 通过 Provider 提供
        bind(FileReadTool.class);
        bind(FileWriteTool.class);
        bind(FileEditTool.class);
        bind(MultiEditTool.class);
        bind(BashTool.class);
        bind(LSTool.class);
        bind(GlobTool.class);
        bind(GrepTool.class);
        bind(ThinkTool.class);
        bind(TodoWriteTool.class);
        bind(TaskTool.class);
        bind(WebSearchTool.class);
        bind(URLFetcherTool.class);
        bind(AskExpertModelTool.class);
        bind(MemoryReadTool.class);
        bind(MemoryWriteTool.class);
        bind(AttemptCompletionTool.class);
        bind(FileTreeTool.class);
        bind(InspectSiteTool.class);
        bind(MemoryTool.class);
        bind(NotebookEditTool.class);
        bind(NotebookReadTool.class);
        bind(ArchitectTool.class);
    }
    
    @Provides
    @Singleton
    ConfigManager provideConfigManager(@WorkingDirectory String workingDir) {
        return new ConfigManager(workingDir);
    }
    
    @Provides
    @Singleton
    ToolRegistry provideToolRegistry(
        FileReadTool fileReadTool,
        FileWriteTool fileWriteTool,
        FileEditTool fileEditTool,
        MultiEditTool multiEditTool,
        BashTool bashTool,
        LSTool lsTool,
        GlobTool globTool,
        GrepTool grepTool,
        ThinkTool thinkTool,
        TodoWriteTool todoWriteTool,
        TaskTool taskTool,
        WebSearchTool webSearchTool,
        URLFetcherTool urlFetcherTool,
        AskExpertModelTool askExpertModelTool,
        MemoryReadTool memoryReadTool,
        MemoryWriteTool memoryWriteTool,
        AttemptCompletionTool attemptCompletionTool,
        FileTreeTool fileTreeTool,
        InspectSiteTool inspectSiteTool,
        MemoryTool memoryTool,
        NotebookEditTool notebookEditTool,
        NotebookReadTool notebookReadTool,
        ArchitectTool architectTool,
        io.shareai.joder.services.mcp.McpServerManager mcpServerManager,
        io.shareai.joder.services.mcp.McpToolRegistry mcpToolRegistry
    ) {
        ToolRegistry registry = new ToolRegistry();
        
        // 注册文件操作工具
        registry.registerTool(fileReadTool);
        registry.registerTool(fileWriteTool);
        registry.registerTool(fileEditTool);
        registry.registerTool(multiEditTool);
        
        // 注册系统工具
        registry.registerTool(bashTool);
        
        // 注册搜索工具
        registry.registerTool(lsTool);
        registry.registerTool(globTool);
        registry.registerTool(grepTool);
        
        // 注册 P1 工具
        registry.registerTool(thinkTool);
        registry.registerTool(todoWriteTool);
        registry.registerTool(taskTool);
        
        // 注册 P2 工具
        registry.registerTool(webSearchTool);
        registry.registerTool(urlFetcherTool);
        registry.registerTool(askExpertModelTool);
        registry.registerTool(memoryReadTool);
        registry.registerTool(memoryWriteTool);
        
        // 注册 P3 工具
        registry.registerTool(attemptCompletionTool);
        registry.registerTool(fileTreeTool);
        registry.registerTool(inspectSiteTool);
        registry.registerTool(memoryTool);
        
        // 注册 P4 最终工具
        registry.registerTool(notebookEditTool);
        registry.registerTool(notebookReadTool);
        registry.registerTool(architectTool);
        
        // 配置 MCP 管理器
        registry.setMcpManagers(mcpServerManager, mcpToolRegistry);
        
        return registry;
    }
}
