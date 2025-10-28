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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Command - 测试运行命令
 * 运行项目测试并报告结果
 */
@Singleton
public class TestCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCommand.class);
    
    @Override
    public String getDescription() {
        return "运行项目测试";
    }
    
    @Override
    public String getUsage() {
        return "test [--unit|--integration|--all] [pattern] - 运行测试";
    }
    
    @Override
    public CommandResult execute(String args) {
        try {
            TestOptions options = parseArguments(args);
            
            // 检测项目类型
            ProjectType projectType = detectProjectType();
            
            if (projectType == ProjectType.UNKNOWN) {
                return CommandResult.error("无法检测项目类型，请在项目根目录运行");
            }
            
            // 运行测试
            TestResult result = runTests(projectType, options);
            
            // 格式化输出
            return CommandResult.success(formatTestResult(result));
            
        } catch (Exception e) {
            logger.error("Failed to execute test command", e);
            return CommandResult.error("测试运行失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析命令参数
     */
    private TestOptions parseArguments(String args) {
        TestOptions options = new TestOptions();
        
        if (args == null || args.trim().isEmpty()) {
            return options;
        }
        
        String[] parts = args.trim().split("\\s+");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.equals("--unit")) {
                options.type = TestType.UNIT;
            } else if (part.equals("--integration")) {
                options.type = TestType.INTEGRATION;
            } else if (part.equals("--all")) {
                options.type = TestType.ALL;
            } else if (!part.startsWith("--")) {
                options.pattern = part;
            }
        }
        
        return options;
    }
    
    /**
     * 检测项目类型
     */
    private ProjectType detectProjectType() {
        if (Files.exists(Paths.get("pom.xml"))) {
            return ProjectType.MAVEN;
        } else if (Files.exists(Paths.get("build.gradle")) || 
                   Files.exists(Paths.get("build.gradle.kts"))) {
            return ProjectType.GRADLE;
        } else if (Files.exists(Paths.get("package.json"))) {
            return ProjectType.NODE;
        } else if (Files.exists(Paths.get("Cargo.toml"))) {
            return ProjectType.RUST;
        } else if (Files.exists(Paths.get("go.mod"))) {
            return ProjectType.GO;
        }
        
        return ProjectType.UNKNOWN;
    }
    
    /**
     * 运行测试
     */
    private TestResult runTests(ProjectType projectType, TestOptions options) throws IOException, InterruptedException {
        String command = buildTestCommand(projectType, options);
        
        logger.info("Running test command: {}", command);
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("sh", "-c", command);
        pb.redirectErrorStream(true);
        
        long startTime = System.currentTimeMillis();
        Process process = pb.start();
        
        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
                logger.debug("Test output: {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        long duration = System.currentTimeMillis() - startTime;
        
        TestResult result = new TestResult();
        result.success = (exitCode == 0);
        result.duration = duration;
        result.output = output;
        result.projectType = projectType;
        
        // 解析测试结果
        parseTestOutput(result, output);
        
        return result;
    }
    
    /**
     * 构建测试命令
     */
    private String buildTestCommand(ProjectType projectType, TestOptions options) {
        return switch (projectType) {
            case MAVEN -> buildMavenCommand(options);
            case GRADLE -> buildGradleCommand(options);
            case NODE -> buildNodeCommand(options);
            case RUST -> "cargo test";
            case GO -> "go test ./...";
            default -> "echo 'Unknown project type'";
        };
    }
    
    /**
     * 构建 Maven 测试命令
     */
    private String buildMavenCommand(TestOptions options) {
        StringBuilder cmd = new StringBuilder("mvn test");
        
        if (options.pattern != null) {
            cmd.append(" -Dtest=").append(options.pattern);
        }
        
        if (options.type == TestType.INTEGRATION) {
            cmd = new StringBuilder("mvn verify -DskipUnitTests");
        } else if (options.type == TestType.UNIT) {
            cmd.append(" -DskipITs");
        }
        
        return cmd.toString();
    }
    
    /**
     * 构建 Gradle 测试命令
     */
    private String buildGradleCommand(TestOptions options) {
        StringBuilder cmd = new StringBuilder("./gradlew test");
        
        if (options.pattern != null) {
            cmd.append(" --tests ").append(options.pattern);
        }
        
        return cmd.toString();
    }
    
    /**
     * 构建 Node 测试命令
     */
    private String buildNodeCommand(TestOptions options) {
        // 检测测试框架
        if (Files.exists(Paths.get("vitest.config.ts")) || 
            Files.exists(Paths.get("vitest.config.js"))) {
            return "npm run test";
        } else if (Files.exists(Paths.get("jest.config.js"))) {
            return "npm test";
        }
        return "npm test";
    }
    
    /**
     * 解析测试输出
     */
    private void parseTestOutput(TestResult result, List<String> output) {
        for (String line : output) {
            // Maven 格式
            if (line.contains("Tests run:")) {
                String[] parts = line.split(",");
                for (String part : parts) {
                    if (part.contains("Tests run:")) {
                        result.totalTests = extractNumber(part);
                    } else if (part.contains("Failures:")) {
                        result.failures = extractNumber(part);
                    } else if (part.contains("Errors:")) {
                        result.errors = extractNumber(part);
                    } else if (part.contains("Skipped:")) {
                        result.skipped = extractNumber(part);
                    }
                }
            }
            
            // Gradle / Node 格式可以继续添加...
        }
        
        result.passed = result.totalTests - result.failures - result.errors - result.skipped;
    }
    
    /**
     * 提取数字
     */
    private int extractNumber(String text) {
        try {
            String[] parts = text.split(":");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
        return 0;
    }
    
    /**
     * 格式化测试结果
     */
    private String formatTestResult(TestResult result) {
        StringBuilder output = new StringBuilder();
        
        output.append("🧪 测试运行报告\n\n");
        
        output.append("═══════════════════════════════════════\n");
        output.append("📊 测试统计\n");
        output.append("═══════════════════════════════════════\n\n");
        
        output.append(String.format("  项目类型:    %s\n", result.projectType));
        output.append(String.format("  运行时间:    %.2f 秒\n", result.duration / 1000.0));
        output.append(String.format("  测试总数:    %d\n", result.totalTests));
        output.append(String.format("  ✅ 通过:     %d\n", result.passed));
        output.append(String.format("  ❌ 失败:     %d\n", result.failures));
        output.append(String.format("  ⚠️  错误:     %d\n", result.errors));
        output.append(String.format("  ⏭️  跳过:     %d\n", result.skipped));
        
        output.append("\n");
        
        // 测试结果
        output.append("═══════════════════════════════════════\n");
        if (result.success) {
            output.append("✅ 测试全部通过！\n");
        } else {
            output.append("❌ 测试失败\n");
        }
        output.append("═══════════════════════════════════════\n");
        
        // 详细输出（可选）
        if (!result.success && !result.output.isEmpty()) {
            output.append("\n最后 10 行输出:\n");
            output.append("─────────────────────────────────────\n");
            int start = Math.max(0, result.output.size() - 10);
            for (int i = start; i < result.output.size(); i++) {
                output.append(result.output.get(i)).append("\n");
            }
        }
        
        return output.toString();
    }
    
    /**
     * 项目类型
     */
    private enum ProjectType {
        MAVEN, GRADLE, NODE, RUST, GO, UNKNOWN
    }
    
    /**
     * 测试类型
     */
    private enum TestType {
        UNIT, INTEGRATION, ALL
    }
    
    /**
     * 测试选项
     */
    private static class TestOptions {
        TestType type = TestType.ALL;
        String pattern = null;
    }
    
    /**
     * 测试结果
     */
    private static class TestResult {
        boolean success;
        long duration;
        int totalTests;
        int passed;
        int failures;
        int errors;
        int skipped;
        ProjectType projectType;
        List<String> output;
    }
}
