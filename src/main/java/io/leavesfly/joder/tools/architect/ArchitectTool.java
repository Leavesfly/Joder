package io.leavesfly.joder.tools.architect;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.services.model.ModelAdapterFactory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Architect Tool - 架构分析工具
 * 
 * 使用 AI 分析项目架构、技术栈和设计模式
 */
@Singleton
public class ArchitectTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(ArchitectTool.class);
    
    private final String workingDirectory;
    private final ModelAdapterFactory modelAdapterFactory;
    
    @Inject
    public ArchitectTool(@WorkingDirectory String workingDirectory,
                         ModelAdapterFactory modelAdapterFactory) {
        this.workingDirectory = workingDirectory;
        this.modelAdapterFactory = modelAdapterFactory;
    }
    
    @Override
    public String getName() {
        return "Architect";
    }
    
    @Override
    public String getDescription() {
        return "使用 AI 分析项目架构、技术栈和设计模式。\n" +
               "- 分析项目结构和组织\n" +
               "- 识别技术栈和框架\n" +
               "- 提取设计模式\n" +
               "- 生成架构建议";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具分析项目的架构和设计。\n\n" +
               "**参数**：\n" +
               "- prompt: 要分析的技术请求或编码任务（必需）\n" +
               "- context: 可选的上下文信息\n" +
               "- directory: 要分析的目录（默认：当前目录）\n\n" +
               "**分析维度**：\n" +
               "1. 项目结构\n" +
               "   - 目录组织\n" +
               "   - 模块划分\n" +
               "   - 依赖关系\n\n" +
               "2. 技术栈\n" +
               "   - 编程语言\n" +
               "   - 框架和库\n" +
               "   - 构建工具\n\n" +
               "3. 设计模式\n" +
               "   - 架构模式（MVC、分层等）\n" +
               "   - 设计模式使用\n" +
               "   - 代码组织方式\n\n" +
               "4. 建议\n" +
               "   - 改进建议\n" +
               "   - 最佳实践\n" +
               "   - 潜在问题\n\n" +
               "**适用场景**：\n" +
               "- 新项目分析\n" +
               "- 重构规划\n" +
               "- 技术选型\n" +
               "- 代码审查\n\n" +
               "**示例**：\n" +
               "```json\n" +
               "{\n" +
               "  \"prompt\": \"分析这个 Java 项目的架构，识别使用的设计模式\",\n" +
               "  \"directory\": \"./src\"\n" +
               "}\n" +
               "```";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true; // 只读分析
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String prompt = (String) input.get("prompt");
        String context = (String) input.get("context");
        String directory = (String) input.getOrDefault("directory", ".");
        
        // 验证参数
        if (prompt == null || prompt.trim().isEmpty()) {
            return ToolResult.error("prompt 不能为空");
        }
        
        try {
            // 解析目录
            Path dirPath = Paths.get(workingDirectory).resolve(directory).normalize();
            
            if (!Files.exists(dirPath)) {
                return ToolResult.error("目录不存在: " + directory);
            }
            
            if (!Files.isDirectory(dirPath)) {
                return ToolResult.error("路径不是目录: " + directory);
            }
            
            logger.info("开始架构分析: {}", dirPath);
            
            // 收集项目信息
            ProjectInfo projectInfo = analyzeProject(dirPath);
            
            // 构建分析请求
            String analysisPrompt = buildAnalysisPrompt(prompt, context, projectInfo);
            
            // 使用 AI 模型进行分析
            ModelAdapter adapter = modelAdapterFactory.createAdapter("gpt-4");
            List<Message> messages = new ArrayList<>();
            messages.add(new Message(MessageRole.USER, analysisPrompt));
            
            String systemPrompt = 
                "你是一个专业的软件架构师。你的任务是分析项目的架构、技术栈和设计模式，" +
                "并提供专业的建议和最佳实践。请以清晰、结构化的方式组织你的分析结果。";
            
            String analysis = adapter.sendMessage(messages, systemPrompt);
            
            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("🏗️  架构分析报告\n\n");
            result.append("═".repeat(60)).append("\n\n");
            
            result.append("📊 项目概览\n");
            result.append("─".repeat(60)).append("\n");
            result.append(String.format("项目路径:     %s\n", dirPath));
            result.append(String.format("总文件数:     %d\n", projectInfo.fileCount));
            result.append(String.format("代码文件:     %d\n", projectInfo.codeFileCount));
            result.append(String.format("主要语言:     %s\n\n", 
                String.join(", ", projectInfo.languages)));
            
            result.append("🤖 AI 分析\n");
            result.append("─".repeat(60)).append("\n");
            result.append(analysis).append("\n\n");
            
            result.append("═".repeat(60)).append("\n");
            
            return ToolResult.success(result.toString());
            
        } catch (Exception e) {
            logger.error("架构分析失败", e);
            return ToolResult.error("分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 分析项目信息
     */
    private ProjectInfo analyzeProject(Path directory) throws IOException {
        ProjectInfo info = new ProjectInfo();
        
        try (Stream<Path> paths = Files.walk(directory, 5)) {
            List<Path> files = paths
                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains("/.") && !p.toString().contains("\\."))
                .collect(Collectors.toList());
            
            info.fileCount = files.size();
            
            // 分析文件类型
            Map<String, Integer> langCount = new HashMap<>();
            for (Path file : files) {
                String ext = getFileExtension(file);
                String lang = mapExtensionToLanguage(ext);
                if (lang != null) {
                    info.codeFileCount++;
                    langCount.put(lang, langCount.getOrDefault(lang, 0) + 1);
                }
            }
            
            // 提取主要语言（前3个）
            info.languages = langCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            // 检测配置文件
            info.buildFiles = files.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.matches("pom\\.xml|build\\.gradle|package\\.json|Cargo\\.toml|go\\.mod"))
                .collect(Collectors.toList());
        }
        
        return info;
    }
    
    /**
     * 构建分析提示
     */
    private String buildAnalysisPrompt(String prompt, String context, ProjectInfo projectInfo) {
        StringBuilder sb = new StringBuilder();
        
        if (context != null && !context.isEmpty()) {
            sb.append("**上下文**:\n").append(context).append("\n\n");
        }
        
        sb.append("**项目信息**:\n");
        sb.append(String.format("- 总文件数: %d\n", projectInfo.fileCount));
        sb.append(String.format("- 代码文件数: %d\n", projectInfo.codeFileCount));
        sb.append(String.format("- 主要语言: %s\n", String.join(", ", projectInfo.languages)));
        
        if (!projectInfo.buildFiles.isEmpty()) {
            sb.append(String.format("- 构建文件: %s\n", String.join(", ", projectInfo.buildFiles)));
        }
        
        sb.append("\n**分析请求**:\n");
        sb.append(prompt);
        
        return sb.toString();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(Path file) {
        String name = file.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }
    
    /**
     * 映射扩展名到语言
     */
    private String mapExtensionToLanguage(String ext) {
        return switch (ext.toLowerCase()) {
            case "java" -> "Java";
            case "kt", "kts" -> "Kotlin";
            case "py" -> "Python";
            case "js", "jsx" -> "JavaScript";
            case "ts", "tsx" -> "TypeScript";
            case "go" -> "Go";
            case "rs" -> "Rust";
            case "rb" -> "Ruby";
            case "php" -> "PHP";
            case "c", "h" -> "C";
            case "cpp", "cc", "cxx", "hpp" -> "C++";
            case "cs" -> "C#";
            case "swift" -> "Swift";
            default -> null;
        };
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String prompt = (String) input.get("prompt");
        return String.format("分析项目架构: %s", 
            prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 架构分析完成";
        } else {
            return "❌ 架构分析失败: " + result.getError();
        }
    }
    
    /**
     * 项目信息类
     */
    private static class ProjectInfo {
        int fileCount = 0;
        int codeFileCount = 0;
        List<String> languages = new ArrayList<>();
        List<String> buildFiles = new ArrayList<>();
    }
}
