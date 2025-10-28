package io.shareai.joder.services.model;

import io.shareai.joder.domain.Message;
import io.shareai.joder.domain.MessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 模拟 AI 适配器
 * 用于演示和测试，不调用真实 API
 */
public class MockModelAdapter implements ModelAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(MockModelAdapter.class);
    
    @Override
    public String sendMessage(List<Message> messages, String systemPrompt) {
        logger.info("Mock AI processing {} messages", messages.size());
        
        // 获取最后一条用户消息
        Message lastUserMessage = messages.stream()
            .filter(m -> m.getRole() == MessageRole.USER)
            .reduce((first, second) -> second)
            .orElse(null);
        
        if (lastUserMessage == null) {
            return "我没有收到您的消息。";
        }
        
        String userInput = lastUserMessage.getContent();
        
        // 根据用户输入生成不同的响应
        if (userInput.toLowerCase().contains("你好") || userInput.toLowerCase().contains("hello")) {
            return "你好！我是 Joder AI 助手（模拟模式）。\n\n" +
                   "目前我运行在模拟模式下，这意味着我不会调用真实的 AI API。\n" +
                   "要启用真实的 AI 功能，请：\n" +
                   "1. 配置 API 密钥（如 ANTHROPIC_API_KEY、OPENAI_API_KEY 等）\n" +
                   "2. 在配置文件中选择真实的模型提供商\n\n" +
                   "您可以尝试以下命令：\n" +
                   "  /help - 查看帮助\n" +
                   "  /config - 查看配置\n" +
                   "  /exit - 退出程序";
        }
        
        if (userInput.toLowerCase().contains("工具") || userInput.toLowerCase().contains("tool")) {
            return "Joder 已实现以下工具系统：\n\n" +
                   "📁 **文件操作工具**：\n" +
                   "  • FileReadTool - 读取文件内容\n" +
                   "  • FileWriteTool - 写入文件\n\n" +
                   "💻 **系统命令工具**：\n" +
                   "  • BashTool - 执行 Bash 命令（带安全检查）\n\n" +
                   "🔐 **权限系统**：\n" +
                   "  支持四种权限模式：default, acceptEdits, plan, bypassPermissions\n\n" +
                   "在真实模式下，AI 可以调用这些工具来帮助您完成任务。";
        }
        
        if (userInput.toLowerCase().contains("功能") || userInput.toLowerCase().contains("feature")) {
            return "Joder 当前已实现的功能：\n\n" +
                   "✅ **阶段一：基础设施**\n" +
                   "  • Maven 项目结构\n" +
                   "  • 配置管理系统（HOCON）\n" +
                   "  • 日志系统（SLF4J + Logback）\n" +
                   "  • 依赖注入（Guice）\n\n" +
                   "✅ **阶段二：终端 UI**\n" +
                   "  • REPL 交互循环\n" +
                   "  • 命令系统\n" +
                   "  • 主题系统\n" +
                   "  • 消息渲染\n\n" +
                   "✅ **阶段三：工具系统**\n" +
                   "  • 工具框架\n" +
                   "  • 权限管理\n" +
                   "  • 文件和 Bash 工具\n\n" +
                   "🚧 **阶段四：模型通信**（当前为模拟模式）\n" +
                   "🚧 **阶段五：MCP 集成**（计划中）";
        }
        
        // 默认响应
        return String.format("您说：「%s」\n\n" +
                           "我是 Joder AI 助手（模拟模式）。当前我正在模拟 AI 响应。\n\n" +
                           "💡 提示：\n" +
                           "  • 询问「工具」了解可用工具\n" +
                           "  • 询问「功能」查看已实现的功能\n" +
                           "  • 使用 /help 查看命令列表\n\n" +
                           "要启用真实 AI 功能，请配置相应的 API 密钥。",
                           userInput);
    }
    
    @Override
    public String getModelName() {
        return "mock-model";
    }
    
    @Override
    public String getProviderName() {
        return "mock";
    }
    
    @Override
    public boolean isConfigured() {
        return true; // 模拟模式始终可用
    }
}
