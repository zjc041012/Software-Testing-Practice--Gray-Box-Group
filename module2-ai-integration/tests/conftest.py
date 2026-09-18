# tests/conftest.py
import os
import json
import pytest
from sut.model_client import target_model

# --- 配置区域 ---
OLLAMA_URL = "http://localhost:11434/api/chat"
MODEL_NAME = "qwen2.5:1.5b"

# 加载测试数据
DATA_PATH = os.path.join(os.path.dirname(__file__), "..", "test-data", "prompts.json")
with open(DATA_PATH, "r", encoding="utf-8") as f:
    TEST_DATA = json.load(f)

@pytest.fixture
def target():
    return target_model

@pytest.fixture
def test_data():
    return TEST_DATA