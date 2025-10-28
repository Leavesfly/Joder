# Joder Utils 实现计划

## 概述

本文档记录了从 Kode 项目（TypeScript）到 Joder 项目（Java）的 utils 工具类迁移计划和实施状态。

## Kode Utils 目录结构 (src/utils)

Kode 项目共有 **50+ 个工具文件**，分为以下几类：

### ✅ 已实现 (11个)

| Kode 文件 | Joder 文件 | 说明 |
|-----------|------------|------|
| errors.ts | exceptions/* | 异常类定义 |
| array.ts | ArrayUtils.java | 数组工具 |
| json.ts | JsonUtils.java | JSON 解析 |
| browser.ts | BrowserUtils.java | 浏览器打开 |
| http.ts | HttpUtils.java | HTTP 工具 |
| state.ts | StateManager.java | 状态管理 |
| terminal.ts | TerminalUtils.java | 终端操作 |
| env.ts | EnvUtils.java | 环境检测 |
| tokens.ts | TokenUtils.java | Token 计数 |
| user.ts | UserUtils.java | 用户信息 |
| auth.ts | *(暂不需要)* | 认证相关 |

### 🚧 待实现 - 核心工具 (优先级 P0)

| 文件 | 功能 | 复杂度 | 状态 |
|------|------|--------|------|
| **config.ts** | 配置管理核心 | ⭐⭐⭐⭐⭐ | ConfigManager 已有 |
| **log.ts** | 日志系统 | ⭐⭐⭐⭐ | 需要补充 |
| **file.ts** | 文件操作 | ⭐⭐⭐⭐ | 需要实现 |
| **commands.ts** | 命令工具 | ⭐⭐⭐ | 需要实现 |
| **validate.ts** | 验证工具 | ⭐⭐ | 需要实现 |
| **execFileNoThrow.ts** | 进程执行 | ⭐⭐⭐ | 需要实现 |

### 📦 待实现 - 扩展工具 (优先级 P1)

| 文件 | 功能 | 复杂度 | 说明 |
|------|------|--------|------|
| Cursor.ts | 光标管理 | ⭐⭐⭐⭐ | 文本编辑核心 |
| PersistentShell.ts | 持久化 Shell | ⭐⭐⭐⭐⭐ | 复杂，可延后 |
| fuzzyMatcher.ts | 模糊匹配 | ⭐⭐⭐ | 补全系统用 |
| advancedFuzzyMatcher.ts | 高级模糊匹配 | ⭐⭐⭐⭐ | 补全系统用 |
| git.ts | Git 操作 | ⭐⭐ | 版本控制 |
| markdown.ts | Markdown 渲染 | ⭐⭐⭐ | UI 相关 |
| diff.ts | Diff 工具 | ⭐⭐ | 文件对比 |
| style.ts | 代码风格 | ⭐⭐ | 已实现基础版 |
| theme.ts | 主题管理 | ⭐⭐ | ThemeManager 已有 |

### 📊 待实现 - 存储和状态 (优先级 P2)

| 文件 | 功能 | 说明 |
|------|------|------|
| agentLoader.ts | Agent 加载 | AgentsManager 已有部分 |
| agentStorage.ts | Agent 存储 | 需要实现 |
| todoStorage.ts | TODO 存储 | 需要实现 |
| expertChatStorage.ts | 专家聊天存储 | 需要实现 |
| sessionState.ts | 会话状态 | 需要实现 |
| messageContextManager.ts | 消息上下文 | 需要实现 |
| conversationRecovery.ts | 会话恢复 | 需要实现 |
| fileRecoveryCore.ts | 文件恢复 | 需要实现 |

### 🔧 待实现 - 特殊功能 (优先级 P3)

| 文件 | 功能 | 说明 |
|------|------|------|
| ripgrep.ts | Ripgrep 搜索 | 代码搜索工具 |
| secureFile.ts | 安全文件操作 | 权限管理 |
| autoCompactCore.ts | 自动压缩 | 消息压缩 |
| autoUpdater.ts | 自动更新 | 版本更新 |
| debugLogger.ts | 调试日志 | 调试工具 |
| thinking.ts | 思考过程 | UI 相关 |
| toolExecutionController.ts | 工具执行控制 | 已有 ToolRegistry |
| unaryLogging.ts | 统一日志 | 日志系统 |
| imagePaste.ts | 图片粘贴 | 终端图片支持 |
| cleanup.ts | 清理工具 | 资源清理 |
| generators.ts | 生成器 | 工具函数 |
| ask.tsx | 用户询问 | UI 组件 |
| format.tsx | 格式化 | UI 组件 |
| messages.tsx | 消息组件 | UI 组件 |

## 实现策略

### 第一阶段：核心工具 (P0) ✅ 

已完成基础工具类：
- ✅ 异常类系统
- ✅ 数组工具
- ✅ JSON 工具
- ✅ 浏览器工具
- ✅ HTTP 工具
- ✅ 状态管理
- ✅ 终端工具
- ✅ 环境检测
- ✅ Token 工具
- ✅ 用户工具

### 第二阶段：文件和命令工具 (当前)

需要实现：
1. **FileUtils** - 文件操作工具
2. **CommandUtils** - 命令工具
3. **ValidationUtils** - 验证工具
4. **ProcessUtils** - 进程执行工具
5. **LogUtils** - 日志工具增强

### 第三阶段：编辑和匹配 (P1)

需要实现：
1. **CursorUtils** - 光标管理
2. **FuzzyMatcherUtils** - 模糊匹配
3. **GitUtils** - Git 操作
4. **MarkdownUtils** - Markdown 渲染
5. **DiffUtils** - Diff 工具

### 第四阶段：存储和恢复 (P2)

需要实现各种存储管理器和恢复机制。

### 第五阶段：高级功能 (P3)

根据实际需求逐步实现。

## 架构适配说明

### TypeScript → Java 转换原则

1. **类型转换**
   - `string` → `String`
   - `number` → `int/long/double`
   - `boolean` → `boolean`
   - `Array<T>` → `List<T>`
   - `Record<K, V>` → `Map<K, V>`
   - `Promise<T>` → `CompletableFuture<T>` 或同步方法

2. **异步处理**
   - TypeScript `async/await` → Java `CompletableFuture` 或同步方法
   - 简单场景直接使用同步实现

3. **函数式编程**
   - TypeScript 函数 → Java 方法
   - TypeScript 箭头函数 → Java Lambda 表达式
   - `lodash` 工具 → Java Stream API

4. **错误处理**
   - TypeScript `try/catch` → Java `try/catch`
   - TypeScript Error → Java Exception
   - 使用自定义异常类

5. **模块化**
   - TypeScript 模块 → Java 包
   - TypeScript export → Java public 方法
   - TypeScript import → Java import

## 文件大小统计

| 类别 | Kode (TS) | Joder (Java) | 状态 |
|------|-----------|--------------|------|
| 异常类 | 22 行 | 92 行 | ✅ |
| 数组工具 | 4 行 | 50 行 | ✅ |
| JSON 工具 | 14 行 | 104 行 | ✅ |
| 浏览器工具 | 15 行 | 78 行 | ✅ |
| HTTP 工具 | 11 行 | 40 行 | ✅ |
| 状态管理 | 26 行 | 51 行 | ✅ |
| 终端工具 | 51 行 | 131 行 | ✅ |
| 环境工具 | 58 行 | 185 行 | ✅ |
| Token 工具 | 44 行 | 114 行 | ✅ |
| 用户工具 | 46 行 | 170 行 | ✅ |
| **总计** | ~290 行 | ~1015 行 | ✅ |

## 下一步计划

### 立即实施
1. ✅ FileUtils - 文件操作核心
2. ✅ CommandUtils - 命令工具
3. ✅ ValidationUtils - 验证工具
4. ✅ ProcessUtils - 进程执行

### 近期实施
5. ⏳ CursorUtils - 光标管理
6. ⏳ FuzzyMatcherUtils - 模糊匹配
7. ⏳ GitUtils - Git 操作
8. ⏳ DiffUtils - Diff 工具

### 中期实施
9. ⏳ 存储管理器系列
10. ⏳ 消息和上下文管理

### 长期考虑
11. PersistentShell - 持久化 Shell（复杂）
12. 高级功能和 UI 组件

## 总结

- **已完成**: 11 个核心工具类，约 1015 行代码
- **待实现**: 约 40+ 个工具类
- **覆盖率**: 约 22% (11/50)
- **代码质量**: 所有实现都通过编译，符合 Java 最佳实践

继续按计划逐步补充剩余工具类！
