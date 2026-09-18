# 被测 AI 模型：Qwen2.5-1.5B

本目录（`sut/`）用于封装模块二实践中的被测 AI 模型。模型通过 **Ollama** 在本地部署，测试代码通过 HTTP API 调用，无需外部网络。

---

## 1. 模型信息

| 项目 | 说明 |
|------|------|
| 模型名称 | Qwen2.5-1.5B |
| 模型类型 | 开源大语言模型（15 亿参数） |
| 部署框架 | Ollama 0.34.0 或更高 |
| 调用接口 | `http://localhost:11434/api/chat` |
| 模型来源 | [https://ollama.com/library/qwen2.5](https://ollama.com/library/qwen2.5) |
| 文件大小 | 约 1 GB |
| 输入规格 | 自然语言提示词（单轮或多轮） |
| 输出规格 | 模型生成的自然语言文本 |

---

## 2. 获取模型

### 2.1 安装 Ollama

访问 [https://ollama.com](https://ollama.com) 下载对应操作系统的安装包，安装后 Ollama 服务通常会自动启动。

### 2.2 拉取模型

在终端执行：

```bash
ollama pull qwen2.5:1.5b
# 查看已加载的模型列表

### 2.3 验证模型

ollama list

# 检查 Ollama 服务版本
curl http://localhost:11434/api/version

### 2.4 手动测试模型响应

curl http://localhost:11434/api/chat -d "{\"model\": \"qwen2.5:1.5b\", \"messages\": [{\"role\": \"user\", \"content\": \"你好\"}], \"stream\": false}"

## 3. 调用方式

### 3.1 直接调用示例

from sut.model_client import target_model

output = target_model("用一句话解释什么是编程语言。")
print(output)

### 3.2 函数签名

def target_model(prompt: str, temperature: float = 0.2) -> str:
    """
    调用本地 Ollama 模型生成回复

    Args:
        prompt: 用户输入的自然语言提示词
        temperature: 采样温度，默认 0.2 降低随机性

    Returns:
        模型生成的文本内容
    """

### 3.3 环境变量

可通过环境变量覆盖默认配置：

```

| 环境变量         | 默认值                            | 说明            |
| ---------------- | --------------------------------- | --------------- |
| `OLLAMA_URL`     | `http://localhost:11434/api/chat` | Ollama API 地址 |
| `OLLAMA_MODEL`   | `qwen2.5:1.5b`                    | 模型名称        |
| `OLLAMA_TIMEOUT` | `90`                             | 请求超时（秒）  |

## 4. 目录文件说明

| 文件              | 说明                                     |
| :---------------- | :--------------------------------------- |
| `__init__.py`     | 标识 `sut` 为 Python 包                  |
| `model_client.py` | 模型调用封装，提供 `target_model()` 函数 |
| `model_info.json` | 模型元数据（名称、版本、来源等）         |
| `README.md`       | 本文件                                   |

注意：模型权重文件（约 1 GB）**不纳入 Git 仓库**，可通过 `ollama pull` 命令在本地获取。

## 5. 注意事项

1. **模型文件较大**，请勿直接提交到 GitHub，仓库中仅保留配置与调用封装。
2. **Ollama 服务需保持运行**，测试前请确认 `ollama serve` 已启动。
3. **非确定性**：即使设置 `temperature=0.2`，同一输入多次调用仍可能产生不同输出，这是大语言模型的固有特性。
4. **超时设置**：默认超时为 90 秒。若硬件性能较低，可适当增加 `OLLAMA_TIMEOUT`。
5. **内存要求**：建议至少 8GB 可用内存，否则模型可能加载失败或响应极慢。