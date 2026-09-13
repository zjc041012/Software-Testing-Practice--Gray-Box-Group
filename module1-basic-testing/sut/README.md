# Module 1 System Under Test Scope

This directory contains the selected system-under-test source snapshot for the current A+B test batch. The snapshot was copied from `/home/zjc/waybill-crosschain-platform/backend/src/main/java` in WSL Ubuntu 22.04; the original thesis source remains outside this repository and was not modified.

The Maven test project compiles `src/main/java` here directly, so the repository no longer depends on the external thesis path for its baseline tests.

Snapshot record:

- Source commit observed: `9c2de4f` (`Finalize real Fabric delivery path`)
- Snapshot date: 2026-09-13
- Snapshot size: 51 Java source files

## Functional slices

### A Authentication and access control

This is an active scope for the current test batch.

- `AuthController`
- `AuthService`
- `JwtService`
- `JwtAuthenticationFilter`
- `SecurityConfig`
- `AccessControlService`

Required DTOs, entities, repositories, and Spring Security configuration are included only as supporting dependencies for this slice.

### B Cross-chain task state control

This is an active scope for the current test batch.

- `CrossChainController`
- `CrossChainService`
- `CrossChainTask`
- `CrossChainTaskStatus`
- `MockFabricGateway`

The active B scope focuses on precheck decisions, task creation, successful execution, failure transition, verification decisions, and retry idempotency. Full Fabric-network deployment remains outside the current unit-test baseline.

## Scope boundary

Frontend display details, full Fabric network deployment, database administration, and unrelated business flows are outside the module-one SUT scope. The selected snapshot is used only to make the A+B baseline reproducible; it does not expand the functional test scope.

The snapshot is intentionally limited to the A+B production classes and their compile-time support types. It is not a copy of the full backend. When the thesis source changes, refresh this snapshot deliberately and record the source commit and date here.
