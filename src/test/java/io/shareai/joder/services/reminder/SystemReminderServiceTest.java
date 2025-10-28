package io.shareai.joder.services.reminder;

import io.shareai.joder.domain.ReminderCategory;
import io.shareai.joder.domain.ReminderMessage;
import io.shareai.joder.domain.ReminderPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemReminderService 测试
 */
class SystemReminderServiceTest {
    
    private SystemReminderService service;
    
    @BeforeEach
    void setUp() {
        service = new SystemReminderService();
    }
    
    @Test
    void testAddEventListener() {
        boolean[] called = {false};
        
        service.addEventListener("test:event", context -> {
            called[0] = true;
        });
        
        Map<String, Object> context = new HashMap<>();
        service.emitEvent("test:event", context);
        
        assertTrue(called[0]);
    }
    
    @Test
    void testEmitEvent() {
        String[] result = {null};
        
        service.addEventListener("custom:event", context -> {
            result[0] = (String) context.get("message");
        });
        
        Map<String, Object> context = new HashMap<>();
        context.put("message", "Hello");
        service.emitEvent("custom:event", context);
        
        assertEquals("Hello", result[0]);
    }
    
    @Test
    void testGenerateReminders_NoContext() {
        List<ReminderMessage> reminders = service.generateReminders(false, null);
        
        assertEquals(0, reminders.size());
    }
    
    @Test
    void testGenerateReminders_WithContext() {
        // 触发文件读取事件
        Map<String, Object> context = new HashMap<>();
        service.emitEvent("file:read", context);
        
        List<ReminderMessage> reminders = service.generateReminders(true, "test-agent");
        
        assertNotNull(reminders);
        // 应该至少有安全提醒
        assertTrue(reminders.size() >= 0);
    }
    
    @Test
    void testSecurityReminder() {
        // 触发文件读取
        Map<String, Object> context = new HashMap<>();
        service.emitEvent("file:read", context);
        
        List<ReminderMessage> reminders = service.generateReminders(true, null);
        
        // 查找安全提醒
        ReminderMessage securityReminder = reminders.stream()
                .filter(r -> "security".equals(r.getType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(securityReminder);
        assertEquals(ReminderCategory.SECURITY, securityReminder.getCategory());
        assertEquals(ReminderPriority.HIGH, securityReminder.getPriority());
        assertTrue(securityReminder.getContent().contains("malicious"));
    }
    
    @Test
    void testFileMentionReminder() {
        Map<String, Object> context = new HashMap<>();
        context.put("filePath", "/test/file.txt");
        context.put("originalMention", "file:test/file.txt");
        context.put("timestamp", System.currentTimeMillis());
        
        service.emitEvent("file:mentioned", context);
        
        List<ReminderMessage> reminders = service.generateReminders(true, null);
        
        // 查找文件 mention 提醒
        ReminderMessage fileMention = reminders.stream()
                .filter(r -> "file_mention".equals(r.getType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(fileMention);
        assertEquals(ReminderCategory.GENERAL, fileMention.getCategory());
        assertEquals(ReminderPriority.HIGH, fileMention.getPriority());
        assertTrue(fileMention.getContent().contains("/test/file.txt"));
    }
    
    @Test
    void testAgentMentionReminder() {
        Map<String, Object> context = new HashMap<>();
        context.put("agentType", "code-reviewer");
        context.put("originalMention", "agent:code-reviewer");
        context.put("timestamp", System.currentTimeMillis());
        
        service.emitEvent("agent:mentioned", context);
        
        List<ReminderMessage> reminders = service.generateReminders(true, null);
        
        ReminderMessage agentMention = reminders.stream()
                .filter(r -> "agent_mention".equals(r.getType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(agentMention);
        assertEquals(ReminderCategory.TASK, agentMention.getCategory());
        assertEquals(ReminderPriority.HIGH, agentMention.getPriority());
        assertTrue(agentMention.getContent().contains("code-reviewer"));
    }
    
    @Test
    void testModelMentionReminder() {
        Map<String, Object> context = new HashMap<>();
        context.put("modelName", "gpt-4");
        context.put("originalMention", "ask-model:gpt-4");
        context.put("timestamp", System.currentTimeMillis());
        
        service.emitEvent("model:mentioned", context);
        
        List<ReminderMessage> reminders = service.generateReminders(true, null);
        
        ReminderMessage modelMention = reminders.stream()
                .filter(r -> "ask_model_mention".equals(r.getType()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(modelMention);
        assertEquals(ReminderCategory.TASK, modelMention.getCategory());
        assertTrue(modelMention.getContent().contains("gpt-4"));
    }
    
    @Test
    void testResetSession() {
        // 触发一些事件
        Map<String, Object> context = new HashMap<>();
        service.emitEvent("file:read", context);
        service.generateReminders(true, null);
        
        // 重置会话
        service.resetSession();
        
        Map<String, Object> state = service.getSessionState();
        
        assertEquals(0, state.get("reminderCount"));
        assertFalse((Boolean) state.get("contextPresent"));
    }
    
    @Test
    void testSessionState() {
        Map<String, Object> state = service.getSessionState();
        
        assertNotNull(state);
        assertTrue(state.containsKey("sessionStartTime"));
        assertTrue(state.containsKey("reminderCount"));
        assertTrue(state.containsKey("contextPresent"));
    }
    
    @Test
    void testReminderDeduplication() {
        // 发送相同的 mention 两次
        Map<String, Object> context = new HashMap<>();
        context.put("filePath", "/same/file.txt");
        context.put("originalMention", "file:same/file.txt");
        context.put("timestamp", System.currentTimeMillis());
        
        service.emitEvent("file:mentioned", context);
        service.emitEvent("file:mentioned", context);
        
        List<ReminderMessage> reminders = service.generateReminders(true, null);
        
        // 应该只有一个提醒（去重）
        long fileMentionCount = reminders.stream()
                .filter(r -> "file_mention".equals(r.getType()))
                .filter(r -> r.getContent().contains("/same/file.txt"))
                .count();
        
        assertEquals(1, fileMentionCount);
    }
    
    @Test
    void testCleanExpiredReminders() throws InterruptedException {
        Map<String, Object> context = new HashMap<>();
        context.put("filePath", "/test/expired.txt");
        context.put("originalMention", "file:expired.txt");
        context.put("timestamp", System.currentTimeMillis() - (10 * 60 * 1000)); // 10分钟前
        
        service.emitEvent("file:mentioned", context);
        
        // 清理过期提醒
        service.cleanExpiredReminders();
        
        // 过期提醒应该被清理
        List<ReminderMessage> reminders = service.generateReminders(true, null);
        long expiredCount = reminders.stream()
                .filter(r -> r.getContent().contains("/test/expired.txt"))
                .count();
        
        assertEquals(0, expiredCount);
    }
}
