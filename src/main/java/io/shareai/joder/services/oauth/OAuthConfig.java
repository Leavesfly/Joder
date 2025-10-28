package io.shareai.joder.services.oauth;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * OAuth 配置
 * 
 * <p>管理 OAuth 2.0 PKCE 流程的配置参数。
 * 配置可以从 application.conf 或环境变量中读取。
 * 
 * <p>配置项：
 * <ul>
 *   <li>joder.oauth.client-id - OAuth 客户端 ID</li>
 *   <li>joder.oauth.authorize-url - 授权端点 URL</li>
 *   <li>joder.oauth.token-url - 令牌交换端点 URL</li>
 *   <li>joder.oauth.api-key-url - API Key 创建端点 URL</li>
 *   <li>joder.oauth.redirect-port - 本地回调端口</li>
 *   <li>joder.oauth.scopes - 授权范围列表</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class OAuthConfig {
    
    /** 默认回调端口 */
    public static final int DEFAULT_REDIRECT_PORT = 54545;
    
    /** 默认授权范围 */
    public static final String[] DEFAULT_SCOPES = {"org:create_api_key", "user:profile"};
    
    /** 手动重定向 URL */
    public static final String MANUAL_REDIRECT_URL = "/oauth/code/callback";
    
    /** 成功页面 URL */
    public static final String SUCCESS_URL = "https://console.anthropic.com/settings/keys";
    
    private final String clientId;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String apiKeyUrl;
    private final int redirectPort;
    private final String[] scopes;
    
    /**
     * 从配置文件加载
     */
    public OAuthConfig() {
        Config config = ConfigFactory.load();
        
        // 从配置或环境变量读取
        this.clientId = getConfigString(config, "joder.oauth.client-id", "OAUTH_CLIENT_ID", "");
        this.authorizeUrl = getConfigString(config, "joder.oauth.authorize-url", "OAUTH_AUTHORIZE_URL", "");
        this.tokenUrl = getConfigString(config, "joder.oauth.token-url", "OAUTH_TOKEN_URL", "");
        this.apiKeyUrl = getConfigString(config, "joder.oauth.api-key-url", "OAUTH_API_KEY_URL", "");
        this.redirectPort = config.hasPath("joder.oauth.redirect-port") 
                ? config.getInt("joder.oauth.redirect-port") 
                : DEFAULT_REDIRECT_PORT;
        
        // 作用域
        if (config.hasPath("joder.oauth.scopes")) {
            this.scopes = config.getStringList("joder.oauth.scopes").toArray(new String[0]);
        } else {
            this.scopes = DEFAULT_SCOPES;
        }
    }
    
    /**
     * 自定义构造函数（用于测试）
     */
    public OAuthConfig(String clientId, String authorizeUrl, String tokenUrl, 
                      String apiKeyUrl, int redirectPort, String[] scopes) {
        this.clientId = clientId;
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.apiKeyUrl = apiKeyUrl;
        this.redirectPort = redirectPort;
        this.scopes = scopes;
    }
    
    /**
     * 从配置或环境变量获取字符串
     */
    private String getConfigString(Config config, String configPath, String envVar, String defaultValue) {
        // 优先使用环境变量
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        
        // 然后使用配置文件
        if (config.hasPath(configPath)) {
            return config.getString(configPath);
        }
        
        // 最后使用默认值
        return defaultValue;
    }
    
    /**
     * 检查 OAuth 是否已配置
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && authorizeUrl != null && !authorizeUrl.isEmpty()
                && tokenUrl != null && !tokenUrl.isEmpty();
    }
    
    // Getters
    
    public String getClientId() {
        return clientId;
    }
    
    public String getAuthorizeUrl() {
        return authorizeUrl;
    }
    
    public String getTokenUrl() {
        return tokenUrl;
    }
    
    public String getApiKeyUrl() {
        return apiKeyUrl;
    }
    
    public int getRedirectPort() {
        return redirectPort;
    }
    
    public String[] getScopes() {
        return scopes;
    }
    
    public String getRedirectUri() {
        return "http://localhost:" + redirectPort + "/callback";
    }
    
    @Override
    public String toString() {
        return "OAuthConfig{" +
                "clientId='" + (clientId != null && !clientId.isEmpty() ? "***" : "not set") + '\'' +
                ", redirectPort=" + redirectPort +
                ", configured=" + isConfigured() +
                '}';
    }
}
