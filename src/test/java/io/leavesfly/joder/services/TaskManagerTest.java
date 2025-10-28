package io.leavesfly.joder.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskManager 单元测试
 */
@DisplayName("TaskManager 任务管理器测试")
public class TaskManagerTest {
    
    private TaskManager taskManager;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        taskManager = new TaskManager(objectMapper);
        taskManager.clearTasks(); // 清空之前的测试数据
    }
    
    @Test
    @DisplayName("应该能添加任务")
    void testAddTask() {
        // Arrange
        Task task = createTestTask("完成功能开发");
        
        // Act
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        
        // Assert
        assertNotNull(added);
        assertEquals(1, added.size());
        assertNotNull(added.get(0).getId());
        assertEquals("完成功能开发", added.get(0).getContent());
    }
    
    @Test
    @DisplayName("应该为新任务生成唯一ID")
    void testGenerateUniqueIdForNewTask() {
        // Arrange
        Task task1 = createTestTask("任务1");
        Task task2 = createTestTask("任务2");
        
        // Act
        List<Task> added = taskManager.addTasks(Arrays.asList(task1, task2));
        
        // Assert
        assertEquals(2, added.size());
        assertNotNull(added.get(0).getId());
        assertNotNull(added.get(1).getId());
        assertNotEquals(added.get(0).getId(), added.get(1).getId());
    }
    
    @Test
    @DisplayName("应该为新任务设置默认状态为PENDING")
    void testSetDefaultStatusToPending() {
        // Arrange
        Task task = createTestTask("测试任务");
        task.setStatus(null);
        
        // Act
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        
        // Assert
        assertEquals(Task.Status.PENDING, added.get(0).getStatus());
    }
    
    @Test
    @DisplayName("应该能添加多个任务")
    void testAddMultipleTasks() {
        // Arrange
        Task task1 = createTestTask("任务1");
        Task task2 = createTestTask("任务2");
        Task task3 = createTestTask("任务3");
        
        // Act
        taskManager.addTasks(Arrays.asList(task1, task2, task3));
        
        // Assert
        List<Task> allTasks = taskManager.getAllTasks();
        assertEquals(3, allTasks.size());
    }
    
    @Test
    @DisplayName("应该能更新任务")
    void testUpdateTask() {
        // Arrange
        Task task = createTestTask("原始内容");
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        String taskId = added.get(0).getId();
        
        Task update = new Task();
        update.setId(taskId);
        update.setContent("更新后的内容");
        update.setStatus(Task.Status.IN_PROGRESS);
        
        // Act
        List<Task> updated = taskManager.updateTasks(Arrays.asList(update));
        
        // Assert
        assertEquals(1, updated.size());
        assertEquals("更新后的内容", updated.get(0).getContent());
        assertEquals(Task.Status.IN_PROGRESS, updated.get(0).getStatus());
    }
    
    @Test
    @DisplayName("应该只更新提供的字段")
    void testUpdateOnlyProvidedFields() {
        // Arrange
        Task task = createTestTask("原始内容");
        task.setStatus(Task.Status.PENDING);
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        String taskId = added.get(0).getId();
        
        Task update = new Task();
        update.setId(taskId);
        update.setStatus(Task.Status.COMPLETE);
        // 不更新 content
        
        // Act
        List<Task> updated = taskManager.updateTasks(Arrays.asList(update));
        
        // Assert
        assertEquals("原始内容", updated.get(0).getContent());
        assertEquals(Task.Status.COMPLETE, updated.get(0).getStatus());
    }
    
    @Test
    @DisplayName("更新不存在的任务应该被忽略")
    void testUpdateNonexistentTaskIsIgnored() {
        // Arrange
        Task update = new Task();
        update.setId("nonexistent_id");
        update.setContent("不存在的任务");
        
        // Act
        List<Task> updated = taskManager.updateTasks(Arrays.asList(update));
        
        // Assert
        assertTrue(updated.isEmpty());
    }
    
    @Test
    @DisplayName("应该能获取所有任务")
    void testGetAllTasks() {
        // Arrange
        taskManager.addTasks(Arrays.asList(
            createTestTask("任务1"),
            createTestTask("任务2"),
            createTestTask("任务3")
        ));
        
        // Act
        List<Task> allTasks = taskManager.getAllTasks();
        
        // Assert
        assertEquals(3, allTasks.size());
    }
    
    @Test
    @DisplayName("应该能按状态筛选任务")
    void testGetTasksByStatus() {
        // Arrange
        Task task1 = createTestTask("待处理任务");
        task1.setStatus(Task.Status.PENDING);
        
        Task task2 = createTestTask("进行中任务");
        task2.setStatus(Task.Status.IN_PROGRESS);
        
        Task task3 = createTestTask("已完成任务");
        task3.setStatus(Task.Status.COMPLETE);
        
        taskManager.addTasks(Arrays.asList(task1, task2, task3));
        
        // Act
        List<Task> pendingTasks = taskManager.getTasksByStatus(Task.Status.PENDING);
        List<Task> inProgressTasks = taskManager.getTasksByStatus(Task.Status.IN_PROGRESS);
        List<Task> completeTasks = taskManager.getTasksByStatus(Task.Status.COMPLETE);
        
        // Assert
        assertEquals(1, pendingTasks.size());
        assertEquals(1, inProgressTasks.size());
        assertEquals(1, completeTasks.size());
    }
    
    @Test
    @DisplayName("应该能获取子任务")
    void testGetSubTasks() {
        // Arrange
        Task parentTask = createTestTask("父任务");
        List<Task> added = taskManager.addTasks(Arrays.asList(parentTask));
        String parentId = added.get(0).getId();
        
        Task subTask1 = createTestTask("子任务1");
        subTask1.setParentTaskId(parentId);
        
        Task subTask2 = createTestTask("子任务2");
        subTask2.setParentTaskId(parentId);
        
        taskManager.addTasks(Arrays.asList(subTask1, subTask2));
        
        // Act
        List<Task> subTasks = taskManager.getSubTasks(parentId);
        
        // Assert
        assertEquals(2, subTasks.size());
        assertTrue(subTasks.stream().allMatch(t -> parentId.equals(t.getParentTaskId())));
    }
    
    @Test
    @DisplayName("应该能删除任务")
    void testDeleteTask() {
        // Arrange
        Task task = createTestTask("待删除任务");
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        String taskId = added.get(0).getId();
        
        // Act
        boolean deleted = taskManager.deleteTask(taskId);
        
        // Assert
        assertTrue(deleted);
        assertFalse(taskManager.getAllTasks().stream()
            .anyMatch(t -> t.getId().equals(taskId)));
    }
    
    @Test
    @DisplayName("删除不存在的任务应该返回false")
    void testDeleteNonexistentTaskReturnsFalse() {
        // Act
        boolean deleted = taskManager.deleteTask("nonexistent_id");
        
        // Assert
        assertFalse(deleted);
    }
    
    @Test
    @DisplayName("应该能清空所有任务")
    void testClearTasks() {
        // Arrange
        taskManager.addTasks(Arrays.asList(
            createTestTask("任务1"),
            createTestTask("任务2")
        ));
        
        // Act
        taskManager.clearTasks();
        
        // Assert
        assertEquals(0, taskManager.getAllTasks().size());
    }
    
    @Test
    @DisplayName("应该能获取任务统计信息")
    void testGetTaskStats() {
        // Arrange
        Task task1 = createTestTask("待处理");
        task1.setStatus(Task.Status.PENDING);
        
        Task task2 = createTestTask("进行中");
        task2.setStatus(Task.Status.IN_PROGRESS);
        
        Task task3 = createTestTask("已完成");
        task3.setStatus(Task.Status.COMPLETE);
        
        Task task4 = createTestTask("已取消");
        task4.setStatus(Task.Status.CANCELLED);
        
        Task task5 = createTestTask("错误");
        task5.setStatus(Task.Status.ERROR);
        
        taskManager.addTasks(Arrays.asList(task1, task2, task3, task4, task5));
        
        // Act
        Map<String, Integer> stats = taskManager.getTaskStats();
        
        // Assert
        assertNotNull(stats);
        assertEquals(5, stats.get("total"));
        assertEquals(1, stats.get("pending"));
        assertEquals(1, stats.get("in_progress"));
        assertEquals(1, stats.get("complete"));
        assertEquals(1, stats.get("cancelled"));
        assertEquals(1, stats.get("error"));
    }
    
    @Test
    @DisplayName("应该能获取任务树")
    void testGetTaskTree() {
        // Arrange
        Task rootTask1 = createTestTask("根任务1");
        Task rootTask2 = createTestTask("根任务2");
        
        List<Task> added = taskManager.addTasks(Arrays.asList(rootTask1, rootTask2));
        
        Task subTask = createTestTask("子任务");
        subTask.setParentTaskId(added.get(0).getId());
        taskManager.addTasks(Arrays.asList(subTask));
        
        // Act
        List<Task> taskTree = taskManager.getTaskTree();
        
        // Assert
        assertEquals(2, taskTree.size());
        assertTrue(taskTree.stream().allMatch(t -> t.getParentTaskId() == null));
    }
    
    @Test
    @DisplayName("应该保留已有ID的任务")
    void testPreserveExistingTaskId() {
        // Arrange
        Task task = createTestTask("测试任务");
        task.setId("custom_id_123");
        
        // Act
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        
        // Assert
        assertEquals("custom_id_123", added.get(0).getId());
    }
    
    @Test
    @DisplayName("应该能更新任务的父任务和位置")
    void testUpdateTaskParentAndPosition() {
        // Arrange
        Task task = createTestTask("测试任务");
        List<Task> added = taskManager.addTasks(Arrays.asList(task));
        String taskId = added.get(0).getId();
        
        Task update = new Task();
        update.setId(taskId);
        update.setParentTaskId("parent_123");
        update.setAfterTaskId("after_456");
        
        // Act
        List<Task> updated = taskManager.updateTasks(Arrays.asList(update));
        
        // Assert
        assertEquals(1, updated.size());
        assertEquals("parent_123", updated.get(0).getParentTaskId());
        assertEquals("after_456", updated.get(0).getAfterTaskId());
    }
    
    @Test
    @DisplayName("批量更新应该返回所有成功更新的任务")
    void testBatchUpdateReturnSuccessfulUpdates() {
        // Arrange
        Task task1 = createTestTask("任务1");
        Task task2 = createTestTask("任务2");
        List<Task> added = taskManager.addTasks(Arrays.asList(task1, task2));
        
        Task update1 = new Task();
        update1.setId(added.get(0).getId());
        update1.setStatus(Task.Status.COMPLETE);
        
        Task update2 = new Task();
        update2.setId("nonexistent_id");
        update2.setStatus(Task.Status.COMPLETE);
        
        // Act
        List<Task> updated = taskManager.updateTasks(Arrays.asList(update1, update2));
        
        // Assert
        assertEquals(1, updated.size());
        assertEquals(added.get(0).getId(), updated.get(0).getId());
    }
    
    /**
     * 创建测试任务
     */
    private Task createTestTask(String content) {
        Task task = new Task();
        task.setContent(content);
        task.setStatus(Task.Status.PENDING);
        return task;
    }
}
