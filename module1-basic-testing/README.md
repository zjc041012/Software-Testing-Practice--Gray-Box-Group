# Module 1: Basic Testing

This module contains the basic software testing practice materials and implementation.

## Current scope

- A: login, JWT issuing/parsing, bearer-token filtering, and role-based access control.
- B: cross-chain precheck, task creation, execution state transitions, failure handling, verification decisions, and retry idempotency.

The current automated baseline has 32 active A+B test methods.

## SUT boundary

The tests use a selected production-code snapshot under [`sut/`](sut/), copied from the thesis system without modifying the original source. See [`sut/README.md`](sut/README.md) for the exact boundary and [`tests/README.md`](tests/README.md) for execution details.
