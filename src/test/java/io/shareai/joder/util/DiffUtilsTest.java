package io.shareai.joder.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DiffUtils 测试
 */
public class DiffUtilsTest {
    
    @Test
    public void testContentEquals() {
        assertTrue(DiffUtils.contentEquals("Hello", "Hello"));
        assertFalse(DiffUtils.contentEquals("Hello", "World"));
        assertTrue(DiffUtils.contentEquals(null, null));
        assertFalse(DiffUtils.contentEquals("Hello", null));
    }
    
    @Test
    public void testCalculateSimilarity() {
        // 完全相同
        assertEquals(1.0, DiffUtils.calculateSimilarity("Hello", "Hello"), 0.01);
        
        // 完全不同
        double similarity = DiffUtils.calculateSimilarity("Hello", "World");
        assertTrue(similarity < 1.0);
        assertTrue(similarity >= 0.0);
        
        // 部分相同
        double partialSimilarity = DiffUtils.calculateSimilarity("Hello World", "Hello Java");
        assertTrue(partialSimilarity > 0.5);
        assertTrue(partialSimilarity < 1.0);
    }
    
    @Test
    public void testGetPatch() {
        String fileContents = "Line 1\nLine 2\nLine 3\n";
        String oldStr = "Line 2";
        String newStr = "Modified Line 2";
        
        var hunks = DiffUtils.getPatch("test.txt", fileContents, oldStr, newStr);
        
        assertNotNull(hunks);
        assertFalse(hunks.isEmpty());
        
        DiffUtils.DiffHunk hunk = hunks.get(0);
        assertNotNull(hunk.getLines());
        assertTrue(hunk.getLines().size() > 0);
        
        System.out.println("Diff hunk:");
        System.out.println(hunk);
    }
    
    @Test
    public void testGetUnifiedDiff() {
        String fileContents = "Line 1\nLine 2\nLine 3\n";
        String oldStr = "Line 2";
        String newStr = "Modified Line 2";
        
        String diff = DiffUtils.getUnifiedDiff("test.txt", fileContents, oldStr, newStr);
        
        assertNotNull(diff);
        assertTrue(diff.contains("test.txt"));
        assertTrue(diff.contains("-") || diff.contains("+")); // 应该包含 diff 标记
        
        System.out.println("Unified diff:");
        System.out.println(diff);
    }
    
    @Test
    public void testPatchNotFound() {
        String fileContents = "Line 1\nLine 2\nLine 3\n";
        String oldStr = "NonExistent";
        String newStr = "Replacement";
        
        var hunks = DiffUtils.getPatch("test.txt", fileContents, oldStr, newStr);
        
        // 如果找不到要替换的内容，应该返回空列表
        assertTrue(hunks.isEmpty());
    }
}
