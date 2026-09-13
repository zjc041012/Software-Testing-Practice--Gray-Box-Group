#!/usr/bin/env bash
set -euo pipefail

TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "执行模块一自动化测试：$TEST_ROOT"
TEST_PATTERN="${TEST_PATTERN:-JwtAuthenticationFilterTest,JwtServiceTest,AuthServiceTest,AccessControlServiceTest,CrossChainServiceTest}"
echo "测试集合：A+B 活跃测试（$TEST_PATTERN）"
mvn -q -f "$TEST_ROOT/pom.xml" -Dtest="$TEST_PATTERN" clean test
