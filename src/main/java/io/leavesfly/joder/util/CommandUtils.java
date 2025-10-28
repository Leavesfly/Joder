package io.leavesfly.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 命令解析工具类
 * 对应 Kode 的 commands.ts
 */
public class CommandUtils {

    private static final Logger logger = LoggerFactory.getLogger(CommandUtils.class);

    /**
     * 命令前缀检测结果
     */
    public static class CommandPrefixResult {
        private final String commandPrefix;
        private final boolean commandInjectionDetected;

        public CommandPrefixResult(String prefix, boolean injectionDetected) {
            this.commandPrefix = prefix;
            this.commandInjectionDetected = injectionDetected;
        }

        public String getCommandPrefix() {
            return commandPrefix;
        }

        public boolean isCommandInjectionDetected() {
            return commandInjectionDetected;
        }

        public static CommandPrefixResult injection() {
            return new CommandPrefixResult(null, true);
        }

        public static CommandPrefixResult safe(String prefix) {
            return new CommandPrefixResult(prefix, false);
        }

        public static CommandPrefixResult none() {
            return new CommandPrefixResult(null, false);
        }
    }

    /**
     * 命令子命令前缀结果
     */
    public static class CommandSubcommandPrefixResult extends CommandPrefixResult {
        private final Map<String, CommandPrefixResult> subcommandPrefixes;

        public CommandSubcommandPrefixResult(String prefix, boolean injectionDetected,
                                           Map<String, CommandPrefixResult> subcommands) {
            super(prefix, injectionDetected);
            this.subcommandPrefixes = subcommands;
        }

        public Map<String, CommandPrefixResult> getSubcommandPrefixes() {
            return subcommandPrefixes;
        }
    }

    /**
     * 命令分隔符集合
     */
    private static final Set<String> COMMAND_SEPARATORS = new HashSet<>(Arrays.asList(
        "&&", "||", ";", ";;", "|", "&"
    ));

    /**
     * 分割命令字符串
     */
    public static List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();

        // 简化的命令分割逻辑，按分隔符分割
        for (String separator : COMMAND_SEPARATORS) {
            if (command.contains(separator)) {
                String[] segments = command.split("\\" + separator.charAt(0));
                for (String segment : segments) {
                    String trimmed = segment.trim();
                    if (!trimmed.isEmpty()) {
                        parts.add(trimmed);
                    }
                }
                // 如果找到分隔符，返回分割结果
                if (!parts.isEmpty()) {
                    return parts;
                }
            }
        }

        // 如果没有分隔符，返回整个命令
        parts.add(command.trim());
        return parts;
    }

    /**
     * 提取命令前缀
     * 例如: git commit -m "msg" => git
     *      git => git
     *      npm test --foo => npm
     */
    public static String extractCommandPrefix(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }

        command = command.trim();

        // 检测命令注入
        if (hasCommandInjection(command)) {
            return "command_injection_detected";
        }

        // 提取第一个单词作为命令
        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            return null;
        }

        return parts[0];
    }

    /**
     * 检测命令注入
     */
    public static boolean hasCommandInjection(String command) {
        // 检查危险的 shell 特殊字符和操作符
        if (command.contains("$(") || command.contains("`") ||
            command.contains("\n") || command.contains("|") && !isCommandList(command)) {
            return true;
        }

        // 检查注释之后的内容
        if (command.contains("#")) {
            int commentIndex = command.indexOf("#");
            if (commentIndex > 0) {
                String afterComment = command.substring(commentIndex + 1);
                // 如果注释后面有代码，说明可能被注入
                if (!afterComment.trim().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查是否为有效的子命令
     */
    public static boolean isValidSubcommand(String command, String subcommand) {
        // git 的有效子命令
        if (command.equals("git")) {
            return isValidGitCommand(subcommand);
        }

        // npm 的有效子命令
        if (command.equals("npm")) {
            return isValidNpmCommand(subcommand);
        }

        // 其他命令
        return !subcommand.startsWith("-");
    }

    /**
     * 检查是否为有效的 git 子命令
     */
    public static boolean isValidGitCommand(String subcommand) {
        Set<String> validGitCommands = new HashSet<>(Arrays.asList(
            "add", "commit", "push", "pull", "fetch", "merge", "rebase", "diff", "status",
            "log", "branch", "checkout", "clone", "init", "reset", "revert", "tag",
            "stash", "apply", "pop", "config", "remote", "user", "help"
        ));
        return validGitCommands.contains(subcommand) && !subcommand.startsWith("-");
    }

    /**
     * 检查是否为有效的 npm 子命令
     */
    public static boolean isValidNpmCommand(String subcommand) {
        Set<String> validNpmCommands = new HashSet<>(Arrays.asList(
            "install", "test", "start", "build", "run", "publish", "unpublish",
            "version", "ls", "list", "search", "update", "upgrade", "audit",
            "ci", "init", "config", "cache", "help"
        ));
        return validNpmCommands.contains(subcommand) && !subcommand.startsWith("-");
    }

    /**
     * 检查是否为命令列表
     */
    public static boolean isCommandList(String command) {
        // 命令列表由以下分隔符连接: &&, ||, ;
        return command.contains("&&") || command.contains("||") || command.contains(";");
    }

    /**
     * 检查是否为不安全的复合命令
     */
    public static boolean isUnsafeCompoundCommand(String command) {
        List<String> subcommands = splitCommand(command);
        return subcommands.size() > 1 && !isCommandList(command);
    }

    /**
     * 获取命令的风险级别
     */
    public static String getRiskLevel(String command) {
        if (hasCommandInjection(command)) {
            return "CRITICAL";
        }

        if (isUnsafeCompoundCommand(command)) {
            return "HIGH";
        }

        String prefix = extractCommandPrefix(command);
        if (prefix == null || prefix.equals("none")) {
            return "LOW";
        }

        // 根据命令前缀判断风险
        String[] parts = prefix.split("\\s+");
        if (parts.length == 0) {
            return "LOW";
        }

        String baseCommand = parts[0];

        // 危险命令
        if (isDangerousCommand(baseCommand)) {
            return "HIGH";
        }

        // 中等危险命令
        if (isModerateDangerousCommand(baseCommand)) {
            return "MEDIUM";
        }

        return "LOW";
    }

    /**
     * 是否为危险命令
     */
    public static boolean isDangerousCommand(String command) {
        Set<String> dangerous = new HashSet<>(Arrays.asList(
            "rm", "rmdir", "dd", "mkfs", "mount", "umount", "chmod", "chown",
            "sudo", "su", "passwd", "kill", "pkill", "killall", "reboot", "shutdown",
            "userdel", "useradd", "groupdel", "groupadd", "visudo"
        ));
        return dangerous.contains(command);
    }

    /**
     * 是否为中等危险命令
     */
    public static boolean isModerateDangerousCommand(String command) {
        Set<String> moderate = new HashSet<>(Arrays.asList(
            "git", "npm", "docker", "curl", "wget", "python", "node", "java", "make"
        ));
        return moderate.contains(command);
    }

    /**
     * 规范化命令（移除多余空格等）
     */
    public static String normalizeCommand(String command) {
        if (command == null) {
            return "";
        }
        return command.replaceAll("\\s+", " ").trim();
    }

    /**
     * 生成命令的签名用于去重
     */
    public static String getCommandSignature(String command) {
        if (command == null) {
            return "";
        }
        String normalized = normalizeCommand(command);
        // 只抽取第一个单词作为签名
        String[] parts = normalized.split("\\s+");
        return parts.length > 0 ? parts[0] : normalized;
    }

    /**
     * 获取命令的所有标记
     */
    public static List<String> extractFlags(String command) {
        List<String> flags = new ArrayList<>();
        String[] parts = command.split("\\s+");

        for (String part : parts) {
            if (part.startsWith("-")) {
                flags.add(part);
            }
        }

        return flags;
    }

    /**
     * 获取命令的参数
     */
    public static List<String> extractArguments(String command) {
        List<String> args = new ArrayList<>();
        String[] parts = command.split("\\s+");

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].startsWith("-")) {
                args.add(parts[i]);
            }
        }

        return args;
    }
}
