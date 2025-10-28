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

import java.util.Map;

/**
 * URL 内容获取工具 - 获取网页内容并转换为 Markdown
 */
public class URLFetcherTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(URLFetcherTool.class);
    
    private final OkHttpClient httpClient;
    
    @Inject
    public URLFetcherTool(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public String getName() {
        return "url_fetcher";
    }
    
    @Override
    public String getDescription() {
        return "获取指定 URL 的网页内容。自动清理广告、导航栏等无关内容，提取主要文本内容。" +
               "适用于读取文档、博客文章、API 文档等。返回清理后的文本内容。";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> params) {
        try {
            String url = getString(params, "url");
            if (url == null || url.trim().isEmpty()) {
                return ToolResult.error("URL 不能为空");
            }
            
            // 验证 URL 格式
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return ToolResult.error("URL 必须以 http:// 或 https:// 开头");
            }
            
            logger.info("Fetching URL: {}", url);
            
            // 获取网页内容
            String content = fetchAndExtractContent(url);
            
            return ToolResult.success(formatResult(url, content));
            
        } catch (IOException e) {
            logger.error("Failed to fetch URL", e);
            return ToolResult.error("获取URL失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error in URLFetcherTool", e);
            return ToolResult.error("处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取并提取网页主要内容
     */
    private String fetchAndExtractContent(String url) throws IOException {
        // 发送 HTTP 请求
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            
            String html = response.body().string();
            return extractMainContent(html);
        }
    }
    
    /**
     * 提取网页主要内容
     */
    private String extractMainContent(String html) {
        Document doc = Jsoup.parse(html);
        
        // 移除不需要的元素
        doc.select("script, style, nav, footer, header, aside, " +
                  ".ads, .advertisement, .sidebar, .menu, " +
                  "#comments, .comment, .social-share").remove();
        
        // 尝试找到主要内容区域
        Element mainContent = findMainContent(doc);
        
        if (mainContent != null) {
            return convertToReadableText(mainContent);
        } else {
            // 如果找不到主要内容，返回 body
            return convertToReadableText(doc.body());
        }
    }
    
    /**
     * 查找主要内容区域
     */
    private Element findMainContent(Document doc) {
        // 常见的主要内容选择器
        String[] selectors = {
            "article",
            "main",
            "[role=main]",
            ".content",
            ".main-content",
            "#content",
            "#main-content",
            ".post-content",
            ".entry-content",
            ".article-content"
        };
        
        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element != null && element.text().length() > 100) {
                return element;
            }
        }
        
        return null;
    }
    
    /**
     * 转换为可读文本（保留基本结构）
     */
    private String convertToReadableText(Element element) {
        StringBuilder sb = new StringBuilder();
        
        // 处理标题
        Elements headers = element.select("h1, h2, h3, h4, h5, h6");
        Elements paragraphs = element.select("p");
        Elements lists = element.select("ul, ol");
        Elements codeBlocks = element.select("pre, code");
        
        // 如果有明确的结构化内容，按结构提取
        if (!headers.isEmpty() || !paragraphs.isEmpty()) {
            extractStructuredContent(element, sb);
        } else {
            // 否则提取所有文本
            sb.append(element.text());
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 提取结构化内容
     */
    private void extractStructuredContent(Element element, StringBuilder sb) {
        for (Element child : element.children()) {
            String tagName = child.tagName();
            
            switch (tagName) {
                case "h1":
                    sb.append("\n# ").append(child.text()).append("\n\n");
                    break;
                case "h2":
                    sb.append("\n## ").append(child.text()).append("\n\n");
                    break;
                case "h3":
                    sb.append("\n### ").append(child.text()).append("\n\n");
                    break;
                case "h4":
                    sb.append("\n#### ").append(child.text()).append("\n\n");
                    break;
                case "p":
                    sb.append(child.text()).append("\n\n");
                    break;
                case "pre":
                case "code":
                    sb.append("\n```\n").append(child.text()).append("\n```\n\n");
                    break;
                case "ul":
                case "ol":
                    extractList(child, sb, tagName.equals("ol"));
                    break;
                case "blockquote":
                    sb.append("> ").append(child.text()).append("\n\n");
                    break;
                default:
                    // 递归处理子元素
                    if (child.children().size() > 0) {
                        extractStructuredContent(child, sb);
                    } else if (!child.text().trim().isEmpty()) {
                        sb.append(child.text()).append("\n");
                    }
            }
        }
    }
    
    /**
     * 提取列表
     */
    private void extractList(Element listElement, StringBuilder sb, boolean ordered) {
        Elements items = listElement.select("> li");
        int counter = 1;
        
        for (Element item : items) {
            if (ordered) {
                sb.append(counter++).append(". ");
            } else {
                sb.append("- ");
            }
            sb.append(item.text()).append("\n");
        }
        sb.append("\n");
    }
    
    /**
     * 格式化结果
     */
    private String formatResult(String url, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("📄 URL内容: ").append(url).append("\n\n");
        sb.append("---\n\n");
        
        // 限制内容长度（避免过长）
        if (content.length() > 5000) {
            sb.append(content, 0, 5000);
            sb.append("\n\n... (内容已截断，总长度: ").append(content.length()).append(" 字符)");
        } else {
            sb.append(content);
        }
        
        return sb.toString();
    }
}
