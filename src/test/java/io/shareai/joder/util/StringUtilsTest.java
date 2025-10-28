package io.shareai.joder.util;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符串工具类测试
 */
public class StringUtilsTest {

    @Test
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("test"));
    }

    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("   "));
        assertFalse(StringUtils.isBlank("test"));
    }

    @Test
    public void testTruncate() {
        assertEquals("Hello...", StringUtils.truncate("Hello World", 5));
        assertEquals("Hi", StringUtils.truncate("Hi", 10));
        assertNull(StringUtils.truncate(null, 5));
    }

    @Test
    public void testPadding() {
        assertEquals("***Hello", StringUtils.padLeft("Hello", 8, '*'));
        assertEquals("Hello***", StringUtils.padRight("Hello", 8, '*'));
    }

    @Test
    public void testRepeat() {
        assertEquals("abcabcabc", StringUtils.repeat("abc", 3));
        assertEquals("", StringUtils.repeat("abc", 0));
        assertEquals("", StringUtils.repeat(null, 3));
    }

    @Test
    public void testReverse() {
        assertEquals("cba", StringUtils.reverse("abc"));
        assertNull(StringUtils.reverse(null));
    }

    @Test
    public void testCapitalize() {
        assertEquals("Hello", StringUtils.capitalize("hello"));
        assertEquals("A", StringUtils.capitalize("a"));
        assertEquals("", StringUtils.capitalize(""));
    }

    @Test
    public void testCamelSnakeConversion() {
        assertEquals("hello_world", StringUtils.camelToSnake("helloWorld"));
        assertEquals("helloWorld", StringUtils.snakeToCamel("hello_world"));
    }

    @Test
    public void testJoin() {
        String[] array = {"a", "b", "c"};
        assertEquals("a, b, c", StringUtils.join(array, ", "));
        
        List<String> list = List.of("x", "y", "z");
        assertEquals("x-y-z", StringUtils.join(list, "-"));
    }

    @Test
    public void testSplitAndTrim() {
        List<String> result = StringUtils.splitAndTrim("a, b , c", ",");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testEqualsIgnoreCase() {
        assertTrue(StringUtils.equalsIgnoreCase("Hello", "hello"));
        assertTrue(StringUtils.equalsIgnoreCase(null, null));
        assertFalse(StringUtils.equalsIgnoreCase("a", "b"));
    }

    @Test
    public void testContainsIgnoreCase() {
        assertTrue(StringUtils.containsIgnoreCase("Hello World", "WORLD"));
        assertFalse(StringUtils.containsIgnoreCase("Hello", "xyz"));
    }

    @Test
    public void testSimilarity() {
        assertEquals(1.0, StringUtils.similarity("hello", "hello"), 0.01);
        assertTrue(StringUtils.similarity("hello", "hallo") > 0.7);
        assertTrue(StringUtils.similarity("abc", "xyz") < 0.5);
    }

    @Test
    public void testExtractNumbers() {
        List<Integer> numbers = StringUtils.extractNumbers("abc123def456");
        assertEquals(2, numbers.size());
        assertEquals(123, numbers.get(0));
        assertEquals(456, numbers.get(1));
    }

    @Test
    public void testRemoveWhitespace() {
        assertEquals("HelloWorld", StringUtils.removeWhitespace("Hello   World"));
        assertEquals("", StringUtils.removeWhitespace("   "));
    }

    @Test
    public void testNormalizeWhitespace() {
        assertEquals("Hello World", StringUtils.normalizeWhitespace("Hello   World"));
        assertEquals("a b c", StringUtils.normalizeWhitespace("  a  b  c  "));
    }

    @Test
    public void testGetByteLength() {
        assertEquals(5, StringUtils.getByteLength("Hello"));
        // 中文字符占3个字节（UTF-8）
        assertTrue(StringUtils.getByteLength("你好") > 2);
    }
}
