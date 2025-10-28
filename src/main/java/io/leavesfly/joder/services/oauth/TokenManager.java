package io.leavesfly.joder.services.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.domain.OAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * OAuth Token 管理器
 * 
 * <p>负责 OAuth 令牌的安全存储、读取、刷新和删除。
 * 令牌存储在用户主目录的 .joder 目录中。
 * 
 * <p>存储位置：~/.joder/oauth-token.json
 * 
 * @author Joder Team
 * @since 1.0.0
 */
@Singleton
public class TokenManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);
    private static final String TOKEN_FILE_NAME = "oauth-token.json";
    private static final String CONFIG_DIR = ".joder";
    
    private final ObjectMapper objectMapper;
    private final Path tokenFilePath;
    
    // 内存缓存
    private OAuthToken cachedToken;
    
    @Inject
    public TokenManager() {
        this.objectMapper = new ObjectMapper();
        
        // 确定令牌文件路径
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, CONFIG_DIR);
        this.tokenFilePath = configDir.resolve(TOKEN_FILE_NAME);
        
        // 确保配置目录存在
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            logger.error("Failed to create config directory: {}", configDir, e);
        }
    }
    
    /**
     * 保存令牌
     * 
     * @param token 要保存的令牌
     * @throws IOException 如果保存失败
     */
    public void saveToken(OAuthToken token) throws IOException {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        
        // 写入临时文件
        Path tempFile = tokenFilePath.resolveSibling(TOKEN_FILE_NAME + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(tempFile.toFile(), token);
        
        // 原子性地移动到目标文件
        Files.move(tempFile, tokenFilePath, StandardCopyOption.REPLACE_EXISTING, 
                StandardCopyOption.ATOMIC_MOVE);
        
        // 更新缓存
        this.cachedToken = token;
        
        logger.info("OAuth token saved successfully");
    }
    
    /**
     * 加载令牌
     * 
     * @return 加载的令牌，如果不存在返回 null
     */
    public OAuthToken loadToken() {
        // 优先返回缓存
        if (cachedToken != null) {
            return cachedToken;
        }
        
        if (!Files.exists(tokenFilePath)) {
            logger.debug("OAuth token file not found: {}", tokenFilePath);
            return null;
        }
        
        try {
            cachedToken = objectMapper.readValue(tokenFilePath.toFile(), OAuthToken.class);
            logger.info("OAuth token loaded successfully");
            return cachedToken;
        } catch (IOException e) {
            logger.error("Failed to load OAuth token", e);
            return null;
        }
    }
    
    /**
     * 删除令牌
     * 
     * @return 如果删除成功返回 true
     */
    public boolean deleteToken() {
        cachedToken = null;
        
        try {
            if (Files.exists(tokenFilePath)) {
                Files.delete(tokenFilePath);
                logger.info("OAuth token deleted successfully");
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to delete OAuth token", e);
        }
        
        return false;
    }
    
    /**
     * 检查是否有有效令牌
     * 
     * @return 如果存在且未过期的令牌返回 true
     */
    public boolean hasValidToken() {
        OAuthToken token = loadToken();
        return token != null && !token.isExpired();
    }
    
    /**
     * 获取访问令牌字符串
     * 
     * @return 访问令牌，如果不存在或已过期返回 null
     */
    public String getAccessToken() {
        OAuthToken token = loadToken();
        if (token == null || token.isExpired()) {
            return null;
        }
        return token.getAccessToken();
    }
    
    /**
     * 检查令牌是否即将过期，需要刷新
     * 
     * @return 如果令牌即将过期返回 true
     */
    public boolean shouldRefreshToken() {
        OAuthToken token = loadToken();
        return token != null && token.isExpiringSoon();
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        this.cachedToken = null;
    }
    
    /**
     * 获取令牌文件路径（用于测试或调试）
     */
    public Path getTokenFilePath() {
        return tokenFilePath;
    }
}
