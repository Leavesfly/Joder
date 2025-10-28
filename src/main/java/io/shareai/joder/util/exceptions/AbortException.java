package io.shareai.joder.util.exceptions;

/**
 * 操作中止异常
 * 对应 Kode 的 AbortError
 */
public class AbortException extends Exception {
    
    public AbortException(String message) {
        super(message);
    }
    
    public AbortException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AbortException(Throwable cause) {
        super(cause);
    }
}
