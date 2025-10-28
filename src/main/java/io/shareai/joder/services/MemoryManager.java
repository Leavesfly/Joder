package io.shareai.joder.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.domain.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆管理器 - 管理项目知识库和记忆
 */
@Singleton
public class MemoryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);
    
    private final Map<String, Memory> memories = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Path memoryStorePath;
    
    @Inject
    public MemoryManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.memoryStorePath = getMemoryStorePath();
        loadMemories();
    }
    
    /**
     * 获取记忆存储路径
     */
    private Path getMemoryStorePath() {
        String userHome = System.getProperty("user.home");
        String workingDir = System.getProperty("user.dir");
        
        // 生成项目哈希
        String projectHash = generateProjectHash(workingDir);
        
        Path memoryDir = Paths.get(userHome, ".joder", "memory", projectHash);
        
        try {
            Files.createDirectories(memoryDir);
        } catch (IOException e) {
            logger.warn("Failed to create memory directory", e);
        }
        
        return memoryDir.resolve("memories.json");
    }
    
    /**
     * 生成项目哈希值
     */
    private String generateProjectHash(String projectPath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(projectPath.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 12);  // 取前12位
        } catch (Exception e) {
            logger.warn("Failed to generate project hash", e);
            return "default";
        }
    }
    
    /**
     * 保存记忆
     */
    public Memory saveMemory(Memory memory) {
        if (memory.getId() == null || memory.getId().isEmpty()) {
            memory.setId(generateMemoryId());
        }
        
        memories.put(memory.getId(), memory);
        persistMemories();
        
        logger.debug("Saved memory: {}", memory.getId());
        return memory;
    }
    
    /**
     * 搜索记忆
     */
    public List<Memory> searchMemories(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(memories.values());
        }
        
        String lowerQuery = query.toLowerCase();
        
        return memories.values().stream()
                .filter(memory -> matchesQuery(memory, lowerQuery))
                .sorted(Comparator.<Memory>comparingDouble(m -> calculateRelevance(m, lowerQuery)).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查记忆是否匹配查询
     */
    private boolean matchesQuery(Memory memory, String query) {
        // 搜索标题
        if (memory.getTitle() != null && memory.getTitle().toLowerCase().contains(query)) {
            return true;
        }
        
        // 搜索内容
        if (memory.getContent() != null && memory.getContent().toLowerCase().contains(query)) {
            return true;
        }
        
        // 搜索关键词
        if (memory.getKeywords() != null) {
            for (String keyword : memory.getKeywords()) {
                if (keyword.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 计算相关度分数
     */
    private double calculateRelevance(Memory memory, String query) {
        double score = 0.0;
        
        // 标题匹配权重最高
        if (memory.getTitle() != null && memory.getTitle().toLowerCase().contains(query)) {
            score += 10.0;
        }
        
        // 关键词匹配
        if (memory.getKeywords() != null) {
            for (String keyword : memory.getKeywords()) {
                if (keyword.toLowerCase().contains(query)) {
                    score += 5.0;
                }
            }
        }
        
        // 内容匹配
        if (memory.getContent() != null && memory.getContent().toLowerCase().contains(query)) {
            score += 1.0;
        }
        
        // 最近更新的权重更高
        long daysSinceUpdate = java.time.Duration.between(
                memory.getUpdatedAt(), 
                java.time.Instant.now()
        ).toDays();
        score += Math.max(0, 5 - daysSinceUpdate * 0.1);
        
        return score;
    }
    
    /**
     * 按分类获取记忆
     */
    public List<Memory> getMemoriesByCategory(String category) {
        return memories.values().stream()
                .filter(m -> category.equals(m.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有记忆
     */
    public List<Memory> getAllMemories() {
        return new ArrayList<>(memories.values());
    }
    
    /**
     * 删除记忆
     */
    public boolean deleteMemory(String id) {
        Memory removed = memories.remove(id);
        if (removed != null) {
            persistMemories();
            return true;
        }
        return false;
    }
    
    /**
     * 清空所有记忆
     */
    public void clearMemories() {
        memories.clear();
        persistMemories();
    }
    
    /**
     * 持久化记忆到文件
     */
    private void persistMemories() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("memories", new ArrayList<>(memories.values()));
            
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(memoryStorePath.toFile(), data);
            
            logger.debug("Persisted {} memories to {}", memories.size(), memoryStorePath);
        } catch (IOException e) {
            logger.error("Failed to persist memories", e);
        }
    }
    
    /**
     * 从文件加载记忆
     */
    private void loadMemories() {
        if (!Files.exists(memoryStorePath)) {
            logger.debug("Memory store file not found, starting with empty memories");
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(
                    memoryStorePath.toFile(),
                    Map.class
            );
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> memoryList = (List<Map<String, Object>>) data.get("memories");
            
            if (memoryList != null) {
                for (Map<String, Object> memoryData : memoryList) {
                    Memory memory = objectMapper.convertValue(memoryData, Memory.class);
                    memories.put(memory.getId(), memory);
                }
            }
            
            logger.debug("Loaded {} memories from {}", memories.size(), memoryStorePath);
        } catch (IOException e) {
            logger.error("Failed to load memories", e);
        }
    }
    
    /**
     * 生成记忆 ID
     */
    private String generateMemoryId() {
        return "mem_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * 获取记忆统计信息
     */
    public Map<String, Integer> getMemoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", memories.size());
        
        // 按分类统计
        Map<String, Long> categoryCount = memories.values().stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCategory() != null ? m.getCategory() : "uncategorized",
                        Collectors.counting()
                ));
        
        categoryCount.forEach((category, count) -> 
                stats.put(category, count.intValue())
        );
        
        return stats;
    }
}
