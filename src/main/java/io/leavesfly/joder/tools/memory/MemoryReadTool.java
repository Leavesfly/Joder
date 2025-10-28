package io.leavesfly.joder.tools.memory;

import io.leavesfly.joder.domain.Memory;
import io.leavesfly.joder.services.MemoryManager;
import io.leavesfly.joder.tools.AbstractTool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * 记忆读取工具 - 读取项目记忆/知识库
 */
public class MemoryReadTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryReadTool.class);
    
    private final MemoryManager memoryManager;
    
    @Inject
    public MemoryReadTool(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }
    
    @Override
    public String getName() {
        return "memory_read";
    }
    
    @Override
    public String getDescription() {
        return "读取项目记忆和知识库。可以根据关键词搜索相关记忆，" +
               "或按分类检索。记忆包括项目配置、技术栈、开发规范、" +
               "常见问题解决方案等信息。适用于了解项目背景、查找历史知识。";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> params) {
        try {
            String query = getString(params, "query");
            String category = getString(params, "category");
            
            List<Memory> results;
            
            if (category != null && !category.trim().isEmpty()) {
                // 按分类检索
                results = memoryManager.getMemoriesByCategory(category);
                logger.info("Retrieved {} memories by category: {}", results.size(), category);
            } else if (query != null && !query.trim().isEmpty()) {
                // 关键词搜索
                results = memoryManager.searchMemories(query);
                logger.info("Found {} memories for query: {}", results.size(), query);
            } else {
                // 获取所有记忆
                results = memoryManager.getAllMemories();
                logger.info("Retrieved all {} memories", results.size());
            }
            
            if (results.isEmpty()) {
                return ToolResult.success("未找到相关记忆");
            }
            
            return ToolResult.success(formatMemories(results, query, category));
            
        } catch (Exception e) {
            logger.error("Error in MemoryReadTool", e);
            return ToolResult.error("读取记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化记忆结果
     */
    private String formatMemories(List<Memory> memories, String query, String category) {
        StringBuilder sb = new StringBuilder();
        
        if (query != null) {
            sb.append(String.format("🧠 搜索结果: \"%s\" (找到 %d 条)\n\n", query, memories.size()));
        } else if (category != null) {
            sb.append(String.format("🧠 分类: %s (找到 %d 条)\n\n", category, memories.size()));
        } else {
            sb.append(String.format("🧠 所有记忆 (共 %d 条)\n\n", memories.size()));
        }
        
        for (int i = 0; i < Math.min(memories.size(), 10); i++) {
            Memory memory = memories.get(i);
            
            sb.append(String.format("%d. **%s**\n", i + 1, memory.getTitle()));
            sb.append(String.format("   ID: %s | 分类: %s | 范围: %s\n",
                    memory.getId(),
                    memory.getCategory() != null ? memory.getCategory() : "未分类",
                    memory.getScope() != null ? memory.getScope() : "workspace"));
            
            if (memory.getKeywords() != null && !memory.getKeywords().isEmpty()) {
                sb.append(String.format("   关键词: %s\n", String.join(", ", memory.getKeywords())));
            }
            
            // 显示内容摘要（前200字符）
            String content = memory.getContent();
            if (content != null && !content.isEmpty()) {
                String summary = content.length() > 200 
                        ? content.substring(0, 200) + "..." 
                        : content;
                sb.append(String.format("   %s\n", summary));
            }
            
            sb.append("\n");
        }
        
        if (memories.size() > 10) {
            sb.append(String.format("... 还有 %d 条记忆未显示\n", memories.size() - 10));
        }
        
        // 添加统计信息
        Map<String, Integer> stats = memoryManager.getMemoryStats();
        sb.append(String.format("\n记忆统计: 总计 %d 条\n", stats.get("total")));
        
        return sb.toString();
    }
}
