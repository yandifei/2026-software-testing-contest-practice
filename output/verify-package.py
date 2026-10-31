"""Verify one completed Maven run and build its single-file contest deliverables."""
from pathlib import Path
import argparse
from datetime import date, datetime, timedelta, timezone
import hashlib
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
import zipfile


SHANGHAI = timezone(timedelta(hours=8), "Asia/Shanghai")
PROJECT_NAMES = {
    "credit": "Credit-Card-Validator",
    "research": "Research-Project-Management-System",
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def mutation_record(mutation):
    """Keep PIT bytecode indexes: line numbers alone do not identify a mutation."""
    record = dict(mutation.attrib)
    for child in mutation:
        if len(child):
            record[child.tag] = [node.text for node in child]
        else:
            record[child.tag] = child.text
    identity = {
        key: record.get(key)
        for key in ("mutatedClass", "mutatedMethod", "methodDescription", "mutator", "indexes", "blocks")
    }
    record["id"] = hashlib.sha256(
        json.dumps(identity, sort_keys=True).encode("utf-8")
    ).hexdigest()
    return record


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("key", choices=PROJECT_NAMES)
    parser.add_argument("--date", type=date.fromisoformat,
                        default=datetime.now(SHANGHAI).date(),
                        help="delivery verification date, YYYY-MM-DD (default: today in Asia/Shanghai)")
    parser.add_argument("--log", type=Path,
                        help="full successful Maven verification log (default: newest verification*.log beside project)")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    out = Path(__file__).resolve().parent / args.key
    project = out / "project"
    baseline = json.loads((out / "source-baseline.json").read_text(encoding="utf-8-sig"))
    protected = [project / "pom.xml", *sorted((project / "src/main").rglob("*"))]
    current = {
        f.relative_to(project).as_posix(): sha(f)
        for f in protected if f.is_file()
    }
    require(current == baseline, "Source/POM file set or hash differs from saved baseline")

    tests = sorted((project / "src/test").rglob("*.java"))
    require(len(tests) == 1, f"Expected one Java test source, found {len(tests)}")
    test = tests[0]
    source = test.read_text(encoding="utf-8")
    require(test.name.endswith("Test.java"), "Test filename must end with Test.java")
    require(re.search(r"\bclass\s+" + re.escape(test.stem) + r"\b", source),
            "Test class name must match filename")
    package_match = re.search(r"^\s*package\s+([\w.]+)\s*;", source, re.MULTILINE)
    test_class = ((package_match.group(1) + ".") if package_match else "") + test.stem
    compiled_test = project / "target/test-classes" / Path(*test_class.split(".")).with_suffix(".class")
    require(compiled_test.is_file(), "Compiled top-level test class is missing")
    require(compiled_test.stat().st_mtime_ns >= test.stat().st_mtime_ns,
            "Test source is newer than its compiled class; rerun full verification")

    logs = sorted(out.glob("verification*.log"), key=lambda f: f.stat().st_mtime_ns)
    log = args.log.resolve() if args.log else (logs[-1] if logs else None)
    require(log is not None and log.is_file(), "Verification log missing; pass --log")
    log_text = log.read_text(encoding="utf-8", errors="replace")
    require("BUILD SUCCESS" in log_text and "BUILD FAILURE" not in log_text,
            f"Verification log is not a successful complete run: {log.name}")
    require("Generated " in log_text and "mutations Killed " in log_text,
            "Verification log has no PIT summary")

    report_paths = sorted((project / "target/surefire-reports").glob("TEST-*.xml"))
    require(len(report_paths) == 1, "Expected exactly one Surefire test-suite XML for the single test class")
    suite = ET.parse(report_paths[0]).getroot()
    require(suite.get("name") == test_class, "Surefire report belongs to a different test class")
    counts = {name: int(suite.attrib.get(name, 0))
              for name in ("tests", "failures", "errors", "skipped")}
    require(counts["tests"] > 0 and all(counts[k] == 0 for k in ("failures", "errors", "skipped")),
            f"Tests must pass without skipped cases: {counts}")
    require(len(suite.findall("testcase")) == counts["tests"], "Surefire testcase count mismatch")

    jacoco_path = project / "target/site/jacoco/jacoco.xml"
    pit_path = project / "target/pit-reports/mutations.xml"
    report_paths += [jacoco_path, pit_path]
    input_paths = [f for f in [*protected, *sorted((project / "src/test").rglob("*"))] if f.is_file()]
    newest_input = max(f.stat().st_mtime_ns for f in input_paths)
    for report in report_paths:
        require(report.is_file(), f"Missing report: {report}")
        require(report.stat().st_mtime_ns >= newest_input,
                f"Report predates current source/POM/test input: {report.name}")
        require(log.stat().st_mtime_ns >= report.stat().st_mtime_ns,
                f"Selected log predates report: {report.name}")
    for index in [project / "target/site/jacoco/index.html", project / "target/pit-reports/index.html"]:
        require(index.is_file(), f"Missing HTML report: {index}")

    jacoco = ET.parse(jacoco_path).getroot()
    coverage = {c.attrib["type"]: {k: int(c.attrib[k]) for k in ("missed", "covered")}
                for c in jacoco.findall("counter")}
    require("BRANCH" in coverage and "LINE" in coverage, "JaCoCo counters missing")
    mutations = [mutation_record(m) for m in ET.parse(pit_path).getroot()]
    require(mutations, "PIT report contains no mutations")
    require(len({m["id"] for m in mutations}) == len(mutations), "PIT mutation identities are not unique")
    statuses = {}
    for mutation in mutations:
        status = mutation["status"]
        statuses[status] = statuses.get(status, 0) + 1
    total, killed = len(mutations), statuses.get("KILLED", 0)
    require(re.search(rf"Generated\s+{total}\s+mutations\s+Killed\s+{killed}\b", log_text),
            "PIT XML counts disagree with selected verification log")
    require(re.search(rf"Tests run:\s*{counts['tests']},\s*Failures:\s*0,\s*Errors:\s*0,\s*Skipped:\s*0", log_text),
            "Surefire XML counts disagree with selected verification log")
    survivors = [m for m in mutations if m["status"] != "KILLED"]

    name = PROJECT_NAMES[args.key]
    archive = out / (name + ".zip")
    temporary_archive = archive.with_suffix(".zip.tmp")
    files = sorted(f for f in project.rglob("*") if f.is_file()
                   and not any(part in (".idea", ".git", ".settings") for part in f.relative_to(project).parts)
                   and f.name not in (".classpath", ".project"))
    with zipfile.ZipFile(temporary_archive, "w", zipfile.ZIP_DEFLATED) as z:
        for f in files:
            z.write(f, name + "/" + f.relative_to(project).as_posix())
    with zipfile.ZipFile(temporary_archive) as z:
        require(z.testzip() is None, "ZIP integrity check failed")
        for f in files:
            require(z.read(name + "/" + f.relative_to(project).as_posix()) == f.read_bytes(),
                    f"ZIP bytes differ: {f}")
        require(sum("/src/test/" in n and n.endswith(".java") for n in z.namelist()) == 1,
                "ZIP must contain one Java test source")
    require(temporary_archive.stat().st_size < 20_000_000 and test.stat().st_size < 2_000_000,
            "Java or ZIP exceeds delivery size limit")
    temporary_archive.replace(archive)
    standalone = out / test.name
    shutil.copy2(test, standalone)
    require(standalone.read_bytes() == test.read_bytes(), "Standalone test copy differs")
    (out / "survivors.json").write_text(json.dumps(survivors, indent=2), encoding="utf-8")

    finished = re.findall(r"Finished at:\s*(\S+)", log_text)
    manifest = {
        "date": args.date.isoformat(),
        "timezone": "Asia/Shanghai",
        "run_finished_at": finished[-1] if finished else None,
        "test_source_sha256": sha(test),
        "tests": counts, "jacoco": coverage, "pit": statuses,
        "protected_files": len(baseline), "source_pom_matches_saved_baseline": True,
        "baseline_sha256": sha(out / "source-baseline.json"),
        "freshness_checks": "source/test/class/report mtimes and complete Maven log summaries; not a cryptographic build attestation",
        "verification_log": {"file": log.name, "sha256": sha(log)},
        "reports": {f.relative_to(project).as_posix(): sha(f) for f in report_paths},
        "zip_entries": len(files),
        "files": {f.name: {"bytes": f.stat().st_size, "sha256": sha(f)} for f in (standalone, archive)},
    }
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    branch, line = coverage["BRANCH"], coverage["LINE"]
    branch_total = branch["covered"] + branch["missed"]
    branch_percent = 100 * branch["covered"] / branch_total if branch_total else 100.0
    readme = f'''# {name} verification and delivery

Delivery verified {args.date.isoformat()} (Asia/Shanghai). Maven run finished: {manifest['run_finished_at']}.
Original contest context: https://www.mooctest.net/student/contest/10
This is a local verification/package result, not evidence of platform submission or current contest availability.

- JUnit: {counts}.
- JaCoCo branches: {branch['covered']}/{branch_total} ({branch_percent:.2f}%).
- JaCoCo lines: {line['covered']}/{line['covered'] + line['missed']}.
- PIT raw KILLED: {killed}/{total} ({100 * killed / total:.2f}%); statuses: {statuses}.
- Source/POM: all {len(baseline)} protected files match the saved original-source baseline. Archive provenance is recorded in input-record.json.
- One UTF-8 test file; standalone test and every ZIP entry checked byte for byte. SHA-256 and sizes are in manifest.json.
- Java < 2 MB, complete project ZIP < 20 MB; ZIP retains target and the complete generated report directories.
- Freshness checks: source/test/class/report modification order, successful Maven log, matching JUnit and PIT totals. These checks do not replace a cryptographic build attestation.
- Verification log: {log.name}; report and test hashes are recorded in manifest.json. Exact JVM/plugin configuration remains in the unchanged POM and execution log.
- Reports: project/target/site/jacoco/index.html and project/target/pit-reports/index.html, also included in ZIP.
- survivors.json preserves class, method descriptor, mutator, bytecode indexes and blocks for each non-KILLED mutation; no denominator adjustment.
- Local metrics are not platform scores. See analysis files for remaining gaps and observed limitations.
'''
    (out / "README.md").write_text(readme, encoding="utf-8")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, ET.ParseError) as exc:
        sys.exit(f"Verification failed: {exc}")
