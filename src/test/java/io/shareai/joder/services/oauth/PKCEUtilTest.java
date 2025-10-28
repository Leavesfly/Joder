package io.shareai.joder.services.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PKCE 工具类测试
 */
class PKCEUtilTest {
    
    @Test
    void testGenerateCodeVerifier() {
        String verifier = PKCEUtil.generateCodeVerifier();
        
        assertNotNull(verifier);
        assertTrue(verifier.length() >= 43); // PKCE 规范要求至少 43 个字符
        assertTrue(verifier.matches("[A-Za-z0-9_-]+")); // 只包含允许的字符
    }
    
    @Test
    void testGenerateCodeChallenge() {
        String verifier = "test-code-verifier";
        String challenge = PKCEUtil.generateCodeChallenge(verifier);
        
        assertNotNull(challenge);
        assertFalse(challenge.isEmpty());
        assertTrue(challenge.matches("[A-Za-z0-9_-]+")); // Base64URL 字符
    }
    
    @Test
    void testCodeChallengeConsistency() {
        String verifier = "test-code-verifier";
        String challenge1 = PKCEUtil.generateCodeChallenge(verifier);
        String challenge2 = PKCEUtil.generateCodeChallenge(verifier);
        
        // 相同的 verifier 应该生成相同的 challenge
        assertEquals(challenge1, challenge2);
    }
    
    @Test
    void testGenerateState() {
        String state = PKCEUtil.generateState();
        
        assertNotNull(state);
        assertFalse(state.isEmpty());
        assertTrue(state.matches("[A-Za-z0-9_-]+"));
    }
    
    @Test
    void testStateRandomness() {
        String state1 = PKCEUtil.generateState();
        String state2 = PKCEUtil.generateState();
        
        // 两次生成的 state 应该不同
        assertNotEquals(state1, state2);
    }
}
