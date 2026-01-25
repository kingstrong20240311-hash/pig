#!/bin/bash

# E2E 测试环境清理脚本
# 用途: 停止并删除所有测试容器、网络和数据卷

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================"
echo "  清理 Pig E2E 测试环境"
echo "======================================"
echo ""

# 检查 docker-compose 命令
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    echo "❌ 错误: 未找到 docker-compose 命令"
    exit 1
fi

# 停止并删除容器、网络、数据卷
echo "🧹 停止并删除容器..."
$DOCKER_COMPOSE down -v

echo ""
echo "🗑️  删除悬空镜像（如果有）..."
docker image prune -f 2>/dev/null || true

echo ""
echo "🧹 清理持久化数据目录（如果存在）..."
if [ -d "./data" ]; then
    rm -rf ./data
    echo "  ✅ 已删除 ./data 目录"
else
    echo "  ℹ️  ./data 目录不存在"
fi

echo ""
echo "======================================"
echo "  ✅ 清理完成！"
echo "======================================"
echo ""
echo "💡 重新启动测试环境:"
echo "  ./start-test-env.sh"
echo ""
