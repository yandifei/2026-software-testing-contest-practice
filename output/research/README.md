# Research-Project-Management-System verification and delivery

Verified 2026-10-29 (Asia/Shanghai). Source: https://www.mooctest.net/student/contest/10
2026 developer individual registration practice; deadline 2026-10-30 20:00.
Java <= 2 MB, project ZIP <= 20 MB. ZIP includes fresh target reports.

- JUnit: {'tests': 83, 'failures': 0, 'errors': 0, 'skipped': 0}.
- JaCoCo branches: 239/240 (99.58%).
- JaCoCo lines: 405/405.
- PIT DEFAULTS KILLED: 278/305 (91.15%); statuses: {'KILLED': 278, 'SURVIVED': 26, 'NO_COVERAGE': 1}.
- Source/POM: all 15 protected files match official original ZIP, before and after execution.
- One UTF-8 test file; standalone file and ZIP contents verified byte for byte. Sizes and SHA-256 in manifest.json.
- Java 21.0.9 / Maven 3.9.11 / original POM / JUnit 5.10.2.
- Command: mvn clean test org.pitest:pitest-maven:mutationCoverage -DargLine=-javaagent:<local JaCoCo 0.8.11 runtime>=destfile=target/jacoco.exec -DoutputFormats=HTML,XML
- Relative JaCoCo destination avoids the documented non-ASCII absolute-path issue. No change to targets or mutators.
- Reports: project/target/site/jacoco/index.html and project/target/pit-reports/index.html, also included in ZIP.
- Existing single-file test assets reviewed and reused; all reported results are from this fresh run. No new platform submission performed.
- Local metrics are not platform scores. AI/reflection permissions were not explicitly stated on the page.
