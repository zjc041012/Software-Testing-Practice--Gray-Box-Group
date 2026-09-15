# 模块一：传统软件测试实践

软件测试与质量保证实践课程项目。被测对象为国际联运单跨链协同平台，本轮测试覆盖用户认证与访问控制、跨链任务状态控制。

本模块采用等价类、边界值、场景法及状态转换分析设计测试，使用 JUnit 5 实现自动化。以下相对路径均以本目录 `module1-basic-testing/` 为基准。

## 项目结构

```text
module1-basic-testing/
├── README.md
├── sut/                    被测生产代码快照及来源说明
│   └── src/main/java/
├── tests/                  独立 Maven 自动化测试工程
│   ├── pom.xml
│   ├── run-tests.sh         常规测试入口
│   ├── run-defect-probe.sh  单项缺陷复现入口
│   └── src/
│       ├── test/java/       常规测试
│       └── defect-probes/java/  缺陷探测测试
├── test-data/              测试数据及相关材料
├── docs/                   用例清单、缺陷报告、测试报告
├── evidence/               执行日志和缺陷证据
└── presentation/           演示材料目录
```

`sut` 是从原毕设系统提取的代码快照，测试工程直接编译该目录；无需访问原毕设目录。它不是完整的前后端部署工程，登录页面和真实跨链业务演示需在原毕设系统进行。来源和提取边界见 [SUT 说明](sut/README.md)。

## 环境配置

| 项目 | 配置 |
| --- | --- |
| JDK | 17；已验证 OpenJDK 17.0.19 |
| Maven | 3.6.3 或更新版本；已验证 3.6.3 |
| 已验证运行环境 | Windows 主机 + WSL Ubuntu 22.04 |
| 测试框架 | JUnit 5、Mockito、AssertJ，由 Spring Boot Starter Test 3.3.5 提供 |
| 构建配置 | Java 17，Maven Surefire 3.5.0，详见 `tests/pom.xml` |

在实际运行测试的终端中检查：

```text
java -version
mvn -version
```

两条命令均应能找到程序，且 Maven 输出的 Java 版本应为 17。使用 Windows 原生 Maven 时，将 `JAVA_HOME` 指向 JDK 17 安装目录，并将 JDK 和 Maven 的 `bin` 加入 `PATH`；使用 WSL 时，需在 WSL 内安装 JDK 和 Maven，Windows 安装不能代替 WSL 环境配置。

WSL Ubuntu 尚未安装依赖时，可在 Ubuntu 终端执行：

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
java -version
mvn -version
```

首次运行 Maven 需要网络下载 `pom.xml` 中的依赖。常规测试通过 Mockito 模拟 Repository、FabricGateway 等外部依赖，不需要启动 MySQL、Fabric 网络或前端，也不需要生产账户、链证书或生产密钥。

## 运行常规测试

请保留本模块的完整目录结构，因为 `tests/pom.xml` 引用了相邻的 `sut/src/main/java`。

### Windows PowerShell + WSL

下面命令适用于仓库位于 `C:\Projects\Software-Testing-Practice--Gray-Box-Group`、WSL 发行版名为 `Ubuntu-22.04` 的环境；其他路径或发行版请相应替换。

```powershell
wsl.exe -d Ubuntu-22.04 -- bash -lc "cd /mnt/c/Projects/Software-Testing-Practice--Gray-Box-Group/module1-basic-testing/tests && bash ./run-tests.sh"
```

`/mnt/c/...` 是 WSL 路径，应放在上述 WSL 命令内或在 Ubuntu 终端使用，不能直接在 PowerShell 中执行 `cd /mnt/c/...`。

### Ubuntu / Bash

从模块一目录执行：

```bash
cd tests
bash ./run-tests.sh
```

### 直接使用 Maven

在已配置 JDK 17 和 Maven 的终端中，从模块一目录执行：

```text
cd tests
mvn clean test
```

也可以从模块一目录运行 `mvn -f tests/pom.xml clean test`。Windows 原生 PowerShell 可使用 Maven 命令，无需 Bash；本仓库现有执行证据来自 WSL 环境。

当前基线共 32 条常规测试：认证与访问控制 23 条、跨链任务状态控制 9 条。对应 5 个测试类：`JwtServiceTest`、`JwtAuthenticationFilterTest`、`AuthServiceTest`、`AccessControlServiceTest`、`CrossChainServiceTest`。正常完成时应看到：

```text
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

执行结果位于 `tests/target/surefire-reports/`，包含 XML 和文本报告。`target/` 不纳入 Git；`clean` 会清除上次构建结果，需要保留的证据应另存。已留档的常规执行日志为 [baseline-32.log](evidence/baseline-32.log)。

## 已知缺陷修复与回归验证

本轮确认的 D-001、D-002、D-003 已在 SUT 快照中修复，并通过独立回归测试。修复前原始日志保存在 `evidence/defect-probes/before/`，修复后日志保存在 `evidence/defect-probes/after/`。三条定向回归不计入上述 32 条常规测试的通过率。

修复前的故意失败探测脚本仍用于解释历史缺陷现象；在当前已修复 SUT 上运行时，测试通过属于“缺陷未复现”，脚本会按历史探测语义退出失败。需要验证修复时，在本模块 `tests/` 的 Bash 终端内运行：

```bash
bash ./run-defect-regression.sh D-001
bash ./run-defect-regression.sh D-002
bash ./run-defect-regression.sh D-003
```

| 编号 | 缺陷 |
| --- | --- |
| D-001 | 目标链回执失败，跨链任务仍被确认 |
| D-002 | Token 在过期秒等于当前秒时仍被接受 |
| D-003 | 文件哈希为空仍通过跨链前置检查 |

PowerShell 示例：

```powershell
wsl.exe -d Ubuntu-22.04 -- bash -lc "cd /mnt/c/Projects/Software-Testing-Practice--Gray-Box-Group/module1-basic-testing/tests && bash ./run-defect-regression.sh D-001"
```

修复后回归应出现 `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` 和 `BUILD SUCCESS`，脚本输出 `REGRESSION_PASSED=D-00X`。修复前的复现日志只作为历史证据保留，不再覆盖。

## 测试交付物

- [测试用例清单](docs/test-cases.xlsx)
- [缺陷报告](docs/defect-report.doc)
- [模块一测试报告](docs/test-report.docx)
- [自动化测试工程说明](tests/README.md)

仓库中的缺陷报告和测试报告均为修复、回归验证后的正式版本；修复前的过程证据保存在 `evidence/defect-probes/before/`，原始报告文件已从当前工作目录移除，但仍可从 Git 历史恢复。

32 条常规用例和 3 条缺陷回归均通过，说明选定范围内的规则符合预期；该结论不外推到本轮未覆盖的文件上传、加密、真实 MySQL/Fabric 集成、性能和渗透测试。
