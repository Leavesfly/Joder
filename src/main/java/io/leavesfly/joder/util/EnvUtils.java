package io.leavesfly.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 环境工具类
 * 对应 Kode 的 env.ts
 */
public class EnvUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvUtils.class);
    
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String CONFIG_BASE_DIR = ".local/share/joder";
    private static final String CONFIG_FILE = ".joder.json";
    
    /**
     * 获取 Joder 基础目录
     */
    public static Path getJoderBaseDir() {
        String configDir = System.getenv("JODER_CONFIG_DIR");
        if (configDir != null && !configDir.isEmpty()) {
            return Paths.get(configDir);
        }
        return Paths.get(USER_HOME, CONFIG_BASE_DIR);
    }
    
    /**
     * 获取全局配置文件路径
     */
    public static Path getGlobalConfigFile() {
        String configDir = System.getenv("JODER_CONFIG_DIR");
        if (configDir != null && !configDir.isEmpty()) {
            return Paths.get(configDir, "config.json");
        }
        return Paths.get(USER_HOME, CONFIG_FILE);
    }
    
    /**
     * 获取内存目录
     */
    public static Path getMemoryDir() {
        return getJoderBaseDir().resolve("memory");
    }
    
    /**
     * 检查是否在 Docker 环境中运行
     */
    public static boolean isDocker() {
        try {
            // 检查 /.dockerenv 文件
            Path dockerEnvFile = Paths.get("/.dockerenv");
            if (Files.exists(dockerEnvFile)) {
                String os = System.getProperty("os.name").toLowerCase();
                return os.contains("linux");
            }
            
            // 检查 /proc/1/cgroup
            Path cgroupFile = Paths.get("/proc/1/cgroup");
            if (Files.exists(cgroupFile)) {
                String content = Files.readString(cgroupFile);
                return content.contains("docker");
            }
            
        } catch (IOException e) {
            logger.debug("检查 Docker 环境失败", e);
        }
        
        return false;
    }
    
    /**
     * 检查是否有互联网连接
     */
    public static boolean hasInternetAccess() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("1.1.1.1", 80), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 检查是否在 CI 环境中
     */
    public static boolean isCI() {
        return "true".equalsIgnoreCase(System.getenv("CI"));
    }
    
    /**
     * 获取平台信息
     */
    public static String getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "macos";
        } else {
            return "linux";
        }
    }
    
    /**
     * 获取 Java 版本
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }
    
    /**
     * 获取终端程序名称
     */
    public static String getTerminal() {
        return System.getenv("TERM_PROGRAM");
    }
    
    /**
     * 环境信息类
     */
    public static class Environment {
        private final boolean isDocker;
        private final boolean hasInternetAccess;
        private final boolean isCI;
        private final String platform;
        private final String javaVersion;
        private final String terminal;
        
        public Environment() {
            this.isDocker = EnvUtils.isDocker();
            this.hasInternetAccess = EnvUtils.hasInternetAccess();
            this.isCI = EnvUtils.isCI();
            this.platform = EnvUtils.getPlatform();
            this.javaVersion = EnvUtils.getJavaVersion();
            this.terminal = EnvUtils.getTerminal();
        }
        
        public boolean isDocker() {
            return isDocker;
        }
        
        public boolean hasInternetAccess() {
            return hasInternetAccess;
        }
        
        public boolean isCI() {
            return isCI;
        }
        
        public String getPlatform() {
            return platform;
        }
        
        public String getJavaVersion() {
            return javaVersion;
        }
        
        public String getTerminal() {
            return terminal;
        }
        
        @Override
        public String toString() {
            return String.format("Environment{platform=%s, java=%s, docker=%s, internet=%s, ci=%s, terminal=%s}",
                platform, javaVersion, isDocker, hasInternetAccess, isCI, terminal);
        }
    }
    
    /**
     * 获取环境信息
     */
    public static Environment getEnvironment() {
        return new Environment();
    }
}
