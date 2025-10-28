package io.shareai.joder.tools.todo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shareai.joder.domain.Task;
import io.shareai.joder.services.TaskManager;
import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Todo 列表管理工具
 * 
 * 允许 AI 添加和更新任务列表
 */
public class TodoWriteTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(TodoWriteTool.class);
    
    private final TaskManager taskManager;
    private final ObjectMapper objectMapper;
    
    @Inject
    public TodoWriteTool(TaskManager taskManager, ObjectMapper objectMapper) {
        this.taskManager = taskManager;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "todo_write";
    }
    
    @Override
    public String getDescription() {
        return "管理 AI 生成的任务列表。支持添加新任务 (add_tasks) 和更新现有任务 (update_tasks)。" +
               "任务可以有层级结构（parent_task_id）和顺序（after_task_id）。" +
               "任务状态包括：PENDING（待处理）、IN_PROGRESS（进行中）、COMPLETE（已完成）、" +
               "CANCELLED（已取消）、ERROR（错误）。";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ToolResult call(Map<String, Object> params) {
        try {
            String action = (String) params.get("action");
            List<Map<String, Object>> taskDataList = (List<Map<String, Object>>) params.get("tasks");
            
            if (taskDataList == null || taskDataList.isEmpty()) {
                return ToolResult.error("tasks 参数不能为空");
            }
            
            // 转换为 Task 对象
            List<Task> tasks = new ArrayList<>();
            for (Map<String, Object> taskData : taskDataList) {
                Task task = objectMapper.convertValue(taskData, Task.class);
                tasks.add(task);
            }
            
            List<Task> result;
            switch (action) {
                case "add_tasks":
                    result = taskManager.addTasks(tasks);
                    logger.info("Added {} tasks", result.size());
                    return ToolResult.success(formatTasksResult(result, "添加"));
                    
                case "update_tasks":
                    result = taskManager.updateTasks(tasks);
                    logger.info("Updated {} tasks", result.size());
                    return ToolResult.success(formatTasksResult(result, "更新"));
                    
                default:
                    return ToolResult.error("未知操作: " + action);
            }
            
        } catch (Exception e) {
            logger.error("Error executing TodoWriteTool", e);
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化任务结果
     */
    private String formatTasksResult(List<Task> tasks, String action) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("成功%s %d 个任务:\n\n", action, tasks.size()));
        
        for (Task task : tasks) {
            String statusIcon = getStatusIcon(task.getStatus());
            sb.append(String.format("  %s [%s] %s\n", 
                    statusIcon, 
                    task.getId(), 
                    task.getContent()));
            
            if (task.getParentTaskId() != null) {
                sb.append(String.format("     └─ 父任务: %s\n", task.getParentTaskId()));
            }
        }
        
        // 添加统计信息
        Map<String, Integer> stats = taskManager.getTaskStats();
        sb.append(String.format("\n任务统计: 总计 %d | 待处理 %d | 进行中 %d | 已完成 %d\n",
                stats.get("total"),
                stats.get("pending"),
                stats.get("in_progress"),
                stats.get("complete")));
        
        return sb.toString();
    }
    
    /**
     * 获取状态图标
     */
    private String getStatusIcon(Task.Status status) {
        switch (status) {
            case PENDING:
                return "⏸";
            case IN_PROGRESS:
                return "▶";
            case COMPLETE:
                return "✓";
            case CANCELLED:
                return "✗";
            case ERROR:
                return "⚠";
            default:
                return "○";
        }
    }
}
