# Module 1: Basic Testing

This module contains the basic software testing practice materials and implementation.

## Current scope

- A: login, JWT issuing/parsing, bearer-token filtering, and role-based access control.
- C: cross-chain precheck, task creation, execution state transitions, failure handling, verification decisions, and retry idempotency.
- B: file upload, encryption, and integrity verification are retained as exploratory material but deferred from the current delivery batch.

The current automated baseline has 32 active A+C test methods. The separate B exploratory tests remain runnable for diagnostic purposes and are not counted in the A+C scope.

## SUT boundary

The tests use the real selected production classes from the thesis system at `/home/zjc/waybill-crosschain-platform` in WSL Ubuntu 22.04. The thesis source is not copied into or modified by this repository. See [`sut/README.md`](sut/README.md) for the exact boundary and [`tests/README.md`](tests/README.md) for execution details.
