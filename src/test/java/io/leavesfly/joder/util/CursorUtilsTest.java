package io.leavesfly.joder.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CursorUtils 测试
 */
public class CursorUtilsTest {
    
    @Test
    public void testBasicCursorCreation() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello World", 80);
        
        assertEquals("Hello World", cursor.getText());
        assertEquals(0, cursor.getOffset());
        assertEquals(80, cursor.getColumns());
    }
    
    @Test
    public void testCursorMovement() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello World", 80, 0);
        
        // 向右移动
        CursorUtils.Cursor right = cursor.right();
        assertEquals(1, right.getOffset());
        
        // 向左移动
        CursorUtils.Cursor left = right.left();
        assertEquals(0, left.getOffset());
    }
    
    @Test
    public void testLineOperations() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Line 1\nLine 2\nLine 3", 80, 0);
        
        // 移动到行尾
        CursorUtils.Cursor endOfLine = cursor.endOfLine();
        assertEquals(6, endOfLine.getOffset()); // "Line 1" 的长度
        
        // 移动到行首
        CursorUtils.Cursor startOfLine = endOfLine.startOfLine();
        assertEquals(0, startOfLine.getOffset());
    }
    
    @Test
    public void testTextInsertion() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello World", 80, 5);
        
        CursorUtils.Cursor inserted = cursor.insert(" Beautiful");
        assertEquals("Hello Beautiful World", inserted.getText());
        assertEquals(15, inserted.getOffset()); // 5 + 10
    }
    
    @Test
    public void testBackspace() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello World", 80, 5);
        
        CursorUtils.Cursor afterBackspace = cursor.backspace();
        // 在位置5（"o"之后）执行 backspace，删除"o"
        assertEquals("Hell World", afterBackspace.getText());
        assertEquals(4, afterBackspace.getOffset());
    }
    
    @Test
    public void testDelete() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello World", 80, 5);
        
        CursorUtils.Cursor afterDelete = cursor.delete();
        assertEquals("HelloWorld", afterDelete.getText());
        assertEquals(5, afterDelete.getOffset());
    }
    
    @Test
    public void testWordOperations() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello beautiful world", 80, 0);
        
        // 移动到下一个单词
        CursorUtils.Cursor nextWord = cursor.nextWord();
        assertTrue(nextWord.getOffset() > 0);
        
        // 移动到上一个单词
        CursorUtils.Cursor prevWord = nextWord.prevWord();
        assertEquals(0, prevWord.getOffset());
    }
    
    @Test
    public void testDeleteWord() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello beautiful world", 80, 6); // 在 "beautiful" 开头
        
        CursorUtils.Cursor afterDelete = cursor.deleteWordAfter();
        assertTrue(afterDelete.getText().length() < "Hello beautiful world".length());
    }
    
    @Test
    public void testMultilineOperations() {
        String text = "Line 1\nLine 2\nLine 3";
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText(text, 80, 7); // Line 2 的开头
        
        // 向上移动
        CursorUtils.Cursor up = cursor.up();
        CursorUtils.Position upPos = up.getPosition();
        assertEquals(0, upPos.getLine());
        
        // 向下移动
        CursorUtils.Cursor down = cursor.down();
        CursorUtils.Position downPos = down.getPosition();
        assertEquals(2, downPos.getLine());
    }
    
    @Test
    public void testPosition() {
        String text = "Line 1\nLine 2\nLine 3";
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText(text, 80, 7); // "L" in "Line 2"
        
        CursorUtils.Position pos = cursor.getPosition();
        assertEquals(1, pos.getLine()); // 第二行（从0开始）
        assertEquals(0, pos.getColumn()); // 第一列
    }
    
    @Test
    public void testIsAtStartEnd() {
        CursorUtils.Cursor cursor = CursorUtils.Cursor.fromText("Hello", 80, 0);
        
        assertTrue(cursor.isAtStart());
        assertFalse(cursor.isAtEnd());
        
        CursorUtils.Cursor atEnd = CursorUtils.Cursor.fromText("Hello", 80, 5);
        assertFalse(atEnd.isAtStart());
        assertTrue(atEnd.isAtEnd());
    }
    
    @Test
    public void testEquals() {
        CursorUtils.Cursor cursor1 = CursorUtils.Cursor.fromText("Hello", 80, 2);
        CursorUtils.Cursor cursor2 = CursorUtils.Cursor.fromText("Hello", 80, 2);
        CursorUtils.Cursor cursor3 = CursorUtils.Cursor.fromText("Hello", 80, 3);
        
        assertTrue(cursor1.equals(cursor2));
        assertFalse(cursor1.equals(cursor3));
    }
}
