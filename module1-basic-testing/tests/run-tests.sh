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
mvn -q -f "$TEST_ROOT/pom.xml" -Dsut.jar="$SUT_ROOT/backend/target/waybill-crosschain-backend-1.0.0.jar.original" clean test
