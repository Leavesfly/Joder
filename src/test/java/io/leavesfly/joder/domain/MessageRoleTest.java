package io.leavesfly.joder.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息角色枚举单元测试
 */
@DisplayName("MessageRole 枚举测试")
public class MessageRoleTest {
    
    @Test
    @DisplayName("应该定义所有预期的消息角色")
    public void testAllRolesAreDefined() {
        // Act & Assert
        assertNotNull(MessageRole.USER);
        assertNotNull(MessageRole.ASSISTANT);
        assertNotNull(MessageRole.SYSTEM);
    }
    
    @Test
    @DisplayName("应该能获取所有枚举值")
    public void testCanGetAllEnumValues() {
        // Act
        MessageRole[] roles = MessageRole.values();
        
        // Assert
        assertEquals(3, roles.length);
    }
    
    @Test
    @DisplayName("应该能通过名称获取枚举值")
    public void testCanGetEnumByName() {
        // Act & Assert
        assertEquals(MessageRole.USER, MessageRole.valueOf("USER"));
        assertEquals(MessageRole.ASSISTANT, MessageRole.valueOf("ASSISTANT"));
        assertEquals(MessageRole.SYSTEM, MessageRole.valueOf("SYSTEM"));
    }
    
    @Test
    @DisplayName("应该在消息中正确使用角色")
    public void testRolesWorkInMessages() {
        // Arrange
        Message userMessage = new Message(MessageRole.USER, "User query");
        Message assistantMessage = new Message(MessageRole.ASSISTANT, "Assistant response");
        Message systemMessage = new Message(MessageRole.SYSTEM, "System info");
        
        // Act & Assert
        assertEquals(MessageRole.USER, userMessage.getRole());
        assertEquals(MessageRole.ASSISTANT, assistantMessage.getRole());
        assertEquals(MessageRole.SYSTEM, systemMessage.getRole());
    }
    
    @Test
    @DisplayName("应该区分不同的角色")
    public void testRolesAreDistinct() {
        // Act & Assert
        assertNotEquals(MessageRole.USER, MessageRole.ASSISTANT);
        assertNotEquals(MessageRole.ASSISTANT, MessageRole.SYSTEM);
        assertNotEquals(MessageRole.USER, MessageRole.SYSTEM);
    }
    
    @Test
    @DisplayName("USER 角色应该代表用户消息")
    public void testUserRoleRepresentsUser() {
        // Arrange & Act
        Message message = new Message(MessageRole.USER, "User query");
        
        // Assert
        assertTrue(message.getRole() == MessageRole.USER);
    }
    
    @Test
    @DisplayName("ASSISTANT 角色应该代表AI助手消息")
    public void testAssistantRoleRepresentsAI() {
        // Arrange & Act
        Message message = new Message(MessageRole.ASSISTANT, "AI response");
        
        // Assert
        assertTrue(message.getRole() == MessageRole.ASSISTANT);
    }
    
    @Test
    @DisplayName("SYSTEM 角色应该代表系统消息")
    public void testSystemRoleRepresentsSystem() {
        // Arrange & Act
        Message message = new Message(MessageRole.SYSTEM, "System notification");
        
        // Assert
        assertTrue(message.getRole() == MessageRole.SYSTEM);
    }
}
