#!/bin/bash

# E2E 测试环境启动脚本
# 用途: 启动完整的测试基础环境（MySQL、Redis、Kafka、Nacos）

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================"
echo "  启动 Pig E2E 测试环境"
echo "======================================"
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 docker-compose 命令
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    echo "❌ 错误: 未找到 docker-compose 命令"
    exit 1
fi

echo "📦 使用命令: $DOCKER_COMPOSE"
echo ""

# 停止并删除旧容器（如果存在）
echo "🧹 清理旧容器..."
$DOCKER_COMPOSE down -v 2>/dev/null || true
echo ""

# 启动服务
echo "🚀 启动服务..."
$DOCKER_COMPOSE up -d

echo ""
echo "⏳ 等待服务启动..."
echo ""

# 等待 MySQL 健康检查
echo "  等待 MySQL 启动..."
timeout=120
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker exec pig-e2e-mysql mysqladmin ping -h localhost -uroot -proot --silent 2>/dev/null; then
        echo "  ✅ MySQL 已就绪"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

if [ $elapsed -ge $timeout ]; then
    echo "  ❌ MySQL 启动超时"
    exit 1
fi

# 等待 Redis 健康检查
echo "  等待 Redis 启动..."
elapsed=0
while [ $elapsed -lt 60 ]; do
    if docker exec pig-e2e-redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo "  ✅ Redis 已就绪"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

# 等待 Kafka 健康检查
echo "  等待 Kafka 启动..."
elapsed=0
while [ $elapsed -lt 90 ]; do
    if docker exec pig-e2e-kafka kafka-broker-api-versions --bootstrap-server localhost:9093 2>/dev/null | grep -q ApiVersion; then
        echo "  ✅ Kafka 已就绪"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

# 等待 Nacos 健康检查
echo "  等待 Nacos 启动..."
elapsed=0
while [ $elapsed -lt 120 ]; do
    if curl -sf http://localhost:8849/nacos/v1/console/health/readiness > /dev/null 2>&1; then
        echo "  ✅ Nacos 已就绪"
        break
    fi
    sleep 3
    elapsed=$((elapsed + 3))
done

echo ""
echo "======================================"
echo "  ✅ 测试环境启动成功！"
echo "======================================"
echo ""
echo "📊 服务地址:"
echo "  MySQL:   localhost:33307 (root/root)"
echo "  Redis:   localhost:36380"
echo "  Kafka:   localhost:9093"
echo "  Nacos:   http://localhost:8849/nacos (nacos/nacos)"
echo ""
echo "🗄️  数据库:"
echo "  - pig (主数据库)"
echo "  - pig_order (订单数据库)"
echo "  - nacos_config (Nacos 配置)"
echo ""
echo "📨 Kafka 主题 (启动时自动创建):"
echo "  - domain.order"
echo "  - domain.vault"
echo "  - domain.market"
echo ""
echo "💡 提示:"
echo "  - 查看日志: $DOCKER_COMPOSE logs -f [service]"
echo "  - 停止环境: $DOCKER_COMPOSE down"
echo "  - 完全清理: ./cleanup-test-env.sh"
echo ""
echo "🧪 运行测试:"
echo "  export PIG_GATEWAY_URL=http://127.0.0.1:9999"
echo "  mvn -pl pig-e2e-test -Pe2e verify"
echo ""
