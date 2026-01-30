#!/usr/bin/env bash
# 从项目根目录执行，启动指定微服务
# 用法: ./start-service.sh [模块路径]
# 示例: ./start-service.sh                    # 默认 pig-order/pig-order-biz
#       ./start-service.sh pig-gateway
#       ./start-service.sh pig-vault/pig-vault-biz

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

MODULE="${1:-pig-order/pig-order-biz}"
APP_NAME="$(basename "$MODULE")"
LOG_DIR="$SCRIPT_DIR/logs/$APP_NAME"

# 若已存在该服务的 Java 进程则先杀掉
PIDS=$(pgrep -f "java.*$APP_NAME" 2>/dev/null || true)
if [ -n "$PIDS" ]; then
	echo "Stopping existing $APP_NAME process(es): $PIDS"
	echo "$PIDS" | xargs kill -9 2>/dev/null || true
	sleep 2
fi

# 删除本次启动将写入的日志文件，避免沿用旧内容
rm -f "$LOG_DIR/debug.log" "$LOG_DIR/error.log"

export JAVA_TOOL_OPTIONS="-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC"
export MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
export MYSQL_PORT="${MYSQL_PORT:-33307}"
export MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
export NACOS_HOST="${NACOS_HOST:-127.0.0.1}"
export NACOS_PORT="${NACOS_PORT:-18849}"
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-36380}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

mvn spring-boot:run -pl "$MODULE" &
