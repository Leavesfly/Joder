package io.leavesfly.joder.tools.bash;

import io.leavesfly.joder.tools.AbstractTool;
import io.leavesfly.joder.tools.ToolResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Bash 命令执行工具
 */
public class BashTool extends AbstractTool {
    
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    
    @Override
    public String getName() {
        return "BashTool";
    }
    
    @Override
    public String getDescription() {
        return "执行 Bash 命令";
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        try {
            // 验证必需参数
            requireParameter(input, "command");
            
            String command = getString(input, "command");
            String workingDir = getString(input, "workingDir", System.getProperty("user.dir"));
            int timeout = getInt(input, "timeout", DEFAULT_TIMEOUT_SECONDS);
            
            // 检查危险命令
            if (isDangerousCommand(command)) {
                return ToolResult.error("拒绝执行危险命令: " + command);
            }
            
            // 构建进程
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sh", "-c", command);
            processBuilder.directory(new java.io.File(workingDir));
            processBuilder.redirectErrorStream(true);
            
            // 启动进程
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待进程完成
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return ToolResult.error("命令执行超时（" + timeout + "秒）");
            }
            
            int exitCode = process.exitValue();
            
            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("命令: ").append(command).append("\n");
            result.append("工作目录: ").append(workingDir).append("\n");
            result.append("退出码: ").append(exitCode).append("\n");
            result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            result.append(output);
            
            if (exitCode == 0) {
                return ToolResult.success(result.toString());
            } else {
                return ToolResult.error(result.toString());
            }
            
        } catch (IOException e) {
            logger.error("Failed to execute command", e);
            return ToolResult.error("执行命令失败: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Command execution interrupted", e);
            Thread.currentThread().interrupt();
            return ToolResult.error("命令执行被中断");
        } catch (Exception e) {
            logger.error("Unexpected error in BashTool", e);
            return ToolResult.error("发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否为危险命令
     */
    private boolean isDangerousCommand(String command) {
        String lowerCmd = command.toLowerCase().trim();
        
        // 危险命令模式
        String[] dangerousPatterns = {
            "rm -rf /",
            "sudo rm",
            ":(){ :|:& };:",  // fork 炸弹
            "mkfs",
            "dd if=/dev/zero",
            "> /dev/sda"
        };
        
        for (String pattern : dangerousPatterns) {
            if (lowerCmd.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        String command = getString(input, "command");
        String workingDir = getString(input, "workingDir");
        
        StringBuilder sb = new StringBuilder();
        sb.append("▶ 执行命令: ").append(command);
        
        if (workingDir != null) {
            sb.append("\n  工作目录: ").append(workingDir);
        }
        
        return sb.toString();
    }
}
