package io.shareai.joder.tools.web;

import io.shareai.joder.tools.Tool;
import io.shareai.joder.tools.ToolResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InspectSite Tool - 网站检查工具
 * 
 * 检查网站的基本信息、性能和结构
 */
@Singleton
public class InspectSiteTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(InspectSiteTool.class);
    
    private final OkHttpClient httpClient;
    
    @Inject
    public InspectSiteTool() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }
    
    @Override
    public String getName() {
        return "InspectSite";
    }
    
    @Override
    public String getDescription() {
        return "检查网站的基本信息、性能和结构。\n" +
               "- 检测响应时间和状态码\n" +
               "- 分析页面标题和元信息\n" +
               "- 提取链接和资源统计\n" +
               "- 检查移动端适配";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具检查网站的技术信息。\n\n" +
               "**参数**：\n" +
               "- url: 要检查的网站 URL（必需）\n\n" +
               "**检查内容**：\n" +
               "1. HTTP 响应信息\n" +
               "   - 状态码\n" +
               "   - 响应时间\n" +
               "   - 重定向次数\n" +
               "   - 服务器类型\n\n" +
               "2. 页面元信息\n" +
               "   - 标题\n" +
               "   - 描述\n" +
               "   - 关键词\n" +
               "   - 字符编码\n\n" +
               "3. 页面结构\n" +
               "   - 链接数量\n" +
               "   - 图片数量\n" +
               "   - 脚本数量\n" +
               "   - 样式表数量\n\n" +
               "4. 移动端适配\n" +
               "   - Viewport 设置\n" +
               "   - 响应式设计\n\n" +
               "**适用场景**：\n" +
               "- SEO 分析\n" +
               "- 性能测试\n" +
               "- 技术栈识别\n" +
               "- 竞品分析";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true; // 只读检查
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String url = (String) input.get("url");
        
        // 验证输入
        if (url == null || url.trim().isEmpty()) {
            return ToolResult.error("URL 不能为空");
        }
        
        // 规范化 URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        try {
            logger.info("检查网站: {}", url);
            
            // 构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (compatible; Joder SiteInspector/1.0)")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build();
            
            // 测量响应时间
            long startTime = System.currentTimeMillis();
            
            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                // 获取响应信息
                int statusCode = response.code();
                String server = response.header("Server", "Unknown");
                String contentType = response.header("Content-Type", "Unknown");
                
                // 获取页面内容
                String html = response.body() != null ? response.body().string() : "";
                
                // 分析页面
                SiteInfo siteInfo = analyzePage(html);
                
                // 构建检查报告
                StringBuilder report = new StringBuilder();
                report.append("🔍 网站检查报告\n\n");
                report.append("═".repeat(60)).append("\n\n");
                
                // HTTP 响应信息
                report.append("📡 HTTP 响应信息\n");
                report.append("─".repeat(60)).append("\n");
                report.append(String.format("URL:          %s\n", url));
                report.append(String.format("状态码:        %d %s\n", statusCode, getStatusText(statusCode)));
                report.append(String.format("响应时间:      %d ms\n", responseTime));
                report.append(String.format("服务器:        %s\n", server));
                report.append(String.format("内容类型:      %s\n\n", contentType));
                
                // 页面元信息
                report.append("📄 页面元信息\n");
                report.append("─".repeat(60)).append("\n");
                report.append(String.format("标题:         %s\n", siteInfo.title));
                report.append(String.format("描述:         %s\n", siteInfo.description));
                report.append(String.format("关键词:       %s\n", siteInfo.keywords));
                report.append(String.format("字符编码:      %s\n\n", siteInfo.charset));
                
                // 页面结构
                report.append("🏗️  页面结构\n");
                report.append("─".repeat(60)).append("\n");
                report.append(String.format("链接数量:      %d\n", siteInfo.linkCount));
                report.append(String.format("图片数量:      %d\n", siteInfo.imageCount));
                report.append(String.format("脚本数量:      %d\n", siteInfo.scriptCount));
                report.append(String.format("样式表数量:    %d\n\n", siteInfo.stylesheetCount));
                
                // 移动端适配
                report.append("📱 移动端适配\n");
                report.append("─".repeat(60)).append("\n");
                report.append(String.format("Viewport:     %s\n", siteInfo.hasViewport ? "✓ 已设置" : "✗ 未设置"));
                report.append(String.format("响应式设计:    %s\n\n", siteInfo.isResponsive ? "✓ 支持" : "? 未知"));
                
                // 性能评分
                report.append("⚡ 性能评分\n");
                report.append("─".repeat(60)).append("\n");
                String performanceScore = getPerformanceScore(responseTime);
                report.append(String.format("响应速度:      %s\n", performanceScore));
                
                report.append("\n═".repeat(60)).append("\n");
                
                return ToolResult.success(report.toString());
            }
            
        } catch (IOException e) {
            logger.error("检查网站失败: {}", url, e);
            return ToolResult.error("检查网站失败: " + e.getMessage());
        }
    }
    
    /**
     * 分析页面内容
     */
    private SiteInfo analyzePage(String html) {
        SiteInfo info = new SiteInfo();
        
        // 提取标题
        Pattern titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher titleMatcher = titlePattern.matcher(html);
        if (titleMatcher.find()) {
            info.title = titleMatcher.group(1).trim();
        }
        
        // 提取描述
        Pattern descPattern = Pattern.compile("<meta[^>]*name=[\"']description[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher descMatcher = descPattern.matcher(html);
        if (descMatcher.find()) {
            info.description = descMatcher.group(1).trim();
        }
        
        // 提取关键词
        Pattern keywordsPattern = Pattern.compile("<meta[^>]*name=[\"']keywords[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher keywordsMatcher = keywordsPattern.matcher(html);
        if (keywordsMatcher.find()) {
            info.keywords = keywordsMatcher.group(1).trim();
        }
        
        // 提取字符编码
        Pattern charsetPattern = Pattern.compile("charset=[\"']?([^\"'>\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher charsetMatcher = charsetPattern.matcher(html);
        if (charsetMatcher.find()) {
            info.charset = charsetMatcher.group(1).toUpperCase();
        }
        
        // 统计链接
        info.linkCount = countOccurrences(html, "<a\\s+[^>]*href");
        
        // 统计图片
        info.imageCount = countOccurrences(html, "<img\\s+[^>]*src");
        
        // 统计脚本
        info.scriptCount = countOccurrences(html, "<script");
        
        // 统计样式表
        info.stylesheetCount = countOccurrences(html, "<link[^>]*rel=[\"']stylesheet");
        
        // 检查 viewport
        info.hasViewport = html.toLowerCase().contains("name=\"viewport\"") || 
                          html.toLowerCase().contains("name='viewport'");
        
        // 检查响应式设计（简单检测）
        info.isResponsive = info.hasViewport || 
                           html.toLowerCase().contains("@media") ||
                           html.toLowerCase().contains("responsive");
        
        return info;
    }
    
    /**
     * 统计匹配次数
     */
    private int countOccurrences(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * 获取状态码文本
     */
    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }
    
    /**
     * 获取性能评分
     */
    private String getPerformanceScore(long responseTime) {
        if (responseTime < 200) {
            return "优秀 (< 200ms) ⭐⭐⭐⭐⭐";
        } else if (responseTime < 500) {
            return "良好 (< 500ms) ⭐⭐⭐⭐";
        } else if (responseTime < 1000) {
            return "一般 (< 1s) ⭐⭐⭐";
        } else if (responseTime < 3000) {
            return "较慢 (< 3s) ⭐⭐";
        } else {
            return "很慢 (>= 3s) ⭐";
        }
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String url = (String) input.get("url");
        return String.format("检查网站: %s", url);
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 网站检查完成";
        } else {
            return "❌ 网站检查失败: " + result.getError();
        }
    }
    
    // 内部类：网站信息
    private static class SiteInfo {
        String title = "未知";
        String description = "无";
        String keywords = "无";
        String charset = "未知";
        int linkCount = 0;
        int imageCount = 0;
        int scriptCount = 0;
        int stylesheetCount = 0;
        boolean hasViewport = false;
        boolean isResponsive = false;
    }
}
