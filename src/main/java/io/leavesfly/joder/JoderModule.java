package io.leavesfly.joder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.cli.commands.*;
import io.leavesfly.joder.core.MainLoop;
import io.leavesfly.joder.core.permission.PermissionManager;
import io.leavesfly.joder.hooks.*;
import io.leavesfly.joder.services.agents.AgentsManager;
import io.leavesfly.joder.services.agents.AgentExecutor;
import io.leavesfly.joder.services.cache.FileContentCache;
import io.leavesfly.joder.services.commands.CustomCommandService;
import io.leavesfly.joder.services.completion.*;
import io.leavesfly.joder.services.context.ContextCompressor;
import io.leavesfly.joder.services.context.TokenCounter;
import io.leavesfly.joder.services.cost.CostTrackingService;
import io.leavesfly.joder.services.freshness.FileFreshnessService;
import io.leavesfly.joder.services.maintenance.MaintenanceScheduler;
import io.leavesfly.joder.services.mcp.McpServerManager;
import io.leavesfly.joder.services.mcp.McpToolRegistry;
import io.leavesfly.joder.services.memory.ProjectMemoryManager;
import io.leavesfly.joder.services.mention.MentionProcessor;
import io.leavesfly.joder.services.model.ModelAdapterFactory;
import io.leavesfly.joder.services.model.ModelPointerManager;
import io.leavesfly.joder.services.model.ModelRouter;
import io.leavesfly.joder.services.oauth.OAuthConfig;
import io.leavesfly.joder.services.oauth.OAuthService;
import io.leavesfly.joder.services.oauth.TokenManager;
import io.leavesfly.joder.services.reminder.SystemReminderService;
import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.tools.ToolRegistry;
import io.leavesfly.joder.tools.ToolExecutor;
import io.leavesfly.joder.ui.renderer.DiffRenderer;
import io.leavesfly.joder.ui.renderer.SyntaxHighlighter;
import io.leavesfly.joder.ui.input.AdvancedInputHandler;
import io.leavesfly.joder.ui.input.CompletionRenderer;
import io.leavesfly.joder.ui.input.CancellationHandler;
import io.leavesfly.joder.ui.permission.PermissionDialog;
import io.leavesfly.joder.ui.style.OutputStyleManager;
import io.leavesfly.joder.screens.ReplScreen;
import io.leavesfly.joder.ui.components.MessageRenderer;
import io.leavesfly.joder.ui.theme.ThemeManager;

import io.leavesfly.joder.tools.bash.BashTool;
import io.leavesfly.joder.tools.file.FileReadTool;
import io.leavesfly.joder.tools.file.FileWriteTool;
import io.leavesfly.joder.tools.ls.LSTool;
import io.leavesfly.joder.tools.glob.GlobTool;
import io.leavesfly.joder.tools.grep.GrepTool;
import io.leavesfly.joder.tools.edit.FileEditTool;
import io.leavesfly.joder.tools.edit.MultiEditTool;
import io.leavesfly.joder.tools.edit.BatchEditTool;
import io.leavesfly.joder.tools.think.ThinkTool;
import io.leavesfly.joder.tools.todo.TodoWriteTool;
import io.leavesfly.joder.tools.task.TaskTool;
import io.leavesfly.joder.tools.web.WebSearchTool;
import io.leavesfly.joder.tools.web.URLFetcherTool;
import io.leavesfly.joder.tools.expert.AskExpertModelTool;
import io.leavesfly.joder.tools.completion.AttemptCompletionTool;
import io.leavesfly.joder.tools.filetree.FileTreeTool;
import io.leavesfly.joder.tools.web.InspectSiteTool;
import io.leavesfly.joder.tools.memory.MemoryTool;
import io.leavesfly.joder.tools.memory.MemoryReadTool;
import io.leavesfly.joder.tools.memory.MemoryWriteTool;
import io.leavesfly.joder.tools.notebook.NotebookEditTool;
import io.leavesfly.joder.tools.notebook.NotebookReadTool;
import io.leavesfly.joder.tools.architect.ArchitectTool;
import io.leavesfly.joder.tools.codebase.CodebaseSummaryTool;
import io.leavesfly.joder.tools.search.SmartSearchTool;
import io.leavesfly.joder.tools.search.SearchStrategyAnalyzer;
import io.leavesfly.joder.tools.search.SearchExecutor;

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
        
        // 核心控制流程
        bind(MainLoop.class).in(Singleton.class);
        
        // UI 组件
        bind(ThemeManager.class);
        bind(MessageRenderer.class);
        bind(ReplScreen.class);
        
        // UI 样式管理
        bind(OutputStyleManager.class).in(Singleton.class);
        
        // 权限管理
        bind(PermissionManager.class);
        
        // 命令系统
        bind(CostCommand.class);
        bind(DoctorCommand.class);
        bind(AgentsCommand.class);
        bind(ResumeCommand.class);
        bind(LoginCommand.class);
        bind(InitCommand.class);
        bind(ModelStatusCommand.class);
        bind(LogoutCommand.class);
        bind(ReviewCommand.class);
        bind(HistoryCommand.class);
        bind(ExportCommand.class);
        bind(ModeCommand.class);
        bind(UndoCommand.class);
        bind(RethinkCommand.class);
        bind(StyleCommand.class);
        // Phase 7: 开发工具和其他命令
        bind(ListenCommand.class);
        bind(BenchmarkCommand.class);
        bind(DebugCommand.class);
        bind(TestCommand.class);
        bind(CompactCommand.class);
        bind(BugCommand.class);
        bind(ReleaseNotesCommand.class);
        bind(RefreshCommandsCommand.class);
        
        // MCP 系统
        bind(McpServerManager.class);
        bind(McpToolRegistry.class);
        
        // 模型系统
        bind(ModelAdapterFactory.class);
        bind(ModelPointerManager.class);
        bind(ModelRouter.class).in(Singleton.class);
        
        // 补全系统
        bind(CompletionManager.class);
        bind(ModelCompletionProvider.class);
        bind(FileCompletionProvider.class);
        bind(CommandCompletionProvider.class);
        bind(CompletionService.class).in(Singleton.class);
        
        // 维护系统
        bind(MaintenanceScheduler.class).in(Singleton.class);
        
        // 自定义命令系统
        bind(CustomCommandService.class).in(Singleton.class);
        
        // OAuth 认证系统
        bind(OAuthConfig.class).in(Singleton.class);
        bind(OAuthService.class).in(Singleton.class);
        bind(TokenManager.class).in(Singleton.class);
        
        // 文件新鲜度追踪系统
        bind(FileFreshnessService.class).in(Singleton.class);
        
        // Mention 处理系统
        bind(MentionProcessor.class).in(Singleton.class);
        
        // 系统提醒服务
        bind(SystemReminderService.class).in(Singleton.class);
        
        // 项目记忆管理
        bind(ProjectMemoryManager.class).in(Singleton.class);
        
        // 上下文管理
        bind(TokenCounter.class).in(Singleton.class);
        bind(ContextCompressor.class).in(Singleton.class);
        
        // 缓存管理
        bind(FileContentCache.class).in(Singleton.class);
        
        // 成本追踪
        bind(CostTrackingService.class).in(Singleton.class);
        
        // UI 渲染器
        bind(SyntaxHighlighter.class).in(Singleton.class);
        bind(DiffRenderer.class).in(Singleton.class);
        
        // UI 输入处理
        bind(AdvancedInputHandler.class).in(Singleton.class);
        bind(CompletionRenderer.class).in(Singleton.class);
        bind(CancellationHandler.class).in(Singleton.class);
        
        // UI 权限对话框
        bind(PermissionDialog.class).in(Singleton.class);
        
        // Agents 系统
        bind(AgentsManager.class);
        bind(AgentExecutor.class).in(Singleton.class);
        
        // Hooks 系统
        bind(ApiKeyVerificationHook.class).in(Singleton.class);
        bind(CommandHistoryHook.class).in(Singleton.class);
        bind(ToolPermissionHook.class).in(Singleton.class);
        bind(DoublePressHook.class).in(Singleton.class);
        bind(MessageLogHook.class).in(Singleton.class);
        bind(StartupTimeHook.class).in(Singleton.class);
        bind(NotifyAfterTimeoutHook.class).in(Singleton.class);
        bind(TerminalSizeHook.class).in(Singleton.class);
        bind(UnifiedCompletionHook.class).in(Singleton.class);
        bind(CancelRequestHook.class).in(Singleton.class);
        
        // 工具执行器
        bind(ToolExecutor.class).in(Singleton.class);
        
        // 工具系统 - 通过 Provider 提供
        bind(FileReadTool.class);
        bind(FileWriteTool.class);
        bind(FileEditTool.class);
        bind(MultiEditTool.class);
        bind(BatchEditTool.class);
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
        bind(CodebaseSummaryTool.class);
        
        // 智能搜索工具
        bind(SearchStrategyAnalyzer.class).in(Singleton.class);
        bind(SearchExecutor.class).in(Singleton.class);
        bind(SmartSearchTool.class);
    }
    
    @Provides
    @Singleton
    ConfigManager provideConfigManager(@WorkingDirectory String workingDir) {
        return new ConfigManager(workingDir);
    }
    
    @Provides
    @Singleton
    ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }
    
    @Provides
    @Singleton
    ToolRegistry provideToolRegistry(
        FileReadTool fileReadTool,
        FileWriteTool fileWriteTool,
        FileEditTool fileEditTool,
        MultiEditTool multiEditTool,
        BatchEditTool batchEditTool,
        BashTool bashTool,
        LSTool lsTool,
        GlobTool globTool,
        GrepTool grepTool,
        SmartSearchTool smartSearchTool,
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
        CodebaseSummaryTool codebaseSummaryTool,
        McpServerManager mcpServerManager,
        McpToolRegistry mcpToolRegistry
    ) {
        ToolRegistry registry = new ToolRegistry();
        
        // 注册文件操作工具
        registry.registerTool(fileReadTool);
        registry.registerTool(fileWriteTool);
        registry.registerTool(fileEditTool);
        registry.registerTool(multiEditTool);
        registry.registerTool(batchEditTool);
        
        // 注册系统工具
        registry.registerTool(bashTool);
        
        // 注册搜索工具
        registry.registerTool(lsTool);
        registry.registerTool(globTool);
        registry.registerTool(grepTool);
        registry.registerTool(smartSearchTool);
        
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
        registry.registerTool(codebaseSummaryTool);
        
        // 配置 MCP 管理器
        registry.setMcpManagers(mcpServerManager, mcpToolRegistry);
        
        return registry;
    }
}
