# 模块一缺陷发现记录

日期：2026-09-13  
测试人：ZhuJiacheng；D-003：ZhangYang
被测对象：`module1-basic-testing/sut/` 选定生产代码快照

本记录保存已复现的缺陷，不代表已经修复或关闭。探测用例使用 JUnit 5、Mockito 和 Mock Fabric Gateway；默认回归集不包含这些故意失败的探测断言。

## D-001 跨链目标链失败仍被确认

- 模块：B 跨链任务状态控制
- 关联逻辑：`CrossChainService.execute`
- 前置条件：任务为 CREATED，源链运单存在，Mock Fabric Gateway 返回 `targetTxId`、`status=FAILED` 和失败原因。
- 复现：执行跨链任务。
- 预期：任务进入 FAILED，保留目标链失败原因，不得进入 CONFIRMED。
- 实际：任务进入 CONFIRMED，回执状态为 FAILED。
- 证据：[D-001 原始运行日志](defect-probes/D-001.log)：`actualTaskStatus=CONFIRMED actualReceiptStatus=FAILED`，断言期望 FAILED，实际为 CONFIRMED。

## D-002 JWT 过期边界被接受

- 模块：A 认证与访问控制
- 关联逻辑：`JwtService.parse`
- 前置条件：生成有效期为 0 分钟的 Token，使 Token 的过期秒数等于当前秒。
- 复现：在过期秒内立即解析 Token。
- 预期：Token 已到期，应返回空 Optional。
- 实际：Token 被解析为有效身份。
- 证据：[D-002 原始运行日志](defect-probes/D-002.log)：`actualTokenAccepted=true`，断言期望 false。

## D-003 文件哈希为空仍通过跨链前置检查

- 测试人：ZhangYang
- 模块：B 跨链任务状态控制
- 关联逻辑：`CrossChainService.precheck`
- 前置条件：Mock Repository 返回运单和文件记录，源链记录存在，但文件 `fileHash` 和 `encryptedFileHash` 均为空。
- 复现：执行跨链前置检查。
- 预期：`文件哈希已生成` 检查失败，整体 precheck 失败。
- 实际：只判断文件对象存在，整体 precheck 通过。
- 证据：[D-003 原始运行日志](defect-probes/D-003.log)：`actualHashCheckPassed=true overallPassed=true`，断言期望 false。

## 当前状态

三个缺陷均已复现，尚未修复、回归或关闭。常规测试执行结果见[32 条测试通过日志](baseline-32.log)；定向探测与常规测试分开运行，故意失败的断言不计入常规测试通过率。本轮不修改 WSL 中的原毕设源代码。
