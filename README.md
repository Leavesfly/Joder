# Joder - AI-Powered Terminal Assistant

Joder 是 Kode 项目的 Java 17 实现版本，是一个功能强大的 AI 驱动终端助手工具。

## 项目概述

Joder 为开发者提供智能化的代码辅助、文件操作、命令执行和多模型协作能力。

### 核心特性

- **多模型协作**：支持 Anthropic Claude、OpenAI GPT、阿里云 Qwen、DeepSeek 等多种 AI 模型
- **智能工具链**：提供文件读写、代码编辑、Bash 执行、内容搜索等自动化工具
- **终端原生交互**：基于 Lanterna 的实时交互界面，提供流式响应和丰富的视觉反馈
- **安全权限控制**：多级权限模式保障系统操作安全性
- **可扩展架构**：支持自定义命令、工具扩展、MCP 协议集成

## 技术栈

- **JDK**: Java 17
- **构建工具**: Maven 3.8+
- **终端 UI**: Lanterna 3.1.x
- **JSON 处理**: Jackson 2.16+
- **HTTP 客户端**: OkHttp 4.x
- **依赖注入**: Google Guice 7.x
- **日志框架**: SLF4J + Logback
- **配置管理**: Typesafe Config (HOCON)
- **CLI 解析**: Picocli 4.x

## 快速开始

### 前置要求

- Java 17 或更高版本
- Maven 3.8 或更高版本

### 构建项目

```bash
cd joder
mvn clean package
```

### 运行应用

```bash
java -jar target/joder-1.0.0.jar
```

或使用 Maven：

```bash
mvn exec:java -Dexec.mainClass="io.shareai.joder.JoderApplication"
```

### 显示版本信息

```bash
java -jar target/joder-1.0.0.jar --version
```

### 显示帮助

```bash
java -jar target/joder-1.0.0.jar --help
```

## 项目结构

```
joder/
├── pom.xml                           # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/shareai/joder/
│   │   │       ├── JoderApplication.java      # 主入口
│   │   │       ├── JoderModule.java           # Guice 模块
│   │   │       ├── cli/                       # CLI 命令
│   │   │       ├── ui/                        # 终端 UI
│   │   │       ├── tools/                     # 工具系统
│   │   │       ├── services/                  # 服务层
│   │   │       ├── core/                      # 核心模块
│   │   │       │   ├── config/               # 配置管理
│   │   │       │   ├── context/              # 上下文管理
│   │   │       │   └── permission/           # 权限控制
│   │   │       ├── domain/                    # 领域模型
│   │   │       └── util/                      # 工具类
│   │   └── resources/
│   │       ├── application.conf              # 默认配置
│   │       ├── logback.xml                   # 日志配置
│   │       └── banner.txt                    # 启动横幅
│   └── test/
│       └── java/                             # 测试代码
└── README.md
```

## 配置

### 全局配置

位置：`~/.config/joder/config.conf`

### 项目配置

位置：`{project}/.joder/config.conf`

### 配置示例

```hocon
joder {
  theme = "dark"
  language = "zh-CN"
  
  model {
    default = "claude-3-sonnet"
    
    profiles {
      claude-3-sonnet {
        provider = "anthropic"
        model = "claude-3-5-sonnet-20241022"
        apiKey = ${?ANTHROPIC_API_KEY}
        maxTokens = 8096
      }
    }
  }
  
  permissions {
    mode = "default"
  }
}
```

## 开发进度

### ✅ 阶段一：基础设施搭建

- [x] Maven 项目结构创建
- [x] 依赖管理配置
- [x] 配置管理系统 (ConfigManager)
- [x] 日志系统 (SLF4J + Logback)
- [x] Guice 依赖注入容器
- [x] 主应用入口 (JoderApplication)

### ✅ 阶段二：终端 UI 与 REPL 实现

- [x] Lanterna 终端 UI 集成
- [x] REPL 循环实现
- [x] 命令系统 (CommandParser, HelpCommand, ClearCommand, ConfigCommand, ExitCommand)
- [x] 消息渲染器 (MessageRenderer)
- [x] 主题系统 (ThemeManager, DarkTheme, LightTheme)

### ✅ 阶段三：工具系统与权限控制

- [x] Tool 接口及抽象基类 (Tool, AbstractTool, ToolResult)
- [x] 工具注册表 (ToolRegistry)
- [x] 权限管理器 (PermissionManager, PermissionMode)
- [x] 核心工具实现
  - [x] FileReadTool - 文件读取
  - [x] FileWriteTool - 文件写入
  - [x] BashTool - Bash 命令执行

### ✅ 阶段四：模型通信与消息处理

- [x] ModelAdapter 接口
- [x] MockModelAdapter（模拟模式，用于演示）
- [x] ModelAdapterFactory 工厂类
- [x] REPL 中集成 AI 对话功能
- [ ] Claude、OpenAI、Qwen、DeepSeek 真实适配器（待实现）
- [ ] 流式响应处理（待实现）

### ✅ 阶段五：MCP 集成与高级功能

- [ ] MCP 客户端（计划中）
- [ ] MCP 服务器管理（计划中）
- [ ] 智能代理系统（计划中）

## 许可证

与 Kode 项目保持一致

## 贡献

欢迎提交 Issue 和 Pull Request

---

**注意**: 本项目当前处于开发阶段，功能正在逐步实现中。
