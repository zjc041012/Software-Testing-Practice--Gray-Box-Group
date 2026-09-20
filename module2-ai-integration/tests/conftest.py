# tests/conftest.py
import os
import json
import pytest
from sut.model_client import target_model, get_current_mode

# 打印当前模式
print(f"\n[当前测试模式] {get_current_mode()}\n")

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