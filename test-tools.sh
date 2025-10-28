#!/bin/bash

# 测试 Joder 工具系统

echo "==================================="
echo "Joder 工具系统测试脚本"
echo "==================================="
echo ""

# 设置 Java 环境
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 构建项目jar路径
JAR_PATH="/Users/yefei.yf/Qoder/Kode/joder/target/joder-1.0.0.jar"

# 测试1: 验证JAR文件存在
echo "✓ 检查JAR文件..."
if [ -f "$JAR_PATH" ]; then
    echo "  ✅ JAR文件存在: $JAR_PATH"
    echo "  大小: $(du -h "$JAR_PATH" | cut -f1)"
else
    echo "  ❌ JAR文件不存在!"
    exit 1
fi
echo ""

# 测试2: 验证JAR可执行
echo "✓ 测试JAR可执行性..."
java -jar "$JAR_PATH" --version 2>&1
if [ $? -eq 0 ]; then
    echo "  ✅ JAR可正常执行"
else
    echo "  ⚠️  JAR执行返回非零状态码"
fi
echo ""

# 测试3: 检查编译的类文件
echo "✓ 检查新编译的工具类..."
CLASSES_DIR="/Users/yefei.yf/Qoder/Kode/joder/target/classes"

declare -a TOOL_CLASSES=(
    "io/shareai/joder/tools/ls/LSTool.class"
    "io/shareai/joder/tools/glob/GlobTool.class"
    "io/shareai/joder/tools/grep/GrepTool.class"
    "io/shareai/joder/tools/edit/FileEditTool.class"
    "io/shareai/joder/tools/edit/MultiEditTool.class"
)

for class in "${TOOL_CLASSES[@]}"; do
    if [ -f "$CLASSES_DIR/$class" ]; then
        echo "  ✅ $class"
    else
        echo "  ❌ 缺少: $class"
    fi
done
echo ""

# 测试4: 使用 jar tf 列出JAR内容
echo "✓ 验证工具类已打包到JAR..."
jar tf "$JAR_PATH" | grep -E "(LSTool|GlobTool|GrepTool|FileEditTool|MultiEditTool)\.class" | sort
echo ""

# 测试5: 统计信息
echo "==================================="
echo "统计信息:"
echo "==================================="
echo "Java类文件总数: $(find "$CLASSES_DIR" -name "*.class" | wc -l)"
echo "工具类数量: ${#TOOL_CLASSES[@]}"
echo ""

# 测试6: 验证依赖
echo "✓ 验证关键依赖..."
jar tf "$JAR_PATH" | grep -E "(guice|lanterna|jackson|okhttp)" | head -5
echo ""

echo "==================================="
echo "测试完成!"
echo "==================================="
