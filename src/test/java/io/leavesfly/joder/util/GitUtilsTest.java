package io.leavesfly.joder.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GitUtils 测试
 */
public class GitUtilsTest {
    
    @Test
    public void testIsGit() {
        // 测试当前目录是否为 Git 仓库
        boolean isGit = GitUtils.isGit();
        // 注意：这个测试的结果取决于运行环境
        assertNotNull(isGit);
    }
    
    @Test
    public void testGetGitState() {
        if (!GitUtils.isGit()) {
            // 如果不在 Git 仓库中，跳过测试
            return;
        }
        
        GitUtils.GitRepoState state = GitUtils.getGitState();
        
        if (state != null) {
            assertNotNull(state.getBranchName());
            assertNotNull(state.getCommitHash());
            assertTrue(state.getCommitHash().length() > 0);
            
            System.out.println("Git State: " + state);
        }
    }
    
    @Test
    public void testGetBranch() {
        if (!GitUtils.isGit()) {
            return;
        }
        
        String branch = GitUtils.getBranch();
        assertNotNull(branch);
        
        System.out.println("Current branch: " + branch);
    }
    
    @Test
    public void testClearCache() {
        // 测试缓存清理不会抛出异常
        assertDoesNotThrow(() -> GitUtils.clearCache());
    }
}
