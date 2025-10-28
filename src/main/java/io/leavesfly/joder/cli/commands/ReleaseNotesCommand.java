package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReleaseNotes Command - 发布说明命令
 * 根据 Git 提交生成发布说明
 */
@Singleton
public class ReleaseNotesCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(ReleaseNotesCommand.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public String getDescription() {
        return "生成版本发布说明";
    }
    
    @Override
    public String getUsage() {
        return "release-notes [版本号] [--from <tag>] [--to <tag>] - 生成发布说明";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            // 检查是否为 Git 仓库
            if (!isGitRepository()) {
                return CommandResult.error("当前目录不是 Git 仓库");
            }
            
            ReleaseOptions options = parseArguments(args);
            
            // 获取 Git 提交
            List<GitCommit> commits = getCommitsSince(options.fromTag, options.toTag);
            
            if (commits.isEmpty()) {
                return CommandResult.error("未找到提交记录");
            }
            
            // 分类提交
            CategorizedCommits categorized = categorizeCommits(commits);
            
            // 生成发布说明
            String releaseNotes = generateReleaseNotes(options.version, categorized);
            
            // 保存到文件（可选）
            if (options.save) {
                Path outputPath = saveReleaseNotes(options.version, releaseNotes);
                return CommandResult.success(
                        releaseNotes + "\n\n" +
                        "✅ 发布说明已保存到: " + outputPath
                );
            }
            
            return CommandResult.success(releaseNotes);
            
        } catch (Exception e) {
            logger.error("Failed to generate release notes", e);
            return CommandResult.error("生成发布说明失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析命令参数
     */
    private ReleaseOptions parseArguments(String args) {
        ReleaseOptions options = new ReleaseOptions();
        
        if (args == null || args.trim().isEmpty()) {
            options.version = "未指定";
            return options;
        }
        
        String[] parts = args.trim().split("\\s+");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.equals("--from") && i + 1 < parts.length) {
                options.fromTag = parts[++i];
            } else if (part.equals("--to") && i + 1 < parts.length) {
                options.toTag = parts[++i];
            } else if (part.equals("--save")) {
                options.save = true;
            } else if (!part.startsWith("--") && options.version.equals("未指定")) {
                options.version = part;
            }
        }
        
        return options;
    }
    
    /**
     * 检查是否为 Git 仓库
     */
    private boolean isGitRepository() {
        return Files.exists(Paths.get(".git"));
    }
    
    /**
     * 获取 Git 提交记录
     */
    private List<GitCommit> getCommitsSince(String fromTag, String toTag) 
            throws IOException, InterruptedException {
        
        // 构建 Git 命令
        String gitRange = fromTag != null ? fromTag + ".." + (toTag != null ? toTag : "HEAD") : "HEAD";
        String command = String.format("git log %s --pretty=format:\"%%H|%%s|%%an|%%ad\" --date=short", gitRange);
        
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        Process process = pb.start();
        
        List<GitCommit> commits = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                GitCommit commit = parseCommitLine(line);
                if (commit != null) {
                    commits.add(commit);
                }
            }
        }
        
        process.waitFor();
        
        return commits;
    }
    
    /**
     * 解析 Git 提交行
     */
    private GitCommit parseCommitLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 4) {
            return null;
        }
        
        GitCommit commit = new GitCommit();
        commit.hash = parts[0];
        commit.message = parts[1];
        commit.author = parts[2];
        commit.date = parts[3];
        
        // 分类提交类型
        commit.type = detectCommitType(commit.message);
        
        return commit;
    }
    
    /**
     * 检测提交类型
     */
    private CommitType detectCommitType(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.startsWith("feat:") || lowerMessage.startsWith("feature:")) {
            return CommitType.FEATURE;
        } else if (lowerMessage.startsWith("fix:")) {
            return CommitType.FIX;
        } else if (lowerMessage.startsWith("docs:")) {
            return CommitType.DOCS;
        } else if (lowerMessage.startsWith("refactor:")) {
            return CommitType.REFACTOR;
        } else if (lowerMessage.startsWith("perf:")) {
            return CommitType.PERFORMANCE;
        } else if (lowerMessage.startsWith("test:")) {
            return CommitType.TEST;
        } else if (lowerMessage.startsWith("chore:")) {
            return CommitType.CHORE;
        } else if (lowerMessage.contains("breaking")) {
            return CommitType.BREAKING;
        }
        
        return CommitType.OTHER;
    }
    
    /**
     * 分类提交
     */
    private CategorizedCommits categorizeCommits(List<GitCommit> commits) {
        CategorizedCommits categorized = new CategorizedCommits();
        
        for (GitCommit commit : commits) {
            switch (commit.type) {
                case FEATURE -> categorized.features.add(commit);
                case FIX -> categorized.fixes.add(commit);
                case BREAKING -> categorized.breaking.add(commit);
                case PERFORMANCE -> categorized.performance.add(commit);
                case REFACTOR -> categorized.refactor.add(commit);
                case DOCS -> categorized.docs.add(commit);
                default -> categorized.other.add(commit);
            }
        }
        
        return categorized;
    }
    
    /**
     * 生成发布说明
     */
    private String generateReleaseNotes(String version, CategorizedCommits categorized) {
        StringBuilder notes = new StringBuilder();
        
        notes.append("# Release Notes - ").append(version).append("\n\n");
        notes.append("**发布日期**: ").append(LocalDate.now().format(DATE_FORMATTER)).append("\n\n");
        
        notes.append("---\n\n");
        
        // Breaking Changes
        if (!categorized.breaking.isEmpty()) {
            notes.append("## ⚠️ Breaking Changes\n\n");
            for (GitCommit commit : categorized.breaking) {
                notes.append("- ").append(cleanMessage(commit.message)).append("\n");
            }
            notes.append("\n");
        }
        
        // New Features
        if (!categorized.features.isEmpty()) {
            notes.append("## ✨ 新功能\n\n");
            for (GitCommit commit : categorized.features) {
                notes.append("- ").append(cleanMessage(commit.message)).append("\n");
            }
            notes.append("\n");
        }
        
        // Bug Fixes
        if (!categorized.fixes.isEmpty()) {
            notes.append("## 🐛 Bug 修复\n\n");
            for (GitCommit commit : categorized.fixes) {
                notes.append("- ").append(cleanMessage(commit.message)).append("\n");
            }
            notes.append("\n");
        }
        
        // Performance
        if (!categorized.performance.isEmpty()) {
            notes.append("## ⚡ 性能优化\n\n");
            for (GitCommit commit : categorized.performance) {
                notes.append("- ").append(cleanMessage(commit.message)).append("\n");
            }
            notes.append("\n");
        }
        
        // Refactoring
        if (!categorized.refactor.isEmpty()) {
            notes.append("## ♻️ 代码重构\n\n");
            for (GitCommit commit : categorized.refactor) {
                notes.append("- ").append(cleanMessage(commit.message)).append("\n");
            }
            notes.append("\n");
        }
        
        // Documentation
        if (!categorized.docs.isEmpty()) {
            notes.append("## 📝 文档更新\n\n");
            for (GitCommit commit : categorized.docs) {
                notes.append("- ").append(cleanMessage(commit.message)).append("\n");
            }
            notes.append("\n");
        }
        
        notes.append("---\n\n");
        notes.append("_自动生成于 ").append(LocalDate.now().format(DATE_FORMATTER)).append("_\n");
        
        return notes.toString();
    }
    
    /**
     * 清理提交消息
     */
    private String cleanMessage(String message) {
        // 移除前缀（feat:, fix: 等）
        Pattern pattern = Pattern.compile("^(feat|fix|docs|refactor|perf|test|chore):\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        
        if (matcher.matches()) {
            return matcher.group(2);
        }
        
        return message;
    }
    
    /**
     * 保存发布说明
     */
    private Path saveReleaseNotes(String version, String content) throws IOException {
        Path outputDir = Paths.get("release-notes");
        Files.createDirectories(outputDir);
        
        String fileName = "RELEASE_" + version + "_" + LocalDate.now().format(DATE_FORMATTER) + ".md";
        Path outputPath = outputDir.resolve(fileName);
        
        Files.writeString(outputPath, content);
        
        logger.info("Release notes saved to: {}", outputPath);
        
        return outputPath;
    }
    
    /**
     * 提交类型
     */
    private enum CommitType {
        FEATURE, FIX, BREAKING, PERFORMANCE, REFACTOR, DOCS, TEST, CHORE, OTHER
    }
    
    /**
     * Git 提交
     */
    private static class GitCommit {
        String hash;
        String message;
        String author;
        String date;
        CommitType type;
    }
    
    /**
     * 分类的提交
     */
    private static class CategorizedCommits {
        List<GitCommit> breaking = new ArrayList<>();
        List<GitCommit> features = new ArrayList<>();
        List<GitCommit> fixes = new ArrayList<>();
        List<GitCommit> performance = new ArrayList<>();
        List<GitCommit> refactor = new ArrayList<>();
        List<GitCommit> docs = new ArrayList<>();
        List<GitCommit> other = new ArrayList<>();
    }
    
    /**
     * 发布选项
     */
    private static class ReleaseOptions {
        String version = "未指定";
        String fromTag = null;
        String toTag = null;
        boolean save = false;
    }
}
