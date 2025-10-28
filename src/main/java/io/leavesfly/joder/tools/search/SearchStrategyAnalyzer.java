package io.leavesfly.joder.tools.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.leavesfly.joder.domain.Message;
import io.leavesfly.joder.domain.MessageRole;
import io.leavesfly.joder.services.model.ModelAdapter;
import io.leavesfly.joder.services.model.ModelAdapterFactory;
import io.leavesfly.joder.services.model.ModelRouter;
import io.leavesfly.joder.services.model.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 搜索策略分析器
 * 使用轻量级 LLM 将自然语言搜索意图转换为结构化搜索策略
 */
public class SearchStrategyAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchStrategyAnalyzer.class);
    
    private final ModelRouter modelRouter;
    private final ModelAdapterFactory modelAdapterFactory;
    private final ObjectMapper objectMapper;
    
    @Inject
    public SearchStrategyAnalyzer(
            ModelRouter modelRouter,
            ModelAdapterFactory modelAdapterFactory,
            ObjectMapper objectMapper) {
        this.modelRouter = modelRouter;
        this.modelAdapterFactory = modelAdapterFactory;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 分析用户的搜索意图并生成搜索策略
     * 
     * @param userIntent 用户的自然语言搜索意图
     * @param projectContext 项目上下文信息（可选）
     * @return 搜索策略
     */
    public SearchStrategy analyze(String userIntent, String projectContext) {
        try {
            // 使用轻量级模型进行分析
            ModelAdapter adapter = modelRouter.routeModel(TaskType.SEARCH_OPTIMIZATION);
            
            String systemPrompt = buildSystemPrompt();
            String userMessage = buildUserMessage(userIntent, projectContext);
            
            List<Message> messages = Collections.singletonList(
                new Message(MessageRole.USER, userMessage)
            );
            
            String response = adapter.sendMessage(messages, systemPrompt);
            
            logger.debug("LLM 搜索策略分析响应: {}", response);
            
            return parseStrategy(response);
            
        } catch (Exception e) {
            logger.error("分析搜索策略失败", e);
            // 降级：返回基本策略
            return createFallbackStrategy(userIntent);
        }
    }
    
    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
            你是一个代码搜索专家，负责将用户的自然语言搜索意图转换为精确的搜索策略。
            
            你需要分析用户意图，并输出 JSON 格式的搜索策略，包含以下字段：
            
            {
              "regex": "正则表达式模式",
              "searchPath": "搜索路径（相对路径，如 'src' 或 '.'）",
              "includePatterns": ["文件包含模式数组，如 '*.java', '*.ts'"],
              "excludePatterns": ["排除模式数组，如 'test', 'node_modules'"],
              "caseSensitive": false,
              "maxResults": 100,
              "filesOnly": false
            }
            
            ## 分析规则：
            
            1. **识别关键词**：提取用户意图中的核心关键词
            2. **构建正则**：根据关键词构建合适的正则表达式
               - 函数定义：如 "function\\\\s+functionName" 或 "def\\\\s+functionName"
               - 类定义：如 "class\\\\s+ClassName"
               - 配置项：如 "key\\\\s*=\\\\s*value" 或 "key:\\\\s*"
               - 错误处理：如 "(try|catch|throw|error)"
               - 导入语句：如 "(import|require|from).*keyword"
            3. **确定文件类型**：根据上下文推断应该搜索的文件类型
               - Java项目：*.java
               - TypeScript/JavaScript：*.ts, *.tsx, *.js, *.jsx
               - Python：*.py
               - 配置文件：*.conf, *.yaml, *.yml, *.json, *.properties
            4. **确定搜索路径**：
               - 如果用户指定了模块或目录，使用该路径
               - 否则使用 "." 搜索整个项目
            5. **排除目录**：默认排除 test, node_modules, target, build, dist, .git
            
            ## 注意事项：
            - 正则表达式中的特殊字符需要转义（如 \\\\s, \\\\w, \\\\., \\\\(, \\\\)）
            - 使用 | 表示或逻辑
            - 使用 .* 表示任意字符
            - 默认不区分大小写（caseSensitive=false）
            
            **只输出 JSON 对象，不要包含任何其他文字说明。**
            """;
    }
    
    /**
     * 构建用户消息
     */
    private String buildUserMessage(String userIntent, String projectContext) {
        StringBuilder msg = new StringBuilder();
        msg.append("用户搜索意图：").append(userIntent).append("\n\n");
        
        if (projectContext != null && !projectContext.isEmpty()) {
            msg.append("项目上下文：\n").append(projectContext).append("\n\n");
        }
        
        msg.append("请分析以上搜索意图，输出 JSON 格式的搜索策略。");
        
        return msg.toString();
    }
    
    /**
     * 解析 LLM 返回的策略
     */
    private SearchStrategy parseStrategy(String llmResponse) {
        try {
            // 提取 JSON 部分（可能被包裹在代码块中）
            String jsonStr = extractJson(llmResponse);
            
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            SearchStrategy strategy = new SearchStrategy();
            strategy.setRegex(jsonNode.get("regex").asText());
            
            if (jsonNode.has("searchPath")) {
                strategy.setSearchPath(jsonNode.get("searchPath").asText());
            }
            
            if (jsonNode.has("includePatterns")) {
                List<String> includes = new ArrayList<>();
                jsonNode.get("includePatterns").forEach(node -> includes.add(node.asText()));
                strategy.setIncludePatterns(includes);
            }
            
            if (jsonNode.has("excludePatterns")) {
                List<String> excludes = new ArrayList<>();
                jsonNode.get("excludePatterns").forEach(node -> excludes.add(node.asText()));
                strategy.setExcludePatterns(excludes);
            }
            
            if (jsonNode.has("caseSensitive")) {
                strategy.setCaseSensitive(jsonNode.get("caseSensitive").asBoolean());
            }
            
            if (jsonNode.has("maxResults")) {
                strategy.setMaxResults(jsonNode.get("maxResults").asInt());
            }
            
            if (jsonNode.has("filesOnly")) {
                strategy.setFilesOnly(jsonNode.get("filesOnly").asBoolean());
            }
            
            logger.info("解析搜索策略成功: {}", strategy);
            return strategy;
            
        } catch (Exception e) {
            logger.error("解析搜索策略失败，响应内容: {}", llmResponse, e);
            throw new RuntimeException("解析搜索策略失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提取 JSON 字符串（去除代码块标记）
     */
    private String extractJson(String text) {
        // 去除可能的 markdown 代码块标记
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
    
    /**
     * 创建降级策略（当 LLM 分析失败时）
     */
    private SearchStrategy createFallbackStrategy(String userIntent) {
        logger.warn("使用降级搜索策略");
        
        SearchStrategy strategy = new SearchStrategy();
        
        // 简单地将用户意图作为关键词
        String escapedIntent = userIntent.replaceAll("[\\[\\](){}.*+?^$|\\\\]", "\\\\$0");
        strategy.setRegex(escapedIntent);
        
        strategy.setSearchPath(".");
        strategy.setCaseSensitive(false);
        strategy.setMaxResults(100);
        strategy.setFilesOnly(false);
        
        // 默认排除常见目录
        strategy.setExcludePatterns(Arrays.asList(
            "node_modules", "target", "build", "dist", ".git", "test"
        ));
        
        return strategy;
    }
}
