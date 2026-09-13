# Module 1 Automated Tests

This directory contains the independent JUnit 5 test project for the current thesis system.

## Environment

- JDK 17
- Maven 3.6.3 or newer
- Spring Boot Test
- JUnit 5
- Mockito

## Run

The selected SUT source snapshot is stored in `../sut/src/main/java`. It was copied from the thesis backend working tree and is not modified by the test run. The original thesis system remains outside this repository and is not required to run the baseline tests.

```bash
bash run-tests.sh
```

Test reports are written to `target/surefire-reports/`. To refresh the snapshot later, copy only the selected A+B production files from the thesis backend and record the source commit in `sut/README.md`; do not edit the thesis source through this project.

The current active test batch covers JWT issue and validation, authentication service decisions, role access control, cross-chain precheck and task state transitions, execution failure handling, verification decisions, and retry idempotency. Fabric is mocked at the service boundary where external network access is not required.
