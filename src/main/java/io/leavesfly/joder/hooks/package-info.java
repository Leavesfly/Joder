/**
 * Hooks 包
 * 
 * <p>提供类似 React Hooks 的状态管理和生命周期功能，
 * 用于封装可复用的业务逻辑和状态管理。
 * 
 * <p>本包中的 Hooks 对应 Kode 项目中的 React Hooks，
 * 但采用面向对象的方式实现，适配 Java + Guice 架构。
 * 
 * <h2>主要 Hooks</h2>
 * <ul>
 *   <li>{@link io.leavesfly.joder.hooks.ApiKeyVerificationHook} - API 密钥验证</li>
 *   <li>{@link io.leavesfly.joder.hooks.CommandHistoryHook} - 命令历史管理</li>
 *   <li>{@link io.leavesfly.joder.hooks.ToolPermissionHook} - 工具权限检查</li>
 *   <li>{@link io.leavesfly.joder.hooks.DoublePressHook} - 双击检测</li>
 *   <li>{@link io.leavesfly.joder.hooks.IntervalHook} - 定时器管理</li>
 *   <li>{@link io.leavesfly.joder.hooks.MessageLogHook} - 消息日志记录</li>
 *   <li>{@link io.leavesfly.joder.hooks.StartupTimeHook} - 启动时间记录</li>
 *   <li>{@link io.leavesfly.joder.hooks.NotifyAfterTimeoutHook} - 超时通知</li>
 *   <li>{@link io.leavesfly.joder.hooks.TerminalSizeHook} - 终端尺寸监听</li>
 *   <li>{@link io.leavesfly.joder.hooks.TextInputHook} - 文本输入处理</li>
 *   <li>{@link io.leavesfly.joder.hooks.UnifiedCompletionHook} - 统一补全系统</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 通过依赖注入使用
 * @Inject
 * public MyService(CommandHistoryHook historyHook) {
 *     this.historyHook = historyHook;
 * }
 * 
 * // 使用历史功能
 * historyHook.addToHistory(command);
 * String previous = historyHook.navigateUp(currentInput);
 * }</pre>
 * 
 * @since 1.0.0
 */
package io.leavesfly.joder.hooks;
