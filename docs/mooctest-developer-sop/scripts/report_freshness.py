"""Validate a PIT mutations.xml before it is used for any final number.

Checks, in this order:
  1. content: non-empty, has no NUL bytes, strict XML parse succeeds;
  2. structure: root is <mutations>, every record is <mutation>, no NOT_STARTED;
  3. count: mutation count equals --expect-total and/or the "Generated N mutations"
     line of the PIT console log;
  4. freshness: the XML mtime is not older than --test-file mtime.

Exit codes: 0 pass, 1 content/structure, 3 count mismatch, 2 stale.
Recovery-style parsing is intentionally NOT offered here: a damaged report is not a
final denominator (see references/error-prevention.md section 3).
"""
import argparse
import os
import re
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

GENERATED = re.compile(r"Generated\s+(\d+)\s+mutations")
STATUSES = {
    "KILLED", "SURVIVED", "TIMED_OUT", "NON_VIABLE", "MEMORY_ERROR",
    "NOT_STARTED", "STARTED", "RUN_ERROR", "NO_COVERAGE",
}


def inspect(xml_path, test_file=None, expect_total=None, console_log=None):
    """Return (exit_code, list_of_problems, stats). Never raises for bad input."""
    problems = []
    stats = {"bytes": 0, "nuls": 0, "mutations": 0, "statuses": {}, "xml_mtime": None,
             "test_mtime": None, "generated": None}
    path = Path(xml_path)
    try:
        data = path.read_bytes()
    except OSError as exc:
        return 1, [f"cannot read {path}: {exc}"], stats
    stats["bytes"] = len(data)
    stats["xml_mtime"] = path.stat().st_mtime
    if not data:
        return 1, ["file is empty"], stats
    nuls = data.count(b"\x00")
    stats["nuls"] = nuls
    if nuls:
        first = data.find(b"\x00")
        problems.append(f"contains {nuls} NUL byte(s), first at offset {first}; "
                        "report is damaged, rerun PIT instead of recovering fragments")
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError as exc:
        problems.append(f"not valid UTF-8: {exc}")
        text = None
    if text is not None:
        try:
            root = ET.fromstring(text)
        except ET.ParseError as exc:
            problems.append(f"strict XML parse failed: {exc}")
            root = None
        if root is not None:
            if root.tag != "mutations":
                problems.append(f"unexpected root <{root.tag}>, expected <mutations>")
            bad = [child.tag for child in root if child.tag != "mutation"]
            if bad:
                problems.append(f"unexpected children inside <mutations>: {sorted(set(bad))[:3]}")
            mutations = [child for child in root if child.tag == "mutation"]
            stats["mutations"] = len(mutations)
            for mutation in mutations:
                status = mutation.get("status")
                stats["statuses"][status] = stats["statuses"].get(status, 0) + 1
                if status not in STATUSES:
                    problems.append(f"unknown mutation status: {status!r}")
                if status in {"NOT_STARTED", "STARTED"}:
                    problems.append(f"unfinished mutation status: {status}")
            if mutations and sum(stats["statuses"].values()) != len(mutations):
                problems.append("status counts do not add up to the mutation total")
    if expect_total is not None and stats["mutations"] != expect_total:
        problems.append(f"mutation count {stats['mutations']} != expected {expect_total}")
    if console_log:
        log_path = Path(console_log)
        try:
            log_text = log_path.read_text(encoding="utf-8", errors="replace")
        except OSError as exc:
            problems.append(f"cannot read console log {log_path}: {exc}")
        else:
            found = GENERATED.search(log_text)
            if not found:
                problems.append("console log has no 'Generated N mutations' line")
            else:
                stats["generated"] = int(found.group(1))
                if stats["mutations"] != stats["generated"]:
                    problems.append(f"XML mutations {stats['mutations']} != console Generated "
                                    f"{stats['generated']}")
    if test_file:
        test_path = Path(test_file)
        if not test_path.exists():
            problems.append(f"test file not found: {test_path}")
        elif stats["xml_mtime"] is not None:
            stats["test_mtime"] = test_path.stat().st_mtime
            if stats["xml_mtime"] < stats["test_mtime"]:
                problems.append("report is OLDER than the test file; rerun test + PIT before "
                                "using these numbers")
    if not problems:
        return 0, [], stats
    if any("OLD" in p or "OLDER" in p for p in problems):
        return 2, problems, stats
    if any("!=" in p for p in problems):
        return 3, problems, stats
    return 1, problems, stats


def _write_fixture(path, count, status="KILLED"):
    records = "".join(
        f'<mutation detected="true" status="{status}"><mutatedClass>demo.C</mutatedClass>'
        f"<mutatedMethod>m</mutatedMethod><methodDescription>()V</methodDescription>"
        f"<lineNumber>{i + 1}</lineNumber><mutator>x.Y</mutator>"
        f"<indexes><index>{i}</index></indexes><blocks><block>0</block></blocks>"
        f"<description>d</description></mutation>"
        for i in range(count)
    )
    path.write_text(f"<mutations>{records}</mutations>", encoding="utf-8")


def self_test():
    failures = []
    with tempfile.TemporaryDirectory() as tmp:
        base = Path(tmp)
        test_file = base / "StudentTest.java"
        test_file.write_text("class StudentTest {}", encoding="utf-8")
        old = time.time() - 120
        os.utime(test_file, (old, old))

        good = base / "good.xml"
        _write_fixture(good, 3)
        code, problems, _ = inspect(good, test_file=test_file, expect_total=3)
        if code != 0:
            failures.append(f"valid report rejected: {problems}")

        stale = base / "stale.xml"
        _write_fixture(stale, 3)
        os.utime(stale, (old - 60, old - 60))
        code, _, _ = inspect(stale, test_file=test_file, expect_total=3)
        if code != 2:
            failures.append(f"stale report: expected 2, got {code}")

        damaged = base / "damaged.xml"
        damaged.write_bytes(b"<mutations>" + b"\x00" * 8 + b"</mutations>")
        code, _, _ = inspect(damaged, test_file=test_file)
        if code != 1:
            failures.append(f"NUL report: expected 1, got {code}")

        mismatch = base / "mismatch.xml"
        _write_fixture(mismatch, 2)
        code, _, _ = inspect(mismatch, test_file=test_file, expect_total=3)
        if code != 3:
            failures.append(f"count mismatch: expected 3, got {code}")

        log = base / "pit.log"
        log.write_text(">> Generated 2 mutations Killed 1 (50%)\n", encoding="utf-8")
        code, _, _ = inspect(mismatch, console_log=log)
        if code != 0:
            failures.append(f"console count should match 2: got {code}")

        bad_log = base / "bad.log"
        bad_log.write_text(">> Generated 5 mutations Killed 1 (20%)\n", encoding="utf-8")
        code, _, _ = inspect(mismatch, console_log=bad_log)
        if code != 3:
            failures.append(f"console mismatch: expected 3, got {code}")

    for line in failures:
        print("SELF-TEST FAIL:", line)
    if failures:
        return 1
    print("SELF-TEST PASS: valid/stale/NUL/count/console cases")
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("xml", nargs="?", help="path to target/pit-reports/mutations.xml")
    parser.add_argument("--test-file", help="StudentTest.java whose mtime the report must follow")
    parser.add_argument("--expect-total", type=int, help="mutation total from an independent source")
    parser.add_argument("--console-log", help="PIT console log containing 'Generated N mutations'")
    parser.add_argument("--self-test", action="store_true", help="run built-in fixture checks")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()
    if not args.xml:
        parser.error("xml is required unless --self-test is used")
    code, problems, stats = inspect(args.xml, args.test_file, args.expect_total, args.console_log)
    print(f"file={args.xml} bytes={stats['bytes']} nuls={stats['nuls']} "
          f"mutations={stats['mutations']} statuses={stats['statuses']}")
    if code == 0:
        print("PASS: report is complete, fresh and consistent; still confirm build completion and scope.")
        return 0
    print("REJECT: do not use these numbers as a final denominator.")
    for problem in problems:
        print("  -", problem)
    return code


if __name__ == "__main__":
    sys.exit(main())
