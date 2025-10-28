package io.shareai.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 光标工具类
 * 对应 Kode 的 Cursor.ts
 * 
 * 简化实现，提供核心的光标操作功能
 */
public class CursorUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(CursorUtils.class);
    
    /**
     * 光标类
     */
    public static class Cursor {
        private final String text;
        private final int offset;
        private final int columns;
        
        public Cursor(String text, int columns, int offset) {
            this.text = text != null ? text : "";
            this.columns = Math.max(1, columns);
            this.offset = Math.max(0, Math.min(this.text.length(), offset));
        }
        
        public static Cursor fromText(String text, int columns) {
            return new Cursor(text, columns, 0);
        }
        
        public static Cursor fromText(String text, int columns, int offset) {
            return new Cursor(text, columns, offset);
        }
        
        // Getters
        
        public String getText() {
            return text;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public int getColumns() {
            return columns;
        }
        
        // Position calculations
        
        /**
         * 获取当前光标位置（行和列）
         */
        public Position getPosition() {
            int line = 0;
            int column = 0;
            
            for (int i = 0; i < offset && i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    column = 0;
                } else {
                    column++;
                }
            }
            
            return new Position(line, column);
        }
        
        /**
         * 从位置获取偏移量
         */
        public int getOffsetFromPosition(Position pos) {
            int currentLine = 0;
            int offset = 0;
            
            for (int i = 0; i < text.length(); i++) {
                if (currentLine == pos.line) {
                    if (offset == pos.column) {
                        return i;
                    }
                    offset++;
                }
                
                if (text.charAt(i) == '\n') {
                    currentLine++;
                    offset = 0;
                }
            }
            
            return text.length();
        }
        
        // Movement operations
        
        public Cursor left() {
            return new Cursor(text, columns, offset - 1);
        }
        
        public Cursor right() {
            return new Cursor(text, columns, offset + 1);
        }
        
        public Cursor up() {
            Position pos = getPosition();
            if (pos.line == 0) {
                return new Cursor(text, columns, 0);
            }
            
            int newOffset = getOffsetFromPosition(new Position(pos.line - 1, pos.column));
            return new Cursor(text, columns, newOffset);
        }
        
        public Cursor down() {
            Position pos = getPosition();
            int lineCount = text.split("\n", -1).length;
            
            if (pos.line >= lineCount - 1) {
                return new Cursor(text, columns, text.length());
            }
            
            int newOffset = getOffsetFromPosition(new Position(pos.line + 1, pos.column));
            return new Cursor(text, columns, newOffset);
        }
        
        public Cursor startOfLine() {
            Position pos = getPosition();
            int newOffset = getOffsetFromPosition(new Position(pos.line, 0));
            return new Cursor(text, columns, newOffset);
        }
        
        public Cursor endOfLine() {
            Position pos = getPosition();
            String[] lines = text.split("\n", -1);
            
            if (pos.line < lines.length) {
                int lineLength = lines[pos.line].length();
                int newOffset = getOffsetFromPosition(new Position(pos.line, lineLength));
                return new Cursor(text, columns, newOffset);
            }
            
            return new Cursor(text, columns, text.length());
        }
        
        public Cursor nextWord() {
            Cursor cursor = this;
            
            // 跳过当前单词字符
            while (!cursor.isAtEnd() && cursor.isOverWordChar()) {
                cursor = cursor.right();
            }
            
            // 跳过非单词字符
            while (!cursor.isAtEnd() && !cursor.isOverWordChar()) {
                cursor = cursor.right();
            }
            
            return cursor;
        }
        
        public Cursor prevWord() {
            Cursor cursor = this;
            
            // 如果已经在单词开头，先退一步
            if (!cursor.left().isOverWordChar()) {
                cursor = cursor.left();
            }
            
            // 跳过非单词字符
            while (!cursor.isAtStart() && !cursor.isOverWordChar()) {
                cursor = cursor.left();
            }
            
            // 移动到单词开头
            if (cursor.isOverWordChar()) {
                while (!cursor.isAtStart() && cursor.left().isOverWordChar()) {
                    cursor = cursor.left();
                }
            }
            
            return cursor;
        }
        
        // Edit operations
        
        public Cursor insert(String str) {
            String newText = text.substring(0, offset) + str + text.substring(offset);
            return new Cursor(newText, columns, offset + str.length());
        }
        
        public Cursor delete() {
            if (isAtEnd()) {
                return this;
            }
            
            String newText = text.substring(0, offset) + text.substring(offset + 1);
            return new Cursor(newText, columns, offset);
        }
        
        public Cursor backspace() {
            if (isAtStart()) {
                return this;
            }
            
            String newText = text.substring(0, offset - 1) + text.substring(offset);
            return new Cursor(newText, columns, offset - 1);
        }
        
        public Cursor deleteToLineStart() {
            return startOfLine().deleteRange(offset);
        }
        
        public Cursor deleteToLineEnd() {
            // 如果在换行符上，只删除换行符
            if (offset < text.length() && text.charAt(offset) == '\n') {
                return delete();
            }
            
            return deleteRange(endOfLine().offset);
        }
        
        public Cursor deleteWordBefore() {
            if (isAtStart()) {
                return this;
            }
            
            Cursor wordStart = prevWord();
            return wordStart.deleteRange(offset);
        }
        
        public Cursor deleteWordAfter() {
            if (isAtEnd()) {
                return this;
            }
            
            Cursor wordEnd = nextWord();
            return deleteRange(wordEnd.offset);
        }
        
        /**
         * 删除从当前位置到指定偏移量的文本
         */
        private Cursor deleteRange(int endOffset) {
            int start = Math.min(offset, endOffset);
            int end = Math.max(offset, endOffset);
            
            String newText = text.substring(0, start) + text.substring(end);
            return new Cursor(newText, columns, start);
        }
        
        // Helper methods
        
        public boolean isAtStart() {
            return offset <= 0;
        }
        
        public boolean isAtEnd() {
            return offset >= text.length();
        }
        
        public boolean isOverWordChar() {
            if (offset >= text.length()) {
                return false;
            }
            
            char c = text.charAt(offset);
            return Character.isLetterOrDigit(c) || c == '_';
        }
        
        public boolean equals(Cursor other) {
            if (other == null) {
                return false;
            }
            return this.offset == other.offset && this.text.equals(other.text);
        }
        
        @Override
        public String toString() {
            Position pos = getPosition();
            return String.format("Cursor{line=%d, col=%d, offset=%d, text.length=%d}",
                pos.line, pos.column, offset, text.length());
        }
    }
    
    /**
     * 位置类（行和列）
     */
    public static class Position {
        private final int line;
        private final int column;
        
        public Position(int line, int column) {
            this.line = line;
            this.column = column;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }
        
        @Override
        public String toString() {
            return String.format("(%d:%d)", line, column);
        }
    }
}
