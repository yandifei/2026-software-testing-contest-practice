"""Compare two verified contest runs without changing either report or denominator.

Usage: python compare-runs.py BEFORE_DIRECTORY AFTER_DIRECTORY [--output diff.json]
Exit status: 0 comparable runs; 2 changed mutation set/source baseline (JSON still
written); 1 invalid or inconsistent evidence. Killing-test names are not identities.
"""
from pathlib import Path
import argparse
from collections import Counter
import hashlib
import json
import sys
import xml.etree.ElementTree as ET


PIT_RELATIVE_PATH = "target/pit-reports/mutations.xml"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def file_sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def mutation_key(node):
    """Identify the bytecode mutation, not its source line or killing test."""
    fields = tuple(node.findtext(tag) for tag in
                   ("mutatedClass", "mutatedMethod", "methodDescription", "mutator"))
    require(all(fields), "PIT mutation lacks class/method/descriptor/mutator")
    indexes = tuple(int(i.text) for i in node.findall("indexes/index"))
    require(indexes, "PIT mutation lacks bytecode indexes")
    return fields + (indexes,)


def mutation_record(key, node):
    return {
        "class": key[0], "method": key[1], "descriptor": key[2],
        "mutator": key[3], "indexes": list(key[4]),
        "blocks": [int(n.text) for n in node.findall("blocks/block")],
        "source_file": node.findtext("sourceFile"),
        "line": node.findtext("lineNumber"),
        "description": node.findtext("description"),
        "status": node.get("status"),
    }


def load_run(directory):
    directory = Path(directory).resolve()
    manifest_path = directory / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    counts = manifest.get("tests", {})
    require(counts.get("tests", 0) > 0 and all(counts.get(k) == 0 for k in ("failures", "errors", "skipped")),
            f"Manifest does not describe a fully passing test run: {directory}")
    project = directory / "project"
    pit_path = project / PIT_RELATIVE_PATH
    pit_hash = file_sha256(pit_path)
    recorded_pit_hash = manifest.get("reports", {}).get(PIT_RELATIVE_PATH)
    require(recorded_pit_hash == pit_hash,
            f"PIT report hash missing or differs from validated manifest: {directory}")
    mutations = {}
    for node in ET.parse(pit_path).getroot():
        if node.tag != "mutation":
            continue
        key = mutation_key(node)
        require(key not in mutations, f"Duplicate PIT mutation identity: {key}")
        record = mutation_record(key, node)
        require(record["status"], f"Missing PIT mutation status: {key}")
        mutations[key] = record
    require(mutations, f"No mutations in {pit_path}")
    statuses = dict(sorted(Counter(m["status"] for m in mutations.values()).items()))
    require(statuses == {k: v for k, v in manifest.get("pit", {}).items() if v},
            f"Manifest PIT counts disagree with XML: {directory}")
    baseline_path = directory / "source-baseline.json"
    baseline = json.loads(baseline_path.read_text(encoding="utf-8-sig"))
    require(bool(baseline), f"Source/POM baseline is empty: {directory}")
    require(manifest.get("source_pom_matches_saved_baseline") is True,
            f"Manifest lacks successful source/POM baseline verification: {directory}")
    require(manifest.get("baseline_sha256") == file_sha256(baseline_path),
            f"Baseline hash differs from manifest: {directory}")
    require(manifest.get("protected_files") == len(baseline),
            f"Protected file count differs from baseline: {directory}")
    # Compare normalized path/hash entries, independent of JSON whitespace.
    baseline_identity = hashlib.sha256(json.dumps(baseline, sort_keys=True).encode("utf-8")).hexdigest()
    return {
        "directory": str(directory), "manifest_sha256": file_sha256(manifest_path),
        "pit_xml_sha256": pit_hash, "baseline_identity": baseline_identity,
        "test_source_sha256": manifest.get("test_source_sha256"),
        "date": manifest.get("date"), "run_finished_at": manifest.get("run_finished_at"),
        "tests": counts, "jacoco": manifest.get("jacoco", {}),
        "statuses": statuses, "generated": len(mutations),
        "killed": statuses.get("KILLED", 0), "mutations": mutations,
    }


def compare_runs(before, after):
    old, new = before["mutations"], after["mutations"]
    old_keys, new_keys = set(old), set(new)
    shared = sorted(old_keys & new_keys)
    removed = sorted(old_keys - new_keys)
    added = sorted(new_keys - old_keys)
    reasons = []
    if removed or added:
        reasons.append("Generated mutation identities changed; raw percentages are not a like-for-like comparison.")
    if before["baseline_identity"] != after["baseline_identity"]:
        reasons.append("Source/POM baselines differ; behavior and mutation configuration may have changed.")
    gains = [key for key in shared if old[key]["status"] != "KILLED" and new[key]["status"] == "KILLED"]
    losses = [key for key in shared if old[key]["status"] == "KILLED" and new[key]["status"] != "KILLED"]

    def transition(key):
        return {**new[key], "before_status": old[key]["status"], "after_status": new[key]["status"]}

    def summary(run):
        return {key: value for key, value in run.items() if key != "mutations"}

    return {
        "like_for_like": not reasons,
        "limitations": reasons,
        "identity_fields": ["class", "method", "descriptor", "mutator", "indexes"],
        "identity_note": "Blocks, source lines and killingTest are not identity fields. Status transitions are computed only for shared identities.",
        "before": summary(before), "after": summary(after),
        "generated_set": {
            "unchanged": not (removed or added), "common_count": len(shared),
            "removed_count": len(removed), "added_count": len(added),
            "removed": [old[key] for key in removed], "added": [new[key] for key in added],
        },
        "shared_mutations": {
            "killed_gain_count": len(gains), "killed_loss_count": len(losses),
            "killed_gains": [transition(key) for key in gains],
            "killed_losses": [transition(key) for key in losses],
            "all_status_changes": [transition(key) for key in shared if old[key]["status"] != new[key]["status"]],
        },
        "denominator_policy": "Original generated totals are retained for both runs. No equivalent-mutant exclusions or adjusted scores are calculated.",
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("before", type=Path)
    parser.add_argument("after", type=Path)
    parser.add_argument("--output", type=Path, help="write JSON here instead of stdout; refuses to overwrite an existing file")
    args = parser.parse_args(argv)
    result = compare_runs(load_run(args.before), load_run(args.after))
    serialized = json.dumps(result, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        # Exclusive creation protects live reports and previous comparison evidence.
        with args.output.open("x", encoding="utf-8") as stream:
            stream.write(serialized)
    else:
        print(serialized, end="")
    return 0 if result["like_for_like"] else 2


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError, ET.ParseError) as exc:
        print(f"Comparison rejected: {exc}", file=sys.stderr)
        sys.exit(1)
