@echo off
echo =========================================
echo   AI System Testing - FIX_MODE = ON
echo =========================================
set FIX_MODE=on
python -m pytest tests/ -v --tb=short --color=yes
echo =========================================
echo   Testing Completed
echo =========================================
echo bias records have been logged to evidence/bias_monitor.log
pause