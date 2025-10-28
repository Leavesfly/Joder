package io.shareai.joder.tools.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能搜索策略
 * LLM 生成的搜索执行策略
 */
public class SearchStrategy {
    
    /**
     * 正则表达式模式
     */
    private String regex;
    
    /**
     * 搜索路径（相对于工作目录）
     */
    private String searchPath;
    
    /**
     * 包含的文件类型模式列表
     */
    private List<String> includePatterns = new ArrayList<>();
    
    /**
     * 排除的目录或文件模式列表
     */
    private List<String> excludePatterns = new ArrayList<>();
    
    /**
     * 是否大小写敏感
     */
    private boolean caseSensitive = false;
    
    /**
     * 最大结果数
     */
    private int maxResults = 100;
    
    /**
     * 是否只返回文件名（不包含匹配内容）
     */
    private boolean filesOnly = false;
    
    public String getRegex() {
        return regex;
    }
    
    public void setRegex(String regex) {
        this.regex = regex;
    }
    
    public String getSearchPath() {
        return searchPath;
    }
    
    public void setSearchPath(String searchPath) {
        this.searchPath = searchPath;
    }
    
    public List<String> getIncludePatterns() {
        return includePatterns;
    }
    
    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }
    
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }
    
    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public int getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    
    public boolean isFilesOnly() {
        return filesOnly;
    }
    
    public void setFilesOnly(boolean filesOnly) {
        this.filesOnly = filesOnly;
    }
    
    @Override
    public String toString() {
        return "SearchStrategy{" +
                "regex='" + regex + '\'' +
                ", searchPath='" + searchPath + '\'' +
                ", includePatterns=" + includePatterns +
                ", excludePatterns=" + excludePatterns +
                ", caseSensitive=" + caseSensitive +
                ", maxResults=" + maxResults +
                ", filesOnly=" + filesOnly +
                '}';
    }
}
