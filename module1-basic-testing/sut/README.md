# Module 1 System Under Test Scope

The system under test is the current thesis system at `/home/zjc/waybill-crosschain-platform` in WSL Ubuntu 22.04. The source project remains outside this repository and is not modified by the test project.

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

### B Electronic credential upload and integrity verification

This is deferred for the current batch because its end-to-end setup spans local files, encryption metadata, database persistence, and the Fabric boundary. The exploratory test sources may remain in the test project, but B is not counted as a current delivery target.

- `FileController`
- `WaybillController` file verification endpoint
- `FileStorageService`
- `HashUtils`
- `CryptoUtils`
- `FabricGateway` with `MockFabricGateway` at the external-ledger boundary

The slice verifies plaintext SHA-256, AES-256-GCM encryption, ciphertext storage, digest persistence, original-file verification, and tampered-file rejection.

### C Cross-chain task state control

This is an active scope for the current test batch.

- `CrossChainController`
- `CrossChainService`
- `CrossChainTask`
- `CrossChainTaskStatus`
- `MockFabricGateway`

The active C scope focuses on precheck decisions, task creation, successful execution, failure transition, and retry idempotency. Full Fabric-network deployment remains outside the current unit-test baseline.

## Scope boundary

Frontend display details, full Fabric network deployment, database administration, and unrelated business flows are outside the module-one SUT scope. The test project may compile against the backend artifact so that the selected slices use their real production classes; this dependency choice does not expand the functional test scope.
