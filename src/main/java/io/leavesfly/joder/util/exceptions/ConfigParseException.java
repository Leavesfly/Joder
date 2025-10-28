package io.leavesfly.joder.util.exceptions;

/**
 * 配置文件解析异常
 * 对应 Kode 的 ConfigParseError
 */
public class ConfigParseException extends Exception {
    
    private final String filePath;
    private final Object defaultConfig;
    
    public ConfigParseException(String message, String filePath, Object defaultConfig) {
        super(message);
        this.filePath = filePath;
        this.defaultConfig = defaultConfig;
    }
    
    public ConfigParseException(String message, String filePath, Object defaultConfig, Throwable cause) {
        super(message, cause);
        this.filePath = filePath;
        this.defaultConfig = defaultConfig;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public Object getDefaultConfig() {
        return defaultConfig;
    }
    
    @Override
    public String toString() {
        return String.format("ConfigParseException: %s (file: %s)", getMessage(), filePath);
    }
}
