package io.shareai.joder.tools.edit;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 大文件优化处理器
 * 对大文件进行分块处理和增量更新
 */
public class LargeFileOptimizer {
    
    private static final int LARGE_FILE_THRESHOLD = 1024 * 1024; // 1MB
    private static final int CHUNK_SIZE = 64 * 1024; // 64KB
    
    /**
     * 判断是否为大文件
     * 
     * @param filePath 文件路径
     * @return 是否为大文件
     */
    public boolean isLargeFile(Path filePath) throws IOException {
        return Files.size(filePath) > LARGE_FILE_THRESHOLD;
    }
    
    /**
     * 优化的大文件编辑
     * 使用内存映射和分块处理
     * 
     * @param filePath 文件路径
     * @param oldString 要替换的字符串
     * @param newString 新字符串
     * @param encoding 文件编码
     * @return 编辑结果
     */
    public EditResult editLargeFile(Path filePath, String oldString, 
                                    String newString, Charset encoding) throws IOException {
        
        long fileSize = Files.size(filePath);
        
        // 读取文件内容（分块）
        StringBuilder content = new StringBuilder((int) fileSize);
        
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
             FileChannel channel = raf.getChannel()) {
            
            ByteBuffer buffer = ByteBuffer.allocate(CHUNK_SIZE);
            
            while (channel.read(buffer) > 0) {
                buffer.flip();
                content.append(encoding.decode(buffer));
                buffer.clear();
            }
        }
        
        String originalContent = content.toString();
        
        // 检查是否包含要替换的字符串
        if (!originalContent.contains(oldString)) {
            return new EditResult(false, "文件中未找到要替换的字符串", 0, 0);
        }
        
        // 执行替换
        String updatedContent = originalContent.replace(oldString, newString);
        
        // 写回文件（分块）
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw");
             FileChannel channel = raf.getChannel()) {
            
            // 清空文件
            channel.truncate(0);
            
            // 分块写入
            byte[] bytes = updatedContent.getBytes(encoding);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
        
        long newSize = Files.size(filePath);
        
        return new EditResult(true, "大文件编辑成功", fileSize, newSize);
    }
    
    /**
     * 增量更新：仅修改变更部分
     * 适用于文件中只有少量内容变更的情况
     * 
     * @param filePath 文件路径
     * @param oldString 要替换的字符串
     * @param newString 新字符串
     * @param encoding 文件编码
     * @return 编辑结果
     */
    public EditResult incrementalEdit(Path filePath, String oldString, 
                                      String newString, Charset encoding) throws IOException {
        
        // 对于增量编辑，仍然需要读取全文件来定位替换位置
        // 这是一个简化实现，真正的增量编辑需要更复杂的算法
        
        String content = Files.readString(filePath, encoding);
        
        int oldIndex = content.indexOf(oldString);
        if (oldIndex == -1) {
            return new EditResult(false, "文件中未找到要替换的字符串", 0, 0);
        }
        
        // 计算替换前后的大小差异
        int sizeDiff = newString.length() - oldString.length();
        
        if (sizeDiff == 0) {
            // 大小相同，可以原地替换
            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
                raf.seek(oldIndex);
                raf.write(newString.getBytes(encoding));
            }
        } else {
            // 大小不同，需要重写文件
            String updatedContent = content.replace(oldString, newString);
            Files.writeString(filePath, updatedContent, encoding);
        }
        
        long newSize = Files.size(filePath);
        
        return new EditResult(true, "增量编辑成功", content.length(), newSize);
    }
    
    /**
     * 编辑结果
     */
    public static class EditResult {
        private final boolean success;
        private final String message;
        private final long oldSize;
        private final long newSize;
        
        public EditResult(boolean success, String message, long oldSize, long newSize) {
            this.success = success;
            this.message = message;
            this.oldSize = oldSize;
            this.newSize = newSize;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getOldSize() {
            return oldSize;
        }
        
        public long getNewSize() {
            return newSize;
        }
        
        public long getSizeDiff() {
            return newSize - oldSize;
        }
        
        @Override
        public String toString() {
            return String.format("EditResult{success=%s, message='%s', oldSize=%d, newSize=%d, diff=%+d}", 
                success, message, oldSize, newSize, getSizeDiff());
        }
    }
}
