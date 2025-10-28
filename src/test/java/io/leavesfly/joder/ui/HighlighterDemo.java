package io.leavesfly.joder.ui;

import io.leavesfly.joder.ui.renderer.DiffRenderer;
import io.leavesfly.joder.ui.renderer.SyntaxHighlighter;

/**
 * 代码高亮和 Diff 可视化演示
 * 
 * <p>运行此类查看高亮和 Diff 效果
 */
public class HighlighterDemo {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("代码高亮 + Diff 可视化演示");
        System.out.println("=".repeat(80));
        System.out.println();
        
        demonstrateSyntaxHighlighting();
        System.out.println();
        
        demonstrateDiffRendering();
        System.out.println();
        
        demonstrateDiffSummary();
    }
    
    /**
     * 演示语法高亮
     */
    private static void demonstrateSyntaxHighlighting() {
        System.out.println("【1】语法高亮演示");
        System.out.println("-".repeat(80));
        
        SyntaxHighlighter highlighter = new SyntaxHighlighter();
        
        // Java 代码高亮
        String javaCode = 
            "public class HelloWorld {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "        int number = 42;\n" +
            "    }\n" +
            "}";
        
        System.out.println("\nJava 代码：");
        System.out.println(highlighter.highlight(javaCode, "java"));
        
        // Python 代码高亮
        String pythonCode = 
            "def fibonacci(n):\n" +
            "    if n <= 1:\n" +
            "        return n\n" +
            "    return fibonacci(n-1) + fibonacci(n-2)\n" +
            "\n" +
            "print(fibonacci(10))";
        
        System.out.println("\nPython 代码：");
        System.out.println(highlighter.highlight(pythonCode, "python"));
        
        // JavaScript 代码高亮
        String jsCode = 
            "const greeting = (name) => {\n" +
            "    console.log(`Hello, ${name}!`);\n" +
            "    return true;\n" +
            "};\n" +
            "\n" +
            "greeting('World');";
        
        System.out.println("\nJavaScript 代码：");
        System.out.println(highlighter.highlight(jsCode, "javascript"));
        
        // JSON 高亮
        String jsonCode = 
            "{\n" +
            "  \"name\": \"Joder\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"enabled\": true,\n" +
            "  \"count\": 42\n" +
            "}";
        
        System.out.println("\nJSON 数据：");
        System.out.println(highlighter.highlight(jsonCode, "json"));
    }
    
    /**
     * 演示 Diff 渲染
     */
    private static void demonstrateDiffRendering() {
        System.out.println("【2】Diff 可视化演示");
        System.out.println("-".repeat(80));
        
        DiffRenderer diffRenderer = new DiffRenderer();
        
        String oldCode = 
            "public class Calculator {\n" +
            "    public int add(int a, int b) {\n" +
            "        return a + b;\n" +
            "    }\n" +
            "    \n" +
            "    public int subtract(int a, int b) {\n" +
            "        return a - b;\n" +
            "    }\n" +
            "}";
        
        String newCode = 
            "public class Calculator {\n" +
            "    public int add(int a, int b) {\n" +
            "        return a + b;\n" +
            "    }\n" +
            "    \n" +
            "    public int subtract(int a, int b) {\n" +
            "        // 修复：确保结果不为负\n" +
            "        return Math.max(0, a - b);\n" +
            "    }\n" +
            "    \n" +
            "    public int multiply(int a, int b) {\n" +
            "        return a * b;\n" +
            "    }\n" +
            "}";
        
        System.out.println("\n统一视图 Diff：");
        System.out.println(diffRenderer.renderStringDiff(oldCode, newCode));
    }
    
    /**
     * 演示 Diff 摘要
     */
    private static void demonstrateDiffSummary() {
        System.out.println("【3】Diff 摘要演示");
        System.out.println("-".repeat(80));
        
        DiffRenderer diffRenderer = new DiffRenderer();
        
        String oldContent = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";
        String newContent = "Line 1\nLine 2 (modified)\nLine 3\nLine 5\nLine 6";
        
        System.out.println("\nDiff 摘要：");
        System.out.println(diffRenderer.renderDiffSummary(oldContent, newContent));
        
        System.out.println("\n并排视图（80列宽）：");
        System.out.println(diffRenderer.renderSideBySide(oldContent, newContent, 80));
    }
}
