package io.shareai.joder.tools.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.domain.Memory;
import io.shareai.joder.services.MemoryManager;
import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * 记忆写入工具 - 写入项目知识到记忆系统
 */
public class MemoryWriteTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryWriteTool.class);
    
    private final MemoryManager memoryManager;
    private final ObjectMapper objectMapper;
    
    @Inject
    public MemoryWriteTool(MemoryManager memoryManager, ObjectMapper objectMapper) {
        this.memoryManager = memoryManager;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "memory_write";
    }
    
    @Override
    public String getDescription() {
        return "保存重要信息到项目记忆系统。用于记录项目配置、技术决策、" +
               "常见问题解决方案、开发规范等知识。支持分类管理和关键词标记，" +
               "便于后续检索。适用于知识积累、经验沉淀。";
    }
    
    @Override
    public boolean needsPermissions() {
        return true;  // 写入记忆需要权限确认
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ToolResult call(Map<String, Object> params) {
        try {
            String title = getString(params, "title");
            String content = getString(params, "content");
            
            if (title == null || title.trim().isEmpty()) {
                return ToolResult.error("标题不能为空");
            }
            
            if (content == null || content.trim().isEmpty()) {
                return ToolResult.error("内容不能为空");
            }
            
            // 创建记忆对象
            Memory memory = new Memory();
            memory.setTitle(title);
            memory.setContent(content);
            
            // 可选字段
            Object keywordsObj = params.get("keywords");
            if (keywordsObj instanceof List) {
                memory.setKeywords((List<String>) keywordsObj);
            } else if (keywordsObj instanceof String) {
                memory.setKeywords(List.of(((String) keywordsObj).split(",")));
            }
            
            String category = getString(params, "category");
            if (category != null) {
                memory.setCategory(category);
            }
            
            String scope = getString(params, "scope", "workspace");
            memory.setScope(scope);
            
            String source = getString(params, "source", "auto");
            memory.setSource(source);
            
            // 保存记忆
            Memory saved = memoryManager.saveMemory(memory);
            logger.info("Saved memory: {}", saved.getId());
            
            return ToolResult.success(formatSaveResult(saved));
            
        } catch (Exception e) {
            logger.error("Error in MemoryWriteTool", e);
            return ToolResult.error("保存记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化保存结果
     */
    private String formatSaveResult(Memory memory) {
        StringBuilder sb = new StringBuilder();
        sb.append("✓ 记忆已保存\n\n");
        sb.append(String.format("ID: %s\n", memory.getId()));
        sb.append(String.format("标题: %s\n", memory.getTitle()));
        sb.append(String.format("分类: %s\n", 
                memory.getCategory() != null ? memory.getCategory() : "未分类"));
        sb.append(String.format("范围: %s\n", memory.getScope()));
        
        if (memory.getKeywords() != null && !memory.getKeywords().isEmpty()) {
            sb.append(String.format("关键词: %s\n", String.join(", ", memory.getKeywords())));
        }
        
        sb.append(String.format("\n内容长度: %d 字符\n", memory.getContent().length()));
        
        // 显示统计
        Map<String, Integer> stats = memoryManager.getMemoryStats();
        sb.append(String.format("\n记忆库统计: 总计 %d 条记忆\n", stats.get("total")));
        
        return sb.toString();
    }
}
