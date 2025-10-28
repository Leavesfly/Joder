package io.leavesfly.joder.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模糊匹配器测试
 */
public class FuzzyMatcherTest {

    @Test
    public void testExactMatch() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("node", "node");
        
        assertEquals(1000.0, result.getScore(), 1.0);
        assertEquals("exact", result.getAlgorithm());
        assertEquals(1.0, result.getConfidence(), 0.01);
    }

    @Test
    public void testPrefixMatch() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("node", "nod");
        
        assertTrue(result.getScore() > 900.0);
        assertEquals("prefix-exact", result.getAlgorithm());
    }

    @Test
    public void testAbbreviationMatch() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("node", "nde");
        
        // 缩写匹配应该有合理的分数
        assertTrue(result.getScore() > 10.0);
    }

    @Test
    public void testSubstringMatch() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("python", "tho");
        
        assertTrue(result.getScore() > 0);
    }

    @Test
    public void testPopularCommands() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult nodeResult = matcher.match("node", "n");
        FuzzyMatcher.MatchResult otherResult = matcher.match("noop", "n");
        
        // 流行命令应该有更高的分数
        assertTrue(nodeResult.getScore() >= otherResult.getScore());
    }

    @Test
    public void testNumericSuffix() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("python3", "py3");
        
        assertTrue(result.getScore() > 20.0);
    }

    @Test
    public void testBatchMatch() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        List<String> candidates = Arrays.asList("node", "npm", "python", "git", "noop");
        
        List<FuzzyMatcher.CandidateMatch> results = matcher.matchMany(candidates, "n");
        
        assertFalse(results.isEmpty());
        // 结果应该按分数排序
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).result.getScore() >= results.get(i + 1).result.getScore());
        }
    }

    @Test
    public void testQuickMatchCommand() {
        FuzzyMatcher.MatchResult result = FuzzyMatcher.matchCommand("git", "g");
        assertTrue(result.getScore() > 0);
    }

    @Test
    public void testQuickMatchCommands() {
        List<String> commands = Arrays.asList("git", "grep", "go", "gcc");
        List<String> results = FuzzyMatcher.matchCommands(commands, "g", 3);
        
        assertTrue(results.size() <= 3);
    }

    @Test
    public void testEditDistance() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("node", "noda");
        
        // 拼写错误应该有一定容忍度
        assertTrue(result.getScore() > 0);
    }

    @Test
    public void testNoMatch() {
        FuzzyMatcher matcher = new FuzzyMatcher();
        FuzzyMatcher.MatchResult result = matcher.match("node", "xyz");
        
        // 完全不匹配的应该分数很低
        assertTrue(result.getScore() < 10.0);
    }

    @Test
    public void testCustomConfig() {
        FuzzyMatcher.Config config = new FuzzyMatcher.Config();
        config.prefixWeight = 0.5;
        config.abbreviationWeight = 0.3;
        config.minScore = 5.0;
        
        FuzzyMatcher matcher = new FuzzyMatcher(config);
        FuzzyMatcher.MatchResult result = matcher.match("node", "n");
        
        assertTrue(result.getScore() > 0);
    }
}
