package io.shareai.joder.services.commands;

import io.shareai.joder.domain.CustomCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义命令执行器
 * 
 * <p>处理自定义命令的执行逻辑，支持：
 * <ul>
 *   <li>参数替换（$ARGUMENTS 和 {argName} 占位符）</li>
 *   <li>文件引用（@filepath 语法）</li>
 *   <li>Bash 命令执行（!`command` 语法）</li>
 *   <li>工具限制（allowed-tools 配置）</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class CustomCommandExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomCommandExecutor.class);
    
    // Bash 命令模式：!`command`
    private static final Pattern BASH_COMMAND_PATTERN = Pattern.compile("!`([^`]+)`");
    
    // 文件引用模式：@filepath
    private static final Pattern FILE_REFERENCE_PATTERN = Pattern.compile("@([a-zA-Z0-9/._-]+(?:\\.[a-zA-Z0-9]+)?)");
    
    // $ARGUMENTS 占位符
    private static final String ARGUMENTS_PLACEHOLDER = "$ARGUMENTS";
    
    // 命令执行超时（秒）
    private static final int BASH_COMMAND_TIMEOUT = 5;
    
    private final String workingDirectory;
    
    /**
     * 构造函数
     * 
     * @param workingDirectory 工作目录
     */
    public CustomCommandExecutor(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    /**
     * 执行自定义命令
     * 
     * @param command 要执行的命令
     * @param args 命令参数
     * @return 处理后的提示词内容
     */
    public String execute(CustomCommand command, String args) throws IOException {
        String prompt = command.getContent();
        
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Command content is empty");
        }
        
        logger.debug("Executing custom command: {}", command.getName());
        
        // 1. 处理参数替换
        prompt = substituteArguments(prompt, args, command.getArgNames());
        
        // 2. 执行 Bash 命令
        prompt = executeBashCommands(prompt);
        
        // 3. 解析文件引用
        prompt = resolveFileReferences(prompt);
        
        // 4. 添加工具限制提示（如果配置了）
        prompt = appendToolRestrictions(prompt, command.getAllowedTools());
        
        return prompt;
    }
    
    /**
     * 参数替换
     * 
     * <p>支持三种替换方式：
     * <ol>
     *   <li>$ARGUMENTS - 替换为完整参数</li>
     *   <li>{argName} - 命名参数替换（需要 argNames 配置）</li>
     *   <li>末尾追加 - 如果没有占位符，追加到提示词末尾</li>
     * </ol>
     */
    private String substituteArguments(String prompt, String args, List<String> argNames) {
        if (args == null || args.trim().isEmpty()) {
            return prompt;
        }
        
        // 1. 处理 $ARGUMENTS 占位符
        if (prompt.contains(ARGUMENTS_PLACEHOLDER)) {
            prompt = prompt.replace(ARGUMENTS_PLACEHOLDER, args);
            return prompt;
        }
        
        // 2. 处理命名参数 {argName}
        if (argNames != null && !argNames.isEmpty()) {
            String[] argValues = args.trim().split("\\s+");
            for (int i = 0; i < argNames.size() && i < argValues.length; i++) {
                String argName = argNames.get(i);
                String argValue = argValues[i];
                String placeholder = "{" + argName + "}";
                prompt = prompt.replace(placeholder, argValue);
            }
            return prompt;
        }
        
        // 3. 如果没有占位符，追加到末尾
        prompt += "\n\nAdditional context: " + args;
        return prompt;
    }
    
    /**
     * 执行 Bash 命令
     * 
     * <p>识别 !`command` 语法并执行命令，将输出替换到原位置。
     * 命令在当前工作目录执行，超时时间为 5 秒。
     */
    private String executeBashCommands(String prompt) {
        Matcher matcher = BASH_COMMAND_PATTERN.matcher(prompt);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String command = matcher.group(1).trim();
            String output;
            
            try {
                output = executeSingleBashCommand(command);
            } catch (Exception e) {
                logger.warn("Failed to execute bash command '{}': {}", command, e.getMessage());
                output = "(error executing: " + command + ")";
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(output));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 执行单个 Bash 命令
     */
    private String executeSingleBashCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // 使用 sh -c 执行命令（兼容性更好）
        processBuilder.command("sh", "-c", command);
        processBuilder.directory(Paths.get(workingDirectory).toFile());
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // 等待进程结束（带超时）
        boolean finished = process.waitFor(BASH_COMMAND_TIMEOUT, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroy();
            throw new IOException("Command timed out after " + BASH_COMMAND_TIMEOUT + " seconds");
        }
        
        String result = output.toString().trim();
        return result.isEmpty() ? "(no output)" : result;
    }
    
    /**
     * 解析文件引用
     * 
     * <p>识别 @filepath 语法，读取文件内容并格式化为 Markdown 代码块。
     */
    private String resolveFileReferences(String prompt) {
        Matcher matcher = FILE_REFERENCE_PATTERN.matcher(prompt);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String filePath = matcher.group(1);
            
            // 跳过 agent 和 ask-model 引用
            if (filePath.startsWith("run-agent-") || 
                filePath.startsWith("agent-") || 
                filePath.startsWith("ask-")) {
                continue;
            }
            
            String fileContent;
            try {
                fileContent = readFileContent(filePath);
            } catch (Exception e) {
                logger.warn("Failed to read file '{}': {}", filePath, e.getMessage());
                fileContent = "(file not found: " + filePath + ")";
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(fileContent));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 读取文件内容并格式化
     */
    private String readFileContent(String filePath) throws IOException {
        Path fullPath = Paths.get(workingDirectory).resolve(filePath);
        
        if (!Files.exists(fullPath)) {
            return "(file not found: " + filePath + ")";
        }
        
        if (!Files.isRegularFile(fullPath)) {
            return "(not a file: " + filePath + ")";
        }
        
        String content = Files.readString(fullPath);
        
        // 格式化为 Markdown 代码块
        return String.format("\n\n## File: %s\n```\n%s\n```\n", filePath, content);
    }
    
    /**
     * 添加工具限制提示
     */
    private String appendToolRestrictions(String prompt, List<String> allowedTools) {
        if (allowedTools == null || allowedTools.isEmpty()) {
            return prompt;
        }
        
        String toolList = String.join(", ", allowedTools);
        String restriction = String.format(
            "\n\nIMPORTANT: You are restricted to using only these tools: %s. " +
            "Do not use any other tools even if they might be helpful for the task.",
            toolList
        );
        
        return prompt + restriction;
    }
}
