package io.shareai.joder.tools.edit;

import io.shareai.joder.tools.ToolResult;
import io.shareai.joder.ui.renderer.DiffRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BatchEditTool 测试
 */
class BatchEditToolTest {
    
    @TempDir
    Path tempDir;
    
    private BatchEditTool batchEditTool;
    private FileEditTool fileEditTool;
    
    @BeforeEach
    void setUp() {
        String workingDir = tempDir.toString();
        fileEditTool = new FileEditTool(workingDir);
        DiffRenderer diffRenderer = new DiffRenderer();
        batchEditTool = new BatchEditTool(workingDir, fileEditTool, diffRenderer);
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup if needed
    }
    
    @Test
    void testBatchEditSuccess() throws Exception {
        // 创建测试文件
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        
        Files.writeString(file1, "Hello World");
        Files.writeString(file2, "Goodbye World");
        
        // 记录文件读取
        fileEditTool.recordFileRead(file1.toString());
        fileEditTool.recordFileRead(file2.toString());
        
        // 准备批量编辑
        List<Map<String, Object>> edits = new ArrayList<>();
        
        Map<String, Object> edit1 = new HashMap<>();
        edit1.put("file_path", file1.toString());
        edit1.put("old_string", "Hello World");
        edit1.put("new_string", "Hello Java");
        edits.add(edit1);
        
        Map<String, Object> edit2 = new HashMap<>();
        edit2.put("file_path", file2.toString());
        edit2.put("old_string", "Goodbye World");
        edit2.put("new_string", "Goodbye Python");
        edits.add(edit2);
        
        Map<String, Object> input = new HashMap<>();
        input.put("edits", edits);
        
        // 执行批量编辑
        ToolResult result = batchEditTool.call(input);
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("成功: 2 个"));
        assertTrue(result.getOutput().contains("失败: 0 个"));
        
        // 验证文件内容
        assertEquals("Hello Java", Files.readString(file1));
        assertEquals("Goodbye Python", Files.readString(file2));
    }
    
    @Test
    void testBatchEditPartialFailure() throws Exception {
        // 创建测试文件
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        
        Files.writeString(file1, "Hello World");
        Files.writeString(file2, "Goodbye World");
        
        // 只记录file1的读取
        fileEditTool.recordFileRead(file1.toString());
        
        // 准备批量编辑
        List<Map<String, Object>> edits = new ArrayList<>();
        
        Map<String, Object> edit1 = new HashMap<>();
        edit1.put("file_path", file1.toString());
        edit1.put("old_string", "Hello World");
        edit1.put("new_string", "Hello Java");
        edits.add(edit1);
        
        Map<String, Object> edit2 = new HashMap<>();
        edit2.put("file_path", file2.toString());
        edit2.put("old_string", "Goodbye World");
        edit2.put("new_string", "Goodbye Python");
        edits.add(edit2);
        
        Map<String, Object> input = new HashMap<>();
        input.put("edits", edits);
        
        // 执行批量编辑
        ToolResult result = batchEditTool.call(input);
        
        // 验证结果 - 部分成功仍返回success
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("成功: 1 个"));
        assertTrue(result.getOutput().contains("失败: 1 个"));
        
        // 验证文件内容
        assertEquals("Hello Java", Files.readString(file1));
        assertEquals("Goodbye World", Files.readString(file2)); // 未修改
    }
    
    @Test
    void testBatchEditEmptyList() {
        Map<String, Object> input = new HashMap<>();
        input.put("edits", new ArrayList<>());
        
        ToolResult result = batchEditTool.call(input);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("编辑列表不能为空"));
    }
    
    @Test
    void testBatchEditNullList() {
        Map<String, Object> input = new HashMap<>();
        input.put("edits", null);
        
        ToolResult result = batchEditTool.call(input);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("编辑列表不能为空"));
    }
    
    @Test
    void testBatchEditExceedsMaxSize() {
        List<Map<String, Object>> edits = new ArrayList<>();
        
        // 创建超过最大限制的编辑列表
        for (int i = 0; i < 15; i++) {
            Map<String, Object> edit = new HashMap<>();
            edit.put("file_path", tempDir.resolve("file" + i + ".txt").toString());
            edit.put("old_string", "old");
            edit.put("new_string", "new");
            edits.add(edit);
        }
        
        Map<String, Object> input = new HashMap<>();
        input.put("edits", edits);
        
        ToolResult result = batchEditTool.call(input);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("批量操作数量超过限制"));
    }
    
    @Test
    void testBatchEditAllFailures() throws Exception {
        // 创建测试文件但不记录读取
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        
        Files.writeString(file1, "Hello World");
        Files.writeString(file2, "Goodbye World");
        
        // 不记录文件读取,导致编辑失败
        
        // 准备批量编辑
        List<Map<String, Object>> edits = new ArrayList<>();
        
        Map<String, Object> edit1 = new HashMap<>();
        edit1.put("file_path", file1.toString());
        edit1.put("old_string", "Hello World");
        edit1.put("new_string", "Hello Java");
        edits.add(edit1);
        
        Map<String, Object> edit2 = new HashMap<>();
        edit2.put("file_path", file2.toString());
        edit2.put("old_string", "Goodbye World");
        edit2.put("new_string", "Goodbye Python");
        edits.add(edit2);
        
        Map<String, Object> input = new HashMap<>();
        input.put("edits", edits);
        
        // 执行批量编辑
        ToolResult result = batchEditTool.call(input);
        
        // 全部失败应返回error
        assertFalse(result.isSuccess());
        String output = result.getError(); // 全部失败时在error中
        assertTrue(output.contains("成功: 0 个"));
        assertTrue(output.contains("失败: 2 个"));
    }
    
    @Test
    void testRenderToolUseMessage() {
        List<Map<String, Object>> edits = new ArrayList<>();
        
        Map<String, Object> edit1 = new HashMap<>();
        edit1.put("file_path", "/path/to/file1.txt");
        edits.add(edit1);
        
        Map<String, Object> edit2 = new HashMap<>();
        edit2.put("file_path", "/path/to/file2.txt");
        edits.add(edit2);
        
        Map<String, Object> input = new HashMap<>();
        input.put("edits", edits);
        
        String message = batchEditTool.renderToolUseMessage(input);
        
        assertTrue(message.contains("批量编辑文件"));
        assertTrue(message.contains("2 个文件"));
    }
    
    @Test
    void testRenderToolResultMessage_Success() {
        ToolResult result = ToolResult.success("📊 批量编辑完成\n其他信息");
        String message = batchEditTool.renderToolResultMessage(result);
        assertEquals("📊 批量编辑完成", message);
    }
    
    @Test
    void testRenderToolResultMessage_Failure() {
        ToolResult result = ToolResult.error("批量编辑失败");
        String message = batchEditTool.renderToolResultMessage(result);
        assertEquals("❌ 批量编辑失败", message);
    }
}
