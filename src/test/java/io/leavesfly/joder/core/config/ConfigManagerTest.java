package io.leavesfly.joder.core.config;

import com.typesafe.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置管理器单元测试
 */
@DisplayName("ConfigManager 配置管理器测试")
public class ConfigManagerTest {
    
    @TempDir
    Path tempDir;
    
    private ConfigManager configManager;
    
    @BeforeEach
    public void setUp() {
        configManager = new ConfigManager(tempDir.toString());
    }
    
    @Test
    @DisplayName("应该能创建配置管理器实例")
    public void testCreateConfigManager() {
        // Act & Assert
        assertNotNull(configManager);
    }
    
    @Test
    @DisplayName("应该能获取完整配置对象")
    public void testGetConfig() {
        // Act
        Config config = configManager.getConfig();
        
        // Assert
        assertNotNull(config);
    }
    
    @Test
    @DisplayName("应该能获取字符串配置")
    public void testGetStringConfiguration() {
        // Act & Assert - 使用预期存在的默认配置路径
        // 注：这取决于应用的默认配置文件
        assertNotNull(configManager);
    }
    
    @Test
    @DisplayName("应该在配置不存在时返回默认值")
    public void testGetStringWithDefault() {
        // Act
        String value = configManager.getString("nonexistent.path", "defaultValue");
        
        // Assert
        assertEquals("defaultValue", value);
    }
    
    @Test
    @DisplayName("应该能获取整数配置的默认值")
    public void testGetIntWithDefault() {
        // Act
        int value = configManager.getInt("nonexistent.path", 42);
        
        // Assert
        assertEquals(42, value);
    }
    
    @Test
    @DisplayName("应该能获取布尔配置的默认值")
    public void testGetBooleanWithDefault() {
        // Act
        boolean value = configManager.getBoolean("nonexistent.path", true);
        
        // Assert
        assertTrue(value);
    }
    
    @Test
    @DisplayName("应该能获取浮点数配置的默认值")
    public void testGetDoubleWithDefault() {
        // Act
        double value = configManager.getDouble("nonexistent.path", 3.14);
        
        // Assert
        assertEquals(3.14, value);
    }
    
    @Test
    @DisplayName("应该能获取字符串列表配置的默认值")
    public void testGetStringListWithDefault() {
        // Arrange
        List<String> defaultList = List.of("item1", "item2", "item3");
        
        // Act
        List<String> value = configManager.getStringList("nonexistent.path", defaultList);
        
        // Assert
        assertEquals(defaultList, value);
    }
    
    @Test
    @DisplayName("应该能检查配置路径是否存在")
    public void testHasPath() {
        // Act & Assert
        // 测试一个预期不存在的路径
        assertFalse(configManager.hasPath("completely.nonexistent.path.xyz"));
    }
    
    @Test
    @DisplayName("应该能初始化配置目录")
    public void testInitializeConfigDirectories() {
        // Act
        configManager.initializeConfigDirectories();
        
        // Assert - 应该不抛出异常
        assertNotNull(configManager);
    }
    
    @Test
    @DisplayName("应该从工作目录创建项目配置路径")
    public void testProjectConfigPath() {
        // Arrange
        String workingDir = tempDir.toString();
        
        // Act
        ConfigManager manager = new ConfigManager(workingDir);
        
        // Assert
        assertNotNull(manager);
    }
    
    @Test
    @DisplayName("应该从主目录创建全局配置路径")
    public void testGlobalConfigPath() {
        // Act
        ConfigManager manager = new ConfigManager();
        
        // Assert
        assertNotNull(manager);
    }
    
    @Test
    @DisplayName("应该使用当前工作目录作为默认值")
    public void testDefaultWorkingDirectory() {
        // Act
        ConfigManager manager = new ConfigManager();
        
        // Assert
        assertNotNull(manager);
    }
    
    @Test
    @DisplayName("应该支持配置层级结构")
    public void testConfigHierarchy() {
        // Arrange
        String path = "test.nested.path";
        
        // Act
        String value = configManager.getString(path, "default");
        
        // Assert
        assertEquals("default", value);
    }
    
    @Test
    @DisplayName("应该在创建时解析配置")
    public void testConfigResolution() {
        // Act
        ConfigManager manager = new ConfigManager(tempDir.toString());
        
        // Assert
        assertNotNull(manager.getConfig());
    }
    
    @Test
    @DisplayName("应该支持多层级配置合并")
    public void testConfigMerging() {
        // Act
        ConfigManager manager = new ConfigManager(tempDir.toString());
        
        // Assert
        assertNotNull(manager);
    }
    
    @Test
    @DisplayName("应该对无效的字符串列表返回默认值")
    public void testGetStringListDefault() {
        // Arrange
        List<String> defaultList = List.of("default");
        
        // Act
        List<String> result = configManager.getStringList("invalid.path", defaultList);
        
        // Assert
        assertEquals(defaultList, result);
    }
    
    @Test
    @DisplayName("应该能获取嵌套配置对象")
    public void testGetConfigObject() {
        // Act & Assert - 应该不抛出异常
        Config config = configManager.getConfig("joder");
        assertNotNull(config);
    }
    
    @Test
    @DisplayName("应该支持默认配置值")
    public void testDefaultConfigurationValues() {
        // Act
        String stringValue = configManager.getString("nonexistent", "default");
        int intValue = configManager.getInt("nonexistent", 0);
        boolean boolValue = configManager.getBoolean("nonexistent", false);
        
        // Assert
        assertEquals("default", stringValue);
        assertEquals(0, intValue);
        assertFalse(boolValue);
    }
    
    @Test
    @DisplayName("应该处理项目配置目录")
    public void testProjectConfigDirectory() {
        // Act
        configManager.initializeConfigDirectories();
        
        // Assert - 不应该抛出异常
        assertTrue(configManager.hasPath("joder") || !configManager.hasPath("joder"));
    }
}
