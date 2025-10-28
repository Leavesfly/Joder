# Joder 快速开始指南

## 简介

Joder 是 Kode 终端 AI 助手的 Java 17 实现版本，支持与多个 AI 提供商（Claude、OpenAI、通义千问、DeepSeek）进行对话交互。

## 安装与配置

### 1. 环境要求

- Java 17 或更高版本
- Maven 3.6+ （仅用于构建）

### 2. 构建项目

```bash
cd joder
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn clean package -DskipTests
```

构建成功后，会在 `target/` 目录生成 `joder-1.0.0.jar` 文件。

### 3. 配置 API 密钥

根据您想使用的 AI 提供商，设置对应的环境变量：

```bash
# Anthropic Claude
export ANTHROPIC_API_KEY="sk-ant-..."

# OpenAI
export OPENAI_API_KEY="sk-..."

# 阿里云通义千问
export QWEN_API_KEY="sk-..."

# DeepSeek
export DEEPSEEK_API_KEY="sk-..."
```

您可以将这些环境变量添加到 `~/.bashrc` 或 `~/.zshrc` 文件中以持久化配置。

## 使用方法

### 启动 Joder

```bash
# 方式一：直接运行 JAR
java -jar target/joder-1.0.0.jar

# 方式二：使用启动脚本
./bin/joder.sh

# 方式三：使用 Maven
mvn exec:java -Dexec.mainClass="io.shareai.joder.JoderApplication"
```

### 基本命令

启动后，您会看到如下界面：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🚀 欢迎使用 Joder AI 助手
  版本: 1.0.0
  当前模型: mock-model (mock)
  配置状态: ✅ 就绪
  主题: dark
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 提示:
  • 输入 /help 查看可用命令
  • 输入 /model 查看或切换模型
  • 直接输入消息开始与 AI 对话
  • 按 Ctrl+C 或输入 /exit 退出

> 
```

### 常用命令列表

| 命令 | 功能 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/model` | 查看当前模型信息 | `/model` |
| `/model <名称>` | 切换到指定模型 | `/model gpt-4o` |
| `/config` | 查看所有配置 | `/config` |
| `/config get <键>` | 查看特定配置 | `/config get joder.theme` |
| `/config set <键> <值>` | 设置配置项 | `/config set joder.theme light` |
| `/clear` | 清空屏幕 | `/clear` |
| `/exit` 或 `/quit` | 退出程序 | `/exit` |

### 切换 AI 模型

Joder 支持多个预配置的模型：

```bash
# 切换到 Claude 3.5 Sonnet
> /model claude-3-sonnet

# 切换到 GPT-4o
> /model gpt-4o

# 切换到通义千问 Max
> /model qwen-max

# 切换到 DeepSeek Chat
> /model deepseek-chat
```

### 与 AI 对话

直接输入您的问题或指令，无需任何前缀：

```bash
> 你好，请介绍一下你自己

> 帮我写一个 Java 冒泡排序算法

> 解释一下什么是依赖注入
```

## 配置文件

### 全局配置

配置文件位于 `src/main/resources/application.conf`，包含：

- 默认模型配置
- 各提供商的 API 端点
- 权限设置
- 日志配置

### 模型配置示例

```hocon
joder {
  model {
    default = "claude-3-sonnet"
    
    profiles {
      claude-3-sonnet {
        provider = "anthropic"
        model = "claude-3-5-sonnet-20241022"
        apiKeyEnv = "ANTHROPIC_API_KEY"
        baseUrl = "https://api.anthropic.com"
        maxTokens = 8096
        temperature = 0.7
      }
      
      gpt-4o {
        provider = "openai"
        model = "gpt-4o"
        apiKeyEnv = "OPENAI_API_KEY"
        baseUrl = "https://api.openai.com/v1"
        maxTokens = 4096
        temperature = 0.7
      }
    }
  }
}
```

## 常见问题

### Q: 提示 "未配置 API Key" 怎么办？

A: 请确保已设置对应的环境变量。使用 `/model` 命令查看当前模型的配置状态。

### Q: 如何修改默认模型？

A: 有两种方式：
1. 修改 `application.conf` 中的 `joder.model.default` 配置
2. 使用命令：`/config set joder.model.default gpt-4o`

### Q: 支持流式响应吗？

A: 是的，所有适配器都支持流式响应，但当前 REPL 实现使用的是非流式模式。流式模式将在后续版本中启用。

### Q: 如何查看日志？

A: 日志文件默认位于 `~/.local/share/joder/logs/application.log`

## 高级功能

### 添加自定义模型配置

您可以在配置文件中添加自定义模型：

```hocon
joder {
  model {
    profiles {
      my-custom-model {
        provider = "openai"  # 或其他兼容的提供商
        model = "custom-model-name"
        apiKeyEnv = "MY_API_KEY"
        baseUrl = "https://api.example.com/v1"
        maxTokens = 4096
        temperature = 0.7
      }
    }
  }
}
```

然后使用 `/model my-custom-model` 切换到该模型。

### 权限模式

Joder 支持四种权限模式（当前版本默认使用 `default` 模式）：

- `default` - 默认模式，编辑操作需要确认
- `acceptEdits` - 自动批准所有编辑操作
- `plan` - 只读模式，不执行任何修改
- `bypassPermissions` - 绕过所有权限检查（高风险）

## 下一步

- 尝试与不同的 AI 模型对话
- 探索各个命令的功能
- 查看 `IMPLEMENTATION_SUMMARY.md` 了解项目详情
- 期待 MCP 集成功能的到来！

## 获取帮助

如有问题，请：
1. 查看日志文件排查错误
2. 使用 `/help` 命令查看帮助
3. 查阅项目文档

祝使用愉快！ 🚀
