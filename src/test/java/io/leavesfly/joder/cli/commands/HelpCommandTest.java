package io.leavesfly.joder.cli.commands;

import io.leavesfly.joder.cli.Command;
import io.leavesfly.joder.cli.CommandParser;
import io.leavesfly.joder.cli.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HelpCommand 单元测试
 */
@DisplayName("HelpCommand 帮助命令测试")
public class HelpCommandTest {
    
    private HelpCommand helpCommand;
    private CommandParser commandParser;
    
    @BeforeEach
    void setUp() {
        commandParser = new CommandParser();
        helpCommand = new HelpCommand(commandParser);
        
        // 注册一些测试命令
        commandParser.registerCommand("help", helpCommand);
        commandParser.registerCommand("clear", new ClearCommand());
        commandParser.registerCommand("exit", new ExitCommand());
    }
    
    @Test
    @DisplayName("应该显示所有注册的命令")
    void testDisplayAllCommands() {
        // Act
        CommandResult result = helpCommand.execute("");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.SUCCESS, result.getType());
        assertTrue(result.getMessage().contains("help"));
        assertTrue(result.getMessage().contains("clear"));
        assertTrue(result.getMessage().contains("exit"));
    }
    
    @Test
    @DisplayName("应该包含命令描述")
    void testIncludeCommandDescriptions() {
        // Act
        CommandResult result = helpCommand.execute("");
        
        // Assert
        assertTrue(result.getMessage().contains("显示帮助信息"));
        assertTrue(result.getMessage().contains("清空屏幕"));
    }
    
    @Test
    @DisplayName("应该包含命令用法")
    void testIncludeCommandUsage() {
        // Act
        CommandResult result = helpCommand.execute("");
        
        // Assert
        assertTrue(result.getMessage().contains("/help"));
        assertTrue(result.getMessage().contains("/clear"));
        assertTrue(result.getMessage().contains("/exit"));
    }
    
    @Test
    @DisplayName("应该包含使用提示")
    void testIncludeUsageTips() {
        // Act
        CommandResult result = helpCommand.execute("");
        
        // Assert
        assertTrue(result.getMessage().contains("提示"));
        assertTrue(result.getMessage().contains("直接输入消息"));
    }
    
    @Test
    @DisplayName("命令描述应该正确")
    void testGetDescription() {
        // Act
        String description = helpCommand.getDescription();
        
        // Assert
        assertEquals("显示帮助信息", description);
    }
    
    @Test
    @DisplayName("命令用法应该正确")
    void testGetUsage() {
        // Act
        String usage = helpCommand.getUsage();
        
        // Assert
        assertEquals("/help", usage);
    }
}
