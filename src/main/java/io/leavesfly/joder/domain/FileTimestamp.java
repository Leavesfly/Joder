package io.leavesfly.joder.domain;

/**
 * 文件时间戳记录
 * 
 * <p>追踪文件的读取、修改和编辑时间，用于检测文件冲突。
 * 
 * @author Joder Team
 * @since 1.0.0
 */
public class FileTimestamp {
    
    /** 文件路径 */
    private String path;
    
    /** 最后读取时间（毫秒） */
    private long lastReadTime;
    
    /** 读取时的文件修改时间（毫秒） */
    private long modifiedTimeAtRead;
    
    /** 文件大小（字节） */
    private long size;
    
    /** Agent 最后编辑时间（毫秒，可选） */
    private Long lastAgentEditTime;
    
    // 构造函数
    
    public FileTimestamp() {}
    
    public FileTimestamp(String path, long lastReadTime, long modifiedTimeAtRead, long size) {
        this.path = path;
        this.lastReadTime = lastReadTime;
        this.modifiedTimeAtRead = modifiedTimeAtRead;
        this.size = size;
    }
    
    // Getters and Setters
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public long getLastReadTime() {
        return lastReadTime;
    }
    
    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }
    
    public long getModifiedTimeAtRead() {
        return modifiedTimeAtRead;
    }
    
    public void setModifiedTimeAtRead(long modifiedTimeAtRead) {
        this.modifiedTimeAtRead = modifiedTimeAtRead;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public Long getLastAgentEditTime() {
        return lastAgentEditTime;
    }
    
    public void setLastAgentEditTime(Long lastAgentEditTime) {
        this.lastAgentEditTime = lastAgentEditTime;
    }
    
    @Override
    public String toString() {
        return "FileTimestamp{" +
                "path='" + path + '\'' +
                ", lastReadTime=" + lastReadTime +
                ", modifiedTimeAtRead=" + modifiedTimeAtRead +
                ", size=" + size +
                ", lastAgentEditTime=" + lastAgentEditTime +
                '}';
    }
}
