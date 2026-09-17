@echo off
REM 用英文提示可以避免 Windows 控制台乱码
echo =========================================
echo   AI System Testing Start (Ollama + Qwen2.5)
echo =========================================
REM 使用 python -m pytest 替代直接调用 pytest，绕过环境变量问题
python -m pytest tests/ -v --tb=short --color=yes
echo =========================================
echo   Testing Completed
echo =========================================
pause