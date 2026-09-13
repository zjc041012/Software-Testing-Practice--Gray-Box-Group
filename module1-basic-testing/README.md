# Module 1: Basic Testing

This module contains the basic software testing practice materials and implementation.

## Current scope

- Authentication and access control: login, JWT issuing/parsing, bearer-token filtering, and role-based access control.
- Cross-chain task state control: precheck, task creation, execution state transitions, failure handling, verification decisions, and retry idempotency.

The current automated baseline has 32 regular test methods. Three separate defect probes reproduce confirmed, unresolved defects.

完整项目结构、环境配置及 Windows/WSL/Maven 运行命令见[仓库根 README](../README.md)。

## SUT boundary

The tests use a selected production-code snapshot under [`sut/`](sut/), copied from the thesis system without modifying the original source. See [`sut/README.md`](sut/README.md) for the exact boundary and [`tests/README.md`](tests/README.md) for execution details.
