package io.leavesfly.joder.services.memory;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * 项目记忆管理器
 * <p>
 * 负责管理 .joder/claude.md 文件,这是 AI 理解项目的核心依据
 * 借鉴 Claude Code 的设计理念,提供结构化的项目记忆
 * </p>
 */
@Singleton
public class ProjectMemoryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectMemoryManager.class);
    
    private static final String MEMORY_DIR = ".joder";
    private static final String MEMORY_FILE = "claude.md";
    
    private final String workingDirectory;
    private final ConfigManager configManager;
    
    private String cachedMemoryContent;
    private Instant lastLoadTime;
    
    @Inject
    public ProjectMemoryManager(
            @WorkingDirectory String workingDirectory,
            ConfigManager configManager) {
        this.workingDirectory = workingDirectory;
        this.configManager = configManager;
        this.cachedMemoryContent = null;
        this.lastLoadTime = null;
    }
    
    /**
     * 获取项目记忆文件路径
     */
    public Path getMemoryFilePath() {
        return Paths.get(workingDirectory, MEMORY_DIR, MEMORY_FILE);
    }
    
    /**
     * 检查项目记忆文件是否存在
     */
    public boolean exists() {
        return Files.exists(getMemoryFilePath());
    }
    
    /**
     * 加载项目记忆内容
     * 
     * @return 项目记忆的完整内容,如果不存在则返回空字符串
     */
    public String load() {
        return load(false);
    }
    
    /**
     * 加载项目记忆内容
     * 
     * @param forceReload 是否强制重新加载(忽略缓存)
     * @return 项目记忆的完整内容,如果不存在则返回空字符串
     */
    public String load(boolean forceReload) {
        Path memoryPath = getMemoryFilePath();
        
        if (!Files.exists(memoryPath)) {
            logger.debug("Project memory file not found: {}", memoryPath);
            return "";
        }
        
        try {
            // 检查是否需要重新加载
            if (!forceReload && cachedMemoryContent != null) {
                Instant fileModifiedTime = Files.getLastModifiedTime(memoryPath).toInstant();
                if (lastLoadTime != null && !fileModifiedTime.isAfter(lastLoadTime)) {
                    logger.debug("Using cached project memory");
                    return cachedMemoryContent;
                }
            }
            
            // 读取文件内容
            String content = Files.readString(memoryPath, StandardCharsets.UTF_8);
            
            // 更新缓存
            cachedMemoryContent = content;
            lastLoadTime = Instant.now();
            
            logger.info("Loaded project memory from: {}", memoryPath);
            return content;
            
        } catch (IOException e) {
            logger.error("Failed to load project memory", e);
            return "";
        }
    }
    
    /**
     * 保存项目记忆内容
     * 
     * @param content 要保存的内容
     */
    public void save(String content) throws IOException {
        Path memoryPath = getMemoryFilePath();
        
        // 确保目录存在
        Files.createDirectories(memoryPath.getParent());
        
        // 写入文件
        Files.writeString(memoryPath, content, StandardCharsets.UTF_8);
        
        // 更新缓存
        cachedMemoryContent = content;
        lastLoadTime = Instant.now();
        
        logger.info("Saved project memory to: {}", memoryPath);
    }
    
    /**
     * 生成初始项目记忆
     * 
     * @return 生成的项目记忆内容
     */
    public String generateInitialMemory() {
        logger.info("Generating initial project memory...");
        
        ProjectAnalyzer analyzer = new ProjectAnalyzer(workingDirectory);
        
        StringBuilder md = new StringBuilder();
        
        // 文档头部
        md.append("# Project Memory\n\n");
        md.append("*This file helps AI understand the project structure and conventions.*\n");
        md.append("*Last updated: ").append(Instant.now()).append("*\n\n");
        md.append("---\n\n");
        
        // Architecture 章节
        md.append("## Architecture\n\n");
        md.append("### Project Overview\n\n");
        md.append("- **Project Name**: ").append(analyzer.getProjectName()).append("\n");
        md.append("- **Type**: ").append(analyzer.detectProjectType()).append("\n");
        md.append("- **Build Tool**: ").append(analyzer.detectBuildTool()).append("\n");
        md.append("- **Working Directory**: `").append(workingDirectory).append("`\n\n");
        
        md.append("### Technology Stack\n\n");
        analyzer.detectTechStack().forEach((key, value) -> {
            md.append("- **").append(key).append("**: ").append(value).append("\n");
        });
        md.append("\n");
        
        md.append("### Directory Structure\n\n");
        md.append("```\n");
        md.append(analyzer.generateDirectoryTree());
        md.append("```\n\n");
        
        // Conventions 章节
        md.append("## Conventions\n\n");
        md.append("### Coding Standards\n\n");
        md.append("- Follow standard ").append(analyzer.detectProjectType()).append(" naming conventions\n");
        md.append("- Use meaningful variable and method names\n");
        md.append("- Add Javadoc for public APIs\n\n");
        
        // Decisions 章节
        md.append("## Decisions\n\n");
        md.append("*Record important technical decisions here.*\n\n");
        md.append("### Example Decision\n\n");
        md.append("- **Date**: ").append(Instant.now().toString().substring(0, 10)).append("\n");
        md.append("- **Decision**: [Describe the decision]\n");
        md.append("- **Rationale**: [Explain why]\n");
        md.append("- **Alternatives Considered**: [What else was considered]\n\n");
        
        // Preferences 章节
        md.append("## Preferences\n\n");
        md.append("*Define your development preferences here.*\n\n");
        md.append("- **Preferred Libraries**: [List preferred libraries]\n");
        md.append("- **Code Style**: [Define coding style preferences]\n");
        md.append("- **Testing Framework**: ").append(analyzer.detectTestFramework()).append("\n\n");
        
        // Context 章节
        md.append("## Context\n\n");
        md.append("*Current development context and open tasks.*\n\n");
        md.append("- **Current Phase**: Initial Setup\n");
        md.append("- **Active Features**: None\n");
        md.append("- **Known Issues**: None\n\n");
        
        // Constraints 章节
        md.append("## Constraints\n\n");
        md.append("*Define limitations and restrictions.*\n\n");
        md.append("- **Restricted Directories**: `.git/`, `target/`, `build/`\n");
        md.append("- **Prohibited Libraries**: [List any libraries to avoid]\n");
        md.append("- **Security Considerations**: [Any security constraints]\n\n");
        
        return md.toString();
    }
    
    /**
     * 初始化项目记忆
     * 如果文件不存在,则生成初始内容
     * 
     * @param force 是否强制重新生成(即使文件已存在)
     * @return 是否成功初始化
     */
    public boolean initialize(boolean force) {
        if (!force && exists()) {
            logger.info("Project memory already exists");
            return true;
        }
        
        try {
            String initialContent = generateInitialMemory();
            save(initialContent);
            logger.info("Project memory initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize project memory", e);
            return false;
        }
    }
    
    /**
     * 更新项目记忆的特定章节
     * 
     * @param section 章节名称 (Architecture/Conventions/Decisions/Preferences/Context/Constraints)
     * @param content 新的章节内容
     */
    public void updateSection(String section, String content) throws IOException {
        String currentContent = load(true);
        
        if (currentContent.isEmpty()) {
            throw new IllegalStateException("Project memory not initialized. Run /init first.");
        }
        
        // 简单的章节替换逻辑(可以后续优化为更智能的合并)
        String sectionHeader = "## " + section;
        int sectionStart = currentContent.indexOf(sectionHeader);
        
        if (sectionStart == -1) {
            // 章节不存在,追加到末尾
            String newContent = currentContent + "\n" + sectionHeader + "\n\n" + content + "\n";
            save(newContent);
        } else {
            // 查找下一个章节的位置
            int nextSectionStart = currentContent.indexOf("\n## ", sectionStart + 1);
            String prefix = currentContent.substring(0, sectionStart);
            String suffix = nextSectionStart == -1 ? "" : currentContent.substring(nextSectionStart);
            
            String newContent = prefix + sectionHeader + "\n\n" + content + "\n" + suffix;
            save(newContent);
        }
        
        logger.info("Updated project memory section: {}", section);
    }
    
    /**
     * 清除缓存,强制下次读取时重新加载
     */
    public void clearCache() {
        cachedMemoryContent = null;
        lastLoadTime = null;
        logger.debug("Project memory cache cleared");
    }
}
