package io.leavesfly.joder.ui.renderer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diff 渲染器测试
 */
public class DiffRendererTest {
    
    private DiffRenderer renderer;
    
    @BeforeEach
    public void setUp() {
        renderer = new DiffRenderer();
    }
    
    @Test
    public void testRenderStringDiff() {
        String oldContent = "line 1\nline 2\nline 3";
        String newContent = "line 1\nline 2 modified\nline 3\nline 4";
        
        String result = renderer.renderStringDiff(oldContent, newContent);
        
        assertNotNull(result);
        assertTrue(result.contains("---"));
        assertTrue(result.contains("+++"));
        assertTrue(result.contains("@@"));
    }
    
    @Test
    public void testRenderFileDiff(@TempDir Path tempDir) throws IOException {
        // 创建临时文件
        Path oldFile = tempDir.resolve("old.txt");
        Path newFile = tempDir.resolve("new.txt");
        
        Files.writeString(oldFile, "line 1\nline 2\nline 3");
        Files.writeString(newFile, "line 1\nline 2 modified\nline 3");
        
        String result = renderer.renderFileDiff(oldFile, newFile);
        
        assertNotNull(result);
        assertTrue(result.contains("old.txt"));
        assertTrue(result.contains("new.txt"));
    }
    
    @Test
    public void testRenderDiffWithLines() {
        List<String> oldLines = Arrays.asList("a", "b", "c");
        List<String> newLines = Arrays.asList("a", "b modified", "c", "d");
        
        String result = renderer.renderDiff(oldLines, newLines, "old", "new");
        
        assertNotNull(result);
        assertTrue(result.contains("old"));
        assertTrue(result.contains("new"));
    }
    
    @Test
    public void testRenderDiffNoChanges() {
        String content = "line 1\nline 2\nline 3";
        
        String result = renderer.renderStringDiff(content, content);
        
        assertNotNull(result);
        assertTrue(result.contains("No changes"));
    }
    
    @Test
    public void testRenderDiffSummary() {
        String oldContent = "line 1\nline 2\nline 3";
        String newContent = "line 1\nline 2 modified\nline 3\nline 4";
        
        String summary = renderer.renderDiffSummary(oldContent, newContent);
        
        assertNotNull(summary);
        assertTrue(summary.contains("+") || summary.contains("-"));
    }
    
    @Test
    public void testRenderDiffSummaryNoChanges() {
        String content = "line 1\nline 2";
        
        String summary = renderer.renderDiffSummary(content, content);
        
        assertNotNull(summary);
        assertTrue(summary.contains("No changes") || summary.contains("✓"));
    }
    
    @Test
    public void testRenderSideBySide() {
        String oldContent = "line 1\nline 2\nline 3";
        String newContent = "line 1\nline 2 modified\nline 3\nline 4";
        
        String result = renderer.renderSideBySide(oldContent, newContent, 80);
        
        assertNotNull(result);
        assertTrue(result.contains("ORIGINAL"));
        assertTrue(result.contains("MODIFIED"));
        assertTrue(result.contains("|"));
    }
    
    @Test
    public void testSetContextLines() {
        renderer.setContextLines(5);
        assertEquals(5, renderer.getContextLines());
    }
    
    @Test
    public void testSetShowLineNumbers() {
        renderer.setShowLineNumbers(false);
        assertFalse(renderer.isShowLineNumbers());
        
        renderer.setShowLineNumbers(true);
        assertTrue(renderer.isShowLineNumbers());
    }
    
    @Test
    public void testSetEnableColors() {
        renderer.setEnableColors(false);
        assertFalse(renderer.isEnableColors());
        
        renderer.setEnableColors(true);
        assertTrue(renderer.isEnableColors());
    }
    
    @Test
    public void testRenderDiffWithoutColors() {
        renderer.setEnableColors(false);
        
        String oldContent = "line 1\nline 2";
        String newContent = "line 1\nline 2 modified";
        
        String result = renderer.renderStringDiff(oldContent, newContent);
        
        assertNotNull(result);
        // 结果不应包含 ANSI 颜色码
        assertFalse(result.contains("\u001B["));
    }
    
    @Test
    public void testRenderDiffWithoutLineNumbers() {
        renderer.setShowLineNumbers(false);
        
        String oldContent = "line 1\nline 2";
        String newContent = "line 1\nline 2 modified";
        
        String result = renderer.renderStringDiff(oldContent, newContent);
        
        assertNotNull(result);
    }
}
