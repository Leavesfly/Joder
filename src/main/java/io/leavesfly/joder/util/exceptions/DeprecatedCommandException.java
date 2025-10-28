package io.leavesfly.joder.util.exceptions;

/**
 * 已废弃的命令异常
 * 对应 Kode 的 DeprecatedCommandError
 */
public class DeprecatedCommandException extends Exception {
    
    public DeprecatedCommandException(String message) {
        super(message);
    }
    
    public DeprecatedCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
