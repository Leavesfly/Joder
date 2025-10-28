package io.leavesfly.joder.util;

import io.leavesfly.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户工具类
 * 对应 Kode 的 user.ts
 */
@Singleton
public class UserUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(UserUtils.class);
    
    private final ConfigManager configManager;
    private String cachedGitEmail;
    private SimpleUser cachedUser;
    
    @Inject
    public UserUtils(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * 获取 Git 邮箱地址
     */
    public String getGitEmail() {
        if (cachedGitEmail != null) {
            return cachedGitEmail;
        }
        
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"git", "config", "user.email"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String email = reader.readLine();
            
            int exitCode = process.waitFor();
            if (exitCode == 0 && email != null && !email.trim().isEmpty()) {
                cachedGitEmail = email.trim();
                return cachedGitEmail;
            }
        } catch (Exception e) {
            logger.debug("获取 Git 邮箱失败", e);
        }
        
        return null;
    }
    
    /**
     * 获取或创建用户 ID
     */
    private String getOrCreateUserID() {
        String userId = configManager.getString("userId", null);
        if (userId == null || userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
            // TODO: 实现配置保存功能后再启用
            // configManager.setString("userId", userId);
            logger.info("创建新的用户 ID: {}", userId);
        }
        return userId;
    }
    
    /**
     * 获取用户信息
     */
    public SimpleUser getUser() {
        if (cachedUser != null) {
            return cachedUser;
        }
        
        String userId = getOrCreateUserID();
        String email = getGitEmail();
        
        SimpleUser user = new SimpleUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setAppVersion("1.0.0"); // TODO: 从配置获取
        user.setUserAgent(EnvUtils.getPlatform());
        
        // 自定义 ID
        Map<String, String> customIds = new HashMap<>();
        customIds.put("sessionId", UUID.randomUUID().toString());
        user.setCustomIds(customIds);
        
        // 自定义信息
        Map<String, Object> custom = new HashMap<>();
        custom.put("javaVersion", EnvUtils.getJavaVersion());
        custom.put("platform", EnvUtils.getPlatform());
        user.setCustom(custom);
        
        cachedUser = user;
        return user;
    }
    
    /**
     * 简单用户信息类
     */
    public static class SimpleUser {
        private String userId;
        private String email;
        private String appVersion;
        private String userAgent;
        private Map<String, String> customIds;
        private Map<String, Object> custom;
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getAppVersion() {
            return appVersion;
        }
        
        public void setAppVersion(String appVersion) {
            this.appVersion = appVersion;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
        
        public Map<String, String> getCustomIds() {
            return customIds;
        }
        
        public void setCustomIds(Map<String, String> customIds) {
            this.customIds = customIds;
        }
        
        public Map<String, Object> getCustom() {
            return custom;
        }
        
        public void setCustom(Map<String, Object> custom) {
            this.custom = custom;
        }
        
        @Override
        public String toString() {
            return String.format("SimpleUser{userId=%s, email=%s, version=%s}",
                userId, email, appVersion);
        }
    }
}
