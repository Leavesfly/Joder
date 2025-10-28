package io.shareai.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 进程执行工具类
 * 对应 Kode 的 execFileNoThrow.ts
 */
public class ProcessUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);
    
    /**
     * 进程执行结果
     */
    public static class ProcessResult {
        private final String stdout;
        private final String stderr;
        private final int exitCode;
        
        public ProcessResult(String stdout, String stderr, int exitCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
        }
        
        public String getStdout() {
            return stdout;
        }
        
        public String getStderr() {
            return stderr;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public boolean isSuccess() {
            return exitCode == 0;
        }
        
        @Override
        public String toString() {
            return String.format("ProcessResult{exitCode=%d, stdout=%s, stderr=%s}",
                exitCode, stdout, stderr);
        }
    }
    
    /**
     * 执行命令（不抛出异常）
     * 
     * @param command 命令
     * @param args 参数
     * @return 执行结果
     */
    public static ProcessResult execNoThrow(String command, String... args) {
        return execNoThrow(command, args, null, 600, true);
    }
    
    /**
     * 执行命令（不抛出异常）
     * 
     * @param command 命令
     * @param args 参数
     * @param workingDir 工作目录
     * @param timeoutSeconds 超时时间（秒）
     * @param preserveOutputOnError 错误时是否保留输出
     * @return 执行结果
     */
    public static ProcessResult execNoThrow(
            String command, 
            String[] args,
            String workingDir,
            int timeoutSeconds,
            boolean preserveOutputOnError) {
        
        try {
            // 构建命令列表
            List<String> commandList = new ArrayList<>();
            commandList.add(command);
            if (args != null) {
                for (String arg : args) {
                    commandList.add(arg);
                }
            }
            
            // 创建进程构建器
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            
            // 设置工作目录
            if (workingDir != null && !workingDir.isEmpty()) {
                processBuilder.directory(new java.io.File(workingDir));
            } else {
                processBuilder.directory(new java.io.File(System.getProperty("user.dir")));
            }
            
            // 启动进程
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    logger.debug("读取标准输出失败", e);
                }
            });
            
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    logger.debug("读取错误输出失败", e);
                }
            });
            
            stdoutThread.start();
            stderrThread.start();
            
            // 等待进程完成
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            // 等待输出线程完成
            stdoutThread.join(1000);
            stderrThread.join(1000);
            
            if (!completed) {
                // 超时，强制终止进程
                process.destroyForcibly();
                logger.warn("进程执行超时: {}", command);
                return new ProcessResult("", "Process timeout", 1);
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0 && !preserveOutputOnError) {
                return new ProcessResult("", "", exitCode);
            }
            
            return new ProcessResult(
                stdout.toString().trim(),
                stderr.toString().trim(),
                exitCode
            );
            
        } catch (Exception e) {
            logger.error("执行命令失败: {}", command, e);
            return new ProcessResult("", e.getMessage(), 1);
        }
    }
    
    /**
     * 执行 Shell 命令
     * 
     * @param shellCommand Shell 命令
     * @return 执行结果
     */
    public static ProcessResult execShell(String shellCommand) {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return execNoThrow("cmd.exe", new String[]{"/c", shellCommand}, 
                null, 600, true);
        } else {
            return execNoThrow("/bin/sh", new String[]{"-c", shellCommand}, 
                null, 600, true);
        }
    }
    
    /**
     * 检查命令是否存在
     * 
     * @param command 命令名称
     * @return 是否存在
     */
    public static boolean commandExists(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessResult result;
        
        if (os.contains("win")) {
            result = execNoThrow("where", new String[]{command}, null, 5, false);
        } else {
            result = execNoThrow("which", new String[]{command}, null, 5, false);
        }
        
        return result.isSuccess();
    }
}
