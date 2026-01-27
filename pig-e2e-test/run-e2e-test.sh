#!/bin/bash

###############################################################################
# Pig E2E 测试执行脚本
# 用于快速执行端到端测试，无需手动设置环境变量
###############################################################################

set -e  # 遇到错误立即退出

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Pig E2E 测试执行脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# ==================== 配置区域 ====================
# 请根据实际环境修改以下配置

# 必填：Gateway 地址
export PIG_GATEWAY_URL="${PIG_GATEWAY_URL:-http://127.0.0.1:9999}"

# 可选：测试账号（如不填，使用默认值）
export PIG_TEST_USERNAME="${PIG_TEST_USERNAME:-test_user}"
export PIG_TEST_PASSWORD="${PIG_TEST_PASSWORD:-test_password}"

# 可选：管理员 Token（如配置，将跳过登录）
# export PIG_ADMIN_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# 可选：超时配置
export PIG_CONNECTION_TIMEOUT="${PIG_CONNECTION_TIMEOUT:-30000}"
export PIG_READ_TIMEOUT="${PIG_READ_TIMEOUT:-60000}"
export PIG_MAX_WAIT_SECONDS="${PIG_MAX_WAIT_SECONDS:-30}"
export PIG_POLL_INTERVAL_MILLIS="${PIG_POLL_INTERVAL_MILLIS:-500}"

# ==================== 打印配置 ====================

echo -e "${YELLOW}环境配置：${NC}"
echo -e "  Gateway URL:         ${PIG_GATEWAY_URL}"
echo -e "  Test Username:       ${PIG_TEST_USERNAME}"
echo -e "  Test Password:       ********"
echo -e "  Admin Token:         ${PIG_ADMIN_TOKEN:-未配置}"
echo -e "  Connection Timeout:  ${PIG_CONNECTION_TIMEOUT}ms"
echo -e "  Read Timeout:        ${PIG_READ_TIMEOUT}ms"
echo -e "  Max Wait Seconds:    ${PIG_MAX_WAIT_SECONDS}s"
echo ""

# ==================== 环境检查 ====================

echo -e "${YELLOW}检查环境...${NC}"

# 检查 Gateway 是否可访问
if ! curl -s -o /dev/null -w "%{http_code}" --max-time 5 "${PIG_GATEWAY_URL}/actuator/health" | grep -q "200"; then
    echo -e "${RED}警告: Gateway (${PIG_GATEWAY_URL}) 可能未启动或不可访问${NC}"
    echo -e "${YELLOW}继续执行测试，但可能会失败...${NC}"
    echo ""
else
    echo -e "${GREEN}✓ Gateway 可访问${NC}"
    echo ""
fi

# ==================== 执行测试 ====================

echo -e "${GREEN}开始执行 E2E 测试...${NC}"
echo ""

# 切换到项目根目录
cd "$(dirname "$0")/.." || exit 1

# 执行测试（使用 Maven Failsafe Plugin）
if mvn -pl pig-e2e-test -Pe2e clean verify; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   ✓ E2E 测试全部通过！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "测试报告位置: pig-e2e-test/target/failsafe-reports/"
    exit 0
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}   ✗ E2E 测试失败${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo -e "请查看测试报告: pig-e2e-test/target/failsafe-reports/"
    exit 1
fi
