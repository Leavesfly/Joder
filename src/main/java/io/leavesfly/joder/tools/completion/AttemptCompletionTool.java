package io.leavesfly.joder.tools.completion;

import io.leavesfly.joder.tools.Tool;
import io.leavesfly.joder.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;

/**
 * AttemptCompletion Tool - 任务完成工具
 * 
 * 用于标记任务完成并提供结果摘要
 */
@Singleton
public class AttemptCompletionTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(AttemptCompletionTool.class);
    
    @Override
    public String getName() {
        return "AttemptCompletion";
    }
    
    @Override
    public String getDescription() {
        return "标记任务完成并提供结果摘要。\n" +
               "- 用于明确表示任务已完成\n" +
               "- 提供工作结果的详细总结\n" +
               "- 包含下一步建议";
    }
    
    @Override
    public String getPrompt() {
        return "使用此工具标记任务完成。\n\n" +
               "**何时使用**：\n" +
               "- 已完成用户请求的所有必需工作\n" +
               "- 已验证结果满足要求\n" +
               "- 准备好向用户报告完成情况\n\n" +
               "**参数**：\n" +
               "- result: 完成结果的详细总结（必需）\n" +
               "- command: 用户可以运行以查看结果的命令（可选）\n\n" +
               "**结果总结应包含**：\n" +
               "1. 完成了什么 - 简要描述完成的工作\n" +
               "2. 主要变更 - 列出关键修改和新增内容\n" +
               "3. 验证结果 - 说明如何验证工作正确完成\n" +
               "4. 下一步建议 - 用户可能想要执行的后续操作\n\n" +
               "**示例**：\n" +
               "```json\n" +
               "{\n" +
               "  \"result\": \"成功实现了用户认证功能：\\n" +
               "1. 添加了 LoginController 和 AuthService\\n" +
               "2. 实现了 JWT token 生成和验证\\n" +
               "3. 添加了单元测试（覆盖率 95%）\\n" +
               "4. 更新了 API 文档\\n\\n" +
               "所有测试通过，代码已提交到 feature/auth 分支。\",\n" +
               "  \"command\": \"mvn test && mvn spring-boot:run\"\n" +
               "}\n" +
               "```\n\n" +
               "注意：此工具仅标记完成，不会自动退出或执行命令。";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public boolean isReadOnly() {
        return true; // 只是标记，不修改状态
    }
    
    @Override
    public boolean isConcurrencySafe() {
        return true;
    }
    
    @Override
    public boolean needsPermissions() {
        return false;
    }
    
    @Override
    public ToolResult call(Map<String, Object> input) {
        String result = (String) input.get("result");
        String command = (String) input.get("command");
        
        // 验证输入
        if (result == null || result.trim().isEmpty()) {
            return ToolResult.error("结果总结不能为空");
        }
        
        logger.info("任务完成标记");
        logger.info("结果: {}", result);
        if (command != null && !command.trim().isEmpty()) {
            logger.info("建议命令: {}", command);
        }
        
        // 构建完成报告
        StringBuilder report = new StringBuilder();
        report.append("✅ 任务完成\n\n");
        report.append("─".repeat(60)).append("\n\n");
        report.append(result).append("\n\n");
        report.append("─".repeat(60)).append("\n\n");
        
        if (command != null && !command.trim().isEmpty()) {
            report.append("💡 建议命令:\n");
            report.append("```\n");
            report.append(command).append("\n");
            report.append("```\n\n");
        }
        
        report.append("任务已成功完成！如需进一步帮助，请告诉我。\n");
        
        return ToolResult.success(report.toString());
    }
    
    @Override
    public String renderToolUseMessage(Map<String, Object> input) {
        return "标记任务完成";
    }
    
    @Override
    public String renderToolResultMessage(ToolResult result) {
        if (result.isSuccess()) {
            return "✅ 任务已标记为完成";
        } else {
            return "❌ 任务完成标记失败: " + result.getError();
        }
    }
}
