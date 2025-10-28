package io.shareai.joder.cli.commands;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import io.shareai.joder.core.config.ConfigManager;
import io.shareai.joder.services.memory.ProjectMemoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

/**
 * /init 命令 - 初始化项目配置
 */
public class InitCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(InitCommand.class);
    
    private final ConfigManager configManager;
    private final ProjectMemoryManager projectMemoryManager;
    
    @Inject
    public InitCommand(ConfigManager configManager, ProjectMemoryManager projectMemoryManager) {
        this.configManager = configManager;
        this.projectMemoryManager = projectMemoryManager;
    }
    
    @Override
    public String getDescription() {
        return "初始化项目配置，创建 .joder 目录和默认配置文件";
    }
    
    @Override
    public String getUsage() {
        return "init [无参数]";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
            logger.info("Initializing project at: {}", projectRoot);
            
            StringBuilder output = new StringBuilder();
            output.append("🚀 初始化 Joder 项目配置\n\n");
            
            // 1. 创建 .joder 目录
            Path joderDir = projectRoot.resolve(".joder");
            if (!Files.exists(joderDir)) {
                Files.createDirectories(joderDir);
                output.append("✓ 创建目录: .joder/\n");
                logger.info("Created .joder directory");
            } else {
                output.append("⊙ 目录已存在: .joder/\n");
            }
            
            // 2. 创建默认配置文件
            Path configFile = joderDir.resolve("config.conf");
            if (!Files.exists(configFile)) {
                String defaultConfig = generateDefaultConfig();
                Files.writeString(configFile, defaultConfig);
                output.append("✓ 创建配置文件: .joder/config.conf\n");
                logger.info("Created config file");
            } else {
                output.append("⊙ 配置文件已存在: .joder/config.conf\n");
            }
            
            // 3. 更新 .gitignore
            Path gitignore = projectRoot.resolve(".gitignore");
            boolean updated = updateGitignore(gitignore);
            if (updated) {
                output.append("✓ 更新 .gitignore\n");
            } else {
                output.append("⊙ .gitignore 已包含 .joder/ 规则\n");
            }
            
            // 4. 初始化项目记忆 (claude.md)
            if (!projectMemoryManager.exists()) {
                projectMemoryManager.initialize(false);
                output.append("✓ 创建项目记忆文件: .joder/claude.md\n");
                logger.info("Created project memory file");
            } else {
                output.append("⊚ 项目记忆文件已存在: .joder/claude.md\n");
            }
            
            // 5. 扫描项目结构
            output.append("\n📁 项目结构扫描:\n");
            ProjectInfo projectInfo = scanProjectStructure(projectRoot);
            output.append(projectInfo.getSummary());
            
            // 6. 显示配置建议
            output.append("\n💡 配置建议:\n");
            output.append("  1. 编辑 .joder/config.conf 配置模型 API Key\n");
            output.append("  2. 编辑 .joder/claude.md 添加项目具体信息、规范和偏好\n");
            output.append("  3. 运行 /model 命令配置默认模型\n");
            output.append("  4. 运行 /doctor 命令检查环境配置\n");
            
            output.append("\n✅ 项目初始化完成！\n");
            
            return CommandResult.success(output.toString());
            
        } catch (IOException e) {
            logger.error("Failed to initialize project", e);
            return CommandResult.error("初始化失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during initialization", e);
            return CommandResult.error("初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成默认配置
     */
    private String generateDefaultConfig() {
        return """
                joder {
                  # 主题设置
                  theme = "dark"
                  
                  # 语言设置
                  language = "zh-CN"
                  
                  # 模型配置
                  model {
                    # 默认模型
                    default = "claude-3-sonnet"
                    
                    # 模型配置示例
                    profiles {
                      claude-3-sonnet {
                        provider = "anthropic"
                        model = "claude-3-5-sonnet-20241022"
                        apiKey = ${?ANTHROPIC_API_KEY}
                        maxTokens = 8096
                        temperature = 1.0
                      }
                      
                      # 添加更多模型配置...
                    }
                    
                    # 模型指针（用于多模型协作）
                    pointers {
                      main = "claude-3-sonnet"
                      task = "qwen-coder"
                      reasoning = "o3"
                      quick = "glm-4.5"
                    }
                  }
                  
                  # 权限设置
                  permissions {
                    mode = "default"  # default, yolo, safe, paranoid
                  }
                  
                  # 工具配置
                  tools {
                    # 启用的工具列表
                    enabled = ["*"]
                  }
                }
                """;
    }
    
    /**
     * 更新 .gitignore
     */
    private boolean updateGitignore(Path gitignore) throws IOException {
        String joderRule = ".joder/";
        
        if (Files.exists(gitignore)) {
            // 检查是否已存在
            String content = Files.readString(gitignore);
            if (content.contains(joderRule)) {
                return false;  // 已存在
            }
            
            // 追加规则
            Files.writeString(gitignore, "\n# Joder\n" + joderRule + "\n",
                    StandardOpenOption.APPEND);
        } else {
            // 创建新文件
            Files.writeString(gitignore, "# Joder\n" + joderRule + "\n");
        }
        
        return true;
    }
    
    /**
     * 扫描项目结构
     */
    private ProjectInfo scanProjectStructure(Path projectRoot) {
        ProjectInfo info = new ProjectInfo();
        
        try (Stream<Path> paths = Files.walk(projectRoot, 3)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String fileName = path.getFileName().toString();
                     
                     // 检测项目类型
                     if (fileName.equals("pom.xml")) {
                         info.projectType = "Maven";
                         info.hasBuildFile = true;
                     } else if (fileName.equals("build.gradle") || fileName.equals("build.gradle.kts")) {
                         info.projectType = "Gradle";
                         info.hasBuildFile = true;
                     } else if (fileName.equals("package.json")) {
                         info.projectType = "Node.js";
                         info.hasBuildFile = true;
                     }
                     
                     // 统计文件
                     String extension = getFileExtension(fileName);
                     info.fileCount++;
                     info.filesByType.put(extension, 
                             info.filesByType.getOrDefault(extension, 0) + 1);
                 });
        } catch (IOException e) {
            logger.warn("Failed to scan project structure", e);
        }
        
        return info;
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "other";
    }
    
    /**
     * 项目信息
     */
    private static class ProjectInfo {
        String projectType = "Unknown";
        boolean hasBuildFile = false;
        int fileCount = 0;
        java.util.Map<String, Integer> filesByType = new java.util.HashMap<>();
        
        String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  项目类型: %s\n", projectType));
            sb.append(String.format("  文件总数: %d\n", fileCount));
            
            // 显示主要文件类型
            filesByType.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .forEach(entry -> sb.append(String.format("    - .%s: %d 个\n", 
                            entry.getKey(), entry.getValue())));
            
            return sb.toString();
        }
    }
}
