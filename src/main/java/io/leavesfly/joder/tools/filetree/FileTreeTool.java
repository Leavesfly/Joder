package io.leavesfly.joder.tools.filetree;

import io.leavesfly.joder.WorkingDirectory;
import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * FileTree Tool - 文件树展示工具
 * 
 * 以树状结构展示目录内容
 */
@Singleton
public class FileTreeTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTreeTool.class);
    
    private final String workingDirectory;
    
    @Inject
    public FileTreeTool(@WorkingDirectory String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public String getName() {
        return "FileTree";
    }
    
    @Override
    public String getDescription() {
        return "以树状结构展示目录内容。\n" +
               "- 递归显示目录结构\n" +
               "- 支持深度限制\n" +
               "- 支持文件类型过滤\n" +
               "- 显示文件大小和类型";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具以树状结构查看目录内容。\n\n" +
               "**参数**：\n" +
               "- path: 要展示的目录路径（默认：当前目录）\n" +
               "- max_depth: 最大递归深度（默认：3）\n" +
               "- show_hidden: 是否显示隐藏文件（默认：false）\n" +
               "- pattern: 文件名模式过滤（可选，如 \"*.java\"）\n\n" +
               "**适用场景**：\n" +
               "- 了解项目结构\n" +
               "- 查找特定类型的文件\n" +
               "- 分析目录组织\n" +
               "- 生成项目文档\n\n" +
               "**示例输出**：\n" +
               "```\n" +
               "src/\n" +
               "├── main/\n" +
               "│   ├── java/\n" +
               "│   │   └── com/\n" +
               "│   │       └── example/\n" +
               "│   │           ├── Application.java (2.1 KB)\n" +
               "│   │           └── controller/\n" +
               "│   │               └── UserController.java (3.5 KB)\n" +
               "│   └── resources/\n" +
               "│       └── application.yml (512 B)\n" +
               "└── test/\n" +
               "    └── java/\n" +
               "```";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true; // 只读操作
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false; // 只读目录不需要特殊权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String pathStr = (String) input.getOrDefault("path", ".");
        Integer maxDepth = input.get("max_depth") != null 
            ? ((Number) input.get("max_depth")).intValue() 
            : 3;
        Boolean showHidden = (Boolean) input.getOrDefault("show_hidden", false);
        String pattern = (String) input.get("pattern");
        
        try {
            // 解析路径
            Path basePath = Paths.get(workingDirectory).resolve(pathStr).normalize();
            
            if (!Files.exists(basePath)) {
                return ToolResult.error("路径不存在: " + pathStr);
            }
            
            if (!Files.isDirectory(basePath)) {
                return ToolResult.error("路径不是目录: " + pathStr);
            }
            
            // 生成文件树
            StringBuilder tree = new StringBuilder();
            tree.append("📁 ").append(basePath.getFileName()).append("/\n");
            
            buildTree(basePath, tree, "", maxDepth, 0, showHidden, pattern);
            
            return ToolResult.success(tree.toString());
            
        } catch (Exception e) {
            logger.error("生成文件树失败", e);
            return ToolResult.error("生成文件树失败: " + e.getMessage());
        }
    }
    
    /**
     * 递归构建文件树
     */
    private void buildTree(Path dir, StringBuilder tree, String prefix, 
                          int maxDepth, int currentDepth, boolean showHidden, String pattern) throws IOException {
        
        if (currentDepth >= maxDepth) {
            return;
        }
        
        // 获取目录内容
        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                // 过滤隐藏文件
                if (!showHidden && entry.getFileName().toString().startsWith(".")) {
                    continue;
                }
                
                // 过滤模式
                if (pattern != null && !entry.getFileName().toString().matches(pattern.replace("*", ".*"))) {
                    continue;
                }
                
                entries.add(entry);
            }
        }
        
        // 排序：目录在前，文件在后
        entries.sort((a, b) -> {
            boolean aIsDir = Files.isDirectory(a);
            boolean bIsDir = Files.isDirectory(b);
            if (aIsDir != bIsDir) {
                return aIsDir ? -1 : 1;
            }
            return a.getFileName().toString().compareTo(b.getFileName().toString());
        });
        
        // 遍历并构建树
        for (int i = 0; i < entries.size(); i++) {
            Path entry = entries.get(i);
            boolean isLast = (i == entries.size() - 1);
            
            String connector = isLast ? "└── " : "├── ";
            String childPrefix = isLast ? "    " : "│   ";
            
            tree.append(prefix).append(connector);
            
            if (Files.isDirectory(entry)) {
                tree.append("📁 ").append(entry.getFileName()).append("/\n");
                buildTree(entry, tree, prefix + childPrefix, maxDepth, currentDepth + 1, showHidden, pattern);
            } else {
                long size = Files.size(entry);
                String sizeStr = formatFileSize(size);
                tree.append("📄 ").append(entry.getFileName()).append(" (").append(sizeStr).append(")\n");
            }
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String path = (String) input.getOrDefault("path", ".");
        return String.format("展示文件树: %s", path);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 文件树生成成功";
        } else {
            return "❌ 文件树生成失败: " + result.getError();
        }
    }
}
