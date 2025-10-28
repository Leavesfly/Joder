package io.leavesfly.joder.util;

/**
 * HTTP 工具类
 * 对应 Kode 的 http.ts
 */
public class HttpUtils {
    
    private static final String VERSION = "1.0.0"; // TODO: 从配置或常量获取
    private static final String PRODUCT_COMMAND = "joder";
    
    /**
     * 获取 User-Agent 字符串
     * 
     * @return User-Agent 字符串
     */
    public static String getUserAgent() {
        String userType = System.getenv("USER_TYPE");
        if (userType == null) {
            userType = "standard";
        }
        
        return String.format("%s/%s (%s)", PRODUCT_COMMAND, VERSION, userType);
    }
    
    /**
     * 获取平台信息
     */
    public static String getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "macos";
        } else {
            return "linux";
        }
    }
}
