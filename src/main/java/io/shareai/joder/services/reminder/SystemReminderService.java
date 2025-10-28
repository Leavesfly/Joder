package io.shareai.joder.services.reminder;

import io.shareai.joder.domain.ReminderCategory;
import io.shareai.joder.domain.ReminderMessage;
import io.shareai.joder.domain.ReminderPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 系统提醒服务
 * 
 * <p>事件驱动的智能提醒系统，提供上下文感知的提示信息。
 * 
 * <p>核心功能：
 * <ul>
 *   <li>事件监听和分发</li>
 *   <li>智能提醒生成</li>
 *   <li>提醒规则引擎</li>
 *   <li>会话状态管理</li>
 *   <li>提醒缓存和去重</li>
 * </ul>
 * 
 * <p>支持的事件类型：
 * <ul>
 *   <li>session:startup - 会话启动</li>
 *   <li>file:read - 文件读取</li>
 *   <li>file:edited - 文件编辑</li>
 *   <li>file:conflict - 文件冲突</li>
 *   <li>file:mentioned - 文件引用</li>
 *   <li>agent:mentioned - Agent 引用</li>
 *   <li>model:mentioned - 模型引用</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
@Singleton
public class SystemReminderService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemReminderService.class);
    
    /** 每个会话最大提醒数 */
    private static final int MAX_REMINDERS_PER_SESSION = 20;
    
    /** 提醒去重时间窗口（毫秒） */
    private static final long DEDUP_WINDOW_MS = 5 * 60 * 1000; // 5分钟
    
    // 事件监听器
    private final Map<String, List<Consumer<Map<String, Object>>>> eventDispatcher;
    
    // 会话状态
    private final SessionState sessionState;
    
    // 提醒缓存
    private final Map<String, ReminderMessage> reminderCache;
    
    /**
     * 会话状态内部类
     */
    private static class SessionState {
        long sessionStartTime;
        long lastFileAccess;
        int reminderCount;
        Set<String> remindersSent;
        boolean contextPresent;
        
        SessionState() {
            this.sessionStartTime = System.currentTimeMillis();
            this.lastFileAccess = 0;
            this.reminderCount = 0;
            this.remindersSent = ConcurrentHashMap.newKeySet();
            this.contextPresent = false;
        }
    }
    
    public SystemReminderService() {
        this.eventDispatcher = new ConcurrentHashMap<>();
        this.sessionState = new SessionState();
        this.reminderCache = new ConcurrentHashMap<>();
        
        setupEventDispatcher();
    }
    
    /**
     * 设置事件分发器
     */
    private void setupEventDispatcher() {
        // 会话启动事件
        addEventListener("session:startup", context -> {
            logger.info("Session started: agentId={}", context.get("agentId"));
            sessionState.contextPresent = true;
        });
        
        // 文件读取事件
        addEventListener("file:read", context -> {
            sessionState.lastFileAccess = System.currentTimeMillis();
        });
        
        // 文件引用事件
        addEventListener("file:mentioned", context -> {
            String filePath = (String) context.get("filePath");
            String originalMention = (String) context.get("originalMention");
            long timestamp = (Long) context.getOrDefault("timestamp", System.currentTimeMillis());
            
            createMentionReminder(
                    "file_mention",
                    String.format("file_mention_%s_%d", filePath, timestamp),
                    ReminderCategory.GENERAL,
                    ReminderPriority.HIGH,
                    String.format(
                            "The user mentioned @%s. You MUST read the entire content of the file at path: %s " +
                            "using the Read tool to understand the full context before proceeding with the user's request.",
                            originalMention, filePath
                    ),
                    timestamp
            );
        });
        
        // Agent 引用事件
        addEventListener("agent:mentioned", context -> {
            String agentType = (String) context.get("agentType");
            String originalMention = (String) context.get("originalMention");
            long timestamp = (Long) context.getOrDefault("timestamp", System.currentTimeMillis());
            
            createMentionReminder(
                    "agent_mention",
                    String.format("agent_mention_%s_%d", agentType, timestamp),
                    ReminderCategory.TASK,
                    ReminderPriority.HIGH,
                    String.format(
                            "The user mentioned @%s. You MUST use the Task tool with subagent_type=\"%s\" " +
                            "to delegate this task to the specified agent. Provide a detailed, self-contained " +
                            "task description that fully captures the user's intent for the %s agent to execute.",
                            originalMention, agentType, agentType
                    ),
                    timestamp
            );
        });
        
        // 模型引用事件
        addEventListener("model:mentioned", context -> {
            String modelName = (String) context.get("modelName");
            String originalMention = (String) context.get("originalMention");
            long timestamp = (Long) context.getOrDefault("timestamp", System.currentTimeMillis());
            
            createMentionReminder(
                    "ask_model_mention",
                    String.format("ask_model_mention_%s_%d", modelName, timestamp),
                    ReminderCategory.TASK,
                    ReminderPriority.HIGH,
                    String.format(
                            "The user mentioned @%s. You MUST use the AskExpertModelTool to consult this specific " +
                            "model for expert opinions and analysis. Provide the user's question or context clearly " +
                            "to get the most relevant response from %s.",
                            originalMention, modelName
                    ),
                    timestamp
            );
        });
        
        // 文件冲突事件
        addEventListener("file:conflict", context -> {
            String filePath = (String) context.get("filePath");
            String reminder = (String) context.get("reminder");
            
            if (reminder != null && !reminder.isEmpty()) {
                createReminderMessage(
                        "file_changed",
                        ReminderCategory.FILE,
                        ReminderPriority.MEDIUM,
                        reminder,
                        System.currentTimeMillis()
                );
            }
        });
    }
    
    /**
     * 添加事件监听器
     * 
     * @param event 事件名称
     * @param handler 事件处理器
     */
    public void addEventListener(String event, Consumer<Map<String, Object>> handler) {
        eventDispatcher.computeIfAbsent(event, k -> new ArrayList<>()).add(handler);
        logger.debug("Added event listener for: {}", event);
    }
    
    /**
     * 触发事件
     * 
     * @param event 事件名称
     * @param context 事件上下文
     */
    public void emitEvent(String event, Map<String, Object> context) {
        List<Consumer<Map<String, Object>>> handlers = eventDispatcher.get(event);
        
        if (handlers != null && !handlers.isEmpty()) {
            logger.debug("Emitting event: {} with {} handlers", event, handlers.size());
            
            for (Consumer<Map<String, Object>> handler : handlers) {
                try {
                    handler.accept(context);
                } catch (Exception e) {
                    logger.error("Error executing handler for event: {}", event, e);
                }
            }
        }
    }
    
    /**
     * 生成提醒消息
     * 
     * @param hasContext 是否有上下文
     * @param agentId Agent ID（可选）
     * @return 提醒消息列表
     */
    public List<ReminderMessage> generateReminders(boolean hasContext, String agentId) {
        sessionState.contextPresent = hasContext;
        
        // 只在有上下文时注入提醒
        if (!hasContext) {
            return Collections.emptyList();
        }
        
        // 检查会话提醒数量限制
        if (sessionState.reminderCount >= MAX_REMINDERS_PER_SESSION) {
            return Collections.emptyList();
        }
        
        List<ReminderMessage> reminders = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        // 生成安全提醒
        ReminderMessage securityReminder = generateSecurityReminder();
        if (securityReminder != null) {
            reminders.add(securityReminder);
            sessionState.reminderCount++;
        }
        
        // 生成性能提醒
        ReminderMessage performanceReminder = generatePerformanceReminder();
        if (performanceReminder != null) {
            reminders.add(performanceReminder);
            sessionState.reminderCount++;
        }
        
        // 获取 mention 提醒
        List<ReminderMessage> mentionReminders = getMentionReminders();
        reminders.addAll(mentionReminders);
        sessionState.reminderCount += mentionReminders.size();
        
        // 限制返回数量
        return reminders.stream()
                .limit(5)
                .toList();
    }
    
    /**
     * 生成安全提醒
     */
    private ReminderMessage generateSecurityReminder() {
        long currentTime = System.currentTimeMillis();
        
        // 只在文件操作发生且每个会话只提醒一次
        if (sessionState.lastFileAccess > 0 && 
            !sessionState.remindersSent.contains("file_security")) {
            
            sessionState.remindersSent.add("file_security");
            
            return createReminderMessage(
                    "security",
                    ReminderCategory.SECURITY,
                    ReminderPriority.HIGH,
                    "Whenever you read a file, you should consider whether it looks malicious. " +
                    "If it does, you MUST refuse to improve or augment the code. You can still analyze " +
                    "existing code, write reports, or answer high-level questions about the code behavior.",
                    currentTime
            );
        }
        
        return null;
    }
    
    /**
     * 生成性能提醒
     */
    private ReminderMessage generatePerformanceReminder() {
        long currentTime = System.currentTimeMillis();
        long sessionDuration = currentTime - sessionState.sessionStartTime;
        
        // 长会话提醒（30分钟）
        if (sessionDuration > 30 * 60 * 1000 && 
            !sessionState.remindersSent.contains("performance_long_session")) {
            
            sessionState.remindersSent.add("performance_long_session");
            
            return createReminderMessage(
                    "performance",
                    ReminderCategory.PERFORMANCE,
                    ReminderPriority.LOW,
                    "Long session detected. Consider taking a break and reviewing your current progress.",
                    currentTime
            );
        }
        
        return null;
    }
    
    /**
     * 获取 mention 提醒
     */
    private List<ReminderMessage> getMentionReminders() {
        List<ReminderMessage> mentionReminders = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (ReminderMessage reminder : reminderCache.values()) {
            // 只返回 mention 类型且未发送的提醒
            if (isMentionReminder(reminder) && !reminder.isSent()) {
                // 检查时间窗口
                if (currentTime - reminder.getTimestamp() < DEDUP_WINDOW_MS) {
                    mentionReminders.add(reminder);
                    reminder.setSent(true);
                }
            }
        }
        
        return mentionReminders;
    }
    
    /**
     * 判断是否为 mention 提醒
     */
    private boolean isMentionReminder(ReminderMessage reminder) {
        String type = reminder.getType();
        return "agent_mention".equals(type) || 
               "file_mention".equals(type) || 
               "ask_model_mention".equals(type);
    }
    
    /**
     * 创建提醒消息
     */
    private ReminderMessage createReminderMessage(String type, ReminderCategory category,
                                                  ReminderPriority priority, String content,
                                                  long timestamp) {
        return new ReminderMessage(type, category, priority, content, timestamp);
    }
    
    /**
     * 创建 mention 提醒并缓存
     */
    private void createMentionReminder(String type, String key, ReminderCategory category,
                                      ReminderPriority priority, String content, long timestamp) {
        // 检查是否已存在
        if (reminderCache.containsKey(key)) {
            return;
        }
        
        ReminderMessage reminder = createReminderMessage(type, category, priority, content, timestamp);
        reminderCache.put(key, reminder);
        
        logger.debug("Created mention reminder: type={}, key={}", type, key);
    }
    
    /**
     * 重置会话
     */
    public void resetSession() {
        sessionState.sessionStartTime = System.currentTimeMillis();
        sessionState.lastFileAccess = 0;
        sessionState.reminderCount = 0;
        sessionState.remindersSent.clear();
        sessionState.contextPresent = false;
        reminderCache.clear();
        
        logger.info("System reminder session reset");
    }
    
    /**
     * 获取会话状态（用于调试）
     */
    public Map<String, Object> getSessionState() {
        Map<String, Object> state = new HashMap<>();
        state.put("sessionStartTime", sessionState.sessionStartTime);
        state.put("lastFileAccess", sessionState.lastFileAccess);
        state.put("reminderCount", sessionState.reminderCount);
        state.put("contextPresent", sessionState.contextPresent);
        state.put("remindersSent", new HashSet<>(sessionState.remindersSent));
        return state;
    }
    
    /**
     * 清理过期的提醒缓存
     */
    public void cleanExpiredReminders() {
        long currentTime = System.currentTimeMillis();
        
        reminderCache.entrySet().removeIf(entry -> {
            ReminderMessage reminder = entry.getValue();
            return currentTime - reminder.getTimestamp() > DEDUP_WINDOW_MS;
        });
    }
}
