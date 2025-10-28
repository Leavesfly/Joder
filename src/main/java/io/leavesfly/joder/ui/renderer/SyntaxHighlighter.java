package io.leavesfly.joder.ui.renderer;

import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代码语法高亮渲染器
 * 
 * <p>支持多种编程语言的语法高亮显示，使用 ANSI 颜色码。
 * 
 * <p>支持的语言：
 * <ul>
 *   <li>Java</li>
 *   <li>Python</li>
 *   <li>JavaScript/TypeScript</li>
 *   <li>JSON</li>
 *   <li>Shell/Bash</li>
 *   <li>Markdown</li>
 *   <li>其他（基础高亮）</li>
 * </ul>
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class SyntaxHighlighter {
    
    private static final Logger logger = LoggerFactory.getLogger(SyntaxHighlighter.class);
    
    /** 是否启用高亮（默认启用） */
    private boolean enabled = true;
    
    /** 颜色方案 */
    private final ColorScheme colorScheme;
    
    /**
     * 颜色方案
     */
    public static class ColorScheme {
        public final Ansi.Color keyword;
        public final Ansi.Color string;
        public final Ansi.Color comment;
        public final Ansi.Color number;
        public final Ansi.Color type;
        public final Ansi.Color function;
        public final Ansi.Color operator;
        
        public ColorScheme(Ansi.Color keyword, Ansi.Color string, Ansi.Color comment,
                          Ansi.Color number, Ansi.Color type, Ansi.Color function,
                          Ansi.Color operator) {
            this.keyword = keyword;
            this.string = string;
            this.comment = comment;
            this.number = number;
            this.type = type;
            this.function = function;
            this.operator = operator;
        }
        
        /** 默认深色主题 */
        public static ColorScheme dark() {
            return new ColorScheme(
                Ansi.Color.MAGENTA,  // keyword
                Ansi.Color.GREEN,    // string
                Ansi.Color.CYAN,     // comment
                Ansi.Color.YELLOW,   // number
                Ansi.Color.BLUE,     // type
                Ansi.Color.YELLOW,   // function
                Ansi.Color.WHITE     // operator
            );
        }
    }
    
    public SyntaxHighlighter() {
        this(ColorScheme.dark());
    }
    
    public SyntaxHighlighter(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }
    
    /**
     * 高亮代码
     * 
     * @param code 源代码
     * @param language 语言（java, python, javascript, etc.）
     * @return 高亮后的代码
     */
    public String highlight(String code, String language) {
        if (!enabled || code == null || code.isEmpty()) {
            return code;
        }
        
        String lang = language != null ? language.toLowerCase() : "";
        
        try {
            switch (lang) {
                case "java":
                    return highlightJava(code);
                case "python":
                case "py":
                    return highlightPython(code);
                case "javascript":
                case "js":
                case "typescript":
                case "ts":
                    return highlightJavaScript(code);
                case "json":
                    return highlightJson(code);
                case "bash":
                case "sh":
                case "shell":
                    return highlightShell(code);
                case "markdown":
                case "md":
                    return highlightMarkdown(code);
                default:
                    return highlightGeneric(code);
            }
        } catch (Exception e) {
            logger.error("Failed to highlight code for language: {}", language, e);
            return code;
        }
    }
    
    /**
     * 高亮 Java 代码
     */
    private String highlightJava(String code) {
        String result = code;
        
        // Java 关键字
        String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "var", "record", "sealed"
        };
        
        // 高亮关键字
        for (String kw : keywords) {
            result = result.replaceAll("\\b(" + kw + ")\\b",
                    Ansi.ansi().fg(colorScheme.keyword).a("$1").reset().toString());
        }
        
        // 高亮字符串
        result = highlightStrings(result);
        
        // 高亮注释
        result = highlightComments(result);
        
        // 高亮数字
        result = highlightNumbers(result);
        
        return result;
    }
    
    /**
     * 高亮 Python 代码
     */
    private String highlightPython(String code) {
        String result = code;
        
        // Python 关键字
        String[] keywords = {
            "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "False", "finally", "for",
            "from", "global", "if", "import", "in", "is", "lambda", "None",
            "nonlocal", "not", "or", "pass", "raise", "return", "True", "try",
            "while", "with", "yield"
        };
        
        for (String kw : keywords) {
            result = result.replaceAll("\\b(" + kw + ")\\b",
                    Ansi.ansi().fg(colorScheme.keyword).a("$1").reset().toString());
        }
        
        result = highlightStrings(result);
        result = highlightComments(result, "#");
        result = highlightNumbers(result);
        
        return result;
    }
    
    /**
     * 高亮 JavaScript/TypeScript 代码
     */
    private String highlightJavaScript(String code) {
        String result = code;
        
        String[] keywords = {
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "export", "extends",
            "false", "finally", "for", "function", "if", "import", "in", "instanceof",
            "let", "new", "null", "return", "static", "super", "switch", "this",
            "throw", "true", "try", "typeof", "var", "void", "while", "with", "yield",
            "interface", "type", "enum", "namespace", "readonly", "private", "public"
        };
        
        for (String kw : keywords) {
            result = result.replaceAll("\\b(" + kw + ")\\b",
                    Ansi.ansi().fg(colorScheme.keyword).a("$1").reset().toString());
        }
        
        result = highlightStrings(result);
        result = highlightComments(result);
        result = highlightNumbers(result);
        
        return result;
    }
    
    /**
     * 高亮 JSON
     */
    private String highlightJson(String code) {
        String result = code;
        
        // 高亮键（带引号的字符串后跟冒号）
        result = result.replaceAll("\"([^\"]+)\"\\s*:",
                Ansi.ansi().fg(Ansi.Color.CYAN).a("\"$1\"").reset().toString() + ":");
        
        // 高亮字符串值
        result = highlightStrings(result);
        
        // 高亮数字和布尔值
        result = result.replaceAll("\\b(true|false|null)\\b",
                Ansi.ansi().fg(colorScheme.keyword).a("$1").reset().toString());
        result = highlightNumbers(result);
        
        return result;
    }
    
    /**
     * 高亮 Shell 代码
     */
    private String highlightShell(String code) {
        String result = code;
        
        String[] keywords = {
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while",
            "do", "done", "function", "return", "exit", "break", "continue"
        };
        
        for (String kw : keywords) {
            result = result.replaceAll("\\b(" + kw + ")\\b",
                    Ansi.ansi().fg(colorScheme.keyword).a("$1").reset().toString());
        }
        
        result = highlightStrings(result);
        result = highlightComments(result, "#");
        
        return result;
    }
    
    /**
     * 高亮 Markdown
     */
    private String highlightMarkdown(String code) {
        String result = code;
        
        // 标题
        result = result.replaceAll("^(#{1,6})\\s+(.+)$",
                Ansi.ansi().fg(Ansi.Color.CYAN).a("$1 $2").reset().toString());
        
        // 粗体
        result = result.replaceAll("\\*\\*([^\\*]+)\\*\\*",
                Ansi.ansi().bold().a("$1").boldOff().reset().toString());
        
        // 斜体
        result = result.replaceAll("\\*([^\\*]+)\\*",
                Ansi.ansi().fg(Ansi.Color.YELLOW).a("$1").reset().toString());
        
        // 代码块
        result = result.replaceAll("`([^`]+)`",
                Ansi.ansi().fg(colorScheme.string).a("`$1`").reset().toString());
        
        return result;
    }
    
    /**
     * 通用高亮（基础）
     */
    private String highlightGeneric(String code) {
        String result = code;
        
        result = highlightStrings(result);
        result = highlightComments(result);
        result = highlightNumbers(result);
        
        return result;
    }
    
    /**
     * 高亮字符串
     */
    private String highlightStrings(String code) {
        // 双引号字符串
        String result = code.replaceAll("\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"",
                Ansi.ansi().fg(colorScheme.string).a("\"$1\"").reset().toString());
        
        // 单引号字符串
        result = result.replaceAll("'([^'\\\\]*(\\\\.[^'\\\\]*)*)+'",
                Ansi.ansi().fg(colorScheme.string).a("'$1'").reset().toString());
        
        return result;
    }
    
    /**
     * 高亮注释
     */
    private String highlightComments(String code) {
        return highlightComments(code, "//");
    }
    
    /**
     * 高亮注释（自定义前缀）
     */
    private String highlightComments(String code, String prefix) {
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int commentIndex = line.indexOf(prefix);
            
            if (commentIndex >= 0) {
                String before = line.substring(0, commentIndex);
                String comment = line.substring(commentIndex);
                result.append(before);
                result.append(Ansi.ansi().fg(colorScheme.comment).a(comment).reset());
            } else {
                result.append(line);
            }
            
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * 高亮数字
     */
    private String highlightNumbers(String code) {
        return code.replaceAll("\\b(\\d+\\.?\\d*)\\b",
                Ansi.ansi().fg(colorScheme.number).a("$1").reset().toString());
    }
    
    /**
     * 启用/禁用高亮
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
