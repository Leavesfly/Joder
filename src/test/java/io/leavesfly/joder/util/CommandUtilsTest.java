package io.leavesfly.joder.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 命令工具类测试
 */
public class CommandUtilsTest {

    @Test
    public void testSplitSimpleCommand() {
        var commands = CommandUtils.splitCommand("git commit -m 'test'");
        assertEquals(1, commands.size());
        assertEquals("git commit -m 'test'", commands.get(0));
    }

    @Test
    public void testSplitCommandWithSeparators() {
        var commands = CommandUtils.splitCommand("cd src && npm test");
        assertTrue(commands.size() >= 1);
    }

    @Test
    public void testExtractCommandPrefix() {
        assertEquals("git", CommandUtils.extractCommandPrefix("git"));
        assertEquals("git", CommandUtils.extractCommandPrefix("git status"));
        assertEquals("npm", CommandUtils.extractCommandPrefix("npm test --foo"));
        assertEquals("cat", CommandUtils.extractCommandPrefix("cat file.txt"));
    }

    @Test
    public void testCommandInjectionDetection() {
        assertTrue(CommandUtils.hasCommandInjection("git diff $(pwd)"));
        assertTrue(CommandUtils.hasCommandInjection("git status`ls`"));
        assertFalse(CommandUtils.hasCommandInjection("rm file"));
    }

    @Test
    public void testValidGitSubcommands() {
        assertTrue(CommandUtils.isValidGitCommand("commit"));
        assertTrue(CommandUtils.isValidGitCommand("push"));
        assertTrue(CommandUtils.isValidGitCommand("pull"));
        assertFalse(CommandUtils.isValidGitCommand("notacommand"));
        assertFalse(CommandUtils.isValidGitCommand("-m"));
    }

    @Test
    public void testValidNpmSubcommands() {
        assertTrue(CommandUtils.isValidNpmCommand("test"));
        assertTrue(CommandUtils.isValidNpmCommand("install"));
        assertTrue(CommandUtils.isValidNpmCommand("run"));
        assertFalse(CommandUtils.isValidNpmCommand("notacommand"));
    }

    @Test
    public void testCommandListDetection() {
        assertTrue(CommandUtils.isCommandList("cmd1 && cmd2"));
        assertTrue(CommandUtils.isCommandList("cmd1 || cmd2"));
        assertTrue(CommandUtils.isCommandList("cmd1 ; cmd2"));
        assertFalse(CommandUtils.isCommandList("cat file.txt"));
    }

    @Test
    public void testUnsafeCompoundCommand() {
        assertTrue(CommandUtils.isUnsafeCompoundCommand("git status | grep main"));
        assertFalse(CommandUtils.isUnsafeCompoundCommand("git status && git push"));
    }

    @Test
    public void testGetRiskLevel() {
        assertEquals("CRITICAL", CommandUtils.getRiskLevel("git diff $(pwd)"));
        assertEquals("HIGH", CommandUtils.getRiskLevel("rm -rf /"));
        assertEquals("MEDIUM", CommandUtils.getRiskLevel("git commit -m 'msg'"));
        assertEquals("LOW", CommandUtils.getRiskLevel("cat file.txt"));
    }

    @Test
    public void testNormalizeCommand() {
        assertEquals("git commit -m msg", 
            CommandUtils.normalizeCommand("git  commit   -m  msg"));
        assertEquals("ls -la", 
            CommandUtils.normalizeCommand("  ls   -la  "));
    }

    @Test
    public void testGetCommandSignature() {
        assertEquals("git", CommandUtils.getCommandSignature("git status"));
        assertEquals("npm", 
            CommandUtils.getCommandSignature("npm test -m 'message'"));
    }

    @Test
    public void testExtractFlags() {
        var flags = CommandUtils.extractFlags("git commit -m msg -a --verbose");
        assertTrue(flags.contains("-m"));
        assertTrue(flags.contains("-a"));
        assertTrue(flags.contains("--verbose"));
        assertEquals(3, flags.size());
    }

    @Test
    public void testExtractArguments() {
        var args = CommandUtils.extractArguments("git commit -m msg file.txt");
        assertTrue(args.contains("msg"));
        assertTrue(args.contains("file.txt"));
    }

    @Test
    public void testIsDangerousCommand() {
        assertEquals("HIGH", CommandUtils.getRiskLevel("rm -rf /"));
        assertEquals("HIGH", CommandUtils.getRiskLevel("sudo chmod 777 /"));
        assertEquals("HIGH", CommandUtils.getRiskLevel("dd if=/dev/zero of=/dev/sda"));
    }
}
