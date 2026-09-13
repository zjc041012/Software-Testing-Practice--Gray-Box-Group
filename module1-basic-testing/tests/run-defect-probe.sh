#!/usr/bin/env bash
set -uo pipefail

TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVIDENCE_ROOT="$TEST_ROOT/../evidence/defect-probes"
PROBE_ID="${1:-}"

case "$PROBE_ID" in
  D-001)
    TEST_CLASS="D001TargetFailureProbeTest"
    EXPECTED_TEXT="D-001 failed target receipt must not confirm the task"
    ;;
  D-002)
    TEST_CLASS="D002JwtExpiryProbeTest"
    EXPECTED_TEXT="D-002 token must be rejected at its expiration second"
    ;;
  D-003)
    TEST_CLASS="D003BlankHashProbeTest"
    EXPECTED_TEXT="D-003 blank file hashes must fail the hash check"
    ;;
  *)
    echo "用法：bash ./run-defect-probe.sh D-001|D-002|D-003" >&2
    exit 2
    ;;
esac

mkdir -p "$EVIDENCE_ROOT"
LOG_FILE="$EVIDENCE_ROOT/$PROBE_ID.log"
echo "缺陷探测：$PROBE_ID ($TEST_CLASS)"
echo "命令：mvn -Pdefect-probes -Dtest=$TEST_CLASS clean test"

mvn -f "$TEST_ROOT/pom.xml" -Pdefect-probes -Dtest="$TEST_CLASS" \
  -Dstyle.color=never clean test 2>&1 | tee "$LOG_FILE"
MAVEN_STATUS=${PIPESTATUS[0]}

if [[ "$MAVEN_STATUS" -eq 0 ]]; then
  echo "探测未复现：测试意外通过，请检查 SUT 是否已变化。" >&2
  exit 1
fi
if ! grep -Fq "$EXPECTED_TEXT" "$LOG_FILE"; then
  echo "探测无效：日志中没有预期的缺陷断言，可能是编译或环境错误。" >&2
  exit 1
fi
if ! grep -Fq 'Tests run: 1, Failures: 1, Errors: 0' "$LOG_FILE"; then
  echo "探测无效：未找到 1 条断言失败且 0 条运行错误的结果。" >&2
  exit 1
fi

OBSERVATION=$(grep -m1 "^$PROBE_ID " "$LOG_FILE")
SUMMARY=$(grep -F 'Tests run: 1, Failures: 1, Errors: 0' "$LOG_FILE" | tail -n 1)
if [[ -z "$OBSERVATION" || -z "$SUMMARY" ]]; then
  echo "探测无效：缺少可核验的实际观察值或测试汇总。" >&2
  exit 1
fi

echo "截图摘要："
echo "$OBSERVATION"
echo "$SUMMARY"
echo "EXPECTED_FAILURE_REPRODUCED=$PROBE_ID"
echo "原始日志：$LOG_FILE"
