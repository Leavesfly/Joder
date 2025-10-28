package io.shareai.joder.tools.web;

import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 网络搜索工具 - 使用 DuckDuckGo HTML 搜索（无需 API Key）
 */
public class WebSearchTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSearchTool.class);
    private static final String DUCKDUCKGO_URL = "https://html.duckduckgo.com/html/";
    private static final int DEFAULT_MAX_RESULTS = 5;
    
    private final OkHttpClient httpClient;
    
    @Inject
    public WebSearchTool(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public String getName() {
        return "web_search";
    }
    
    @Override
    public String getDescription() {
        return "在互联网上搜索信息。使用 DuckDuckGo 搜索引擎查找相关网页。" +
               "返回搜索结果包括标题、链接和摘要。适用于获取最新信息、查找文档、研究技术问题等。";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;  // 搜索是只读操作
    }
    
    @Override
    public boolean needsPermissions() {
        return false;  // 网络搜索不需要特殊权限
    }
    
    @Override
    public ToolResult call(Map<String, Object> params) {
        try {
            String query = getString(params, "query");
            if (query == null || query.trim().isEmpty()) {
                return ToolResult.error("搜索关键词不能为空");
            }
            
            int maxResults = getInt(params, "max_results", DEFAULT_MAX_RESULTS);
            
            logger.info("Searching for: {}", query);
            
            // 执行搜索
            List<SearchResult> results = performSearch(query, maxResults);
            
            if (results.isEmpty()) {
                return ToolResult.success("未找到相关结果");
            }
            
            // 格式化结果
            String formattedResults = formatSearchResults(query, results);
            return ToolResult.success(formattedResults);
            
        } catch (Exception e) {
            logger.error("Web search failed", e);
            return ToolResult.error("搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行搜索
     */
    private List<SearchResult> performSearch(String query, int maxResults) throws IOException {
        // 构建搜索 URL
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = DUCKDUCKGO_URL + "?q=" + encodedQuery;
        
        // 发送请求
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            
            String html = response.body().string();
            return parseSearchResults(html, maxResults);
        }
    }
    
    /**
     * 解析搜索结果
     */
    private List<SearchResult> parseSearchResults(String html, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        Document doc = Jsoup.parse(html);
        Elements resultElements = doc.select(".result");
        
        for (int i = 0; i < Math.min(resultElements.size(), maxResults); i++) {
            Element resultElement = resultElements.get(i);
            
            try {
                // 提取标题和链接
                Element titleElement = resultElement.selectFirst(".result__a");
                if (titleElement == null) {
                    continue;
                }
                
                String title = titleElement.text();
                String link = titleElement.attr("href");
                
                // 提取摘要
                Element snippetElement = resultElement.selectFirst(".result__snippet");
                String snippet = snippetElement != null ? snippetElement.text() : "";
                
                results.add(new SearchResult(title, link, snippet));
                
            } catch (Exception e) {
                logger.debug("Failed to parse search result", e);
                // 继续处理下一个结果
            }
        }
        
        return results;
    }
    
    /**
     * 格式化搜索结果
     */
    private String formatSearchResults(String query, List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 搜索结果: \"%s\" (找到 %d 条)\n\n", query, results.size()));
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            sb.append(String.format("%d. **%s**\n", i + 1, result.title));
            sb.append(String.format("   %s\n", result.url));
            if (!result.snippet.isEmpty()) {
                sb.append(String.format("   %s\n", result.snippet));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 搜索结果数据类
     */
    private static class SearchResult {
        final String title;
        final String url;
        final String snippet;
        
        SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
