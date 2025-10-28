# Joder Hooks 实现文档

## 概述

本文档说明 Joder 项目中 Hooks 的实现，这些 Hooks 对应 Kode 项目中的 React Hooks 功能。

由于 Joder 采用 **Java + Guice** 架构而非 React 的函数式组件架构，我们使用**面向对象的单例服务类**来实现相同的功能。

## 架构对比

### Kode (TypeScript + React)
- 基于 React Hooks（函数式组件）
- 使用 `useState`, `useEffect`, `useCallback` 等
- 组件生命周期管理

### Joder (Java + Guice)
- 基于依赖注入（Guice）
- 使用单例服务类管理状态
- 传统的面向对象模式

## Hooks 实现列表

### 1. ApiKeyVerificationHook
**对应**: `useApiKeyVerification.ts`

**功能**: API 密钥验证状态管理

**实现**:
```java
@Inject
public MyService(ApiKeyVerificationHook apiKeyHook) {
    VerificationStatus status = apiKeyHook.verify();
    if (status == VerificationStatus.VALID) {
        // 密钥有效
    }
}
```

**状态**:
- `LOADING` - 验证中
- `VALID` - 有效
- `INVALID` - 无效
- `MISSING` - 缺失
- `ERROR` - 错误

---

### 2. CommandHistoryHook
**对应**: `useArrowKeyHistory.ts`

**功能**: 命令历史管理，支持上下箭头导航

**实现**:
```java
@Inject
public ReplScreen(CommandHistoryHook historyHook) {
    // 添加到历史
    historyHook.addToHistory("some command");
    
    // 向上导航
    String prev = historyHook.navigateUp(currentInput);
    
    // 向下导航
    String next = historyHook.navigateDown(currentInput);
}
```

**特性**:
- 最多保存 1000 条历史
- 自动去重连续重复的命令
- 支持上下箭头导航
- 记住用户正在输入的内容

---

### 3. ToolPermissionHook
**对应**: `useCanUseTool.ts`

**功能**: 工具权限检查逻辑

**实现**:
```java
@Inject
public ToolExecutor(ToolPermissionHook permissionHook) {
    PermissionResult result = permissionHook.canUseTool(tool, input);
    
    if (result.isAllowed()) {
        // 执行工具
    } else if (result.needsApproval()) {
        // 请求用户批准
    } else {
        // 拒绝执行
    }
}
```

**权限结果**:
- `allowed()` - 允许执行
- `needsApproval()` - 需要批准
- `denied()` - 拒绝执行

---

### 4. DoublePressHook
**对应**: `useDoublePress.ts`

**功能**: 双击/双按键检测（2秒内）

**实现**:
```java
DoublePressHook doublePress = new DoublePressHook();

doublePress.handlePress(
    () -> System.out.println("第一次按键"),
    () -> System.out.println("双击触发！"),
    isPending -> setExitPending(isPending)
);
```

**应用场景**:
- Ctrl+C 双击退出
- Ctrl+D 双击退出
- ESC 双击清空输入

---

### 5. IntervalHook
**对应**: `useInterval.ts`

**功能**: 定时器管理

**实现**:
```java
IntervalHook interval = new IntervalHook(
    () -> System.out.println("定时执行"),
    1000  // 1秒
);

interval.start();
// ... 稍后
interval.stop();
```

**特性**:
- 自动清理资源
- 支持启动/停止/重启
- 线程安全

---

### 6. MessageLogHook
**对应**: `useLogMessages.ts`

**功能**: 消息日志记录

**实现**:
```java
@Inject
public ChatService(MessageLogHook logHook) {
    // 记录消息
    logHook.logMessages(messages, "session-123", 0);
    
    // 追加消息
    logHook.appendMessage(newMessage, "session-123", 0);
    
    // 读取消息
    List<Message> history = logHook.readMessages("session-123", 0);
}
```

**特性**:
- JSON 格式存储
- 支持会话和分叉
- 自动清理旧日志

---

### 7. StartupTimeHook
**对应**: `useLogStartupTime.ts`

**功能**: 启动时间记录和监控

**实现**:
```java
@Inject
public Application(StartupTimeHook startupHook) {
    startupHook.logStartupTime();
    
    long uptime = startupHook.getUptimeMs();
    String formatted = startupHook.getFormattedUptime();
}
```

---

### 8. NotifyAfterTimeoutHook
**对应**: `useNotifyAfterTimeout.ts`

**功能**: 超时通知（用户无交互时）

**实现**:
```java
@Inject
public Service(NotifyAfterTimeoutHook notifyHook) {
    notifyHook.startNotifyAfterTimeout(
        "任务已完成",
        6000,  // 6秒
        message -> sendDesktopNotification(message)
    );
    
    // 更新交互时间
    notifyHook.updateLastInteractionTime();
}
```

---

### 9. TerminalSizeHook
**对应**: `useTerminalSize.ts`

**功能**: 终端尺寸监听

**实现**:
```java
@Inject
public Renderer(TerminalSizeHook sizeHook) {
    TerminalSize size = sizeHook.getSize();
    int cols = size.getColumns();
    int rows = size.getRows();
    
    // 刷新尺寸
    sizeHook.updateSizeFromTput();
}
```

---

### 10. TextInputHook
**对应**: `useTextInput.ts`

**功能**: 文本输入处理（光标、编辑等）

**实现**:
```java
TextInputHook inputHook = new TextInputHook(historyHook);

// 处理输入
inputHook.handleInput('a', 
    value -> updateDisplay(value),
    value -> submitCommand(value)
);

// 处理特殊键
inputHook.handleSpecialKey(
    SpecialKey.UP_ARROW,
    value -> updateDisplay(value)
);

// 快捷操作
inputHook.deleteWord(value -> updateDisplay(value));
inputHook.deleteToLineStart(value -> updateDisplay(value));
inputHook.moveToPrevWord();
inputHook.moveToNextWord();
```

**支持的操作**:
- 光标移动（左右、行首行尾、单词）
- 删除操作（退格、删除单词、删除到行首/尾）
- 历史导航（上下箭头）
- 特殊键处理

---

### 11. UnifiedCompletionHook
**对应**: `useUnifiedCompletion.ts`

**功能**: 统一补全系统（命令、文件、代理等）

**实现**:
```java
@Inject
public InputHandler(UnifiedCompletionHook completionHook) {
    // 触发补全
    completionHook.triggerCompletion(input, cursorOffset);
    
    // 选择下一个
    completionHook.selectNext();
    
    // 应用补全
    String completed = completionHook.applySelected();
    
    // 处理 Tab 键
    boolean handled = completionHook.handleTab(
        currentInput,
        cursorOffset,
        (newInput, newOffset) -> updateInput(newInput, newOffset)
    );
}
```

**补全类型**:
- 命令补全
- 文件路径补全
- 代理补全
- 模型补全

---

### 12. CancelRequestHook
**对应**: `useCancelRequest.ts`

**功能**: 请求取消处理（ESC 键）

**实现**:
```java
@Inject
public RequestHandler(CancelRequestHook cancelHook) {
    // 注册取消回调
    cancelHook.setOnCancel(() -> {
        abortController.abort();
        cleanupResources();
    });
    
    // 检查是否取消
    if (cancelHook.isCancelled()) {
        return;
    }
    
    // 抛出异常（如果已取消）
    cancelHook.throwIfCancelled();
}
```

---

## 依赖注入配置

在 `JoderModule.java` 中绑定这些 Hooks：

```java
@Override
protected void configure() {
    // Hooks
    bind(ApiKeyVerificationHook.class).in(Singleton.class);
    bind(CommandHistoryHook.class).in(Singleton.class);
    bind(ToolPermissionHook.class).in(Singleton.class);
    bind(DoublePressHook.class).in(Singleton.class);
    bind(MessageLogHook.class).in(Singleton.class);
    bind(StartupTimeHook.class).in(Singleton.class);
    bind(NotifyAfterTimeoutHook.class).in(Singleton.class);
    bind(TerminalSizeHook.class).in(Singleton.class);
    bind(UnifiedCompletionHook.class).in(Singleton.class);
    bind(CancelRequestHook.class).in(Singleton.class);
    
    // TextInputHook 需要特殊处理（依赖 CommandHistoryHook）
}
```

## 使用指南

### 1. 基本使用

通过构造函数注入：

```java
@Inject
public MyService(
    CommandHistoryHook historyHook,
    ToolPermissionHook permissionHook,
    MessageLogHook logHook
) {
    this.historyHook = historyHook;
    this.permissionHook = permissionHook;
    this.logHook = logHook;
}
```

### 2. 生命周期管理

某些 Hooks 需要清理资源：

```java
// 应用关闭时
IntervalHook.shutdown();
notifyAfterTimeoutHook.shutdown();
```

### 3. 测试

Hooks 都是独立的单例服务，易于测试：

```java
@Test
public void testCommandHistory() {
    CommandHistoryHook hook = new CommandHistoryHook();
    hook.addToHistory("ls");
    hook.addToHistory("pwd");
    
    String prev = hook.navigateUp("");
    assertEquals("pwd", prev);
}
```

## 与 Kode 的对应关系

| Kode Hook | Joder Hook | 说明 |
|-----------|------------|------|
| useApiKeyVerification | ApiKeyVerificationHook | API 密钥验证 |
| useArrowKeyHistory | CommandHistoryHook | 命令历史 |
| useCanUseTool | ToolPermissionHook | 工具权限 |
| useCancelRequest | CancelRequestHook | 请求取消 |
| useDoublePress | DoublePressHook | 双击检测 |
| useExitOnCtrlCD | DoublePressHook | Ctrl+C/D 退出 |
| useInterval | IntervalHook | 定时器 |
| useLogMessages | MessageLogHook | 消息日志 |
| useLogStartupTime | StartupTimeHook | 启动时间 |
| useNotifyAfterTimeout | NotifyAfterTimeoutHook | 超时通知 |
| usePermissionRequestLogging | MessageLogHook | 权限日志 |
| useTerminalSize | TerminalSizeHook | 终端尺寸 |
| useTextInput | TextInputHook | 文本输入 |
| useUnifiedCompletion | UnifiedCompletionHook | 统一补全 |

## 总结

Joder 的 Hooks 实现完全对应 Kode 的功能，但采用了更适合 Java 生态的实现方式：

✅ **面向对象** - 使用类而非函数  
✅ **依赖注入** - 使用 Guice 而非 React Context  
✅ **类型安全** - 充分利用 Java 的类型系统  
✅ **单例管理** - 状态由单例服务管理  
✅ **易于测试** - 每个 Hook 都是独立可测试的类  

所有功能都已实现，可以直接在 Joder 项目中使用！
