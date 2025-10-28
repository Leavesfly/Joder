package io.shareai.joder.cli.commands;

import io.shareai.joder.cli.Command;
import io.shareai.joder.cli.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Logout Command - 登出命令
 * 清除认证信息和会话数据
 */
@Singleton
public class LogoutCommand implements Command {
    
    private static final Logger logger = LoggerFactory.getLogger(LogoutCommand.class);
    
    private static final String SESSION_FILE = ".joder/session.json";
    private static final String AUTH_FILE = ".joder/auth.json";
    private static final String GLOBAL_SESSION_FILE = ".config/joder/session.json";
    
    @Override
    public String getDescription() {
        return "登出并清除会话信息";
    }
    
    @Override
    public String getUsage() {
        return "logout [--global] - 登出当前会话";
    }
    
    @Override
    public CommandResult execute(String args) {
        boolean isGlobal = args != null && args.contains("--global");
        
        try {
            int clearedCount = 0;
            StringBuilder output = new StringBuilder();
            output.append("🚪 正在登出...\n\n");
            
            // 清除项目级会话
            if (clearSessionFile(SESSION_FILE)) {
                output.append("✓ 已清除项目会话\n");
                clearedCount++;
            }
            
            // 清除项目级认证
            if (clearSessionFile(AUTH_FILE)) {
                output.append("✓ 已清除项目认证信息\n");
                clearedCount++;
            }
            
            // 清除全局会话（如果指定）
            if (isGlobal) {
                String homeDir = System.getProperty("user.home");
                Path globalSessionPath = Paths.get(homeDir, GLOBAL_SESSION_FILE);
                
                if (clearSessionFile(globalSessionPath.toString())) {
                    output.append("✓ 已清除全局会话\n");
                    clearedCount++;
                }
            }
            
            if (clearedCount == 0) {
                output.append("ℹ️  未找到需要清除的会话信息\n");
            } else {
                output.append("\n✅ 登出成功！已清除 ").append(clearedCount).append(" 个会话文件。\n");
            }
            
            output.append("\n💡 提示：\n");
            output.append("  - 使用 /login 重新登录\n");
            output.append("  - 使用 /logout --global 清除全局会话\n");
            
            return CommandResult.success(output.toString());
            
        } catch (Exception e) {
            logger.error("Failed to logout", e);
            return CommandResult.error("登出失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除会话文件
     */
    private boolean clearSessionFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Cleared session file: {}", filePath);
                return true;
            }
        } catch (IOException e) {
            logger.warn("Failed to delete session file: {}", filePath, e);
        }
        return false;
    }
}
