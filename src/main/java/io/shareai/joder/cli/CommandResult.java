package io.shareai.joder.cli;

/**
 * 命令执行结果
 */
public class CommandResult {
    
    private final ResultType type;
    private final String message;
    private final boolean shouldExit;
    
    private CommandResult(ResultType type, String message, boolean shouldExit) {
        this.type = type;
        this.message = message;
        this.shouldExit = shouldExit;
    }
    
    /**
     * 创建成功结果
     */
    public static CommandResult success(String message) {
        return new CommandResult(ResultType.SUCCESS, message, false);
    }
    
    /**
     * 创建错误结果
     */
    public static CommandResult error(String message) {
        return new CommandResult(ResultType.ERROR, message, false);
    }
    
    /**
     * 创建用户消息结果
     */
    public static CommandResult userMessage(String message) {
        return new CommandResult(ResultType.USER_MESSAGE, message, false);
    }
    
    /**
     * 创建空结果
     */
    public static CommandResult empty() {
        return new CommandResult(ResultType.EMPTY, "", false);
    }
    
    /**
     * 创建退出结果
     */
    public static CommandResult exit() {
        return new CommandResult(ResultType.SUCCESS, "再见！", true);
    }
    
    public ResultType getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean shouldExit() {
        return shouldExit;
    }
    
    /**
     * 结果类型枚举
     */
    public enum ResultType {
        SUCCESS,
        ERROR,
        USER_MESSAGE,
        EMPTY
    }
}
