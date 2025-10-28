#!/bin/bash

# Joder 启动脚本 (Linux/macOS)

# 检查 Java 版本
REQUIRED_VERSION=17
JAVA_CMD="java"

if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

# 获取 Java 版本
JAVA_VERSION=$($JAVA_CMD -version 2>&1 | grep -oP 'version "?(1\.)?\K\d+' | head -1)

if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt "$REQUIRED_VERSION" ]; then
    echo "错误: 需要 Java $REQUIRED_VERSION 或更高版本"
    echo "当前 Java 版本: $JAVA_VERSION"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/../target/joder-1.0.0.jar"

# 检查 JAR 文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 找不到 JAR 文件: $JAR_FILE"
    echo "请先运行 'mvn clean package' 构建项目"
    exit 1
fi

# 设置 JVM 参数
JVM_OPTS="-Xmx256m -Dfile.encoding=UTF-8"

# 启动应用
$JAVA_CMD $JVM_OPTS -jar "$JAR_FILE" "$@"
