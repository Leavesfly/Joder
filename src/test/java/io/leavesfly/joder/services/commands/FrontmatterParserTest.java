package io.leavesfly.joder.services.commands;

import io.leavesfly.joder.domain.CustomCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FrontmatterParser 单元测试
 */
class FrontmatterParserTest {
    
    private FrontmatterParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new FrontmatterParser();
    }
    
    @Test
    void testParseSimpleFrontmatter() {
        String markdown = """
                ---
                name: test-command
                description: Test description
                enabled: true
                ---
                Command content here
                """;
        
        FrontmatterParser.ParseResult result = parser.parse(markdown);
        
        assertNotNull(result);
        assertEquals("Command content here\n", result.getContent());
        
        Map<String, Object> frontmatter = result.getFrontmatter();
        assertEquals("test-command", frontmatter.get("name"));
        assertEquals("Test description", frontmatter.get("description"));
        assertTrue((Boolean) frontmatter.get("enabled"));
    }
    
    @Test
    void testParseArraysInline() {
        String markdown = """
                ---
                name: test
                aliases: [cmd, c]
                ---
                Content
                """;
        
        FrontmatterParser.ParseResult result = parser.parse(markdown);
        Map<String, Object> frontmatter = result.getFrontmatter();
        
        assertTrue(frontmatter.get("aliases") instanceof java.util.List);
    }
    
    @Test
    void testParseArraysMultiline() {
        String markdown = """
                ---
                name: test
                allowed-tools:
                  - BashTool
                  - FileRead
                ---
                Content
                """;
        
        FrontmatterParser.ParseResult result = parser.parse(markdown);
        Map<String, Object> frontmatter = result.getFrontmatter();
        
        assertTrue(frontmatter.get("allowed-tools") instanceof java.util.List);
    }
    
    @Test
    void testParseNoFrontmatter() {
        String markdown = "Just some content without frontmatter";
        
        FrontmatterParser.ParseResult result = parser.parse(markdown);
        
        assertTrue(result.getFrontmatter().isEmpty());
        assertEquals(markdown, result.getContent());
    }
    
    @Test
    void testApplyToCustomCommand() {
        String markdown = """
                ---
                name: my-command
                description: My description
                aliases: [cmd, c]
                enabled: false
                hidden: true
                argNames: [arg1, arg2]
                allowed-tools: [BashTool]
                ---
                Content
                """;
        
        FrontmatterParser.ParseResult result = parser.parse(markdown);
        CustomCommand command = new CustomCommand();
        FrontmatterParser.applyToCustomCommand(result.getFrontmatter(), command);
        
        assertEquals("my-command", command.getName());
        assertEquals("My description", command.getDescription());
        assertEquals(Arrays.asList("cmd", "c"), command.getAliases());
        assertFalse(command.isEnabled());
        assertTrue(command.isHidden());
        assertEquals(Arrays.asList("arg1", "arg2"), command.getArgNames());
        assertEquals(Arrays.asList("BashTool"), command.getAllowedTools());
    }
}
