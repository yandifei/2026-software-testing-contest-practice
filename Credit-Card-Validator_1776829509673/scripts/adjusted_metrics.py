"""Audit raw reports and calculate explicitly adjusted, not official, metrics."""

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def normalized_hash(path):
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def mutation_key(mutation):
    return (
        mutation.findtext("mutatedClass"), mutation.findtext("mutatedMethod"),
        mutation.findtext("methodDescription"), int(mutation.findtext("lineNumber")),
        mutation.findtext("mutator"),
        tuple(int(index.text) for index in mutation.findall("indexes/index")),
    )


def policy_key(entry):
    return (entry["class"], entry["method"], entry["descriptor"], entry["line"],
            entry["mutator"], tuple(entry["indexes"]))


def require(condition, message):
    if not condition:
        raise ValueError(message)


def calculate(jacoco, pit, policy):
    """Exclude only reviewed identities; new gaps remain in the denominator."""
    counter = jacoco.find("counter[@type='BRANCH']")
    require(counter is not None, "Missing JaCoCo branch counter")
    covered, missed = int(counter.get("covered")), int(counter.get("missed"))
    lines = {}
    for package in jacoco.findall("package"):
        for source in package.findall("sourcefile"):
            for line in source.findall("line"):
                key = (package.get("name"), source.get("name"), int(line.get("nr")))
                require(key not in lines, "Duplicate JaCoCo source line")
                lines[key] = (int(line.get("cb")), int(line.get("mb")))
    require(all(c >= 0 and m >= 0 for c, m in lines.values()), "Negative branch counters")
    require(sum(c for c, _ in lines.values()) == covered and
            sum(m for _, m in lines.values()) == missed, "Inconsistent JaCoCo totals")
    require(covered + missed == policy["branch_total"], "Branch inventory changed; review policy")
    classes = {c.get("name") for c in jacoco.findall("package/class")}
    require(classes == set(policy["classes"]), "Covered class inventory changed")

    branch_exclusions = {}
    for entry in policy["unreachable_branches"]:
        key = (entry["package"], entry["file"], entry["line"])
        require(key not in branch_exclusions, "Duplicate branch exclusion")
        require(lines.get(key) == (1, 1), "Reviewed branch shape changed: " + str(key))
        branch_exclusions[key] = 1
    excluded_branches = len(branch_exclusions)

    mutations = {}
    allowed_statuses = {"KILLED", "SURVIVED", "TIMED_OUT", "NO_COVERAGE", "RUN_ERROR",
                        "NON_VIABLE", "MEMORY_ERROR", "NOT_STARTED", "STARTED"}
    for mutation in pit.findall("mutation"):
        key = mutation_key(mutation)
        require(key not in mutations, "Duplicate PIT mutation identity")
        require(mutation.get("status") in allowed_statuses, "Unknown PIT status")
        mutations[key] = mutation.get("status")
    require(len(mutations) == policy["mutation_total"], "Mutation inventory changed; review policy")
    equivalent = set()
    for entry in policy["equivalent_mutations"]:
        key = policy_key(entry)
        require(key not in equivalent, "Duplicate equivalent mutation exclusion")
        require(mutations.get(key) == "SURVIVED", "Equivalent mutation missing or status changed")
        equivalent.add(key)

    statuses = Counter(mutations.values())
    branch_denominator = covered + missed - excluded_branches
    mutation_denominator = len(mutations) - len(equivalent)
    require(branch_denominator > 0 and mutation_denominator > 0, "Empty adjusted denominator")
    return {
        "scope": "Adjusted metrics only; original JaCoCo and PIT reports are unchanged",
        "raw": {"branch_covered": covered, "branch_total": covered + missed,
                "branch_percent": round(100 * covered / (covered + missed), 2),
                "mutations_killed": statuses["KILLED"], "mutation_total": len(mutations),
                "mutation_percent": round(100 * statuses["KILLED"] / len(mutations), 2),
                "mutation_statuses": dict(sorted(statuses.items()))},
        "excluded": {"unreachable_branches": excluded_branches,
                     "equivalent_mutations": len(equivalent)},
        "adjusted": {"branch_covered": covered, "branch_total": branch_denominator,
                     "branch_percent": round(100 * covered / branch_denominator, 2),
                     "mutations_killed": statuses["KILLED"], "mutation_total": mutation_denominator,
                     "mutation_percent": round(100 * statuses["KILLED"] / mutation_denominator, 2)},
        "passed": covered == branch_denominator and statuses["KILLED"] == mutation_denominator,
    }


def audit(project):
    policy_path = project / "reports/adjustment-policy.json"
    policy = json.loads(policy_path.read_text(encoding="utf-8-sig"))
    actual_sources = {p.relative_to(project).as_posix() for p in (project / "src/main").rglob("*.java")}
    actual_tests = {p.relative_to(project).as_posix() for p in (project / "src/test").rglob("*.java")}
    require(set(policy["input_sha256_lf"]) == actual_sources | actual_tests | {"pom.xml"},
            "Source/config/test inventory changed; rerun tests and review policy")
    for relative, expected in policy["input_sha256_lf"].items():
        require(normalized_hash(project / relative) == expected,
                "Source/config/test hash changed: " + relative)
    jacoco = ET.parse(project / "reports/jacoco.xml").getroot()
    pit = ET.parse(project / "reports/mutations.xml").getroot()
    result = calculate(jacoco, pit, policy)
    result["policy"] = "reports/adjustment-policy.json"
    result["input_sha256_lf"] = {name: normalized_hash(project / name) for name in
                                  ("reports/jacoco.xml", "reports/mutations.xml",
                                   "reports/adjustment-policy.json")}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="Optional separate JSON output; never overwrite raw reports")
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[1]
    try:
        if args.output:
            require(args.output.resolve() == (project / "reports/adjusted-metrics.json").resolve(),
                    "Output must be reports/adjusted-metrics.json")
        result = audit(project)
        rendered = json.dumps(result, indent=2, ensure_ascii=False) + "\n"
        if args.output:
            args.output.write_text(rendered, encoding="utf-8")
        print(rendered, end="")
        return 0 if result["passed"] else 1
    except (ValueError, OSError, ET.ParseError, KeyError, TypeError) as error:
        print("Audit failed: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
