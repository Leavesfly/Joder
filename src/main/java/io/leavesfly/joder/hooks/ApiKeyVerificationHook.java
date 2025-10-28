package io.leavesfly.joder.hooks;

import io.leavesfly.joder.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * API 密钥验证钩子
 * 对应 Kode 的 useApiKeyVerification hook
 */
@Singleton
public class ApiKeyVerificationHook {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyVerificationHook.class);
    
    private final ConfigManager configManager;
    private VerificationStatus status;
    private String error;
    
    public enum VerificationStatus {
        LOADING,
        VALID,
        INVALID,
        MISSING,
        ERROR
    }
    
    @Inject
    public ApiKeyVerificationHook(ConfigManager configManager) {
        this.configManager = configManager;
        this.status = VerificationStatus.LOADING;
        this.error = null;
    }
    
    /**
     * 验证 API 密钥
     */
    public VerificationStatus verify() {
        String apiKey = configManager.getString("openai.apiKey", null);
        
        if (apiKey == null || apiKey.isEmpty()) {
            status = VerificationStatus.MISSING;
            return status;
        }
        
        // TODO: 实际调用 API 验证
        // 目前简单判断格式
        if (apiKey.startsWith("sk-") || apiKey.startsWith("sk_")) {
            status = VerificationStatus.VALID;
        } else {
            status = VerificationStatus.INVALID;
        }
        
        return status;
    }
    
    /**
     * 重新验证
     */
    public void reverify() {
        status = VerificationStatus.LOADING;
        verify();
    }
    
    public VerificationStatus getStatus() {
        return status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
        this.status = VerificationStatus.ERROR;
    }
}
