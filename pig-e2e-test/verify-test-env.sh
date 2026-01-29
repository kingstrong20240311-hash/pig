#!/bin/bash

# E2E 测试环境验证脚本
# 用途: 验证所有服务是否正常运行并配置正确

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "  验证 Pig E2E 测试环境"
echo "======================================"
echo ""

# 计数器
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# 检查函数
check() {
    local name=$1
    local command=$2
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    echo -n "[$TOTAL_CHECKS] 检查 $name... "
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 通过${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

# 1. 检查 Docker 容器
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 Docker 容器状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check "MySQL 容器运行" "docker ps | grep -q pig-e2e-mysql"
check "Redis 容器运行" "docker ps | grep -q pig-e2e-redis"
check "Kafka 容器运行" "docker ps | grep -q pig-e2e-kafka"
check "Zookeeper 容器运行" "docker ps | grep -q pig-e2e-zookeeper"
check "Nacos 容器运行" "docker ps | grep -q pig-e2e-nacos"

echo ""

# 2. 检查端口连通性
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔌 端口连通性"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check "MySQL 端口 33307" "nc -z localhost 33307"
check "Redis 端口 36380" "nc -z localhost 36380"
check "Kafka 端口 9093" "nc -z localhost 9093"
check "Nacos 端口 8849" "nc -z localhost 8849"

echo ""

# 3. 检查服务健康状态
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💚 服务健康状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check "MySQL 健康检查" "docker exec pig-e2e-mysql mysqladmin ping -h localhost -uroot -proot --silent"
check "Redis 健康检查" "docker exec pig-e2e-redis redis-cli ping | grep -q PONG"
check "Nacos 健康检查" "curl -sf http://localhost:8849/nacos/v1/console/health/readiness"

echo ""

# 4. 检查数据库
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🗄️  数据库验证"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check "数据库 pig 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'USE pig' 2>/dev/null"
check "数据库 pig_order 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'USE pig_order' 2>/dev/null"
check "数据库 pig_vault 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'USE pig_vault' 2>/dev/null"
check "数据库 nacos_config 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'USE nacos_config' 2>/dev/null"
check "表 sys_user 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'SELECT 1 FROM pig.sys_user LIMIT 1' 2>/dev/null"
check "表 ord_order 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'SELECT 1 FROM pig_order.ord_order LIMIT 1' 2>/dev/null"
check "表 vault_account 存在" "docker exec pig-e2e-mysql mysql -uroot -proot -e 'SELECT 1 FROM pig_vault.vault_account LIMIT 1' 2>/dev/null"

echo ""

# 5. 检查 Kafka 主题
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📨 Kafka 主题"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check "主题 domain.order 存在" "docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093 2>/dev/null | grep -q 'domain.order'"
check "主题 domain.vault 存在" "docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093 2>/dev/null | grep -q 'domain.vault'"
check "主题 domain.market 存在" "docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093 2>/dev/null | grep -q 'domain.market'"

echo ""

# 6. 检查 Nacos 配置
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "⚙️  Nacos 配置"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check "命名空间 dev 存在" "curl -sf 'http://localhost:8849/nacos/v1/console/namespaces' | grep -q 'dev'"
check "配置 application-dev.yml 存在" "curl -sf 'http://localhost:8849/nacos/v1/cs/configs?dataId=application-dev.yml&group=DEFAULT_GROUP&tenant=dev'"
check "配置 pig-order-dev.yml 存在" "curl -sf 'http://localhost:8849/nacos/v1/cs/configs?dataId=pig-order-dev.yml&group=DEFAULT_GROUP&tenant=dev'"
check "配置 pig-vault-dev.yml 存在" "curl -sf 'http://localhost:8849/nacos/v1/cs/configs?dataId=pig-vault-dev.yml&group=DEFAULT_GROUP&tenant=dev'"

echo ""

# 7. 详细信息
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 详细信息"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "🐳 Docker 容器列表:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep pig-e2e || echo "  无运行中的容器"

echo ""
echo "📨 Kafka 主题列表:"
docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093 2>/dev/null || echo "  无法获取主题列表"

echo ""
echo "🗄️  MySQL 数据库列表:"
docker exec pig-e2e-mysql mysql -uroot -proot -e "SHOW DATABASES" 2>/dev/null | grep -E "pig|nacos" || echo "  无法获取数据库列表"

echo ""

# 8. 测试结果总结
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📈 测试结果总结"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "总检查项: $TOTAL_CHECKS"
echo -e "${GREEN}通过: $PASSED_CHECKS${NC}"
echo -e "${RED}失败: $FAILED_CHECKS${NC}"
echo ""

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}✅ 所有检查通过！环境配置正确！${NC}"
    echo ""
    echo "🎉 可以开始运行 E2E 测试了："
    echo "   export PIG_GATEWAY_URL=http://127.0.0.1:9999"
    echo "   mvn -pl pig-e2e-test -Pe2e verify"
    exit 0
else
    echo -e "${RED}❌ 部分检查失败，请查看详细信息${NC}"
    echo ""
    echo "💡 故障排查建议："
    echo "   1. 查看失败的检查项"
    echo "   2. 运行: docker-compose logs [service]"
    echo "   3. 参考: TROUBLESHOOTING.md"
    echo "   4. 尝试重启: ./cleanup-test-env.sh && ./start-test-env.sh"
    exit 1
fi
