package io.leavesfly.joder.core.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限模式枚举单元测试
 */
@DisplayName("PermissionMode 权限模式测试")
public class PermissionModeTest {
    
    @Test
    @DisplayName("应该定义所有预期的权限模式")
    public void testAllModesDefined() {
        // Act & Assert
        assertNotNull(PermissionMode.DEFAULT);
        assertNotNull(PermissionMode.ACCEPT_EDITS);
        assertNotNull(PermissionMode.PLAN);
        assertNotNull(PermissionMode.BYPASS_PERMISSIONS);
    }
    
    @Test
    @DisplayName("应该能获取所有枚举值")
    public void testCanGetAllEnumValues() {
        // Act
        PermissionMode[] modes = PermissionMode.values();
        
        // Assert
        assertEquals(4, modes.length);
    }
    
    @Test
    @DisplayName("应该能通过名称获取模式")
    public void testGetModeByName() {
        // Act & Assert
        assertEquals(PermissionMode.DEFAULT, PermissionMode.valueOf("DEFAULT"));
        assertEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.valueOf("ACCEPT_EDITS"));
        assertEquals(PermissionMode.PLAN, PermissionMode.valueOf("PLAN"));
        assertEquals(PermissionMode.BYPASS_PERMISSIONS, PermissionMode.valueOf("BYPASS_PERMISSIONS"));
    }
    
    @Test
    @DisplayName("应该能获取模式值")
    public void testGetModeValue() {
        // Act & Assert
        assertEquals("default", PermissionMode.DEFAULT.getValue());
        assertEquals("acceptEdits", PermissionMode.ACCEPT_EDITS.getValue());
        assertEquals("plan", PermissionMode.PLAN.getValue());
        assertEquals("bypassPermissions", PermissionMode.BYPASS_PERMISSIONS.getValue());
    }
    
    @Test
    @DisplayName("应该能从值创建模式（小写）")
    public void testFromValueLowercase() {
        // Act & Assert
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromValue("default"));
        assertEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.fromValue("acceptEdits"));
        assertEquals(PermissionMode.PLAN, PermissionMode.fromValue("plan"));
        assertEquals(PermissionMode.BYPASS_PERMISSIONS, PermissionMode.fromValue("bypassPermissions"));
    }
    
    @Test
    @DisplayName("应该能从值创建模式（大写）")
    public void testFromValueUppercase() {
        // Act & Assert
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromValue("DEFAULT"));
        assertEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.fromValue("ACCEPTEDITS"));
        assertEquals(PermissionMode.PLAN, PermissionMode.fromValue("PLAN"));
        assertEquals(PermissionMode.BYPASS_PERMISSIONS, PermissionMode.fromValue("BYPASSPERMISSIONS"));
    }
    
    @Test
    @DisplayName("应该在无效值时返回 DEFAULT 模式")
    public void testFromValueReturnsDefaultForInvalid() {
        // Act
        PermissionMode mode = PermissionMode.fromValue("invalidMode");
        
        // Assert
        assertEquals(PermissionMode.DEFAULT, mode);
    }
    
    @Test
    @DisplayName("应该对空值返回 DEFAULT 模式")
    public void testFromValueReturnsDefaultForNull() {
        // Act
        PermissionMode mode = PermissionMode.fromValue(null);
        
        // Assert
        assertEquals(PermissionMode.DEFAULT, mode);
    }
    
    @Test
    @DisplayName("DEFAULT 模式应该需要用户确认")
    public void testDefaultModeDescription() {
        // Act & Assert
        assertEquals("default", PermissionMode.DEFAULT.getValue());
    }
    
    @Test
    @DisplayName("ACCEPT_EDITS 模式应该自动批准编辑")
    public void testAcceptEditsModeDescription() {
        // Act & Assert
        assertEquals("acceptEdits", PermissionMode.ACCEPT_EDITS.getValue());
    }
    
    @Test
    @DisplayName("PLAN 模式应该只允许只读操作")
    public void testPlanModeDescription() {
        // Act & Assert
        assertEquals("plan", PermissionMode.PLAN.getValue());
    }
    
    @Test
    @DisplayName("BYPASS_PERMISSIONS 模式应该绕过所有检查")
    public void testBypassPermissionsModeDescription() {
        // Act & Assert
        assertEquals("bypassPermissions", PermissionMode.BYPASS_PERMISSIONS.getValue());
    }
    
    @Test
    @DisplayName("应该区分不同的权限模式")
    public void testModesAreDistinct() {
        // Act & Assert
        assertNotEquals(PermissionMode.DEFAULT, PermissionMode.ACCEPT_EDITS);
        assertNotEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.PLAN);
        assertNotEquals(PermissionMode.PLAN, PermissionMode.BYPASS_PERMISSIONS);
        assertNotEquals(PermissionMode.DEFAULT, PermissionMode.BYPASS_PERMISSIONS);
    }
    
    @Test
    @DisplayName("应该能在模式之间切换")
    public void testCanSwitchBetweenModes() {
        // Act & Assert
        PermissionMode mode1 = PermissionMode.DEFAULT;
        PermissionMode mode2 = PermissionMode.ACCEPT_EDITS;
        
        assertNotEquals(mode1, mode2);
        assertEquals(mode1, PermissionMode.DEFAULT);
        assertEquals(mode2, PermissionMode.ACCEPT_EDITS);
    }
    
    @Test
    @DisplayName("应该支持大小写不敏感的值转换")
    public void testCaseInsensitiveFromValue() {
        // Act & Assert
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromValue("DeFaUlT"));
        assertEquals(PermissionMode.PLAN, PermissionMode.fromValue("PlAn"));
    }
}
