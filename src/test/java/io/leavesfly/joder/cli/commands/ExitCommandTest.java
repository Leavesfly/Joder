package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExitCommand 单元测试
 */
@DisplayName("ExitCommand 退出命令测试")
public class ExitCommandTest {
    
    private ExitCommand exitCommand;
    
    @BeforeEach
    void setUp() {
        exitCommand = new ExitCommand();
    }
    
    @Test
    @DisplayName("应该返回退出结果")
    void testExecuteReturnsExit() {
        // Act
        CommandResult result = exitCommand.execute("");
        
        // Assert
        assertNotNull(result);
        assertTrue(result.shouldExit());
        assertEquals("再见！", result.getMessage());
    }
    
    @Test
    @DisplayName("无论参数如何都应该退出")
    void testExecuteWithAnyArgs() {
        // Act
        CommandResult result1 = exitCommand.execute("");
        CommandResult result2 = exitCommand.execute("some args");
        
        // Assert
        assertTrue(result1.shouldExit());
        assertTrue(result2.shouldExit());
    }
    
    @Test
    @DisplayName("命令描述应该正确")
    void testGetDescription() {
        // Act
        String description = exitCommand.getDescription();
        
        // Assert
        assertEquals("退出 Joder", description);
    }
    
    @Test
    @DisplayName("命令用法应该正确")
    void testGetUsage() {
        // Act
        String usage = exitCommand.getUsage();
        
        // Assert
        assertEquals("/exit", usage);
    }
}
