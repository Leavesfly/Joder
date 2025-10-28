package io.shareai.joder.ui.components;

import com.googlecode.lanterna.TextColor;
import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import io.shareai.joder.ui.renderer.DiffRenderer;
import io.shareai.joder.ui.renderer.SyntaxHighlighter;
import io.shareai.joder.ui.theme.Theme;
import io.shareai.joder.ui.theme.ThemeManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息渲染器
 */
@Singleton
public class MessageRenderer {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // 代码块匹配模式：```language\n...\n```
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "```([a-zA-Z0-9]*)\\n(.*?)```",
        Pattern.DOTALL
    );
    
    // Diff 块匹配模式
    private static final Pattern DIFF_PATTERN = Pattern.compile(
        "(?:diff|Changes:).*?\\n(.*?)(?=\\n\\n|$)",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    
    private final ThemeManager themeManager;
    private final SyntaxHighlighter syntaxHighlighter;
    private final DiffRenderer diffRenderer;
    
    @Inject
    public MessageRenderer(ThemeManager themeManager, 
                          SyntaxHighlighter syntaxHighlighter,
                          DiffRenderer diffRenderer) {
        this.themeManager = themeManager;
        this.syntaxHighlighter = syntaxHighlighter;
        this.diffRenderer = diffRenderer;
    }
    
    /**
     * 渲染消息
     */
    public String render(Message message) {
        Theme theme = themeManager.getCurrentTheme();
        StringBuilder sb = new StringBuilder();
        
        // 根据角色选择颜色和前缀
        String prefix;
        TextColor color;
        
        if (message.getRole() == MessageRole.USER) {
            prefix = "User";
            color = theme.getUserMessageColor();
        } else if (message.getRole() == MessageRole.ASSISTANT) {
            prefix = "Assistant";
            color = theme.getAssistantMessageColor();
        } else {
            prefix = "System";
            color = theme.getForegroundColor();
        }
        
        // 构建消息文本
        sb.append("\n");
        sb.append(prefix).append(": ");
        
        // 增强内容渲染：自动高亮代码块
        String content = enhanceContent(message.getContent());
        sb.append(content);
        sb.append("\n");
        
        return sb.toString();
    }
    
    /**
     * 增强内容渲染：处理代码块和 Diff
     */
    private String enhanceContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // 处理代码块
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (codeMatcher.find()) {
            String language = codeMatcher.group(1);
            String code = codeMatcher.group(2);
            
            // 应用语法高亮
            String highlighted = syntaxHighlighter.highlight(code, language);
            
            // 构建渲染后的代码块
            String replacement = "```" + language + "\n" + highlighted + "```";
            codeMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        codeMatcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 渲染代码块
     */
    public String renderCodeBlock(String code, String language) {
        String highlighted = syntaxHighlighter.highlight(code, language);
        return "```" + (language != null ? language : "") + "\n" + 
               highlighted + "\n" +
               "```";
    }
    
    /**
     * 渲染 Diff
     */
    public String renderDiff(String oldContent, String newContent) {
        return diffRenderer.renderStringDiff(oldContent, newContent);
    }
    
    /**
     * 渲染 Diff 摘要
     */
    public String renderDiffSummary(String oldContent, String newContent) {
        return diffRenderer.renderDiffSummary(oldContent, newContent);
    }
    
    /**
     * 渲染多条消息
     */
    public List<String> renderMessages(List<Message> messages) {
        List<String> rendered = new ArrayList<>();
        for (Message message : messages) {
            rendered.add(render(message));
        }
        return rendered;
    }
    
    /**
     * 渲染错误信息
     */
    public String renderError(String errorMessage) {
        return "\n❌ Error: " + errorMessage + "\n";
    }
    
    /**
     * 渲染成功信息
     */
    public String renderSuccess(String successMessage) {
        return "\n✓ " + successMessage + "\n";
    }
    
    /**
     * 渲染警告信息
     */
    public String renderWarning(String warningMessage) {
        return "\n⚠ Warning: " + warningMessage + "\n";
    }
    
    /**
     * 渲染信息提示
     */
    public String renderInfo(String infoMessage) {
        return "\nℹ " + infoMessage + "\n";
    }
}
