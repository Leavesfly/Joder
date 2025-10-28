package io.shareai.joder.tools.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索匹配结果
 */
public class SearchMatch {
    
    /**
     * 文件路径
     */
    private Path filePath;
    
    /**
     * 文件最后修改时间
     */
    private long lastModified;
    
    /**
     * 匹配行列表
     */
    private List<MatchedLine> matchedLines = new ArrayList<>();
    
    /**
     * 相关性分数（用于排序）
     */
    private double relevanceScore = 0.0;
    
    public Path getFilePath() {
        return filePath;
    }
    
    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    public List<MatchedLine> getMatchedLines() {
        return matchedLines;
    }
    
    public void setMatchedLines(List<MatchedLine> matchedLines) {
        this.matchedLines = matchedLines;
    }
    
    public void addMatchedLine(MatchedLine line) {
        this.matchedLines.add(line);
    }
    
    public double getRelevanceScore() {
        return relevanceScore;
    }
    
    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
    
    /**
     * 匹配的行
     */
    public static class MatchedLine {
        /**
         * 行号（从1开始）
         */
        private int lineNumber;
        
        /**
         * 行内容
         */
        private String content;
        
        /**
         * 匹配位置（列号，从0开始）
         */
        private int matchStart;
        
        /**
         * 匹配长度
         */
        private int matchLength;
        
        public MatchedLine(int lineNumber, String content, int matchStart, int matchLength) {
            this.lineNumber = lineNumber;
            this.content = content;
            this.matchStart = matchStart;
            this.matchLength = matchLength;
        }
        
        public int getLineNumber() {
            return lineNumber;
        }
        
        public String getContent() {
            return content;
        }
        
        public int getMatchStart() {
            return matchStart;
        }
        
        public int getMatchLength() {
            return matchLength;
        }
    }
}
