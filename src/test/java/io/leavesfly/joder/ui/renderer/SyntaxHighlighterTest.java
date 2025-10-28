package io.leavesfly.joder.ui.renderer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 语法高亮渲染器测试
 */
public class SyntaxHighlighterTest {
    
    private SyntaxHighlighter highlighter;
    
    @BeforeEach
    public void setUp() {
        highlighter = new SyntaxHighlighter();
    }
    
    @Test
    public void testHighlightJava() {
        String code = "public class Test {\n    private int value;\n}";
        String result = highlighter.highlight(code, "java");
        
        assertNotNull(result);
        assertTrue(result.contains("class"));
        assertTrue(result.contains("private"));
    }
    
    @Test
    public void testHighlightPython() {
        String code = "def hello():\n    return True";
        String result = highlighter.highlight(code, "python");
        
        assertNotNull(result);
        assertTrue(result.contains("def"));
        assertTrue(result.contains("return"));
    }
    
    @Test
    public void testHighlightJavaScript() {
        String code = "const x = 42;\nfunction test() { return x; }";
        String result = highlighter.highlight(code, "javascript");
        
        assertNotNull(result);
        assertTrue(result.contains("const"));
        assertTrue(result.contains("function"));
    }
    
    @Test
    public void testHighlightJson() {
        String code = "{\n  \"name\": \"test\",\n  \"value\": 123\n}";
        String result = highlighter.highlight(code, "json");
        
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }
    
    @Test
    public void testHighlightShell() {
        String code = "#!/bin/bash\nif [ -f test.txt ]; then\n  echo \"found\"\nfi";
        String result = highlighter.highlight(code, "bash");
        
        assertNotNull(result);
        assertTrue(result.contains("if"));
        assertTrue(result.contains("fi"));
    }
    
    @Test
    public void testHighlightMarkdown() {
        String code = "# Title\n\n**Bold** and *italic*\n\n`code`";
        String result = highlighter.highlight(code, "markdown");
        
        assertNotNull(result);
    }
    
    @Test
    public void testDisableHighlight() {
        highlighter.setEnabled(false);
        String code = "public class Test {}";
        String result = highlighter.highlight(code, "java");
        
        assertEquals(code, result);
    }
    
    @Test
    public void testNullCode() {
        String result = highlighter.highlight(null, "java");
        assertNull(result);
    }
    
    @Test
    public void testEmptyCode() {
        String result = highlighter.highlight("", "java");
        assertEquals("", result);
    }
    
    @Test
    public void testUnknownLanguage() {
        String code = "some code";
        String result = highlighter.highlight(code, "unknown");
        
        assertNotNull(result);
    }
}
