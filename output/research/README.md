# Research-Project-Management-System verification and delivery

Delivery verified 2026-10-31 (Asia/Shanghai). Maven run finished: 2026-10-31T22:58:15+08:00.
Original contest context: https://www.mooctest.net/student/contest/10
This is a local verification/package result, not evidence of platform submission or current contest availability.

- JUnit: {'tests': 87, 'failures': 0, 'errors': 0, 'skipped': 0}.
- JaCoCo branches: 240/240 (100.00%).
- JaCoCo lines: 405/405.
- PIT raw KILLED: 284/305 (93.11%); statuses: {'KILLED': 284, 'SURVIVED': 21}.
- Source/POM: all 15 protected files match the saved original-source baseline. Archive provenance is recorded in input-record.json.
- One UTF-8 test file; standalone test and every ZIP entry checked byte for byte. SHA-256 and sizes are in manifest.json.
- Java < 2 MB, complete project ZIP < 20 MB; ZIP retains target and the complete generated report directories.
- Freshness checks: source/test/class/report modification order, successful Maven log, matching JUnit and PIT totals. These checks do not replace a cryptographic build attestation.
- Verification log: verification-whitebox-2026-10-31.log; report and test hashes are recorded in manifest.json. Exact JVM/plugin configuration remains in the unchanged POM and execution log.
- Reports: project/target/site/jacoco/index.html and project/target/pit-reports/index.html, also included in ZIP.
- survivors.json preserves class, method descriptor, mutator, bytecode indexes and blocks for each non-KILLED mutation; no denominator adjustment.
- Local metrics are not platform scores. See analysis files for remaining gaps and observed limitations.

This package is a local white-box research version. It uses reflective field observations and restores a temporarily controlled static counter. Platform permission for this technique has not been established. Overall original double-100% target is NOT MET; see whitebox-audit.md.
