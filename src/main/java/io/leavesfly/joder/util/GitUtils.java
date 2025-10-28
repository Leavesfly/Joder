package io.leavesfly.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Git 工具类
 * 对应 Kode 的 git.ts
 */
public class GitUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(GitUtils.class);
    
    // 缓存 Git 检测结果
    private static final ConcurrentMap<String, Boolean> isGitCache = new ConcurrentHashMap<>();
    
    /**
     * Git 仓库状态
     */
    public static class GitRepoState {
        private final String commitHash;
        private final String branchName;
        private final String remoteUrl;
        private final boolean isHeadOnRemote;
        private final boolean isClean;
        
        public GitRepoState(String commitHash, String branchName, String remoteUrl,
                           boolean isHeadOnRemote, boolean isClean) {
            this.commitHash = commitHash;
            this.branchName = branchName;
            this.remoteUrl = remoteUrl;
            this.isHeadOnRemote = isHeadOnRemote;
            this.isClean = isClean;
        }
        
        public String getCommitHash() {
            return commitHash;
        }
        
        public String getBranchName() {
            return branchName;
        }
        
        public String getRemoteUrl() {
            return remoteUrl;
        }
        
        public boolean isHeadOnRemote() {
            return isHeadOnRemote;
        }
        
        public boolean isClean() {
            return isClean;
        }
        
        @Override
        public String toString() {
            return String.format("GitRepoState{branch=%s, commit=%s, remote=%s, clean=%s}",
                branchName, commitHash.substring(0, 8), remoteUrl, isClean);
        }
    }
    
    /**
     * 检查当前目录是否为 Git 仓库
     * 
     * @return 是否为 Git 仓库
     */
    public static boolean isGit() {
        return isGit(null);
    }
    
    /**
     * 检查指定目录是否为 Git 仓库（带缓存）
     * 
     * @param workingDir 工作目录，null 表示当前目录
     * @return 是否为 Git 仓库
     */
    public static boolean isGit(String workingDir) {
        String cacheKey = workingDir != null ? workingDir : System.getProperty("user.dir");
        
        return isGitCache.computeIfAbsent(cacheKey, dir -> {
            ProcessUtils.ProcessResult result = ProcessUtils.execNoThrow(
                "git",
                new String[]{"rev-parse", "--is-inside-work-tree"},
                dir,
                5,
                false
            );
            return result.isSuccess();
        });
    }
    
    /**
     * 获取当前 HEAD 的 commit hash
     * 
     * @return commit hash
     */
    public static String getHead() {
        return getHead(null);
    }
    
    /**
     * 获取当前 HEAD 的 commit hash
     * 
     * @param workingDir 工作目录
     * @return commit hash
     */
    public static String getHead(String workingDir) {
        ProcessUtils.ProcessResult result = ProcessUtils.execNoThrow(
            "git",
            new String[]{"rev-parse", "HEAD"},
            workingDir,
            5,
            false
        );
        
        return result.isSuccess() ? result.getStdout().trim() : "";
    }
    
    /**
     * 获取当前分支名称
     * 
     * @return 分支名称
     */
    public static String getBranch() {
        return getBranch(null);
    }
    
    /**
     * 获取当前分支名称
     * 
     * @param workingDir 工作目录
     * @return 分支名称
     */
    public static String getBranch(String workingDir) {
        ProcessUtils.ProcessResult result = ProcessUtils.execNoThrow(
            "git",
            new String[]{"rev-parse", "--abbrev-ref", "HEAD"},
            workingDir,
            5,
            false
        );
        
        return result.isSuccess() ? result.getStdout().trim() : "";
    }
    
    /**
     * 获取远程仓库 URL
     * 
     * @return 远程仓库 URL，失败返回 null
     */
    public static String getRemoteUrl() {
        return getRemoteUrl(null);
    }
    
    /**
     * 获取远程仓库 URL
     * 
     * @param workingDir 工作目录
     * @return 远程仓库 URL，失败返回 null
     */
    public static String getRemoteUrl(String workingDir) {
        ProcessUtils.ProcessResult result = ProcessUtils.execNoThrow(
            "git",
            new String[]{"remote", "get-url", "origin"},
            workingDir,
            5,
            false
        );
        
        return result.isSuccess() ? result.getStdout().trim() : null;
    }
    
    /**
     * 检查 HEAD 是否在远程分支上
     * 
     * @return 是否在远程分支上
     */
    public static boolean isHeadOnRemote() {
        return isHeadOnRemote(null);
    }
    
    /**
     * 检查 HEAD 是否在远程分支上
     * 
     * @param workingDir 工作目录
     * @return 是否在远程分支上
     */
    public static boolean isHeadOnRemote(String workingDir) {
        ProcessUtils.ProcessResult result = ProcessUtils.execNoThrow(
            "git",
            new String[]{"rev-parse", "@{u}"},
            workingDir,
            5,
            false
        );
        
        return result.isSuccess();
    }
    
    /**
     * 检查工作区是否干净（无未提交的修改）
     * 
     * @return 是否干净
     */
    public static boolean isClean() {
        return isClean(null);
    }
    
    /**
     * 检查工作区是否干净（无未提交的修改）
     * 
     * @param workingDir 工作目录
     * @return 是否干净
     */
    public static boolean isClean(String workingDir) {
        ProcessUtils.ProcessResult result = ProcessUtils.execNoThrow(
            "git",
            new String[]{"status", "--porcelain"},
            workingDir,
            5,
            false
        );
        
        if (!result.isSuccess()) {
            return false;
        }
        
        return result.getStdout().trim().isEmpty();
    }
    
    /**
     * 获取 Git 仓库状态
     * 
     * @return Git 仓库状态，失败返回 null
     */
    public static GitRepoState getGitState() {
        return getGitState(null);
    }
    
    /**
     * 获取 Git 仓库状态
     * 
     * @param workingDir 工作目录
     * @return Git 仓库状态，失败返回 null
     */
    public static GitRepoState getGitState(String workingDir) {
        try {
            // 并行执行所有 Git 命令
            String commitHash = getHead(workingDir);
            String branchName = getBranch(workingDir);
            String remoteUrl = getRemoteUrl(workingDir);
            boolean headOnRemote = isHeadOnRemote(workingDir);
            boolean clean = isClean(workingDir);
            
            return new GitRepoState(
                commitHash,
                branchName,
                remoteUrl,
                headOnRemote,
                clean
            );
            
        } catch (Exception e) {
            logger.debug("获取 Git 状态失败", e);
            return null;
        }
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        isGitCache.clear();
    }
}
