package io.leavesfly.joder.services.memory;

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
 * 项目分析器
 * <p>
 * 用于分析项目结构、技术栈和配置
 * </p>
 */
public class ProjectAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectAnalyzer.class);
    
    private final Path rootPath;
    
    public ProjectAnalyzer(String workingDirectory) {
        this.rootPath = Paths.get(workingDirectory);
    }
    
    /**
     * 获取项目名称(基于目录名)
     */
    public String getProjectName() {
        return rootPath.getFileName().toString();
    }
    
    /**
     * 检测项目类型
     */
    public String detectProjectType() {
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            return "Maven Java Project";
        } else if (Files.exists(rootPath.resolve("build.gradle")) || 
                   Files.exists(rootPath.resolve("build.gradle.kts"))) {
            return "Gradle Project";
        } else if (Files.exists(rootPath.resolve("package.json"))) {
            return "Node.js Project";
        } else if (Files.exists(rootPath.resolve("requirements.txt")) || 
                   Files.exists(rootPath.resolve("setup.py"))) {
            return "Python Project";
        } else if (Files.exists(rootPath.resolve("Cargo.toml"))) {
            return "Rust Project";
        } else if (Files.exists(rootPath.resolve("go.mod"))) {
            return "Go Project";
        }
        return "Generic Project";
    }
    
    /**
     * 检测构建工具
     */
    public String detectBuildTool() {
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            return "Maven";
        } else if (Files.exists(rootPath.resolve("build.gradle")) || 
                   Files.exists(rootPath.resolve("build.gradle.kts"))) {
            return "Gradle";
        } else if (Files.exists(rootPath.resolve("package.json"))) {
            return "npm/yarn";
        } else if (Files.exists(rootPath.resolve("Cargo.toml"))) {
            return "Cargo";
        } else if (Files.exists(rootPath.resolve("go.mod"))) {
            return "Go Modules";
        }
        return "Unknown";
    }
    
    /**
     * 检测测试框架
     */
    public String detectTestFramework() {
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            // 检查 Maven 项目的测试依赖
            return "JUnit (assumed)";
        } else if (Files.exists(rootPath.resolve("package.json"))) {
            return "Jest/Mocha (assumed)";
        } else if (Files.exists(rootPath.resolve("requirements.txt"))) {
            return "pytest (assumed)";
        }
        return "Unknown";
    }
    
    /**
     * 检测技术栈
     */
    public Map<String, String> detectTechStack() {
        Map<String, String> stack = new LinkedHashMap<>();
        
        // Java 项目检测
        if (Files.exists(rootPath.resolve("pom.xml"))) {
            stack.put("Language", "Java");
            stack.put("Build Tool", "Maven");
            
            // 尝试检测 Spring Boot
            try {
                String pomContent = Files.readString(rootPath.resolve("pom.xml"));
                if (pomContent.contains("spring-boot")) {
                    stack.put("Framework", "Spring Boot");
                }
                if (pomContent.contains("guice")) {
                    stack.put("DI Framework", "Google Guice");
                }
            } catch (IOException e) {
                logger.debug("Failed to analyze pom.xml", e);
            }
        }
        
        // Node.js 项目检测
        else if (Files.exists(rootPath.resolve("package.json"))) {
            stack.put("Language", "JavaScript/TypeScript");
            stack.put("Runtime", "Node.js");
            
            try {
                String packageJson = Files.readString(rootPath.resolve("package.json"));
                if (packageJson.contains("\"react\"")) {
                    stack.put("Framework", "React");
                } else if (packageJson.contains("\"vue\"")) {
                    stack.put("Framework", "Vue.js");
                } else if (packageJson.contains("\"next\"")) {
                    stack.put("Framework", "Next.js");
                }
            } catch (IOException e) {
                logger.debug("Failed to analyze package.json", e);
            }
        }
        
        // Python 项目检测
        else if (Files.exists(rootPath.resolve("requirements.txt"))) {
            stack.put("Language", "Python");
            
            try {
                String requirements = Files.readString(rootPath.resolve("requirements.txt"));
                if (requirements.contains("django")) {
                    stack.put("Framework", "Django");
                } else if (requirements.contains("flask")) {
                    stack.put("Framework", "Flask");
                } else if (requirements.contains("fastapi")) {
                    stack.put("Framework", "FastAPI");
                }
            } catch (IOException e) {
                logger.debug("Failed to analyze requirements.txt", e);
            }
        }
        
        return stack;
    }
    
    /**
     * 生成目录树结构
     * 
     * @return 目录树的文本表示
     */
    public String generateDirectoryTree() {
        return generateDirectoryTree(2); // 默认深度为 2
    }
    
    /**
     * 生成目录树结构
     * 
     * @param maxDepth 最大深度
     * @return 目录树的文本表示
     */
    public String generateDirectoryTree(int maxDepth) {
        StringBuilder tree = new StringBuilder();
        tree.append(rootPath.getFileName()).append("/\n");
        
        try {
            buildTree(rootPath, "", tree, 0, maxDepth, new HashSet<>(Arrays.asList(
                ".git", ".idea", ".vscode", "target", "build", 
                "node_modules", ".DS_Store", "*.class"
            )));
        } catch (IOException e) {
            logger.error("Failed to generate directory tree", e);
            tree.append("  [Error reading directory structure]\n");
        }
        
        return tree.toString();
    }
    
    /**
     * 递归构建目录树
     */
    private void buildTree(
            Path dir, 
            String prefix, 
            StringBuilder tree, 
            int currentDepth, 
            int maxDepth,
            Set<String> excludes) throws IOException {
        
        if (currentDepth >= maxDepth) {
            return;
        }
        
        try (Stream<Path> paths = Files.list(dir)) {
            List<Path> entries = paths
                .filter(p -> !shouldExclude(p, excludes))
                .sorted(Comparator.comparing(p -> !Files.isDirectory(p)))
                .collect(Collectors.toList());
            
            for (int i = 0; i < entries.size(); i++) {
                Path entry = entries.get(i);
                boolean isLast = (i == entries.size() - 1);
                
                String connector = isLast ? "└── " : "├── ";
                String childPrefix = isLast ? "    " : "│   ";
                
                tree.append(prefix).append(connector);
                tree.append(entry.getFileName());
                
                if (Files.isDirectory(entry)) {
                    tree.append("/");
                }
                tree.append("\n");
                
                if (Files.isDirectory(entry)) {
                    buildTree(entry, prefix + childPrefix, tree, currentDepth + 1, maxDepth, excludes);
                }
            }
        }
    }
    
    /**
     * 判断路径是否应该被排除
     */
    private boolean shouldExclude(Path path, Set<String> excludes) {
        String fileName = path.getFileName().toString();
        return excludes.contains(fileName) || fileName.startsWith(".");
    }
}
