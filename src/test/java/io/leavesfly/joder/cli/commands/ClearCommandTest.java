package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClearCommand 单元测试
 */
@DisplayName("ClearCommand 清空命令测试")
public class ClearCommandTest {
    
    private ClearCommand clearCommand;
    
    @BeforeEach
    void setUp() {
        clearCommand = new ClearCommand();
    }
    
    @Test
    @DisplayName("应该返回成功结果")
    void testExecuteReturnsSuccess() {
        // Act
        CommandResult result = clearCommand.execute("");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.SUCCESS, result.getType());
        assertEquals("屏幕已清空", result.getMessage());
    }
    
    @Test
    @DisplayName("无论参数如何都应该成功")
    void testExecuteWithAnyArgs() {
        // Act
        CommandResult result1 = clearCommand.execute("");
        CommandResult result2 = clearCommand.execute("some args");
        CommandResult result3 = clearCommand.execute(null);
        
        // Assert
        assertEquals(CommandResult.ResultType.SUCCESS, result1.getType());
        assertEquals(CommandResult.ResultType.SUCCESS, result2.getType());
        assertEquals(CommandResult.ResultType.SUCCESS, result3.getType());
    }
    
    @Test
    @DisplayName("命令描述应该正确")
    void testGetDescription() {
        // Act
        String description = clearCommand.getDescription();
        
        // Assert
        assertEquals("清空屏幕", description);
    }
    
    @Test
    @DisplayName("命令用法应该正确")
    void testGetUsage() {
        // Act
        String usage = clearCommand.getUsage();
        
        // Assert
        assertEquals("/clear", usage);
    }
}
