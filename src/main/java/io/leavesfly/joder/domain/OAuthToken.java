package io.leavesfly.joder.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * OAuth Token 数据模型
 * 
 * <p>存储 OAuth 2.0 认证获取的访问令牌和相关信息。
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class OAuthToken {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌（可选）
     */
    private String refreshToken;
    
    /**
     * 令牌类型（通常是 "Bearer"）
     */
    private String tokenType;
    
    /**
     * 过期时间（Unix 时间戳）
     */
    private long expiresAt;
    
    /**
     * 授权范围
     */
    private String scope;
    
    /**
     * 账户信息
     */
    private AccountInfo accountInfo;
    
    /**
     * 账户信息内部类
     */
    public static class AccountInfo {
        private String accountUuid;
        private String emailAddress;
        private String organizationUuid;
        private String organizationName;
        
        public AccountInfo() {}
        
        public AccountInfo(String accountUuid, String emailAddress) {
            this.accountUuid = accountUuid;
            this.emailAddress = emailAddress;
        }
        
        // Getters and Setters
        
        public String getAccountUuid() {
            return accountUuid;
        }
        
        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
        
        public String getEmailAddress() {
            return emailAddress;
        }
        
        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }
        
        public String getOrganizationUuid() {
            return organizationUuid;
        }
        
        public void setOrganizationUuid(String organizationUuid) {
            this.organizationUuid = organizationUuid;
        }
        
        public String getOrganizationName() {
            return organizationName;
        }
        
        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }
    }
    
    // 构造函数
    
    public OAuthToken() {}
    
    public OAuthToken(String accessToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresAt = Instant.now().getEpochSecond() + expiresIn;
    }
    
    // Getters and Setters
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public AccountInfo getAccountInfo() {
        return accountInfo;
    }
    
    public void setAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }
    
    // 辅助方法
    
    /**
     * 检查令牌是否已过期
     * 
     * @return 如果令牌已过期返回 true
     */
    @JsonIgnore
    public boolean isExpired() {
        return Instant.now().getEpochSecond() >= expiresAt;
    }
    
    /**
     * 检查令牌是否即将过期（5分钟内）
     * 
     * @return 如果令牌即将过期返回 true
     */
    @JsonIgnore
    public boolean isExpiringSoon() {
        long fiveMinutes = 5 * 60;
        return Instant.now().getEpochSecond() >= (expiresAt - fiveMinutes);
    }
    
    /**
     * 获取剩余有效时间（秒）
     * 
     * @return 剩余秒数，如果已过期返回 0
     */
    @JsonIgnore
    public long getRemainingSeconds() {
        long remaining = expiresAt - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
    
    @Override
    public String toString() {
        return "OAuthToken{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresAt=" + expiresAt +
                ", scope='" + scope + '\'' +
                ", expired=" + isExpired() +
                '}';
    }
}
