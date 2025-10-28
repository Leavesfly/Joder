package io.leavesfly.joder.util;

import java.util.regex.Pattern;

/**
 * 验证工具类
 * 对应 Kode 的 validate.ts
 */
public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );
    
    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^\\d{5}(-\\d{4})?$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+1\\s?)?(\\d{3}[-.\\s]??)?\\d{3}[-.\\s]??\\d{4}$");
    private static final Pattern CITY_PATTERN = Pattern.compile("^[a-zA-Z\\s.-]+$");
    
    /**
     * 验证错误类
     */
    public static class ValidationError {
        private final String message;
        
        public ValidationError(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return message;
        }
    }
    
    /**
     * 验证邮箱地址
     * 
     * @param email 邮箱地址
     * @return 验证错误，null 表示验证通过
     */
    public static ValidationError validateEmail(String email) {
        String trimmed = email == null ? "" : email.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("邮箱地址不能为空");
        }
        
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return new ValidationError("请输入有效的邮箱地址");
        }
        
        return null;
    }
    
    /**
     * 验证名称
     */
    public static ValidationError validateName(String name) {
        String trimmed = name == null ? "" : name.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("名称不能为空");
        }
        
        if (trimmed.length() < 2) {
            return new ValidationError("名称至少需要2个字符");
        }
        
        return null;
    }
    
    /**
     * 验证非空字符串
     */
    public static ValidationError validateRequired(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError(fieldName + "不能为空");
        }
        
        return null;
    }
    
    /**
     * 验证手机号码（中国）
     */
    public static ValidationError validatePhoneCN(String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("手机号码不能为空");
        }
        
        // 中国手机号：1开头，11位数字
        if (!trimmed.matches("^1[3-9]\\d{9}$")) {
            return new ValidationError("请输入有效的手机号码");
        }
        
        return null;
    }
    
    /**
     * 验证手机号码（美国）
     */
    public static ValidationError validatePhoneUS(String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("Phone number is required");
        }
        
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            return new ValidationError("Please enter a valid US phone number");
        }
        
        return null;
    }
    
    /**
     * 验证 URL
     */
    public static ValidationError validateURL(String url) {
        String trimmed = url == null ? "" : url.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("URL 不能为空");
        }
        
        try {
            new java.net.URL(trimmed);
            return null;
        } catch (java.net.MalformedURLException e) {
            return new ValidationError("请输入有效的 URL");
        }
    }
    
    /**
     * 验证数字范围
     */
    public static ValidationError validateNumberRange(int value, int min, int max) {
        if (value < min || value > max) {
            return new ValidationError(
                String.format("数值必须在 %d 到 %d 之间", min, max)
            );
        }
        return null;
    }
    
    /**
     * 验证字符串长度
     */
    public static ValidationError validateLength(String value, int minLength, int maxLength) {
        int length = value == null ? 0 : value.length();
        
        if (length < minLength) {
            return new ValidationError(
                String.format("长度至少需要 %d 个字符", minLength)
            );
        }
        
        if (length > maxLength) {
            return new ValidationError(
                String.format("长度不能超过 %d 个字符", maxLength)
            );
        }
        
        return null;
    }
    
    /**
     * 验证正则表达式
     */
    public static ValidationError validatePattern(String value, Pattern pattern, String errorMessage) {
        String trimmed = value == null ? "" : value.trim();
        
        if (!pattern.matcher(trimmed).matches()) {
            return new ValidationError(errorMessage);
        }
        
        return null;
    }
    
    /**
     * 验证数字
     */
    public static ValidationError validateNumber(String value) {
        String trimmed = value == null ? "" : value.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("数值不能为空");
        }
        
        try {
            Double.parseDouble(trimmed);
            return null;
        } catch (NumberFormatException e) {
            return new ValidationError("请输入有效的数字");
        }
    }
    
    /**
     * 验证整数
     */
    public static ValidationError validateInteger(String value) {
        String trimmed = value == null ? "" : value.trim();
        
        if (trimmed.isEmpty()) {
            return new ValidationError("数值不能为空");
        }
        
        try {
            Integer.parseInt(trimmed);
            return null;
        } catch (NumberFormatException e) {
            return new ValidationError("请输入有效的整数");
        }
    }
}
