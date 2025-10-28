package io.leavesfly.joder.tools;

import java.util.Map;

/**
 * 工具接口
 * 所有工具必须实现此接口
 */
public interface Tool {

    /**
     * 获取工具唯一标识名称
     */
    String getName();

    /**
     * 获取工具功能描述（提供给AI）
     */
    String getDescription();

    /**
     * 获取工具使用提示词
     */
    String getPrompt();

    /**
     * 工具是否启用
     */
    boolean isEnabled();

    /**
     * 是否为只读工具（不修改系统状态）
     */
    boolean isReadOnly();

    /**
     * 是否支持并发调用
     */
    boolean isConcurrencySafe();

    /**
     * 是否需要权限确认
     */
    boolean needsPermissions();

    /**
     * 执行工具逻辑
     *
     * @param input 工具输入参数（JSON格式）
     * @return 工具执行结果
     */
    ToolResult call(Map<String, Object> input);

    /**
     * 渲染工具调用消息
     */
    String renderToolUseMessage(Map<String, Object> input);

    /**
     * 渲染工具执行结果消息
     */
    String renderToolResultMessage(ToolResult result);
}
