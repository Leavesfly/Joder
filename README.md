# Joder - AI 驱动的终端助手

<div align="center">

**Joder** 是 Kode 项目的 Java 17 实现版本，一个功能强大的 AI 驱动终端助手工具。

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

[快速开始](#快速开始) • [功能特性](#核心功能) • [使用文档](#使用指南) • [开发文档](#开发指南)

</div>

---

## 📖 项目概述

Joder 为开发者提供智能化的代码辅助、文件操作、命令执行和多模型协作能力，通过终端原生交互界面，帮助您更高效地完成开发任务。

### ✨ 核心特性

- **🤖 多模型协作**：支持 Anthropic Claude、OpenAI GPT、阿里云 Qwen、DeepSeek 等多种 AI 模型，灵活切换
- **🛠️ 智能工具链**：17+ 内置工具，包括文件操作、代码编辑、Bash 执行、网络搜索、任务管理、记忆系统等
- **💻 终端原生交互**：基于 Lanterna 的实时交互界面，支持流式响应、语法高亮、主题切换
- **🔒 安全权限控制**：四级权限模式保障系统操作安全性，支持工具白名单与文件系统隔离
- **🔌 可扩展架构**：支持自定义命令、工具扩展、MCP (Model Context Protocol) 协议集成
- **📝 智能上下文**：自动检测文件新鲜度、Git 变更、Mention 引用，提供精准上下文

---

## 🌟 核心功能

### 🤖 AI 模型支持

Joder 支持多个主流 AI 提供商，可灵活切换：

| 提供商 | 支持模型 | API 类型 |
|---------|----------|----------|
| **Anthropic** | Claude 3.5 Sonnet, Claude 3 Opus | Messages API |
| **OpenAI** | GPT-4o, GPT-4 Turbo | ChatCompletions API |
| **阿里云** | Qwen Max, Qwen Turbo | DashScope API |
| **DeepSeek** | DeepSeek Chat, DeepSeek Coder | Compatible API |

所有模型均支持：
- ✅ 同步调用
- ✅ 流式响应 (SSE)
- ✅ 上下文管理
- ✅ 工具调用 (Function Calling)
- ✅ 错误重试机制

### 🛠️ 内置工具 (17+)

Joder 提供丰富的内置工具，让 AI 能够完成复杂任务：

#### 📂 文件操作

- **FileReadTool** - 读取文件内容，支持行范围指定
- **FileWriteTool** - 创建或覆盖文件，自动创建父目录
- **FileTreeTool** - 显示目录树结构
- **LsTool** - 列出目录内容，支持过滤
- **GlobTool** - 通配符文件搜索
- **GrepTool** - 正则表达式搜索文件内容

#### ✏️ 代码编辑

- **EditFileTool** - 基于 diff 的智能代码编辑
- **SearchReplaceTool** - 批量搜索替换

#### 🖥️ 命令执行

- **BashTool** - 执行 Bash 命令，内置危险命令检测

#### 🌐 网络工具

- **WebSearchTool** - DuckDuckGo 网络搜索，无需 API Key
- **URLFetcherTool** - 获取网页主要内容，智能清理广告
- **WebReadabilityTool** - 高级网页内容提取

#### 🧠 记忆系统

- **MemoryReadTool** - 搜索项目记忆/知识库
- **MemoryWriteTool** - 保存重要信息，支持项目隔离
- **MemoryDeleteTool** - 删除过时记忆

#### ☑️ 任务管理

- **TodoWriteTool** - 任务增删改查，支持层级结构
- **TodoReadTool** - 查看任务列表与状态

#### 🤔 其他工具

- **ThinkTool** - AI 内部推理过程记录
- **AskExpertModelTool** - 咨询专家模型
- **CompletionTool** - 智能上下文补全

### 🔌 MCP 协议支持

Joder 完整实现 Model Context Protocol (MCP) 规范：

- **JSON-RPC 协议层** - 完整的 JSON-RPC 2.0 实现
- **MCP 客户端** - stdio 通信，支持进程管理
- **服务器管理** - 动态启停、配置重载
- **工具适配** - 自动将 MCP 工具转换为 Joder 工具
- **命令集成** - `/mcp` 系列命令管理 MCP 服务

示例配置：

```hocon
mcp {
  servers {
    filesystem {
      command = "npx"
      args = ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"]
      enabled = true
    }
    github {
      command = "npx"
      args = ["-y", "@modelcontextprotocol/server-github"]
      env {
        GITHUB_TOKEN = ${?GITHUB_TOKEN}
      }
      enabled = true
    }
  }
}
```

### 📊 智能上下文系统

Joder 提供多维度的上下文管理：

- **文件新鲜度检测** - 自动检测文件修改时间
- **Git 集成** - 自动读取 Git 变更信息
- **Mention 引用** - 支持 `@file`、`@folder` 引用
- **上下文压缩** - 智能 Token 管理
- **代码分析** - 自动提取代码结构

### 🎨 用户界面

- **主题系统** - 深色/浅色主题切换
- **语法高亮** - 支持 Java、Python、JavaScript 等多种语言
- **Diff 渲染** - 美化的代码差异显示
- **流式输出** - 实时显示 AI 响应
- **进度指示** - 长时间操作显示进度

---

## 🛠️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 构建工具 | Maven | 3.8+ |
| 终端 UI | Lanterna | 3.1.2 |
| JSON 处理 | Jackson | 2.16.1 |
| HTTP 客户端 | OkHttp (+SSE) | 4.12.0 |
| 依赖注入 | Google Guice | 7.0.0 |
| 日志框架 | SLF4J + Logback | 2.0.11 / 1.4.14 |
| 配置管理 | Typesafe Config | 1.4.3 |
| CLI 解析 | Picocli | 4.7.5 |
| HTML 解析 | Jsoup | 1.17.2 |
| Diff 工具 | java-diff-utils | 4.12 |
| YAML 解析 | SnakeYAML | 2.2 |
| 终端颜色 | Jansi | 2.4.1 |
| 测试框架 | JUnit 5 + Mockito | 5.10.1 / 5.8.0 |

---

## 🚀 快速开始

### 前置要求

- **Java 17+**：请确保已安装 JDK 17 或更高版本
- **Maven 3.8+**：用于项目构建（可选）

### 安装步骤

#### 1️⃣ 克隆仓库

```bash
git clone https://github.com/yourusername/joder.git
cd joder
```

#### 2️⃣ 构建项目

```bash
mvn clean package
```

构建成功后，将在 `target/` 目录下生成 `joder-1.0.0.jar`。

#### 3️⃣ 配置 API Key

根据您使用的 AI 模型，设置相应的环境变量：

```bash
# Anthropic Claude
export ANTHROPIC_API_KEY="sk-ant-..."

# OpenAI GPT
export OPENAI_API_KEY="sk-..."

# 阿里云通义千问
export QWEN_API_KEY="sk-..."

# DeepSeek
export DEEPSEEK_API_KEY="sk-..."
```

💡 **提示**：可将这些配置添加到 `~/.bashrc` 或 `~/.zshrc` 以永久生效。

### 运行方式

```bash
# 方式一：直接运行 JAR 包（推荐）
java -jar target/joder-1.0.0.jar

# 方式二：使用启动脚本
./bin/joder.sh                    # macOS/Linux
.\bin\joder.bat                   # Windows

# 方式三：通过 Maven 运行
mvn exec:java -Dexec.mainClass="io.shareai.joder.JoderApplication"
```

### 命令行参数

```bash
# 显示版本信息
java -jar target/joder-1.0.0.jar --version

# 显示帮助信息
java -jar target/joder-1.0.0.jar --help

# 指定工作目录
java -jar target/joder-1.0.0.jar --cwd /path/to/project

# 启用详细日志
java -jar target/joder-1.0.0.jar --verbose
```

---

## 📁 项目结构

```
joder/
├── bin/                                  # 启动脚本
│   ├── joder.sh                         # Linux/macOS 启动脚本
│   └── joder.bat                        # Windows 启动脚本
├── doc/                                  # 项目文档
│   ├── QUICK_START.md                   # 快速开始指南
│   ├── IMPLEMENTATION_SUMMARY.md        # 实现总结
│   ├── JODER_FEATURES_IMPLEMENTATION_REPORT.md
│   └── ...                              # 其他技术文档
├── src/
│   ├── main/
│   │   ├── java/io/shareai/joder/
│   │   │   ├── JoderApplication.java    # 应用主入口
│   │   │   ├── JoderModule.java         # Guice 依赖注入模块
│   │   │   ├── WorkingDirectory.java    # 工作目录注解
│   │   │   ├── cli/                     # CLI 命令层
│   │   │   │   ├── Command.java
│   │   │   │   ├── CommandParser.java
│   │   │   │   └── commands/            # 24+ 内置命令
│   │   │   ├── ui/                      # 用户界面层
│   │   │   │   ├── components/          # UI 组件
│   │   │   │   │   └── MessageRenderer.java
│   │   │   │   ├── renderer/            # 渲染器
│   │   │   │   │   ├── DiffRenderer.java
│   │   │   │   │   └── SyntaxHighlighter.java
│   │   │   │   └── theme/               # 主题系统
│   │   │   ├── screens/                 # 界面
│   │   │   │   └── ReplScreen.java
│   │   │   ├── tools/                   # 工具系统 (17+ 工具)
│   │   │   │   ├── Tool.java
│   │   │   │   ├── AbstractTool.java
│   │   │   │   ├── ToolRegistry.java
│   │   │   │   ├── file/                # 文件操作
│   │   │   │   ├── edit/                # 代码编辑
│   │   │   │   ├── bash/                # 命令执行
│   │   │   │   ├── web/                 # 网络工具
│   │   │   │   ├── memory/              # 记忆系统
│   │   │   │   ├── todo/                # 任务管理
│   │   │   │   ├── mcp/                 # MCP 集成
│   │   │   │   └── ...                  # 其他工具
│   │   │   ├── services/                # 服务层
│   │   │   │   ├── model/               # 模型适配器
│   │   │   │   ├── adapters/            # API 适配器
│   │   │   │   ├── mcp/                 # MCP 服务
│   │   │   │   ├── completion/          # 上下文补全
│   │   │   │   ├── MemoryManager.java
│   │   │   │   └── TaskManager.java
│   │   │   ├── core/                    # 核心模块
│   │   │   │   ├── config/              # 配置管理
│   │   │   │   ├── context/             # 上下文管理
│   │   │   │   └── permission/          # 权限系统
│   │   │   ├── domain/                  # 领域模型
│   │   │   ├── hooks/                   # 事件钩子
│   │   │   ├── util/                    # 工具类 (20+ 工具)
│   │   │   └── examples/                # 示例代码
│   │   └── resources/
│   │       ├── agents/                  # Agent 配置
│   │       ├── application.conf         # 默认配置
│   │       ├── logback.xml              # 日志配置
│   │       └── banner.txt               # 启动横幅
│   └── test/java/                       # 单元测试
├── pom.xml                               # Maven 配置
├── README.md                             # 项目说明
└── test-tools.sh                         # 工具测试脚本
```

---

## 📚 配置管理

### 配置文件位置

Joder 支持多层级配置，优先级从高到低：

1. **命令行参数** - 最高优先级
2. **项目配置** - `{project}/.joder/config.conf`
3. **全局配置** - `~/.config/joder/config.conf`
4. **默认配置** - `src/main/resources/application.conf`

### 配置示例

```hocon
joder {
  # 全局设置
  theme = "dark"                    # 主题: dark/light
  language = "zh-CN"                # 语言
  
  # 模型配置
  model {
    default = "claude-3-sonnet"     # 默认模型
    
    profiles {
      # Anthropic Claude
      claude-3-sonnet {
        provider = "anthropic"
        model = "claude-3-5-sonnet-20241022"
        apiKeyEnv = "ANTHROPIC_API_KEY"
        baseUrl = "https://api.anthropic.com"
        maxTokens = 8096
        temperature = 0.7
      }
      
      # OpenAI GPT
      gpt-4o {
        provider = "openai"
        model = "gpt-4o"
        apiKeyEnv = "OPENAI_API_KEY"
        baseUrl = "https://api.openai.com/v1"
        maxTokens = 4096
        temperature = 0.7
      }
      
      # 阿里云通义千问
      qwen-max {
        provider = "qwen"
        model = "qwen-max"
        apiKeyEnv = "QWEN_API_KEY"
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        maxTokens = 8000
        temperature = 0.7
      }
      
      # DeepSeek
      deepseek-chat {
        provider = "deepseek"
        model = "deepseek-chat"
        apiKeyEnv = "DEEPSEEK_API_KEY"
        baseUrl = "https://api.deepseek.com/v1"
        maxTokens = 4096
        temperature = 0.7
      }
    }
  }
  
  # 权限设置
  permissions {
    mode = "default"                # default/acceptEdits/plan/bypassPermissions
    trustedTools = []               # 受信任工具列表
    fileSystemWhitelist = []        # 文件系统白名单
  }
  
  # MCP 服务器配置
  mcp {
    servers {
      # 示例：文件系统服务器
      # filesystem {
      #   command = "npx"
      #   args = ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/dir"]
      #   enabled = true
      # }
    }
  }
}

---

## 📝 使用指南

### 启动界面

启动 Joder 后，您将看到如下欢迎界面：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🚀 欢迎使用 Joder AI 助手
  版本: 1.0.0
  当前模型: claude-3-sonnet (anthropic)
  配置状态: ✅ 就绪
  主题: dark
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 提示:
  • 输入 /help 查看可用命令
  • 输入 /model 查看或切换模型
  • 直接输入消息开始与 AI 对话
  • 按 Ctrl+C 或输入 /exit 退出

> 
```

### 内置命令

Joder 提供 24+ 内置命令，以下是常用命令：

| 命令 | 功能 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/model` | 查看当前模型信息 | `/model` |
| `/model <名称>` | 切换到指定模型 | `/model gpt-4o` |
| `/config` | 查看所有配置 | `/config` |
| `/config get <键>` | 查看特定配置 | `/config get joder.theme` |
| `/config set <键> <值>` | 设置配置项 | `/config set joder.theme light` |
| `/clear` | 清空屏幕 | `/clear` |
| `/history` | 显示对话历史 | `/history` |
| `/init` | 初始化项目配置 | `/init` |
| `/mcp list` | 列出 MCP 服务器 | `/mcp list` |
| `/mcp tools` | 显示 MCP 工具 | `/mcp tools` |
| `/doctor` | 检查系统配置 | `/doctor` |
| `/cost` | 查看 API 调用成本 | `/cost` |
| `/export` | 导出对话历史 | `/export conversation.json` |
| `/exit` / `/quit` | 退出程序 | `/exit` |

💡 **提示**：输入 `/help` 查看全部命令列表。

### 与 AI 对话

直接输入您的问题或指令，无需任何前缀：

```bash
> 你好，请介绍一下你自己

> 帮我写一个 Java 冒泡排序算法

> 解释一下什么是依赖注入

> 读取 README.md 文件并总结主要内容
```

### 切换 AI 模型

```bash
# 查看当前模型
> /model

# 切换到 Claude 3.5 Sonnet
> /model claude-3-sonnet

# 切换到 GPT-4o
> /model gpt-4o

# 切换到通义千问 Max
> /model qwen-max

# 切换到 DeepSeek Chat
> /model deepseek-chat
```

### 使用工具

AI 可以自动调用工具来完成任务，例如：

```bash
# 示例 1：文件操作
> 读取 src/main/java/Main.java 文件的前 50 行

# 示例 2：代码编辑
> 将 README.md 中的所有 "Kode" 替换为 "Joder"

# 示例 3：网络搜索
> 搜索 Java 17 新特性

# 示例 4：任务管理
> 帮我创建一个任务列表：1. 实现登录功能 2. 编写单元测试
```

### 权限管理

Joder 支持四种权限模式：

| 模式 | 说明 | 使用场景 |
|------|------|----------|
| `default` | 编辑操作需要确认 | 默认模式，平衡安全与便利 |
| `acceptEdits` | 自动批准所有编辑操作 | 信任 AI，快速开发 |
| `plan` | 只读模式，不执行任何修改 | 仅查看 AI 计划 |
| `bypassPermissions` | 绕过所有权限检查 | 高风险，不推荐 |

修改权限模式：

```bash
# 通过命令修改
> /config set joder.permissions.mode acceptEdits

# 或编辑配置文件
~/.config/joder/config.conf
```

---

## 👨‍💻 开发指南

### 项目架构

Joder 采用分层架构设计：

```
┌───────────────────────────────────┐
│        CLI 层 (commands/)          │
│  命令解析、命令处理、参数验证    │
├───────────────────────────────────┤
│         UI 层 (ui/)              │
│  REPL界面、消息渲染、主题管理   │
├───────────────────────────────────┤
│      服务层 (services/)          │
│  模型适配、MCP管理、上下文补全  │
├───────────────────────────────────┤
│       工具层 (tools/)            │
│  文件操作、代码编辑、网络工具   │
├───────────────────────────────────┤
│       核心层 (core/)             │
│  配置管理、权限控制、上下文   │
├───────────────────────────────────┤
│      领域层 (domain/)           │
│  数据模型、业务实体、枚举类型   │
├───────────────────────────────────┤
│       工具层 (util/)             │
│  通用工具类、辅助功能           │
└───────────────────────────────────┘
```

### 核心组件

#### 1. 依赖注入 (Guice)

```java
// JoderModule.java - 配置绑定
public class JoderModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConfigManager.class).in(Singleton.class);
        bind(ToolRegistry.class).in(Singleton.class);
        bind(PermissionManager.class).in(Singleton.class);
        // ...
    }
}

// 使用示例
@Inject
public MyService(ConfigManager config, ToolRegistry tools) {
    this.config = config;
    this.tools = tools;
}
```

#### 2. 配置管理 (ConfigManager)

```java
// 读取配置
String theme = configManager.getString("joder.theme", "dark");
int maxTokens = configManager.getInt("joder.model.profiles.claude-3-sonnet.maxTokens", 4096);

// 设置配置
configManager.set("joder.theme", "light");
```

#### 3. 工具系统 (Tool)

```java
// 自定义工具
public class MyCustomTool extends AbstractTool {
    public MyCustomTool() {
        super("my_custom_tool", "自定义工具描述");
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        // 工具实现逻辑
        return ToolResult.success("执行成功");
    }
    
    @Override
    public Map<String, Object> getParameterSchema() {
        // 定义参数 Schema
        return Map.of("type", "object", "properties", Map.of(...));
    }
}

// 注册工具
toolRegistry.register(new MyCustomTool());
```

#### 4. 模型适配器 (ModelAdapter)

```java
// 创建适配器
ModelAdapter adapter = modelAdapterFactory.createAdapter("claude-3-sonnet");

// 同步调用
ModelResponse response = adapter.chat(request);

// 异步流式调用
adapter.chatStream(request, new StreamHandler() {
    @Override
    public void onEvent(StreamEvent event) {
        if (event.getType() == StreamEvent.Type.CONTENT_DELTA) {
            System.out.print(event.getContent());
        }
    }
});
```

### 添加新功能

#### 添加新命令

1. 在 `cli/commands/` 下创建命令类：

```java
public class MyCommand implements Command {
    @Override
    public String getName() {
        return "mycommand";
    }
    
    @Override
    public String getDescription() {
        return "自定义命令描述";
    }
    
    @Override
    public CommandResult execute(String[] args) {
        // 命令实现
        return CommandResult.success("执行成功");
    }
}
```

2. 在 `CommandParser` 中注册：

```java
commands.put("mycommand", injector.getInstance(MyCommand.class));
```

#### 添加新工具

1. 在 `tools/` 下创建目录和工具类
2. 继承 `AbstractTool` 并实现方法
3. 在 `JoderModule` 中注册：

```java
toolRegistry.register(injector.getInstance(MyCustomTool.class));
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ToolRegistryTest

# 运行特定测试方法
mvn test -Dtest=ToolRegistryTest#testRegisterTool

# 跳过测试构建
mvn clean package -DskipTests
```

### 代码规范

- **命名规范**：
  - 类名：PascalCase (`MyClassName`)
  - 方法名：camelCase (`myMethodName`)
  - 常量：UPPER_SNAKE_CASE (`MY_CONSTANT`)
  
- **注释规范**：
  - 公共 API 必须有 Javadoc
  - 复杂逻辑需要内联注释
  
- **日志规范**：
  - 使用 SLF4J Logger
  - 日志级别：DEBUG < INFO < WARN < ERROR

### 调试技巧

```bash
# 启用详细日志
java -jar target/joder-1.0.0.jar --verbose

# 查看日志文件
tail -f ~/.local/share/joder/logs/application.log

# 设置日志级别
export JODER_LOG_LEVEL=DEBUG
java -Dlogback.configurationFile=custom-logback.xml -jar target/joder-1.0.0.jar
```

### 常见问题

#### Q: 提示“未配置 API Key”怎么办？

A: 请确保已设置对应的环境变量。使用 `/model` 命令查看当前模型的配置状态。

```bash
export ANTHROPIC_API_KEY="your-api-key"
```

#### Q: 如何修改默认模型？

A: 有两种方式：

1. 修改 `application.conf` 中的 `joder.model.default` 配置
2. 使用命令：`/config set joder.model.default gpt-4o`

#### Q: 支持流式响应吗？

A: 是的，所有适配器都支持流式响应。REPL 会实时显示 AI 输出。

#### Q: 如何查看日志？

A: 日志文件默认位于 `~/.local/share/joder/logs/application.log`

```bash
tail -f ~/.local/share/joder/logs/application.log
```

---

## 📚 相关文档

- [**快速开始指南**](doc/QUICK_START.md) - 详细的入门教程
- [**实现总结**](doc/IMPLEMENTATION_SUMMARY.md) - 项目实现的技术细节
- [**功能实现报告**](doc/JODER_FEATURES_IMPLEMENTATION_REPORT.md) - 功能实现进度
- [**最终报告**](doc/JODER_FINAL_COMPLETE_REPORT.md) - 项目完整总结

---

## 🤝 贡献指南

欢迎为 Joder 项目贡献！请遵循以下流程：

1. **Fork** 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 **Pull Request**

### 代码风格

- 遵循 Java 编码规范
- 编写单元测试
- 添加必要的注释和文档

---

## 📝 许可证

Apache License 2.0 - 与 Kode 项目保持一致

---

## 🚀 路线图

- [x] 基础设施搭建
- [x] 终端 UI 与 REPL 实现
- [x] 工具系统与权限控制
- [x] 模型通信与消息处理
- [x] MCP 集成与高级功能
- [x] 多模型适配器 (Claude/OpenAI/Qwen/DeepSeek)
- [x] 流式响应处理
- [x] 智能上下文管理
- [ ] Notebook 支持 (Jupyter)
- [ ] 更多模型提供商集成
- [ ] 性能优化与并发处理

---

## ⭐ Star History

如果您觉得 Joder 对您有帮助，请给我们一个 Star ⭐！

---

<div align="center">

**使用 ❤️ 和 Java 构建**

[GitHub](https://github.com/yourusername/joder) • [Issues](https://github.com/yourusername/joder/issues) • [Discussions](https://github.com/yourusername/joder/discussions)

</div>
