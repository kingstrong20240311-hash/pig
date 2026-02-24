#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

LOG_DIR="${SCRIPT_DIR}/logs/pig-boot"
BUILD_LOG_FILE="${LOG_DIR}/build.log"
RUN_LOG_FILE="${LOG_DIR}/standalone.log"
mkdir -p "${LOG_DIR}"

echo "Building pig-boot and dependent modules (skip tests)..."
if ! mvn -Pboot -pl pig-boot -am -DskipTests install > "${BUILD_LOG_FILE}" 2>&1; then
  echo "Build failed. See log: ${BUILD_LOG_FILE}"
  tail -n 40 "${BUILD_LOG_FILE}" || true
  exit 1
fi
echo "Build finished. Log file: ${BUILD_LOG_FILE}"

PIDS="$(pgrep -f 'pig-boot.*spring-boot:run|spring-boot:run.*pig-boot|java.*pig-boot|pig-boot.*java' || true)"

if [ -n "${PIDS}" ]; then
  echo "Killing existing pig-boot process(es): ${PIDS}"
  kill ${PIDS} || true
  sleep 2

  REMAINING_PIDS="$(pgrep -f 'pig-boot.*spring-boot:run|spring-boot:run.*pig-boot|java.*pig-boot|pig-boot.*java' || true)"
  if [ -n "${REMAINING_PIDS}" ]; then
    echo "Force killing remaining process(es): ${REMAINING_PIDS}"
    kill -9 ${REMAINING_PIDS} || true
  fi
fi

cd pig-boot
nohup mvn spring-boot:run > "${RUN_LOG_FILE}" 2>&1 &
NEW_PID=$!

echo "pig-boot started in background, PID: ${NEW_PID}"
echo "Run log file: ${RUN_LOG_FILE}"
