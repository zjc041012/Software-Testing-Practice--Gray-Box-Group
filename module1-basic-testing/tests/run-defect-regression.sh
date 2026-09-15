#!/usr/bin/env bash
set -euo pipefail

TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVIDENCE_ROOT="$TEST_ROOT/../evidence/defect-probes/after"
REGRESSION_ID="${1:-}"

case "$REGRESSION_ID" in
  D-001)
    TEST_CLASS="D001TargetFailureProbeTest"
    ;;
  D-002)
    TEST_CLASS="D002JwtExpiryProbeTest"
    ;;
  D-003)
    TEST_CLASS="D003BlankHashProbeTest"
    ;;
  *)
    echo "用法：bash ./run-defect-regression.sh D-001|D-002|D-003" >&2
    exit 2
    ;;
esac

mkdir -p "$EVIDENCE_ROOT"
LOG_FILE="$EVIDENCE_ROOT/$REGRESSION_ID.log"
echo "缺陷回归验证：$REGRESSION_ID ($TEST_CLASS)"
echo "命令：mvn -Pdefect-probes -Dtest=$TEST_CLASS clean test"

mvn -f "$TEST_ROOT/pom.xml" -Pdefect-probes -Dtest="$TEST_CLASS" \
  -Dstyle.color=never clean test 2>&1 | tee "$LOG_FILE"

if ! grep -Fq 'Tests run: 1, Failures: 0, Errors: 0, Skipped: 0' "$LOG_FILE"; then
  echo "回归验证失败：未找到单测通过结果。" >&2
  exit 1
fi

OBSERVATION=$(grep -m1 "^$REGRESSION_ID " "$LOG_FILE")
if [[ -z "$OBSERVATION" ]]; then
  echo "回归验证失败：缺少可核验的实际观察值。" >&2
  exit 1
fi

echo "验证摘要："
echo "$OBSERVATION"
echo "REGRESSION_PASSED=$REGRESSION_ID"
echo "修复后日志：$LOG_FILE"
