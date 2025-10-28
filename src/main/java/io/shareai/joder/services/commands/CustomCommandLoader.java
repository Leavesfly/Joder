package io.shareai.joder.services.commands;

import io.shareai.joder.domain.CustomCommand;
import io.shareai.joder.domain.CustomCommand.CommandScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自定义命令加载器
 * 
 * <p>从指定目录扫描并加载 Markdown 格式的自定义命令文件。
 * 支持以下目录（按优先级排序）：
 * <ol>
 *   <li>项目级 Kode 命令：{project}/.kode/commands/</li>
 *   <li>项目级 Claude 命令：{project}/.claude/commands/</li>
 *   <li>用户级 Kode 命令：~/.kode/commands/</li>
 *   <li>用户级 Claude 命令：~/.claude/commands/</li>
 * </ol>
 * 
 * <p>目录结构支持命名空间：
 * <pre>
 * .kode/commands/
 * ├── test.md              -> project:test
 * └── development/
 *     └── build.md         -> project:development:build
 * </pre>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class CustomCommandLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomCommandLoader.class);
    
    private final FrontmatterParser parser;
    private final String workingDirectory;
    
    /**
     * 构造函数
     * 
     * @param workingDirectory 当前工作目录
     */
    public CustomCommandLoader(String workingDirectory) {
        this.parser = new FrontmatterParser();
        this.workingDirectory = workingDirectory;
    }
    
    /**
     * 加载所有自定义命令
     * 
     * @return 启用的自定义命令列表
     */
    public List<CustomCommand> loadAll() {
        List<CustomCommand> commands = new ArrayList<>();
        
        // 项目级目录（优先级更高）
        Path projectKodeDir = Paths.get(workingDirectory, ".kode", "commands");
        Path projectClaudeDir = Paths.get(workingDirectory, ".claude", "commands");
        
        // 用户级目录
        String userHome = System.getProperty("user.home");
        Path userKodeDir = Paths.get(userHome, ".kode", "commands");
        Path userClaudeDir = Paths.get(userHome, ".claude", "commands");
        
        // 按优先级加载（项目 > 用户，Kode > Claude）
        commands.addAll(loadFromDirectory(projectKodeDir, CommandScope.PROJECT));
        commands.addAll(loadFromDirectory(projectClaudeDir, CommandScope.PROJECT));
        commands.addAll(loadFromDirectory(userKodeDir, CommandScope.USER));
        commands.addAll(loadFromDirectory(userClaudeDir, CommandScope.USER));
        
        // 过滤启用的命令
        List<CustomCommand> enabledCommands = commands.stream()
                .filter(CustomCommand::isEnabled)
                .collect(Collectors.toList());
        
        logger.info("Loaded {} custom commands ({} total, {} enabled)",
                enabledCommands.size(), commands.size(), enabledCommands.size());
        
        return enabledCommands;
    }
    
    /**
     * 从指定目录加载命令
     */
    private List<CustomCommand> loadFromDirectory(Path directory, CommandScope scope) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            logger.debug("Directory not found: {}", directory);
            return Collections.emptyList();
        }
        
        List<CustomCommand> commands = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> markdownFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .collect(Collectors.toList());
            
            for (Path filePath : markdownFiles) {
                try {
                    CustomCommand command = loadFromFile(filePath, directory, scope);
                    if (command != null) {
                        commands.add(command);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load custom command from {}: {}",
                            filePath, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan directory {}: {}", directory, e.getMessage());
        }
        
        return commands;
    }
    
    /**
     * 从单个文件加载命令
     */
    private CustomCommand loadFromFile(Path filePath, Path baseDirectory, CommandScope scope) throws IOException {
        String content = Files.readString(filePath);
        FrontmatterParser.ParseResult result = parser.parse(content);
        
        CustomCommand command = new CustomCommand();
        command.setFilePath(filePath.toString());
        command.setScope(scope);
        command.setContent(result.getContent());
        
        // 应用 frontmatter
        Map<String, Object> frontmatter = result.getFrontmatter();
        FrontmatterParser.applyToCustomCommand(frontmatter, command);
        
        // 如果 frontmatter 中没有指定名称，从文件路径生成
        if (command.getName() == null || command.getName().isEmpty()) {
            String generatedName = generateCommandName(filePath, baseDirectory, scope);
            command.setName(generatedName);
        } else {
            // 确保名称有正确的前缀
            String prefix = scope == CommandScope.USER ? "user:" : "project:";
            if (!command.getName().startsWith(prefix)) {
                command.setName(prefix + command.getName());
            }
        }
        
        // 设置默认描述
        if (command.getDescription() == null || command.getDescription().isEmpty()) {
            command.setDescription("Custom command: " + command.getName());
        }
        
        // 设置默认进度消息
        if (command.getProgressMessage() == null || command.getProgressMessage().isEmpty()) {
            command.setProgressMessage("Running " + command.getName() + "...");
        }
        
        return command;
    }
    
    /**
     * 从文件路径生成命令名称
     * 
     * <p>示例：
     * <ul>
     *   <li>.kode/commands/test.md -> project:test</li>
     *   <li>.kode/commands/dev/build.md -> project:dev:build</li>
     * </ul>
     */
    private String generateCommandName(Path filePath, Path baseDirectory, CommandScope scope) {
        Path relativePath = baseDirectory.relativize(filePath);
        String pathString = relativePath.toString();
        
        // 移除 .md 扩展名
        if (pathString.endsWith(".md")) {
            pathString = pathString.substring(0, pathString.length() - 3);
        }
        
        // 将路径分隔符替换为冒号
        String namespace = pathString.replace("/", ":").replace("\\", ":");
        
        // 添加作用域前缀
        String prefix = scope == CommandScope.USER ? "user:" : "project:";
        return prefix + namespace;
    }
    
    /**
     * 重新加载所有命令
     * 
     * <p>清除缓存并重新扫描所有目录。
     */
    public List<CustomCommand> reload() {
        logger.info("Reloading custom commands...");
        return loadAll();
    }
}
