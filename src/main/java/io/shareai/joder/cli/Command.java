package io.shareai.joder.cli;

/**
 * 命令接口
 */
public interface Command {
    
    /**
     * 执行命令
     * 
     * @param args 命令参数
     * @return 命令执行结果
     */
    CommandResult execute(String args);
    
    /**
     * 获取命令描述
     */
    String getDescription();
    
    /**
     * 获取命令用法
     */
    String getUsage();
}
