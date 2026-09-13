#!/usr/bin/env bash
set -euo pipefail

TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUT_ROOT="${SUT_ROOT:-$HOME/waybill-crosschain-platform}"

if [[ ! -f "$SUT_ROOT/backend/pom.xml" ]]; then
  echo "未找到毕设系统后端：$SUT_ROOT/backend/pom.xml" >&2
  echo "请设置 SUT_ROOT，例如：export SUT_ROOT=\"$HOME/waybill-crosschain-platform\"" >&2
  exit 1
fi

echo "离线打包当前毕设后端：$SUT_ROOT/backend"
pushd "$SUT_ROOT/backend" >/dev/null
mvn -q -DskipTests package
popd >/dev/null

echo "执行模块一自动化测试：$TEST_ROOT"
if [[ "${RUN_ALL:-0}" == "1" ]]; then
  echo "测试集合：A+C 活跃测试 + B 延期探索测试"
  mvn -q -f "$TEST_ROOT/pom.xml" -Dsut.jar="$SUT_ROOT/backend/target/waybill-crosschain-backend-1.0.0.jar.original" clean test
else
  TEST_PATTERN="${TEST_PATTERN:-JwtAuthenticationFilterTest,JwtServiceTest,AuthServiceTest,AccessControlServiceTest,CrossChainServiceTest}"
  echo "测试集合：A+C 活跃测试（$TEST_PATTERN）"
  mvn -q -f "$TEST_ROOT/pom.xml" -Dsut.jar="$SUT_ROOT/backend/target/waybill-crosschain-backend-1.0.0.jar.original" -Dtest="$TEST_PATTERN" clean test
fi
