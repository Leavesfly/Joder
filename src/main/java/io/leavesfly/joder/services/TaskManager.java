package io.leavesfly.joder.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.domain.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务管理器 - 管理 AI 生成的任务列表
 */
@Singleton
public class TaskManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Path taskStorePath;
    
    @Inject
    public TaskManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.taskStorePath = getTaskStorePath();
        loadTasks();
    }
    
    private Path getTaskStorePath() {
        String userHome = System.getProperty("user.home");
        Path joderDir = Paths.get(userHome, ".joder");
        
        try {
            Files.createDirectories(joderDir);
        } catch (IOException e) {
            logger.warn("Failed to create .joder directory", e);
        }
        
        return joderDir.resolve("tasks.json");
    }
    
    /**
     * 添加任务列表
     */
    public List<Task> addTasks(List<Task> newTasks) {
        for (Task task : newTasks) {
            if (task.getId() == null || task.getId().isEmpty()) {
                task.setId(generateTaskId());
            }
            if (task.getStatus() == null) {
                task.setStatus(Task.Status.PENDING);
            }
            tasks.put(task.getId(), task);
        }
        
        saveTasks();
        return newTasks;
    }
    
    /**
     * 更新任务列表
     */
    public List<Task> updateTasks(List<Task> updatedTasks) {
        List<Task> result = new ArrayList<>();
        
        for (Task update : updatedTasks) {
            Task existing = tasks.get(update.getId());
            if (existing != null) {
                if (update.getContent() != null) {
                    existing.setContent(update.getContent());
                }
                if (update.getStatus() != null) {
                    existing.setStatus(update.getStatus());
                }
                if (update.getParentTaskId() != null) {
                    existing.setParentTaskId(update.getParentTaskId());
                }
                if (update.getAfterTaskId() != null) {
                    existing.setAfterTaskId(update.getAfterTaskId());
                }
                result.add(existing);
            } else {
                logger.warn("Task not found for update: {}", update.getId());
            }
        }
        
        saveTasks();
        return result;
    }
    
    /**
     * 获取所有任务
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * 按状态筛选任务
     */
    public List<Task> getTasksByStatus(Task.Status status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务树（带层级结构）
     */
    public List<Task> getTaskTree() {
        // 获取所有根任务（没有 parent）
        List<Task> rootTasks = tasks.values().stream()
                .filter(task -> task.getParentTaskId() == null)
                .sorted(Comparator.comparing(Task::getCreatedAt))
                .collect(Collectors.toList());
        
        return rootTasks;
    }
    
    /**
     * 获取子任务
     */
    public List<Task> getSubTasks(String parentId) {
        return tasks.values().stream()
                .filter(task -> parentId.equals(task.getParentTaskId()))
                .sorted(Comparator.comparing(Task::getCreatedAt))
                .collect(Collectors.toList());
    }
    
    /**
     * 清空所有任务
     */
    public void clearTasks() {
        tasks.clear();
        saveTasks();
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        Task removed = tasks.remove(taskId);
        if (removed != null) {
            saveTasks();
            return true;
        }
        return false;
    }
    
    /**
     * 保存任务到文件
     */
    private void saveTasks() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("tasks", new ArrayList<>(tasks.values()));
            
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(taskStorePath.toFile(), data);
            
            logger.debug("Tasks saved to {}", taskStorePath);
        } catch (IOException e) {
            logger.error("Failed to save tasks", e);
        }
    }
    
    /**
     * 从文件加载任务
     */
    private void loadTasks() {
        if (!Files.exists(taskStorePath)) {
            logger.debug("Task store file not found, starting with empty task list");
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(
                    taskStorePath.toFile(),
                    Map.class
            );
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taskList = (List<Map<String, Object>>) data.get("tasks");
            
            if (taskList != null) {
                for (Map<String, Object> taskData : taskList) {
                    Task task = objectMapper.convertValue(taskData, Task.class);
                    tasks.put(task.getId(), task);
                }
            }
            
            logger.debug("Loaded {} tasks from {}", tasks.size(), taskStorePath);
        } catch (IOException e) {
            logger.error("Failed to load tasks", e);
        }
    }
    
    /**
     * 生成任务 ID
     */
    private String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * 获取任务统计信息
     */
    public Map<String, Integer> getTaskStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", tasks.size());
        stats.put("pending", (int) tasks.values().stream()
                .filter(t -> t.getStatus() == Task.Status.PENDING).count());
        stats.put("in_progress", (int) tasks.values().stream()
                .filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count());
        stats.put("complete", (int) tasks.values().stream()
                .filter(t -> t.getStatus() == Task.Status.COMPLETE).count());
        stats.put("cancelled", (int) tasks.values().stream()
                .filter(t -> t.getStatus() == Task.Status.CANCELLED).count());
        stats.put("error", (int) tasks.values().stream()
                .filter(t -> t.getStatus() == Task.Status.ERROR).count());
        
        return stats;
    }
}
