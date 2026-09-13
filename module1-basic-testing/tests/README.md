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

Test reports are written to `target/surefire-reports/`. To refresh the snapshot later, copy only the selected production files from the thesis backend and record the source commit in `sut/README.md`; do not edit the thesis source through this project.

The current active test batch covers JWT issue and validation, authentication service decisions, role access control, cross-chain precheck and task state transitions, execution failure handling, verification decisions, and retry idempotency. Fabric is mocked at the service boundary where external network access is not required.

## Reproduce the three open defects

The intentionally failing probes live in `src/defect-probes/java`. The `defect-probes` Maven profile selects that directory instead of the regular test sources, so `bash run-tests.sh` and plain `mvn clean test` keep running the 32 regular tests.

Run one probe at a time from this directory:

```bash
bash ./run-defect-probe.sh D-001
bash ./run-defect-probe.sh D-002
bash ./run-defect-probe.sh D-003
```

Each script run prints the test name and assertion failure, verifies that the failure is the expected defect rather than a build error, and saves the raw Maven output to `../evidence/defect-probes/D-00X.log`. An expected defect failure makes the script exit successfully. These probes use Mockito and do not modify the thesis source, MySQL, or a Fabric network.

For a terminal screenshot, capture the final `截图摘要` block: it shows the observed value, `Tests run: 1, Failures: 1, Errors: 0`, and `EXPECTED_FAILURE_REPRODUCED`. In Windows, press `Win+Shift+S` and select that block. Keep the original `.log` file as the verifiable evidence; `BUILD FAILURE` in these optional runs is the intentionally failing defect assertion.

From Windows PowerShell, use this form (replace the final defect ID as needed):

```powershell
wsl.exe -d Ubuntu-22.04 -- bash -lc "cd /mnt/c/Projects/Software-Testing-Practice--Gray-Box-Group/module1-basic-testing/tests && bash ./run-defect-probe.sh D-001"
```
