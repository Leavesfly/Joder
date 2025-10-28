package io.shareai.joder.services.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE (Proof Key for Code Exchange) 工具类
 * 
 * <p>实现 RFC 7636 规范，用于 OAuth 2.0 的安全增强。
 * PKCE 通过在授权请求中添加 code_challenge，在令牌交换时验证 code_verifier，
 * 防止授权码被拦截和滥用。
 * 
 * @author Joder Team
 * @since 1.0.0
 * @see <a href="https://tools.ietf.org/html/rfc7636">RFC 7636</a>
 */
public class PKCEUtil {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_VERIFIER_LENGTH = 128; // 43-128 字符
    
    /**
     * 生成 code_verifier
     * 
     * <p>code_verifier 是一个高熵的加密随机字符串，
     * 长度在 43 到 128 个字符之间，使用字符集 [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
     * 
     * @return Base64URL 编码的随机字符串
     */
    public static String generateCodeVerifier() {
        byte[] randomBytes = new byte[32]; // 32 bytes = 256 bits
        SECURE_RANDOM.nextBytes(randomBytes);
        return base64UrlEncode(randomBytes);
    }
    
    /**
     * 从 code_verifier 生成 code_challenge
     * 
     * <p>使用 SHA-256 哈希 code_verifier，然后进行 Base64URL 编码。
     * 这是推荐的 S256 方法，比 plain 方法更安全。
     * 
     * @param codeVerifier code verifier 字符串
     * @return Base64URL 编码的 code_challenge
     * @throws RuntimeException 如果 SHA-256 算法不可用
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return base64UrlEncode(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Base64URL 编码（RFC 4648 Section 5）
     * 
     * <p>与标准 Base64 的区别：
     * <ul>
     *   <li>使用 - 替代 +</li>
     *   <li>使用 _ 替代 /</li>
     *   <li>移除末尾的 = 填充</li>
     * </ul>
     * 
     * @param data 要编码的字节数组
     * @return Base64URL 编码的字符串
     */
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data);
    }
    
    /**
     * 生成随机状态字符串
     * 
     * <p>state 参数用于防止 CSRF 攻击，应该在每次授权请求时生成新的随机值。
     * 
     * @return 随机状态字符串
     */
    public static String generateState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return base64UrlEncode(randomBytes);
    }
}
