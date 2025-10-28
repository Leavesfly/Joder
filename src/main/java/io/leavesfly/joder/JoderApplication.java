package io.leavesfly.joder;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.leavesfly.joder.core.config.ConfigManager;
import io.leavesfly.joder.screens.ReplScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Joder 应用主入口
 */
@Command(
        name = "joder",
        mixinStandardHelpOptions = true,
        version = "Joder 1.0.0",
        description = "AI-Powered Terminal Assistant"
)
public class JoderApplication implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(JoderApplication.class);

    @Option(names = {"-c", "--cwd"}, description = "Working directory")
    private String workingDirectory = System.getProperty("user.dir");

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;

    @Option(names = {"--version"}, description = "Display version information", versionHelp = true)
    private boolean versionRequested;

    private Injector injector;
    private ConfigManager configManager;

    public static void main(String[] args) {
        // 设置日志目录
        String logDir = System.getProperty("user.home") + "/.local/share/joder/logs";
        Path logPath = Paths.get(logDir);
        try {
            Files.createDirectories(logPath);
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }

        // 执行应用
        int exitCode = new CommandLine(new JoderApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            // 显示欢迎横幅
            displayBanner();

            // 初始化依赖注入容器
            logger.info("Initializing Guice injector...");
            injector = Guice.createInjector(new JoderModule(workingDirectory));

            // 获取配置管理器
            configManager = injector.getInstance(ConfigManager.class);

            // 初始化配置目录
            configManager.initializeConfigDirectories();

            // 显示基本信息
            displayInfo();

            // 启动 REPL 界面
            logger.info("Starting REPL...");
            ReplScreen replScreen = injector.getInstance(ReplScreen.class);
            replScreen.start();

            return 0;

        } catch (Exception e) {
            logger.error("Application failed to start", e);
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    /**
     * 显示欢迎横幅
     */
    private void displayBanner() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("banner.txt")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load banner", e);
        }
    }

    /**
     * 显示应用信息
     */
    private void displayInfo() {
        if (verbose) {
            System.out.println("\n=== Joder Configuration ===");
            System.out.println("Working Directory: " + workingDirectory);
            System.out.println("Theme: " + configManager.getString("joder.theme", "dark"));
            System.out.println("Language: " + configManager.getString("joder.language", "zh-CN"));
            System.out.println("===========================\n");
        }
    }
}
