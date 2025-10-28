package io.shareai.joder.services.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.shareai.joder.domain.OAuthToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OAuth 2.0 服务
 * 
 * <p>实现 OAuth 2.0 授权码流程，支持 PKCE 扩展以增强安全性。
 * 
 * <p>授权流程：
 * <ol>
 *   <li>启动本地 HTTP 服务器监听回调</li>
 *   <li>生成 PKCE code_verifier 和 code_challenge</li>
 *   <li>打开浏览器跳转到授权页面</li>
 *   <li>用户授权后，浏览器重定向到本地服务器</li>
 *   <li>使用授权码和 code_verifier 交换访问令牌</li>
 *   <li>存储令牌和账户信息</li>
 * </ol>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
@Singleton
public class OAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuthService.class);
    private static final int SERVER_TIMEOUT_SECONDS = 300; // 5 分钟超时
    
    private final OAuthConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private HttpServer callbackServer;
    private String codeVerifier;
    private String expectedState;
    private CompletableFuture<AuthorizationCallback> callbackFuture;
    
    /**
     * 授权回调结果
     */
    private static class AuthorizationCallback {
        final String code;
        final String state;
        final boolean useManualRedirect;
        
        AuthorizationCallback(String code, String state, boolean useManualRedirect) {
            this.code = code;
            this.state = state;
            this.useManualRedirect = useManualRedirect;
        }
    }
    
    @Inject
    public OAuthService(OAuthConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 启动 OAuth 授权流程
     * 
     * @param urlHandler 处理授权 URL 的回调函数（用于在 UI 中显示）
     * @return OAuth 令牌
     * @throws Exception 如果授权失败
     */
    public OAuthToken startOAuthFlow(AuthUrlHandler urlHandler) throws Exception {
        if (!config.isConfigured()) {
            throw new IllegalStateException("OAuth not configured. Please set OAuth configuration in application.conf or environment variables.");
        }
        
        // 生成 PKCE 参数
        this.codeVerifier = PKCEUtil.generateCodeVerifier();
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
        this.expectedState = PKCEUtil.generateState();
        
        // 构建授权 URL
        String autoUrl = buildAuthUrl(codeChallenge, expectedState, false);
        String manualUrl = buildAuthUrl(codeChallenge, expectedState, true);
        
        // 启动本地服务器
        this.callbackFuture = new CompletableFuture<>();
        startCallbackServer();
        
        // 调用 URL 处理器（显示给用户并打开浏览器）
        urlHandler.handle(autoUrl, manualUrl);
        
        // 等待回调（带超时）
        AuthorizationCallback callback = callbackFuture.get(SERVER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // 验证 state
        if (!expectedState.equals(callback.state)) {
            throw new SecurityException("Invalid state parameter - possible CSRF attack");
        }
        
        // 交换授权码获取令牌
        OAuthToken token = exchangeCodeForToken(callback.code, callback.useManualRedirect);
        
        // 关闭服务器
        stopCallbackServer();
        
        logger.info("OAuth flow completed successfully");
        return token;
    }
    
    /**
     * 构建授权 URL
     */
    private String buildAuthUrl(String codeChallenge, String state, boolean useManualRedirect) {
        StringBuilder url = new StringBuilder(config.getAuthorizeUrl());
        url.append("?client_id=").append(urlEncode(config.getClientId()));
        url.append("&response_type=code");
        url.append("&redirect_uri=").append(urlEncode(
                useManualRedirect ? OAuthConfig.MANUAL_REDIRECT_URL : config.getRedirectUri()));
        url.append("&scope=").append(urlEncode(String.join(" ", config.getScopes())));
        url.append("&code_challenge=").append(urlEncode(codeChallenge));
        url.append("&code_challenge_method=S256");
        url.append("&state=").append(urlEncode(state));
        
        return url.toString();
    }
    
    /**
     * 启动本地 HTTP 服务器接收回调
     */
    private void startCallbackServer() throws IOException {
        callbackServer = HttpServer.create(new InetSocketAddress(config.getRedirectPort()), 0);
        
        callbackServer.createContext("/callback", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    // 解析查询参数
                    Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                    
                    String code = params.get("code");
                    String state = params.get("state");
                    
                    if (code == null || code.isEmpty()) {
                        sendError(exchange, 400, "Authorization code not found");
                        callbackFuture.completeExceptionally(
                                new IllegalStateException("No authorization code received"));
                        return;
                    }
                    
                    if (state == null || !state.equals(expectedState)) {
                        sendError(exchange, 400, "Invalid state parameter");
                        callbackFuture.completeExceptionally(
                                new SecurityException("Invalid state parameter"));
                        return;
                    }
                    
                    // 重定向到成功页面
                    sendRedirect(exchange, OAuthConfig.SUCCESS_URL);
                    
                    // 完成回调
                    callbackFuture.complete(new AuthorizationCallback(code, state, false));
                    
                } catch (Exception e) {
                    logger.error("Error handling OAuth callback", e);
                    sendError(exchange, 500, "Internal server error");
                    callbackFuture.completeExceptionally(e);
                }
            }
        });
        
        callbackServer.setExecutor(null); // 使用默认线程池
        callbackServer.start();
        
        logger.info("OAuth callback server started on port {}", config.getRedirectPort());
    }
    
    /**
     * 停止回调服务器
     */
    private void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
            logger.info("OAuth callback server stopped");
        }
    }
    
    /**
     * 交换授权码获取访问令牌
     */
    private OAuthToken exchangeCodeForToken(String authorizationCode, boolean useManualRedirect) throws IOException {
        // 构建请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "authorization_code");
        requestBody.put("code", authorizationCode);
        requestBody.put("redirect_uri", 
                useManualRedirect ? config.MANUAL_REDIRECT_URL : config.getRedirectUri());
        requestBody.put("client_id", config.getClientId());
        requestBody.put("code_verifier", codeVerifier);
        requestBody.put("state", expectedState);
        
        // 发送请求
        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
                .url(config.getTokenUrl())
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Token exchange failed: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // 解析令牌
            OAuthToken token = new OAuthToken();
            token.setAccessToken(jsonNode.get("access_token").asText());
            token.setTokenType(jsonNode.has("token_type") ? jsonNode.get("token_type").asText() : "Bearer");
            
            if (jsonNode.has("expires_in")) {
                long expiresIn = jsonNode.get("expires_in").asLong();
                token.setExpiresAt(System.currentTimeMillis() / 1000 + expiresIn);
            }
            
            if (jsonNode.has("refresh_token")) {
                token.setRefreshToken(jsonNode.get("refresh_token").asText());
            }
            
            if (jsonNode.has("scope")) {
                token.setScope(jsonNode.get("scope").asText());
            }
            
            // 解析账户信息
            if (jsonNode.has("account")) {
                JsonNode account = jsonNode.get("account");
                OAuthToken.AccountInfo accountInfo = new OAuthToken.AccountInfo();
                accountInfo.setAccountUuid(account.get("uuid").asText());
                accountInfo.setEmailAddress(account.get("email_address").asText());
                
                if (jsonNode.has("organization")) {
                    JsonNode org = jsonNode.get("organization");
                    accountInfo.setOrganizationUuid(org.get("uuid").asText());
                    accountInfo.setOrganizationName(org.get("name").asText());
                }
                
                token.setAccountInfo(accountInfo);
            }
            
            return token;
        }
    }
    
    /**
     * 处理手动授权回调
     * 
     * <p>当自动回调失败时，用户可以手动复制授权码并调用此方法。
     */
    public void processManualCallback(String authorizationCode, String state) {
        if (callbackFuture != null && !callbackFuture.isDone()) {
            callbackFuture.complete(new AuthorizationCallback(authorizationCode, state, true));
        }
    }
    
    // 辅助方法
    
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }
    
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(
                        URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                );
            }
        }
        
        return params;
    }
    
    private void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
    
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
        exchange.close();
    }
    
    /**
     * 授权 URL 处理器接口
     */
    @FunctionalInterface
    public interface AuthUrlHandler {
        /**
         * 处理授权 URL
         * 
         * @param autoUrl 自动重定向 URL（用于打开浏览器）
         * @param manualUrl 手动重定向 URL（用于显示给用户）
         * @throws Exception 如果处理失败
         */
        void handle(String autoUrl, String manualUrl) throws Exception;
    }
}
