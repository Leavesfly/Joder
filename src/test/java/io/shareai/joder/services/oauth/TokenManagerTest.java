package io.shareai.joder.services.oauth;

import io.shareai.joder.domain.OAuthToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenManager 测试
 */
class TokenManagerTest {
    
    private TokenManager tokenManager;
    
    @BeforeEach
    void setUp() {
        tokenManager = new TokenManager();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试令牌
        tokenManager.deleteToken();
    }
    
    @Test
    void testSaveAndLoadToken() throws IOException {
        OAuthToken token = new OAuthToken("test-access-token", "Bearer", 3600);
        token.setRefreshToken("test-refresh-token");
        token.setScope("test-scope");
        
        // 保存
        tokenManager.saveToken(token);
        
        // 加载
        OAuthToken loaded = tokenManager.loadToken();
        
        assertNotNull(loaded);
        assertEquals(token.getAccessToken(), loaded.getAccessToken());
        assertEquals(token.getRefreshToken(), loaded.getRefreshToken());
        assertEquals(token.getTokenType(), loaded.getTokenType());
        assertEquals(token.getScope(), loaded.getScope());
    }
    
    @Test
    void testDeleteToken() throws IOException {
        OAuthToken token = new OAuthToken("test-token", "Bearer", 3600);
        tokenManager.saveToken(token);
        
        assertTrue(tokenManager.hasValidToken());
        
        boolean deleted = tokenManager.deleteToken();
        
        assertTrue(deleted);
        assertFalse(tokenManager.hasValidToken());
    }
    
    @Test
    void testHasValidToken() throws IOException {
        assertFalse(tokenManager.hasValidToken());
        
        OAuthToken token = new OAuthToken("test-token", "Bearer", 3600);
        tokenManager.saveToken(token);
        
        assertTrue(tokenManager.hasValidToken());
    }
    
    @Test
    void testExpiredToken() throws IOException {
        // 创建已过期的令牌（过期时间为 -1 秒）
        OAuthToken token = new OAuthToken("test-token", "Bearer", -1);
        tokenManager.saveToken(token);
        
        assertFalse(tokenManager.hasValidToken());
    }
    
    @Test
    void testGetAccessToken() throws IOException {
        OAuthToken token = new OAuthToken("my-access-token", "Bearer", 3600);
        tokenManager.saveToken(token);
        
        String accessToken = tokenManager.getAccessToken();
        
        assertEquals("my-access-token", accessToken);
    }
    
    @Test
    void testClearCache() throws IOException {
        OAuthToken token = new OAuthToken("test-token", "Bearer", 3600);
        tokenManager.saveToken(token);
        
        // 第一次加载（从缓存）
        OAuthToken loaded1 = tokenManager.loadToken();
        assertNotNull(loaded1);
        
        // 清除缓存
        tokenManager.clearCache();
        
        // 第二次加载（从文件）
        OAuthToken loaded2 = tokenManager.loadToken();
        assertNotNull(loaded2);
        assertEquals(loaded1.getAccessToken(), loaded2.getAccessToken());
    }
    
    @Test
    void testTokenFilePath() {
        String path = tokenManager.getTokenFilePath().toString();
        
        assertTrue(path.contains(".joder"));
        assertTrue(path.contains("oauth-token.json"));
    }
}
