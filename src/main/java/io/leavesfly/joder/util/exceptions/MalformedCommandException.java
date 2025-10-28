package io.leavesfly.joder.util.exceptions;

/**
 * 格式错误的命令异常
 * 对应 Kode 的 MalformedCommandError
 */
public class MalformedCommandException extends IllegalArgumentException {
    
    public MalformedCommandException(String message) {
        super(message);
    }
    
    public MalformedCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
