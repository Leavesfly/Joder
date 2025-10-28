package io.shareai.joder.util;

import io.shareai.joder.util.exceptions.ConfigParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置工具类测试
 */
public class ConfigUtilsTest {

    @Test
    public void testProjectConfigBasics() {
        ConfigUtils.ProjectConfig config = new ConfigUtils.ProjectConfig();
        
        assertTrue(config.getAllowedTools().isEmpty());
        
        config.getAllowedTools().add("bash_tool");
        assertEquals(1, config.getAllowedTools().size());
    }

    @Test
    public void testProjectConfigContext() {
        ConfigUtils.ProjectConfig config = new ConfigUtils.ProjectConfig();
        
        config.getContext().put("project_name", "test_project");
        assertEquals("test_project", config.getContext().get("project_name"));
    }

    @Test
    public void testGlobalConfigDefaults() {
        ConfigUtils.GlobalConfig config = new ConfigUtils.GlobalConfig();
        
        assertEquals("dark", config.getTheme());
        assertEquals("anthropic", config.getPrimaryProvider());
        assertEquals(0, config.getNumStartups());
    }

    @Test
    public void testGlobalConfigModification() {
        ConfigUtils.GlobalConfig config = new ConfigUtils.GlobalConfig();
        
        config.setTheme("light");
        assertEquals("light", config.getTheme());
        
        config.setNumStartups(5);
        assertEquals(5, config.getNumStartups());
    }

    @Test
    public void testConfigValidation() {
        ConfigUtils.GlobalConfig config = new ConfigUtils.GlobalConfig();
        config.setTheme("dark");
        
        assertTrue(ConfigUtils.isValidConfig(config));
        
        config.setTheme("invalid");
        assertFalse(ConfigUtils.isValidConfig(config));
    }

    @Test
    public void testGetConfigValues() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("key1", "value1");
        config.put("flag1", true);
        config.put("count", 42);
        
        assertEquals("value1", ConfigUtils.getString(config, "key1", "default"));
        assertEquals("default", ConfigUtils.getString(config, "missing", "default"));
        
        assertTrue(ConfigUtils.getBoolean(config, "flag1", false));
        assertFalse(ConfigUtils.getBoolean(config, "missing", false));
        
        assertEquals(42, ConfigUtils.getInt(config, "count", 0));
        assertEquals(0, ConfigUtils.getInt(config, "missing", 0));
    }

    @Test
    public void testProjectConfigFile() {
        String filePath = ConfigUtils.getProjectConfigFile("/home/user/project");
        assertNotNull(filePath);
        assertTrue(filePath.contains(".kode"));
    }

    @Test
    public void testGlobalConfigFile() {
        String filePath = ConfigUtils.getGlobalConfigFile();
        assertNotNull(filePath);
        assertTrue(filePath.contains(".kode"));
        assertTrue(filePath.endsWith("config.json"));
    }
}
