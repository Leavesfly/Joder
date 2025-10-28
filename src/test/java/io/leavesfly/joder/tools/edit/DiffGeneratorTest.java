package io.leavesfly.joder.tools.edit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DiffGenerator 测试
 */
class DiffGeneratorTest {
    
    private DiffGenerator diffGenerator;
    
    @BeforeEach
    void setUp() {
        diffGenerator = new DiffGenerator();
    }
    
    @Test
    void testGenerateSideBySideDiff_SimpleChange() {
        String oldString = "Hello World";
        String newString = "Hello Java";
        
        String diff = diffGenerator.generateSideBySideDiff("", oldString, newString);
        
        assertNotNull(diff);
        assertTrue(diff.contains("- Hello World"));
        assertTrue(diff.contains("+ Hello Java"));
    }
    
    @Test
    void testGenerateSideBySideDiff_MultiLineChange() {
        String oldString = "line1\nline2\nline3";
        String newString = "line1\nmodified line2\nline3";
        
        String diff = diffGenerator.generateSideBySideDiff("", oldString, newString);
        
        assertNotNull(diff);
        assertTrue(diff.contains("- line2"));
        assertTrue(diff.contains("+ modified line2"));
    }
    
    @Test
    void testGenerateSideBySideDiff_Addition() {
        String oldString = "line1\nline2";
        String newString = "line1\nline2\nline3";
        
        String diff = diffGenerator.generateSideBySideDiff("", oldString, newString);
        
        assertNotNull(diff);
        assertTrue(diff.contains("+ line3"));
    }
    
    @Test
    void testGenerateSideBySideDiff_Deletion() {
        String oldString = "line1\nline2\nline3";
        String newString = "line1\nline3";
        
        String diff = diffGenerator.generateSideBySideDiff("", oldString, newString);
        
        assertNotNull(diff);
        assertTrue(diff.contains("- line2"));
    }
    
    @Test
    void testGenerateStats_Addition() {
        String oldString = "line1\nline2";
        String newString = "line1\nline2\nline3\nline4";
        
        String stats = diffGenerator.generateStats(oldString, newString);
        
        assertNotNull(stats);
        assertTrue(stats.contains("-2"));
        assertTrue(stats.contains("+4"));
    }
    
    @Test
    void testGenerateStats_Deletion() {
        String oldString = "line1\nline2\nline3";
        String newString = "line1";
        
        String stats = diffGenerator.generateStats(oldString, newString);
        
        assertNotNull(stats);
        assertTrue(stats.contains("-3"));
        assertTrue(stats.contains("+1"));
    }
    
    @Test
    void testGenerateStats_EmptyOldString() {
        String oldString = "";
        String newString = "new content\nline2";
        
        String stats = diffGenerator.generateStats(oldString, newString);
        
        assertNotNull(stats);
        assertTrue(stats.contains("+2"));
    }
    
    @Test
    void testGenerateStats_EmptyNewString() {
        String oldString = "old content\nline2";
        String newString = "";
        
        String stats = diffGenerator.generateStats(oldString, newString);
        
        assertNotNull(stats);
        assertTrue(stats.contains("-2"));
    }
    
    @Test
    void testGenerateDiff_SimpleFile() {
        String original = "line1\nline2\nline3\nline4\nline5";
        String oldStr = "line3";
        String newStr = "modified line3";
        
        String diff = diffGenerator.generateDiff(original, oldStr, newStr, "test.txt");
        
        assertNotNull(diff);
        assertTrue(diff.contains("--- test.txt"));
        assertTrue(diff.contains("+++ test.txt"));
        assertTrue(diff.contains("-line3"));
        assertTrue(diff.contains("+modified line3"));
    }
    
    @Test
    void testGenerateDiff_WithContext() {
        String original = "line1\nline2\nline3\nline4\nline5\nline6\nline7";
        String oldStr = "line4";
        String newStr = "changed line4";
        
        String diff = diffGenerator.generateDiff(original, oldStr, newStr, "file.txt");
        
        assertNotNull(diff);
        // 应该包含上下文行
        assertTrue(diff.contains("line1") || diff.contains("line2") || diff.contains("line3"));
        assertTrue(diff.contains("line5") || diff.contains("line6") || diff.contains("line7"));
    }
    
    @Test
    void testGenerateDiff_NoChange() {
        String original = "line1\nline2\nline3";
        String oldStr = "nonexistent";
        String newStr = "replacement";
        
        String diff = diffGenerator.generateDiff(original, oldStr, newStr, "test.txt");
        
        // 由于oldStr不存在,replace不会生效,内容不变
        assertEquals("无变更", diff);
    }
}
