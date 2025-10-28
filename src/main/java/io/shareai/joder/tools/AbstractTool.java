package io.shareai.joder.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 抽象工具基类
 * 提供通用功能的默认实现
 */
public abstract class AbstractTool implements Tool {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return !isReadOnly();
    }
    
    @Override
    public String getPrompt() {
        return getDescription();
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        return String.format("▶ 使用工具: %s\n  参数: %s", getName(), input);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return String.format("✓ 工具执行成功\n%s", result.getOutput());
        } else {
            return String.format("✗ 工具执行失败\n%s", result.getError());
        }
    }
    
    /**
     * 验证必需参数
     */
    protected void requireParameter(Map<String, Object> input, String key) {
        if (!input.containsKey(key) || input.get(key) == null) {
            throw new IllegalArgumentException("缺少必需参数: " + key);
        }
    }
    
    /**
     * 获取字符串参数
     */
    protected String getString(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取字符串参数（带默认值）
     */
    protected String getString(Map<String, Object> input, String key, String defaultValue) {
        String value = getString(input, key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取整数参数
     */
    protected Integer getInt(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
    
    /**
     * 获取整数参数（带默认值）
     */
    protected int getInt(Map<String, Object> input, String key, int defaultValue) {
        Integer value = getInt(input, key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取布尔参数
     */
    protected Boolean getBoolean(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * 获取布尔参数（带默认值）
     */
    protected boolean getBoolean(Map<String, Object> input, String key, boolean defaultValue) {
        Boolean value = getBoolean(input, key);
        return value != null ? value : defaultValue;
    }
}
