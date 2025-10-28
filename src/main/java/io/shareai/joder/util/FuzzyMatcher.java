package io.shareai.joder.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模糊匹配工具类
 * 对应 Kode 的 fuzzyMatcher.ts
 * 
 * 实现多算法加权评分系统，适用于命令/终端补全场景
 */
public class FuzzyMatcher {

    /**
     * 匹配结果
     */
    public static class MatchResult {
        private final double score;
        private final String algorithm;
        private final double confidence;

        public MatchResult(double score, String algorithm, double confidence) {
            this.score = score;
            this.algorithm = algorithm;
            this.confidence = confidence;
        }

        public double getScore() { return score; }
        public String getAlgorithm() { return algorithm; }
        public double getConfidence() { return confidence; }

        @Override
        public String toString() {
            return String.format("MatchResult{score=%.2f, algorithm='%s', confidence=%.2f}", 
                score, algorithm, confidence);
        }
    }

    /**
     * 配置类
     */
    public static class Config {
        public double prefixWeight = 0.35;
        public double substringWeight = 0.20;
        public double abbreviationWeight = 0.30;
        public double editDistanceWeight = 0.10;
        public double popularityWeight = 0.05;
        
        public double minScore = 10.0;
        public int maxEditDistance = 2;
        public Set<String> popularCommands = new HashSet<>(Arrays.asList(
            "node", "npm", "git", "ls", "cd", "cat", "grep", "find", "cp", "mv",
            "python", "java", "docker", "curl", "wget", "vim", "nano"
        ));
    }

    private final Config config;

    public FuzzyMatcher() {
        this(new Config());
    }

    public FuzzyMatcher(Config config) {
        this.config = config;
        normalizeWeights();
    }

    /**
     * 归一化权重
     */
    private void normalizeWeights() {
        double sum = config.prefixWeight + config.substringWeight + 
                    config.abbreviationWeight + config.editDistanceWeight + 
                    config.popularityWeight;
        
        if (Math.abs(sum - 1.0) > 0.01) {
            config.prefixWeight /= sum;
            config.substringWeight /= sum;
            config.abbreviationWeight /= sum;
            config.editDistanceWeight /= sum;
            config.popularityWeight /= sum;
        }
    }

    /**
     * 计算候选项与查询的模糊匹配分数
     */
    public MatchResult match(String candidate, String query) {
        String text = candidate.toLowerCase();
        String pattern = query.toLowerCase();

        // 完全匹配
        if (text.equals(pattern)) {
            return new MatchResult(1000.0, "exact", 1.0);
        }

        // 前缀精确匹配
        if (text.startsWith(pattern)) {
            return new MatchResult(900.0 + (10 - pattern.length()), "prefix-exact", 0.95);
        }

        // 运行所有算法
        Map<String, Double> scores = new HashMap<>();
        scores.put("prefix", prefixScore(text, pattern));
        scores.put("substring", substringScore(text, pattern));
        scores.put("abbreviation", abbreviationScore(text, pattern));
        scores.put("editDistance", editDistanceScore(text, pattern));
        scores.put("popularity", popularityScore(text));

        // 加权组合
        double rawScore = 0.0;
        rawScore += scores.get("prefix") * config.prefixWeight;
        rawScore += scores.get("substring") * config.substringWeight;
        rawScore += scores.get("abbreviation") * config.abbreviationWeight;
        rawScore += scores.get("editDistance") * config.editDistanceWeight;
        rawScore += scores.get("popularity") * config.popularityWeight;

        // 长度惩罚
        double lengthPenalty = Math.max(0, text.length() - 6) * 1.5;
        double finalScore = Math.max(0, rawScore - lengthPenalty);

        // 确定主要算法和置信度
        String maxAlgorithm = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");

        double confidence = Math.min(1.0, finalScore / 100.0);

        return new MatchResult(finalScore, maxAlgorithm, confidence);
    }

    /**
     * 算法1: 前缀匹配
     */
    private double prefixScore(String text, String pattern) {
        if (!text.startsWith(pattern)) {
            return 0.0;
        }
        double coverage = (double) pattern.length() / text.length();
        return 100.0 * coverage;
    }

    /**
     * 算法2: 子串匹配
     */
    private double substringScore(String text, String pattern) {
        int index = text.indexOf(pattern);
        if (index != -1) {
            double positionFactor = Math.max(0, 10 - index) / 10.0;
            double coverageFactor = (double) pattern.length() / text.length();
            return 80.0 * positionFactor * coverageFactor;
        }

        // 处理数字后缀（如 py3 → python3）
        if (pattern.matches("^.+?\\d+$")) {
            int lastDigitIndex = pattern.length() - 1;
            while (lastDigitIndex > 0 && Character.isDigit(pattern.charAt(lastDigitIndex))) {
                lastDigitIndex--;
            }
            lastDigitIndex++;
            
            String prefix = pattern.substring(0, lastDigitIndex);
            String num = pattern.substring(lastDigitIndex);
            
            if (text.startsWith(prefix) && text.endsWith(num)) {
                double coverageFactor = (double) pattern.length() / text.length();
                return 70.0 * coverageFactor + 20.0;
            }
        }

        return 0.0;
    }

    /**
     * 算法3: 缩写匹配
     */
    private double abbreviationScore(String text, String pattern) {
        double score = 0.0;
        int textPos = 0;
        boolean perfectStart = false;
        int consecutiveMatches = 0;
        int wordBoundaryMatches = 0;

        String textClean = text.replace("-", "").toLowerCase();

        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            boolean charFound = false;

            for (int j = textPos; j < textClean.length(); j++) {
                if (textClean.charAt(j) == ch) {
                    charFound = true;

                    // 连续字符奖励
                    if (j == textPos) {
                        consecutiveMatches++;
                    } else {
                        consecutiveMatches = 1;
                    }

                    // 位置敏感评分
                    if (i == 0 && j == 0) {
                        score += 50;  // 第一个字符完美匹配
                        perfectStart = true;
                    } else if (j <= 2) {
                        score += 20;  // 早期位置
                    } else if (j <= 6) {
                        score += 10;  // 中期位置
                    } else {
                        score += 5;   // 晚期位置
                    }

                    // 连续字符奖励
                    if (consecutiveMatches > 1) {
                        score += consecutiveMatches * 5;
                    }

                    textPos = j + 1;
                    break;
                }
            }

            if (!charFound) {
                return 0.0;  // 无效缩写
            }
        }

        // 关键奖励
        if (perfectStart) score += 30;
        if (wordBoundaryMatches >= 2) score += 25;
        if (textPos <= textClean.length() * 0.8) score += 15;

        // 数字结尾匹配奖励
        char lastPatternChar = pattern.charAt(pattern.length() - 1);
        char lastTextChar = text.charAt(text.length() - 1);
        if (Character.isDigit(lastPatternChar) && lastPatternChar == lastTextChar) {
            score += 25;
        }

        return score;
    }

    /**
     * 算法4: 编辑距离（拼写错误容忍）
     */
    private double editDistanceScore(String text, String pattern) {
        if (pattern.length() > text.length() + config.maxEditDistance) {
            return 0.0;
        }

        int distance = levenshteinDistance(pattern, text);
        if (distance > config.maxEditDistance) {
            return 0.0;
        }

        return Math.max(0.0, 30.0 - distance * 10.0);
    }

    /**
     * 计算 Levenshtein 距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                    );
                }
            }
        }

        return dp[m][n];
    }

    /**
     * 算法5: 命令流行度
     */
    private double popularityScore(String text) {
        if (config.popularCommands.contains(text)) {
            return 40.0;
        }
        if (text.length() <= 5) {
            return 10.0;
        }
        return 0.0;
    }

    /**
     * 批量匹配多个候选项并返回排序结果
     */
    public List<CandidateMatch> matchMany(List<String> candidates, String query) {
        return candidates.stream()
            .map(candidate -> new CandidateMatch(candidate, match(candidate, query)))
            .filter(item -> item.result.score >= config.minScore)
            .sorted((a, b) -> Double.compare(b.result.score, a.result.score))
            .collect(Collectors.toList());
    }

    /**
     * 候选匹配结果
     */
    public static class CandidateMatch {
        public final String candidate;
        public final MatchResult result;

        public CandidateMatch(String candidate, MatchResult result) {
            this.candidate = candidate;
            this.result = result;
        }

        @Override
        public String toString() {
            return String.format("%s: %.2f", candidate, result.score);
        }
    }

    /**
     * 默认匹配器实例
     */
    private static final FuzzyMatcher DEFAULT_MATCHER = new FuzzyMatcher();

    /**
     * 快捷匹配方法
     */
    public static MatchResult matchCommand(String command, String query) {
        return DEFAULT_MATCHER.match(command, query);
    }

    /**
     * 快捷批量匹配方法
     */
    public static List<String> matchCommands(List<String> commands, String query, int limit) {
        return DEFAULT_MATCHER.matchMany(commands, query).stream()
            .limit(limit)
            .map(match -> match.candidate)
            .collect(Collectors.toList());
    }
}
