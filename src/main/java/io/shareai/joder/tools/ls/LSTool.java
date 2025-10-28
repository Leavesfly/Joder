package io.shareai.joder.tools.ls;

import io.shareai.joder.WorkingDirectory;
import io.shareai.joder.tools.Tool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * LS Tool - 列出目录内容工具
 * 递归遍历目录并以树状结构显示文件和子目录
 */
public class LSTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(LSTool.class);
    private static final int MAX_FILES = 1000;
    private static final String TRUNCATED_MESSAGE = 
        String.format("目录包含超过 %d 个文件。使用 LS 工具指定具体路径，或使用 Bash、Glob 等工具探索嵌套目录。前 %d 个文件和目录如下：\n\n", 
        MAX_FILES, MAX_FILES);
    
    private final String workingDirectory;
    
    @Inject
    public LSTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public String getName() {
        return "LS";
    }
    
    @Override
    public String getDescription() {
        return "列出指定路径下的文件和目录。路径参数必须是绝对路径。如果知道要搜索的目录，建议优先使用 Glob 和 Grep 工具。";
    }
    
    @Override
    public String getPrompt() {
        return getDescription();
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
        return true; // LSTool 是只读的，支持并发执行
    }
    
    @Override
    public boolean needsPermissions() {
        return false; // 列出目录通常不需要特殊权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String pathStr = (String) input.get("path");
        
        if (pathStr == null || pathStr.trim().isEmpty()) {
            return ToolResult.error("路径参数不能为空");
        }
        
        Path path = Paths.get(pathStr);
        
        // 确保是绝对路径
        if (!path.isAbsolute()) {
            path = Paths.get(workingDirectory).resolve(pathStr).normalize();
        }
        
        if (!Files.exists(path)) {
            return ToolResult.error("路径不存在: " + path);
        }
        
        if (!Files.isDirectory(path)) {
            return ToolResult.error("路径不是目录: " + path);
        }
        
        try {
            List<String> allPaths = listDirectory(path);
            
            // 排序
            Collections.sort(allPaths);
            
            // 创建树状结构
            String tree = printTree(createFileTree(allPaths, path));
            
            String result;
            if (allPaths.size() >= MAX_FILES) {
                result = TRUNCATED_MESSAGE + tree;
            } else {
                result = tree;
            }
            
            return ToolResult.success(result);
            
        } catch (IOException e) {
            logger.error("列出目录失败: {}", path, e);
            return ToolResult.error("列出目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 递归列出目录内容
     */
    private List<String> listDirectory(Path rootPath) throws IOException {
        List<String> results = new ArrayList<>();
        Path cwd = Paths.get(workingDirectory);
        
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (results.size() > MAX_FILES) {
                    return FileVisitResult.TERMINATE;
                }
                
                if (shouldSkip(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                if (!dir.equals(rootPath)) {
                    String relativePath = cwd.relativize(dir).toString();
                    results.add(relativePath + File.separator);
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (results.size() > MAX_FILES) {
                    return FileVisitResult.TERMINATE;
                }
                
                if (shouldSkip(file)) {
                    return FileVisitResult.CONTINUE;
                }
                
                String relativePath = cwd.relativize(file).toString();
                results.add(relativePath);
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("访问文件失败: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return results;
    }
    
    /**
     * 判断是否应该跳过该路径
     */
    private boolean shouldSkip(Path path) {
        String name = path.getFileName().toString();
        
        // 跳过隐藏文件和目录（以 . 开头）
        if (!name.equals(".") && name.startsWith(".")) {
            return true;
        }
        
        // 跳过 __pycache__ 目录
        if (name.equals("__pycache__")) {
            return true;
        }
        
        // 跳过 node_modules 目录
        if (name.equals("node_modules")) {
            return true;
        }
        
        // 跳过 target 目录（Maven）
        if (name.equals("target")) {
            return true;
        }
        
        // 跳过 build 目录
        if (name.equals("build")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 创建文件树结构
     */
    private TreeNode[] createFileTree(List<String> sortedPaths, Path rootPath) {
        List<TreeNode> root = new ArrayList<>();
        
        for (String pathStr : sortedPaths) {
            String[] parts = pathStr.split(File.separator.equals("\\") ? "\\\\" : File.separator);
            List<TreeNode> currentLevel = root;
            StringBuilder currentPath = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) {
                    continue;
                }
                
                if (currentPath.length() > 0) {
                    currentPath.append(File.separator);
                }
                currentPath.append(part);
                
                boolean isLastPart = (i == parts.length - 1);
                
                // 查找是否已存在
                TreeNode existingNode = null;
                for (TreeNode node : currentLevel) {
                    if (node.name.equals(part)) {
                        existingNode = node;
                        break;
                    }
                }
                
                if (existingNode != null) {
                    currentLevel = existingNode.children;
                } else {
                    TreeNode newNode = new TreeNode();
                    newNode.name = part;
                    newNode.path = currentPath.toString();
                    newNode.type = isLastPart ? "file" : "directory";
                    newNode.children = isLastPart ? null : new ArrayList<>();
                    
                    currentLevel.add(newNode);
                    currentLevel = newNode.children != null ? newNode.children : new ArrayList<>();
                }
            }
        }
        
        return root.toArray(new TreeNode[0]);
    }
    
    /**
     * 打印树状结构
     */
    private String printTree(TreeNode[] tree) {
        StringBuilder result = new StringBuilder();
        result.append("- ").append(workingDirectory).append(File.separator).append("\n");
        printTreeLevel(tree, "  ", result);
        return result.toString();
    }
    
    private void printTreeLevel(TreeNode[] nodes, String prefix, StringBuilder result) {
        if (nodes == null) {
            return;
        }
        
        for (TreeNode node : nodes) {
            result.append(prefix).append("- ").append(node.name);
            if ("directory".equals(node.type)) {
                result.append(File.separator);
            }
            result.append("\n");
            
            if (node.children != null && !node.children.isEmpty()) {
                printTreeLevel(node.children.toArray(new TreeNode[0]), prefix + "  ", result);
            }
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String path = (String) input.get("path");
        Path absolutePath = Paths.get(path);
        if (!absolutePath.isAbsolute()) {
            absolutePath = Paths.get(workingDirectory).resolve(path);
        }
        Path cwd = Paths.get(workingDirectory);
        String relativePath = cwd.relativize(absolutePath).toString();
        return String.format("列出目录: \"%s\"", relativePath.isEmpty() ? "." : relativePath);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            String content = result.getOutput();
            String[] lines = content.split("\n");
            int maxLines = 5;
            
            if (lines.length <= maxLines) {
                return "✅ 目录列表已生成";
            } else {
                return String.format("✅ 目录列表已生成 (+%d 项未显示)", lines.length - maxLines);
            }
        } else {
            return "❌ 列出目录失败: " + result.getError();
        }
    }
    
    /**
     * 树节点类
     */
    private static class TreeNode {
        String name;
        String path;
        String type;
        List<TreeNode> children;
    }
}
