/**
 * Util 包 - 通用工具类集合
 * 
 * <p>本包提供了一系列通用工具类，对应 Kode 项目中的 src/utils 目录。
 * 这些工具类封装了常用的功能，提高代码复用性和可维护性。
 * 
 * <h2>核心工具类</h2>
 * <ul>
 *   <li>{@link io.shareai.joder.util.ArrayUtils} - 数组工具</li>
 *   <li>{@link io.shareai.joder.util.JsonUtils} - JSON 解析和序列化</li>
 *   <li>{@link io.shareai.joder.util.FileUtils} - 文件操作</li>
 *   <li>{@link io.shareai.joder.util.ValidationUtils} - 数据验证</li>
 *   <li>{@link io.shareai.joder.util.ProcessUtils} - 进程执行</li>
 * </ul>
 * 
 * <h2>系统工具类</h2>
 * <ul>
 *   <li>{@link io.shareai.joder.util.BrowserUtils} - 浏览器操作</li>
 *   <li>{@link io.shareai.joder.util.HttpUtils} - HTTP 工具</li>
 *   <li>{@link io.shareai.joder.util.TerminalUtils} - 终端操作</li>
 *   <li>{@link io.shareai.joder.util.EnvUtils} - 环境检测</li>
 *   <li>{@link io.shareai.joder.util.StateManager} - 状态管理</li>
 * </ul>
 * 
 * <h2>业务工具类</h2>
 * <ul>
 *   <li>{@link io.shareai.joder.util.TokenUtils} - Token 计数</li>
 *   <li>{@link io.shareai.joder.util.UserUtils} - 用户信息</li>
 * </ul>
 * 
 * <h2>异常类</h2>
 * <ul>
 *   <li>{@link io.shareai.joder.util.exceptions.MalformedCommandException} - 格式错误的命令</li>
 *   <li>{@link io.shareai.joder.util.exceptions.DeprecatedCommandException} - 已废弃的命令</li>
 *   <li>{@link io.shareai.joder.util.exceptions.AbortException} - 操作中止</li>
 *   <li>{@link io.shareai.joder.util.exceptions.ConfigParseException} - 配置解析错误</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // JSON 解析
 * JsonNode node = JsonUtils.safeParseJSON(jsonString);
 * 
 * // 文件操作
 * String content = FileUtils.readFileSafe("/path/to/file");
 * 
 * // 验证
 * ValidationError error = ValidationUtils.validateEmail(email);
 * 
 * // 进程执行
 * ProcessResult result = ProcessUtils.execNoThrow("git", "status");
 * 
 * // 环境检测
 * Environment env = EnvUtils.getEnvironment();
 * }</pre>
 * 
 * @since 1.0.0
 */
package io.shareai.joder.util;
