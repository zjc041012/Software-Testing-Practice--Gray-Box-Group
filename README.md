# Software-Testing-Practice--Gray-Box-Group

软件测试课程实践仓库，包含传统软件测试与 AI 系统测试两个独立模块。两个模块均已包含测试代码、测试数据、正式报告、执行证据和汇报材料。

| 模块 | 被测对象与范围 | 测试实现 | 用例规模 |
| --- | --- | --- | --- |
| [模块一：传统软件测试实践](module1-basic-testing/README.md) | 国际联运单跨链协同平台的用户认证、访问控制和跨链任务状态控制 | JUnit 5、Mockito、Maven | 32 条常规用例，另有 3 条缺陷回归 |
| [模块二：AI 系统测试实践](module2-ai-integration/README.md) | Ollama 本地部署的 Qwen2.5-1.5B，覆盖鲁棒性、公平性、安全性 | pytest、CheckLLM、HTTP 模型调用 | 17 条用例：鲁棒性 6 条、公平性 5 条、安全性 6 条 |

模块二使用独立的 AI 被测对象，不依赖模块一的业务系统。下文命令均注明执行目录；首次使用可通过 Git 克隆或下载仓库 ZIP，保留两个模块各自的完整目录结构。

## 项目结构

```text
Software-Testing-Practice--Gray-Box-Group/
├── README.md                       项目总览与交付入口
├── module1-basic-testing/
│   ├── README.md                   模块一环境与运行说明
│   ├── sut/                       被测 Java 代码快照及来源说明
│   ├── tests/                     Maven 工程、JUnit 用例及运行脚本
│   ├── test-data/                 测试数据目录
│   ├── docs/                      用例清单、V1.2 缺陷报告和测试报告
│   ├── evidence/                  常规测试日志、修复前后缺陷证据
│   └── presentation/              模块一成果汇报 PPT
└── module2-ai-integration/
    ├── README.md                   模块二环境与运行说明
    ├── requirements.txt            Python 依赖
    ├── run_all.bat                 关闭防御，运行全部 AI 测试
    ├── run_fix.bat                 开启防御，运行全部 AI 测试
    ├── sut/                       Ollama 调用封装与防御实现
    ├── tests/                     鲁棒性、公平性、安全性测试
    ├── test-data/prompts.json      提示词测试数据
    ├── ai-records/                 AI 辅助过程记录
    ├── docs/                      用例清单、缺陷报告和测试报告
    ├── evidence/                  执行截图与偏见检测日志
    └── presentation/              模块二成果汇报 PPT（含嵌入视频）
```

## 环境配置

| 项目 | 模块一 | 模块二 |
| --- | --- | --- |
| 运行环境 | JDK 17、Maven 3.6.3 或更新版本 | Python、Ollama、`qwen2.5:1.5b` 模型 |
| 留档环境 | Windows + WSL Ubuntu 22.04；OpenJDK 17.0.19、Maven 3.6.3 | Windows；执行截图记录 Python 3.12.2、pytest 9.1.1、CheckLLM 5.1.0；模型配置记录 Ollama 0.34.0 |
| 依赖配置 | `module1-basic-testing/tests/pom.xml` | `module2-ai-integration/requirements.txt` |
| 外部服务 | Repository、FabricGateway 等由 Mockito 模拟，无需启动 MySQL、Fabric 或前端 | 需要启动本地 Ollama，默认请求 `http://localhost:11434/api/chat` |
| 首次准备 | Maven 需要联网下载依赖 | 需要安装 Python 依赖并下载模型；准备完成后，被测模型通过本地接口调用 |

在运行模块一的终端中执行 `java -version`、`mvn -version`，确认 Maven 使用 JDK 17。模块二建议使用已有执行记录中的 Python 3.12 环境。

模块一可以在配置好 Java/Maven 的 Windows 终端直接运行，也可以使用 WSL；模块二的 `.bat` 脚本在 Windows 终端中运行。WSL 的进入方法及路径转换见 [模块一运行说明](module1-basic-testing/README.md)。

## 运行模块一

### 常规测试

在仓库根目录执行，Windows PowerShell 和已配置 Java/Maven 的 Bash 均可使用：

```text
mvn -f module1-basic-testing/tests/pom.xml clean test
```

也可在 Bash 中，从仓库根目录使用现有脚本：

```bash
bash ./module1-basic-testing/tests/run-tests.sh
```

两种方式任选一种，均运行 32 条常规测试。测试工程会直接编译相邻的 `sut/src/main/java`，因此交付时必须同时保留 `sut/` 和 `tests/`，无需访问原毕设工程。

成功时可看到：

```text
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 缺陷回归

在仓库根目录执行以下命令，单独运行 3 条缺陷回归：

```text
mvn -f module1-basic-testing/tests/pom.xml -Pdefect-probes clean test
```

该 profile 会切换到缺陷回归用例目录，不包含前述 32 条常规用例。三个缺陷分别是：目标链失败回执仍被确认、JWT 过期边界判断错误、空文件哈希仍通过跨链前置检查。当前 SUT 快照已包含对应修复。

测试结果写入 `module1-basic-testing/tests/target/surefire-reports/`；`clean` 会清除上次构建结果。需要逐项回归并保存日志时，使用 `run-defect-regression.sh`，具体操作见 [测试工程说明](module1-basic-testing/tests/README.md)。历史脚本 `run-defect-probe.sh` 以复现修复前失败为目标，不作为当前版本的验收入口。

## 运行模块二

### 准备模型与依赖

先安装并启动 Ollama。以下命令在 Windows PowerShell 中执行，从仓库根目录进入模块二：

```powershell
cd .\module2-ai-integration
python --version
python -m pip install -r requirements.txt
ollama pull qwen2.5:1.5b
ollama list
```

确认 `ollama list` 中已有 `qwen2.5:1.5b`。如果 Ollama 服务尚未启动，在另一个终端运行 `ollama serve` 并保持运行。模型权重不包含在仓库中。

### 一键运行

在 `module2-ai-integration` 目录的 PowerShell 中，按要验证的模式选择一个命令：

```powershell
# 修复前模式：FIX_MODE=off，尝试复现原始模型的缺陷
.\run_all.bat
```

```powershell
# 修复后模式：FIX_MODE=on，验证防御实现
.\run_fix.bat
```

两个脚本都执行全部 17 条测试，并在结束后等待按键。运行结果以 pytest 的 `passed` / `failed` 汇总为准，脚本中的 `Testing Completed` 仅表示执行结束。

修复在 `sut/model_client.py` 的调用封装层实现：加入公平性系统提示词、检测偏见并重试或返回兜底回复、记录触发日志。该过程未修改或重新训练 Qwen 模型权重。

### 直接运行 pytest

如需无按键等待的执行方式，可在模块二目录的 PowerShell 中运行：

```powershell
$env:FIX_MODE = "on"
python -m pytest tests/ -v
```

将 `"on"` 改为 `"off"` 即关闭防御。Linux/macOS 的 Bash 可在模块二目录使用：

```bash
FIX_MODE=on python -m pytest tests/ -v
```

执行汇总显示在终端；偏见检测事件记录在 `module2-ai-integration/evidence/bias_monitor.log`。可用 `OLLAMA_URL`、`OLLAMA_MODEL`、`OLLAMA_TIMEOUT` 调整连接配置，当前默认超时为 90 秒，详情见 [模型调用代码](module2-ai-integration/sut/model_client.py)。

## 已留档结果与交付物

下表依据仓库内保存的日志和截图，表示留档时的执行结果。不同版本代码、模型输出或依赖环境下的复测，应以实际运行结果为准。

| 测试批次 | 留档结果 | 证据 |
| --- | --- | --- |
| 模块一：修复后常规测试 | 32 通过，0 失败，0 错误，0 跳过 | [Maven 日志](module1-basic-testing/evidence/after-fix/baseline-32-after-fix.log) |
| 模块一：缺陷定向回归 | D-001、D-002、D-003 各 1 条通过 | [修复前证据](module1-basic-testing/evidence/defect-probes/before/) / [修复后证据](module1-basic-testing/evidence/defect-probes/after/) |
| 模块二：修复前 | 16 通过，1 失败；宗教属性关联偏见用例失败 | [完整执行截图](module2-ai-integration/evidence/all_test.png) / [单项缺陷截图](module2-ai-integration/evidence/defect1.png) |
| 模块二：修复后 | 17 通过，0 失败 | [回归截图](module2-ai-integration/evidence/fix_test.png) |

模块一结论限于选定业务范围及 Mock 环境，未覆盖整个毕设系统或真实双链网络。模块二包含关键词规则和 CheckLLM 检查；模型输出具有非确定性，单次 17 条通过不能证明模型在所有输入下均公平、安全，也不保证修复前缺陷每次都能复现。

| 交付内容 | 模块一 | 模块二 |
| --- | --- | --- |
| 测试用例清单 | [Excel](module1-basic-testing/docs/测试用例清单_模块一.xlsx) | [Excel](module2-ai-integration/docs/测试用例清单_模块二.xlsx) |
| 缺陷报告 | [Word · V1.2](module1-basic-testing/docs/缺陷报告_模块一_V1.2.doc) | [Word](module2-ai-integration/docs/缺陷报告_模块二.doc) |
| 测试报告 | [Word · V1.2](module1-basic-testing/docs/测试报告_模块一_V1.2.docx) | [Word](module2-ai-integration/docs/测试报告_模块二.docx) |
| 自动化测试源代码 | [JUnit 工程](module1-basic-testing/tests/) + [SUT](module1-basic-testing/sut/) | [pytest 用例](module2-ai-integration/tests/) + [模型封装](module2-ai-integration/sut/) + [提示词数据](module2-ai-integration/test-data/prompts.json) |
| 执行证据 | [日志与缺陷证据](module1-basic-testing/evidence/) | [截图与检测日志](module2-ai-integration/evidence/) |
| 成果汇报 | [模块一 PPT (含视频)](module1-basic-testing/presentation/软件测试模块一ppt.pptx) | [模块二 PPT（含视频）](module2-ai-integration/presentation/软件测试模块二ppt.pptx) |
| AI 辅助记录 | — | [AI 对话记录](module2-ai-integration/ai-records/AI对话记录.md) |

模块二演示视频已嵌入 PPT，请下载后在支持视频播放的演示软件中查看。当前仓库没有独立的视频文件。

## 成员分工

以下比例按各模块测试报告分别列出，不合并为项目总贡献比例。

| 模块 | 成员 | 主要贡献 | 占比 |
| --- | --- | --- | --- |
| 模块一 | Zhu Jiacheng（朱佳诚） | 测试设计、自动化与执行、SUT 快照、缺陷修复回归及文档整理 | 70% |
| 模块一 | Zhang Yang（张阳） | PPT、视频录制、部分代码优化、发现 D-003 | 30% |
| 模块二 | Zhang Yang（张阳） | 测试设计与执行、发现 AI-ST-011、三层防御实现与验证、文档整理 | 60% |
| 模块二 | Zhu Jiacheng（朱佳诚） | AI-ST-001 超时问题与代码优化、PPT、视频录制及报告校对 | 40% |
