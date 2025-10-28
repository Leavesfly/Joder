package io.shareai.joder.tools.think;

import io.shareai.joder.tools.AbstractTool;
import io.shareai.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 思考工具 - 允许 AI 显式表达思考过程
 * 
 * 这个工具的输出不会直接显示给用户，仅用于 AI 的内部推理链。
 * 它允许 AI 在处理复杂任务时明确记录中间推理步骤。
 */
public class ThinkTool extends AbstractTool {
    
    private static final Logger logger = LoggerFactory.getLogger(ThinkTool.class);
    
    @Override
    public String getName() {
        return "think";
    }
    
    @Override
    public String getDescription() {
        return "允许 AI 记录内部思考过程。用于复杂任务分解、多步骤规划和问题诊断分析。" +
               "思考内容不会显示给用户，仅记录在日志中供调试使用。" +
               "使用场景：1) 分析复杂问题时的推理步骤 2) 任务分解的中间思考 3) 算法设计的思路演变";
    }
    
    @Override
    public boolean isReadOnly() {
        return true;  // 思考工具是只读的，不修改系统状态
    }
    
    @Override
    public boolean needsPermissions() {
        return false;  // 不需要权限确认
    }
    
    @Override
    public ToolResult call(Map<String, Object> params) {
        try {
            String thought = getString(params, "thought");
            
            if (thought == null || thought.trim().isEmpty()) {
                return ToolResult.error("思考内容不能为空");
            }
            
            // 记录思考内容到日志（仅 DEBUG 级别）
            logger.debug("🤔 AI Thought Process:\n{}", thought);
            
            // 返回静默结果（不显示给用户）
            // 但告诉 AI 思考已被记录
            return ToolResult.success("<思考已记录>");
            
        } catch (Exception e) {
            logger.error("Error in ThinkTool", e);
            return ToolResult.error("思考记录失败: " + e.getMessage());
        }
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        // 思考工具的结果不应该显示给用户
        // 返回空字符串或简短确认
        if (result.isSuccess()) {
            return "";  // 静默成功
        } else {
            // 错误时还是需要显示
            return "⚠ 思考工具错误: " + result.getError();
        }
    }
}
