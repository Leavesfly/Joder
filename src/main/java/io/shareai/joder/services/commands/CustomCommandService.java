package io.shareai.joder.services.commands;

import io.shareai.joder.WorkingDirectory;
import io.shareai.joder.domain.CustomCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 自定义命令服务
 * 
 * <p>统一管理自定义命令的生命周期，包括加载、缓存、执行等。
 * 作为单例服务，确保命令只加载一次并可以被多处引用。
 * 
 * <p>使用示例：
 * <pre>
 * CustomCommandService service = injector.getInstance(CustomCommandService.class);
 * List&lt;CustomCommand&gt; commands = service.getAllCommands();
 * Optional&lt;CustomCommand&gt; cmd = service.findCommand("project:test");
 * String prompt = service.executeCommand(cmd.get(), "arg1 arg2");
 * </pre>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
@Singleton
public class CustomCommandService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomCommandService.class);
    
    private final CustomCommandLoader loader;
    private final CustomCommandExecutor executor;
    
    // 命令缓存
    private List<CustomCommand> commandCache;
    private long lastLoadTime;
    private static final long CACHE_TTL_MS = 60_000; // 1分钟缓存
    
    /**
     * 构造函数（通过依赖注入）
     * 
     * @param workingDirectory 当前工作目录
     */
    @Inject
    public CustomCommandService(@WorkingDirectory String workingDirectory) {
        this.loader = new CustomCommandLoader(workingDirectory);
        this.executor = new CustomCommandExecutor(workingDirectory);
        this.commandCache = null;
        this.lastLoadTime = 0;
    }
    
    /**
     * 获取所有启用的自定义命令
     * 
     * <p>结果会被缓存，缓存有效期为 1 分钟。
     * 
     * @return 自定义命令列表
     */
    public List<CustomCommand> getAllCommands() {
        long now = System.currentTimeMillis();
        
        // 检查缓存是否有效
        if (commandCache != null && (now - lastLoadTime) < CACHE_TTL_MS) {
            return new ArrayList<>(commandCache);
        }
        
        // 重新加载
        try {
            commandCache = loader.loadAll();
            lastLoadTime = now;
            logger.info("Loaded {} custom commands", commandCache.size());
        } catch (Exception e) {
            logger.error("Failed to load custom commands", e);
            commandCache = new ArrayList<>();
        }
        
        return new ArrayList<>(commandCache);
    }
    
    /**
     * 根据名称或别名查找命令
     * 
     * @param commandName 命令名称（支持完整名称或别名）
     * @return 找到的命令，如果不存在则返回 Optional.empty()
     */
    public Optional<CustomCommand> findCommand(String commandName) {
        if (commandName == null || commandName.trim().isEmpty()) {
            return Optional.empty();
        }
        
        List<CustomCommand> commands = getAllCommands();
        
        return commands.stream()
                .filter(cmd -> cmd.matches(commandName))
                .findFirst();
    }
    
    /**
     * 检查命令是否存在
     * 
     * @param commandName 命令名称
     * @return 如果命令存在返回 true
     */
    public boolean hasCommand(String commandName) {
        return findCommand(commandName).isPresent();
    }
    
    /**
     * 执行自定义命令
     * 
     * @param command 要执行的命令
     * @param args 命令参数（可以为 null 或空字符串）
     * @return 处理后的提示词内容
     * @throws IOException 如果执行过程中发生 IO 错误
     */
    public String executeCommand(CustomCommand command, String args) throws IOException {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        
        logger.debug("Executing custom command: {} with args: {}", 
                command.getName(), args);
        
        return executor.execute(command, args);
    }
    
    /**
     * 刷新命令缓存
     * 
     * <p>强制重新扫描命令目录，忽略缓存。
     */
    public void refreshCommands() {
        logger.info("Refreshing custom commands cache...");
        commandCache = null;
        lastLoadTime = 0;
        getAllCommands();
    }
    
    /**
     * 获取所有命令的名称列表（用于补全）
     * 
     * @return 命令名称列表
     */
    public List<String> getCommandNames() {
        return getAllCommands().stream()
                .map(CustomCommand::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有命令的别名列表（用于补全）
     * 
     * @return 别名列表
     */
    public List<String> getCommandAliases() {
        return getAllCommands().stream()
                .flatMap(cmd -> cmd.getAliases().stream())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有可见命令（不包括隐藏的）
     * 
     * @return 可见命令列表
     */
    public List<CustomCommand> getVisibleCommands() {
        return getAllCommands().stream()
                .filter(cmd -> !cmd.isHidden())
                .collect(Collectors.toList());
    }
    
    /**
     * 按作用域过滤命令
     * 
     * @param scope 命令作用域
     * @return 指定作用域的命令列表
     */
    public List<CustomCommand> getCommandsByScope(CustomCommand.CommandScope scope) {
        return getAllCommands().stream()
                .filter(cmd -> cmd.getScope() == scope)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户级命令
     */
    public List<CustomCommand> getUserCommands() {
        return getCommandsByScope(CustomCommand.CommandScope.USER);
    }
    
    /**
     * 获取项目级命令
     */
    public List<CustomCommand> getProjectCommands() {
        return getCommandsByScope(CustomCommand.CommandScope.PROJECT);
    }
}
