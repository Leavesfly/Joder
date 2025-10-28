package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.domain.AgentConfig;
import io.shareai.joder.services.agents.AgentsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Review Command - 代码审查命令
 * 使用 AI 进行代码审查和质量分析
 */
@Singleton
public class ReviewCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewCommand.class);
    
    private final AgentsManager agentsManager;
    
    @Inject
    public ReviewCommand(AgentsManager agentsManager) {
        this.agentsManager = agentsManager;
    }
    
    @Override
    public String getDescription() {
        return "AI 代码审查和质量分析";
    }
    
    @Override
    public String getUsage() {
        return "review [文件路径|目录] [--agent <agent-name>] - 审查代码";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            // 解析参数
            ReviewOptions options = parseArguments(args);
            
            // 获取审查代理
            AgentConfig agent = getReviewAgent(options.agentName);
            
            // 收集需要审查的文件
            List<Path> filesToReview = collectFiles(options.target);
            
            if (filesToReview.isEmpty()) {
                return CommandResult.error("未找到需要审查的文件");
            }
            
            // 生成审查报告
            String report = generateReviewReport(filesToReview, agent, options);
            
            return CommandResult.success(report);
            
        } catch (Exception e) {
            logger.error("Failed to execute review command", e);
            return CommandResult.error("代码审查失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析命令参数
     */
    private ReviewOptions parseArguments(String args) {
        ReviewOptions options = new ReviewOptions();
        
        if (args == null || args.trim().isEmpty()) {
            options.target = ".";  // 默认当前目录
            return options;
        }
        
        String[] parts = args.trim().split("\\s+");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.equals("--agent") && i + 1 < parts.length) {
                options.agentName = parts[++i];
            } else if (part.equals("--depth") && i + 1 < parts.length) {
                try {
                    options.maxDepth = Integer.parseInt(parts[++i]);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid depth value: {}", parts[i]);
                }
            } else if (!part.startsWith("--")) {
                options.target = part;
            }
        }
        
        return options;
    }
    
    /**
     * 获取审查代理
     */
    private AgentConfig getReviewAgent(String agentName) {
        if (agentName != null) {
            return agentsManager.getAgent(agentName)
                    .orElse(AgentConfig.createCodeExpert());
        }
        
        // 优先使用 code-expert 代理
        return agentsManager.getAgent("code-expert")
                .orElse(AgentConfig.createCodeExpert());
    }
    
    /**
     * 收集需要审查的文件
     */
    private List<Path> collectFiles(String target) throws IOException {
        List<Path> files = new ArrayList<>();
        Path targetPath = Paths.get(target);
        
        if (!Files.exists(targetPath)) {
            return files;
        }
        
        if (Files.isRegularFile(targetPath)) {
            // 单个文件
            if (isSourceFile(targetPath)) {
                files.add(targetPath);
            }
        } else if (Files.isDirectory(targetPath)) {
            // 目录，扫描源代码文件
            try (Stream<Path> paths = Files.walk(targetPath, 3)) {
                paths.filter(Files::isRegularFile)
                     .filter(this::isSourceFile)
                     .filter(path -> !isIgnored(path))
                     .limit(20)  // 限制文件数量
                     .forEach(files::add);
            }
        }
        
        return files;
    }
    
    /**
     * 检查是否为源代码文件
     */
    private boolean isSourceFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".java") ||
               fileName.endsWith(".js") ||
               fileName.endsWith(".ts") ||
               fileName.endsWith(".jsx") ||
               fileName.endsWith(".tsx") ||
               fileName.endsWith(".py") ||
               fileName.endsWith(".go") ||
               fileName.endsWith(".rs") ||
               fileName.endsWith(".c") ||
               fileName.endsWith(".cpp") ||
               fileName.endsWith(".h");
    }
    
    /**
     * 检查是否应该忽略
     */
    private boolean isIgnored(Path path) {
        String pathStr = path.toString();
        return pathStr.contains("/target/") ||
               pathStr.contains("/build/") ||
               pathStr.contains("/node_modules/") ||
               pathStr.contains("/.git/") ||
               pathStr.contains("/dist/");
    }
    
    /**
     * 生成审查报告
     */
    private String generateReviewReport(List<Path> files, AgentConfig agent, ReviewOptions options) {
        StringBuilder report = new StringBuilder();
        
        report.append("📋 代码审查报告\n\n");
        report.append("👤 审查代理: ").append(agent.getName()).append("\n");
        report.append("📁 审查范围: ").append(options.target).append("\n");
        report.append("📄 文件数量: ").append(files.size()).append("\n\n");
        
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        // 文件列表
        report.append("📂 待审查文件:\n\n");
        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            Path relativePath = Paths.get(options.target).relativize(file);
            report.append(String.format("  %d. %s\n", i + 1, relativePath));
        }
        
        report.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        // 审查建议
        report.append("💡 审查重点:\n\n");
        report.append("  ✓ 代码质量和可读性\n");
        report.append("  ✓ 潜在的 Bug 和安全问题\n");
        report.append("  ✓ 性能优化机会\n");
        report.append("  ✓ 最佳实践遵循情况\n");
        report.append("  ✓ 代码结构和设计模式\n\n");
        
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        // 使用提示
        report.append("🚀 下一步操作:\n\n");
        report.append("  1. 使用 AskExpertModelTool 进行详细审查\n");
        report.append("     示例: ask_expert_model(expert_type=\"code-expert\", ...)\n\n");
        report.append("  2. 逐个文件审查\n");
        report.append("     示例: /review <具体文件路径>\n\n");
        report.append("  3. 指定特定代理\n");
        report.append("     示例: /review . --agent my-reviewer\n\n");
        
        report.append("💡 提示: 当前为概览模式，详细审查需要调用 AI 模型。\n");
        
        return report.toString();
    }
    
    /**
     * 审查选项
     */
    private static class ReviewOptions {
        String target = ".";
        String agentName = null;
        int maxDepth = 3;
    }
}
