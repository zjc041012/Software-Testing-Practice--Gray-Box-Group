# tests/conftest.py
import os
import json
import pytest
import requests

# --- 配置区域 ---
OLLAMA_URL = "http://localhost:11434/api/chat"
MODEL_NAME = "qwen2.5:1.5b"

# --- 加载测试数据 ---
DATA_PATH = os.path.join(os.path.dirname(__file__), "..", "test-data", "prompts.json")
with open(DATA_PATH, "r", encoding="utf-8") as f:
    TEST_DATA = json.load(f)

# --- 被测模型调用函数 ---
def target_model(prompt: str, temperature: float = 0.2) -> str:
    response = requests.post(
        OLLAMA_URL,
        json={
            "model": MODEL_NAME,
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
            "options": {"temperature": temperature}
        },
        timeout=300
    )
    response.raise_for_status()
    return response.json()["message"]["content"]

# --- Fixtures ---
# checkllm 的 pytest 插件会自动提供 check fixture，
# 我们不需要自己定义它，直接删掉原来的 check() 函数即可。

@pytest.fixture
def target():
    """被测系统调用入口"""
    return target_model

@pytest.fixture
def test_data():
    """提供测试数据"""
    return TEST_DATA