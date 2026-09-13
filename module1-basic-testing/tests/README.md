# Module 1 Automated Tests

This directory contains the independent JUnit 5 test project for the current thesis system.

## Environment

- JDK 17
- Maven 3.6.3 or newer
- Spring Boot Test
- JUnit 5
- Mockito

## Run

The thesis system is kept outside this test project and is not modified. The runner first packages the current backend working tree offline, then uses its original plain JAR as a local test dependency.

```bash
export SUT_ROOT="$HOME/waybill-crosschain-platform"
bash run-tests.sh
```

The default `SUT_ROOT` is `$HOME/waybill-crosschain-platform` in WSL Ubuntu 22.04. Test reports are written to `target/surefire-reports/`.

The current active test batch covers JWT issue and validation, authentication service decisions, role access control, cross-chain precheck and task state transitions, execution failure handling, and retry idempotency. Fabric is mocked at the service boundary where external network access is not required.

The file-upload and AES/SHA-256 tests currently present in the source tree are exploratory material for the deferred B scope; they are not counted as the current A+C delivery boundary.
