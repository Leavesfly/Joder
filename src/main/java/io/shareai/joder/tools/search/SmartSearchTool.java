package io.shareai.joder.tools.search;

import io.shareai.joder.WorkingDirectory;
import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 智能代码搜索工具
 * 结合 LLM 理解用户意图并构建精确的搜索表达式
 */
public class SmartSearchTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartSearchTool.class);
    
    private final String workingDirectory;
    private final SearchStrategyAnalyzer strategyAnalyzer;
    private final SearchExecutor searchExecutor;
    
    @Inject
    public SmartSearchTool(
            @WorkingDirectory String workingDirectory,
            SearchStrategyAnalyzer strategyAnalyzer,
            SearchExecutor searchExecutor) {
        this.workingDirectory = workingDirectory;
        this.strategyAnalyzer = strategyAnalyzer;
        this.searchExecutor = searchExecutor;
    }
    
    @Override
    public String getName() {
        return "SmartSearch";
    }
    
    @Override
    public String getDescription() {
        return "智能代码搜索工具，使用自然语言描述搜索意图。\n" +
               "- 自动理解用户的搜索意图并构建精确的搜索表达式\n" +
               "- 智能识别文件类型和搜索范围\n" +
               "- 自动过滤无关文件（如 node_modules、target 等）\n" +
               "- 按相关性和文件新鲜度排序结果\n" +
               "- 适用于复杂的代码搜索场景，如查找函数定义、配置项、错误处理等\n\n" +
               "参数:\n" +
               "- query: 自然语言搜索意图（必需），例如：\n" +
               "  * \"查找所有的用户认证相关代码\"\n" +
               "  * \"找到处理文件上传的函数\"\n" +
               "  * \"搜索数据库配置相关的代码\"\n" +
               "- context: 项目上下文信息（可选），帮助更准确地理解搜索意图\n" +
               "- maxResults: 最大返回结果数（可选，默认100）\n" +
               "- filesOnly: 是否只返回文件路径不显示匹配内容（可选，默认false）";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取参数
            String query = getString(input, "query");
            if (query == null || query.trim().isEmpty()) {
                return ToolResult.error("查询内容不能为空");
            }
            
            String context = getString(input, "context", "");
            int maxResults = getInt(input, "maxResults", 100);
            boolean filesOnly = getBoolean(input, "filesOnly", false);
            
            logger.info("开始智能搜索: query={}, maxResults={}, filesOnly={}", 
                       query, maxResults, filesOnly);
            
            // 第一步：使用 LLM 分析搜索意图并生成策略
            logger.debug("分析搜索意图...");
            SearchStrategy strategy = strategyAnalyzer.analyze(query, context);
            strategy.setMaxResults(maxResults);
            strategy.setFilesOnly(filesOnly);
            
            logger.info("生成搜索策略: {}", strategy);
            
            // 第二步：执行搜索
            logger.debug("执行搜索...");
            Path workingDir = Paths.get(workingDirectory);
            List<SearchMatch> matches = searchExecutor.execute(strategy, workingDir);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 第三步：格式化结果
            String result = formatResults(matches, workingDir, strategy, duration);
            
            logger.info("搜索完成: 找到 {} 个匹配，耗时 {}ms", matches.size(), duration);
            
            return ToolResult.success(result);
            
        } catch (Exception e) {
            logger.error("智能搜索失败", e);
            return ToolResult.error("搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化搜索结果
     */
    private String formatResults(List<SearchMatch> matches, Path workingDir, 
                                 SearchStrategy strategy, long duration) {
        StringBuilder result = new StringBuilder();
        
        // 显示搜索策略
        result.append("🔍 搜索策略:\n");
        result.append("  - 正则表达式: ").append(strategy.getRegex()).append("\n");
        if (!strategy.getIncludePatterns().isEmpty()) {
            result.append("  - 包含文件: ").append(String.join(", ", strategy.getIncludePatterns())).append("\n");
        }
        if (!strategy.getExcludePatterns().isEmpty()) {
            result.append("  - 排除模式: ").append(String.join(", ", strategy.getExcludePatterns())).append("\n");
        }
        result.append("\n");
        
        // 显示结果统计
        result.append(String.format("📊 找到 %d 个匹配文件（耗时: %dms）\n\n", matches.size(), duration));
        
        if (matches.isEmpty()) {
            result.append("未找到匹配的文件。\n");
            result.append("建议：\n");
            result.append("  - 尝试使用更通用的搜索词\n");
            result.append("  - 检查搜索路径是否正确\n");
            result.append("  - 尝试使用不同的表达方式重新描述搜索意图\n");
        } else {
            // 显示匹配结果
            boolean truncated = matches.size() >= strategy.getMaxResults();
            int displayCount = Math.min(matches.size(), strategy.getMaxResults());
            
            for (int i = 0; i < displayCount; i++) {
                SearchMatch match = matches.get(i);
                Path relativePath = workingDir.relativize(match.getFilePath());
                
                result.append("📄 ").append(relativePath.toString()).append("\n");
                
                if (!strategy.isFilesOnly() && !match.getMatchedLines().isEmpty()) {
                    // 显示前几行匹配内容
                    int lineCount = Math.min(match.getMatchedLines().size(), 5);
                    for (int j = 0; j < lineCount; j++) {
                        SearchMatch.MatchedLine line = match.getMatchedLines().get(j);
                        result.append(String.format("   %4d: %s\n", 
                                                   line.getLineNumber(), 
                                                   line.getContent().trim()));
                    }
                    
                    if (match.getMatchedLines().size() > lineCount) {
                        result.append(String.format("   ... 还有 %d 行匹配\n", 
                                                   match.getMatchedLines().size() - lineCount));
                    }
                }
                
                result.append("\n");
            }
            
            if (truncated) {
                result.append("⚠️  结果已截断，考虑使用更具体的搜索条件或增加 maxResults 参数\n");
            }
        }
        
        return result.toString().trim();
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String query = getString(input, "query");
        return "🔍 智能搜索: \"" + query + "\"";
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            String output = result.getOutput();
            // 提取匹配文件数
            if (output.contains("找到")) {
                int start = output.indexOf("找到 ") + 3;
                int end = output.indexOf(" 个匹配", start);
                if (start > 0 && end > start) {
                    String count = output.substring(start, end);
                    return "✅ 找到 " + count + " 个匹配文件";
                }
            }
            return "✅ 搜索完成";
        } else {
            return "❌ 搜索失败: " + result.getError();
        }
    }
}
