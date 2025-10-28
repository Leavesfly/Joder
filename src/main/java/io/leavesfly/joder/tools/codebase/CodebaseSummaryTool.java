package io.leavesfly.joder.tools.codebase;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import io.leavesfly.joder.services.memory.ProjectAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * CodebaseSummary Tool - 代码库架构总结工具
 * 生成项目的架构总结,利用ProjectAnalyzer生成结构化信息
 */
public class CodebaseSummaryTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(CodebaseSummaryTool.class);
    
    private final String workingDirectory;
    
    @Inject
    public CodebaseSummaryTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public String getName() {
        return "CodebaseSummary";
    }
    
    @Override
    public String getDescription() {
        return "生成代码库架构总结,包括目录结构、主要组件、技术栈和依赖关系。";
    }
    
    @Override
    public String getPrompt() {
        return "此工具用于生成代码库的架构总结,帮助理解项目整体结构。\n\n" +
               "使用场景:\n" +
               "1. 初次接触新项目,需要快速了解架构\n" +
               "2. 向他人介绍项目结构\n" +
               "3. 记录项目架构变更\n" +
               "4. 生成文档或README内容\n\n" +
               "输入参数:\n" +
               "1. depth: 目录遍历深度(可选,默认2)\n\n" +
               "返回内容:\n" +
               "- 项目基本信息(名称、类型、技术栈)\n" +
               "- 目录结构树\n" +
               "- 主要技术栈信息";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
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
        int depth = getIntParameter(input, "depth", 2);
        
        try {
            ProjectAnalyzer analyzer = new ProjectAnalyzer(workingDirectory);
            
            StringBuilder summary = new StringBuilder();
            
            // 项目基本信息
            summary.append("# ").append(analyzer.getProjectName())
                   .append(" - 项目架构总结\n\n");
            
            summary.append("## 📋 基本信息\n\n");
            summary.append("- **项目名称**: ").append(analyzer.getProjectName()).append("\n");
            summary.append("- **项目类型**: ").append(analyzer.detectProjectType()).append("\n");
            summary.append("- **构建工具**: ").append(analyzer.detectBuildTool()).append("\n");
            
            // 技术栈
            Map<String, String> techStackMap = analyzer.detectTechStack();
            if (!techStackMap.isEmpty()) {
                techStackMap.forEach((key, value) -> {
                    summary.append("- **").append(key).append("**: ").append(value).append("\n");
                });
            }
            summary.append("\n");
            
            // 目录结构
            summary.append("## 📂 目录结构\n\n");
            summary.append("```\n");
            summary.append(analyzer.generateDirectoryTree(depth));
            summary.append("```\n\n");
            
            logger.info("成功生成代码库总结: {}", analyzer.getProjectName());
            return ToolResult.success(summary.toString());
            
        } catch (Exception e) {
            logger.error("生成代码库总结失败", e);
            return ToolResult.error("生成代码库总结失败: " + e.getMessage());
        }
    }
    
    private int getIntParameter(Map<String, Object> input, String key, int defaultValue) {
        Object value = input.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        return "生成代码库架构总结";
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 代码库总结已生成";
        } else {
            return "❌ 生成失败: " + result.getError();
        }
    }
}
