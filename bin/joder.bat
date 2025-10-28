@echo off
REM Joder 启动脚本 (Windows)

REM 检查 Java 版本
set REQUIRED_VERSION=17
set JAVA_CMD=java

if defined JAVA_HOME (
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
)

REM 获取脚本所在目录
set SCRIPT_DIR=%~dp0
set JAR_FILE=%SCRIPT_DIR%..\target\joder-1.0.0.jar

REM 检查 JAR 文件是否存在
if not exist "%JAR_FILE%" (
    echo 错误: 找不到 JAR 文件: %JAR_FILE%
    echo 请先运行 'mvn clean package' 构建项目
    exit /b 1
)

REM 设置编码为 UTF-8
chcp 65001 >nul

REM 设置 JVM 参数
set JVM_OPTS=-Xmx256m -Dfile.encoding=UTF-8

REM 启动应用
"%JAVA_CMD%" %JVM_OPTS% -jar "%JAR_FILE%" %*
