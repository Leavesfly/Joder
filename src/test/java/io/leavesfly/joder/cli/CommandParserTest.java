package io.leavesfly.joder.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandParser 单元测试
 */
@DisplayName("CommandParser 命令解析器测试")
public class CommandParserTest {
    
    private CommandParser parser;
    private TestCommand testCommand;
    
    /**
     * 测试命令实现
     */
    private static class TestCommand implements Command {
        private String lastArgs;
        private boolean executed = false;
        
        @Override
        public CommandResult execute(String args) {
            this.lastArgs = args;
            this.executed = true;
            return CommandResult.success("Command executed with args: " + args);
        }
        
        @Override
        public String getDescription() {
            return "Test command description";
        }
        
        @Override
        public String getUsage() {
            return "/test [args]";
        }
        
        public String getLastArgs() {
            return lastArgs;
        }
        
        public boolean isExecuted() {
            return executed;
        }
        
        public void reset() {
            lastArgs = null;
            executed = false;
        }
    }
    
    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        testCommand = new TestCommand();
        parser.registerCommand("test", testCommand);
    }
    
    @Test
    @DisplayName("应该能注册命令")
    void testRegisterCommand() {
        // Arrange
        Command newCommand = new TestCommand();
        
        // Act
        parser.registerCommand("new", newCommand);
        
        // Assert
        Map<String, Command> commands = parser.getCommands();
        assertTrue(commands.containsKey("new"));
        assertEquals(newCommand, commands.get("new"));
    }
    
    @Test
    @DisplayName("应该正确解析命令")
    void testParseCommand() {
        // Act
        CommandResult result = parser.parse("/test");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.SUCCESS, result.getType());
        assertTrue(testCommand.isExecuted());
    }
    
    @Test
    @DisplayName("应该正确解析带参数的命令")
    void testParseCommandWithArgs() {
        // Act
        CommandResult result = parser.parse("/test arg1 arg2");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.SUCCESS, result.getType());
        assertEquals("arg1 arg2", testCommand.getLastArgs());
    }
    
    @Test
    @DisplayName("空输入应该返回空结果")
    void testParseEmptyInput() {
        // Act
        CommandResult result = parser.parse("");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.EMPTY, result.getType());
        assertFalse(testCommand.isExecuted());
    }
    
    @Test
    @DisplayName("null 输入应该返回空结果")
    void testParseNullInput() {
        // Act
        CommandResult result = parser.parse(null);
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.EMPTY, result.getType());
        assertFalse(testCommand.isExecuted());
    }
    
    @Test
    @DisplayName("只有空格的输入应该返回空结果")
    void testParseWhitespaceOnlyInput() {
        // Act
        CommandResult result = parser.parse("   ");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.EMPTY, result.getType());
    }
    
    @Test
    @DisplayName("非命令输入应该返回用户消息")
    void testParseNonCommandInput() {
        // Act
        CommandResult result = parser.parse("Hello, this is not a command");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.USER_MESSAGE, result.getType());
        assertEquals("Hello, this is not a command", result.getMessage());
    }
    
    @Test
    @DisplayName("未知命令应该返回错误")
    void testParseUnknownCommand() {
        // Act
        CommandResult result = parser.parse("/unknown");
        
        // Assert
        assertNotNull(result);
        assertEquals(CommandResult.ResultType.ERROR, result.getType());
        assertTrue(result.getMessage().contains("未知命令"));
        assertTrue(result.getMessage().contains("unknown"));
    }
    
    @Test
    @DisplayName("命令名应该不区分大小写")
    void testCommandNameCaseInsensitive() {
        // Act
        CommandResult result1 = parser.parse("/TEST");
        testCommand.reset();
        CommandResult result2 = parser.parse("/TeSt");
        testCommand.reset();
        CommandResult result3 = parser.parse("/test");
        
        // Assert
        assertEquals(CommandResult.ResultType.SUCCESS, result1.getType());
        assertEquals(CommandResult.ResultType.SUCCESS, result2.getType());
        assertEquals(CommandResult.ResultType.SUCCESS, result3.getType());
    }
    
    @Test
    @DisplayName("命令前后的空格应该被去除")
    void testCommandTrimming() {
        // Act
        CommandResult result = parser.parse("  /test arg1  ");
        
        // Assert
        assertEquals(CommandResult.ResultType.SUCCESS, result.getType());
        assertEquals("arg1", testCommand.getLastArgs());
    }
    
    @Test
    @DisplayName("命令执行异常应该返回错误")
    void testCommandExecutionException() {
        // Arrange
        Command failingCommand = new Command() {
            @Override
            public CommandResult execute(String args) {
                throw new RuntimeException("Test exception");
            }
            
            @Override
            public String getDescription() {
                return "Failing command";
            }
            
            @Override
            public String getUsage() {
                return "/fail";
            }
        };
        parser.registerCommand("fail", failingCommand);
        
        // Act
        CommandResult result = parser.parse("/fail");
        
        // Assert
        assertEquals(CommandResult.ResultType.ERROR, result.getType());
        assertTrue(result.getMessage().contains("命令执行失败"));
    }
    
    @Test
    @DisplayName("应该能获取所有注册的命令")
    void testGetAllCommands() {
        // Arrange
        parser.registerCommand("cmd1", new TestCommand());
        parser.registerCommand("cmd2", new TestCommand());
        
        // Act
        Map<String, Command> commands = parser.getCommands();
        
        // Assert
        assertEquals(3, commands.size()); // test, cmd1, cmd2
        assertTrue(commands.containsKey("test"));
        assertTrue(commands.containsKey("cmd1"));
        assertTrue(commands.containsKey("cmd2"));
    }
    
    @Test
    @DisplayName("获取命令应该返回副本")
    void testGetCommandsReturnsCopy() {
        // Act
        Map<String, Command> commands1 = parser.getCommands();
        Map<String, Command> commands2 = parser.getCommands();
        
        // Assert
        assertNotSame(commands1, commands2);
        assertEquals(commands1.size(), commands2.size());
    }
    
    @Test
    @DisplayName("命令可以被覆盖注册")
    void testCommandOverride() {
        // Arrange
        TestCommand newCommand = new TestCommand();
        
        // Act
        parser.registerCommand("test", newCommand);
        CommandResult result = parser.parse("/test");
        
        // Assert
        assertTrue(newCommand.isExecuted());
        assertFalse(testCommand.isExecuted());
    }
    
    @Test
    @DisplayName("不带参数的命令应该收到空字符串")
    void testCommandWithoutArgsReceivesEmptyString() {
        // Act
        parser.parse("/test");
        
        // Assert
        assertEquals("", testCommand.getLastArgs());
    }
    
    @Test
    @DisplayName("应该正确处理多个连续空格作为分隔符")
    void testMultipleSpacesAsDelimiter() {
        // Act
        CommandResult result = parser.parse("/test    args    with    spaces");
        
        // Assert
        assertEquals(CommandResult.ResultType.SUCCESS, result.getType());
        assertEquals("args    with    spaces", testCommand.getLastArgs());
    }
    
    @Test
    @DisplayName("只有命令前缀斜杠应该被识别为命令")
    void testOnlySlashPrefixRecognized() {
        // Act
        CommandResult result1 = parser.parse("test without slash");
        CommandResult result2 = parser.parse("/test with slash");
        
        // Assert
        assertEquals(CommandResult.ResultType.USER_MESSAGE, result1.getType());
        assertEquals(CommandResult.ResultType.SUCCESS, result2.getType());
    }
}
