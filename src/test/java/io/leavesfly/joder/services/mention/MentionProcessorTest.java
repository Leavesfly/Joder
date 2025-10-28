package io.leavesfly.joder.services.mention;

import io.leavesfly.joder.domain.Mention;
import io.leavesfly.joder.domain.MentionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MentionProcessor 测试
 */
class MentionProcessorTest {
    
    @TempDir
    Path tempDir;
    
    private MentionProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new MentionProcessor();
    }
    
    @Test
    void testParseMentions_File() {
        String input = "Read @file:src/main/Main.java please";
        
        List<Mention> mentions = processor.parseMentions(input);
        
        assertEquals(1, mentions.size());
        Mention mention = mentions.get(0);
        Assertions.assertEquals(MentionType.FILE, mention.getType());
        assertEquals("src/main/Main.java", mention.getValue());
        assertEquals("@file:src/main/Main.java", mention.getRawText());
    }
    
    @Test
    void testParseMentions_Agent() {
        String input = "Use @agent:code-reviewer to check this";
        
        List<Mention> mentions = processor.parseMentions(input);
        
        assertEquals(1, mentions.size());
        Mention mention = mentions.get(0);
        assertEquals(MentionType.AGENT, mention.getType());
        assertEquals("code-reviewer", mention.getValue());
    }
    
    @Test
    void testParseMentions_Multiple() {
        String input = "@file:test.txt and @agent:helper and @model:gpt-4";
        
        List<Mention> mentions = processor.parseMentions(input);
        
        assertEquals(3, mentions.size());
        assertEquals(MentionType.FILE, mentions.get(0).getType());
        assertEquals(MentionType.AGENT, mentions.get(1).getType());
        assertEquals(MentionType.MODEL, mentions.get(2).getType());
    }
    
    @Test
    void testParseMentions_RunAgent() {
        String input = "Execute @run-agent:task-executor";
        
        List<Mention> mentions = processor.parseMentions(input);
        
        assertEquals(1, mentions.size());
        assertEquals(MentionType.RUN_AGENT, mentions.get(0).getType());
        assertEquals("task-executor", mentions.get(0).getValue());
    }
    
    @Test
    void testParseMentions_AskModel() {
        String input = "Query @ask-model:claude-3";
        
        List<Mention> mentions = processor.parseMentions(input);
        
        assertEquals(1, mentions.size());
        assertEquals(MentionType.ASK_MODEL, mentions.get(0).getType());
        assertEquals("claude-3", mentions.get(0).getValue());
    }
    
    @Test
    void testValidateMention_FileExists() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");
        
        Mention mention = new Mention(
                MentionType.FILE,
                testFile.toString(),
                "@file:" + testFile,
                0, 10
        );
        
        processor.validateMention(mention, tempDir.toString());
        
        assertTrue(mention.isValid());
    }
    
    @Test
    void testValidateMention_FileNotExists() {
        Mention mention = new Mention(
                MentionType.FILE,
                "nonexistent.txt",
                "@file:nonexistent.txt",
                0, 10
        );
        
        processor.validateMention(mention, tempDir.toString());
        
        assertFalse(mention.isValid());
        assertNotNull(mention.getValidationMessage());
        assertTrue(mention.getValidationMessage().contains("not found"));
    }
    
    @Test
    void testValidateMention_Agent() {
        Mention mention = new Mention(
                MentionType.AGENT,
                "code-reviewer",
                "@agent:code-reviewer",
                0, 10
        );
        
        processor.validateMention(mention, null);
        
        assertTrue(mention.isValid());
    }
    
    @Test
    void testValidateMention_Model() {
        Mention mention = new Mention(
                MentionType.MODEL,
                "gpt-4",
                "@model:gpt-4",
                0, 10
        );
        
        processor.validateMention(mention, null);
        
        assertTrue(mention.isValid());
    }
    
    @Test
    void testFuzzyMatchFile() {
        List<String> candidates = Arrays.asList(
                "src/main/java/Main.java",
                "src/test/java/MainTest.java",
                "src/main/resources/config.properties",
                "README.md"
        );
        
        List<String> matches = processor.fuzzyMatchFile("Main", candidates, 3);
        
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(m -> m.contains("Main")));
    }
    
    @Test
    void testExpandMention_File() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello World");
        
        Mention mention = new Mention(
                MentionType.FILE,
                testFile.toString(),
                "@file:" + testFile,
                0, 10
        );
        mention.setValid(true);
        
        String expanded = processor.expandMention(mention, tempDir.toString());
        
        assertTrue(expanded.contains("Hello World"));
        assertTrue(expanded.contains("File:"));
    }
    
    @Test
    void testProcessInput() throws IOException {
        Path testFile = tempDir.resolve("data.txt");
        Files.writeString(testFile, "Important data");
        
        String input = "Check @file:" + testFile.toString();
        
        String processed = processor.processInput(input, tempDir.toString());
        
        assertTrue(processed.contains("Important data"));
    }
    
    @Test
    void testGetInvalidMentions() {
        String input = "@file:nonexistent.txt and @agent:good-agent";
        
        List<Mention> invalid = processor.getInvalidMentions(input, tempDir.toString());
        
        assertEquals(1, invalid.size());
        assertEquals(MentionType.FILE, invalid.get(0).getType());
    }
}
