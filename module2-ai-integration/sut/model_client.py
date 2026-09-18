"""
被测 AI 模型调用封装
模型: Qwen2.5-1.5B
部署: Ollama (本地)
"""

import os
import requests

# 默认配置，可通过环境变量覆盖
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/chat")
MODEL_NAME = os.getenv("OLLAMA_MODEL", "qwen2.5:1.5b")
DEFAULT_TIMEOUT = int(os.getenv("OLLAMA_TIMEOUT", "90"))

def target_model(prompt: str, temperature: float = 0.2) -> str:
    payload = {
        "model": MODEL_NAME,
        "messages": [{"role": "user", "content": prompt}],
        "stream": False,
        "options": {
            "temperature": temperature,
            "num_predict": 200,   # 限制输出最多 200 个 token
            "num_ctx": 1024       # 限制上下文窗口，减少计算量
        }
    }
    response = requests.post(OLLAMA_URL, json=payload, timeout=DEFAULT_TIMEOUT)
    response.raise_for_status()
    return response.json()["message"]["content"]