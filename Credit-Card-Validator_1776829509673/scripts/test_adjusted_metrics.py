"""Ensure metric adjustment cannot silently hide new coverage gaps or survivors."""

import copy
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from adjusted_metrics import audit, calculate


class AdjustedMetricsTest(unittest.TestCase):
    def setUp(self):
        self.project = Path(__file__).resolve().parents[1]
        self.jacoco = ET.parse(self.project / "reports/jacoco.xml").getroot()
        self.pit = ET.parse(self.project / "reports/mutations.xml").getroot()
        self.policy = json.loads((self.project / "reports/adjustment-policy.json").read_text("utf-8-sig"))

    def test_real_evidence_preserves_raw_metrics_and_separately_reports_adjusted_metrics(self):
        result = audit(self.project)
        self.assertEqual((233, 236), (result["raw"]["branch_covered"], result["raw"]["branch_total"]))
        self.assertEqual((263, 272), (result["raw"]["mutations_killed"], result["raw"]["mutation_total"]))
        self.assertEqual(233, result["adjusted"]["branch_total"])
        self.assertEqual(263, result["adjusted"]["mutation_total"])
        self.assertTrue(result["passed"])

    def test_new_survivor_is_not_excluded(self):
        self.pit.find("mutation[@status='KILLED']").set("status", "SURVIVED")
        result = calculate(self.jacoco, self.pit, self.policy)
        self.assertEqual(9, result["excluded"]["equivalent_mutations"])
        self.assertEqual(262, result["adjusted"]["mutations_killed"])
        self.assertFalse(result["passed"])

    def test_timeout_is_not_counted_as_a_strict_kill(self):
        self.pit.find("mutation[@status='KILLED']").set("status", "TIMED_OUT")
        result = calculate(self.jacoco, self.pit, self.policy)
        self.assertFalse(result["passed"])

    def test_additional_uncovered_branch_fails_adjusted_gate(self):
        line = next(line for line in self.jacoco.findall("package/sourcefile/line")
                    if int(line.get("cb")) >= 2 and int(line.get("mb")) == 0)
        line.set("cb", str(int(line.get("cb")) - 1))
        line.set("mb", "1")
        total = self.jacoco.find("counter[@type='BRANCH']")
        total.set("covered", "232")
        total.set("missed", "4")
        result = calculate(self.jacoco, self.pit, self.policy)
        self.assertEqual(233, result["adjusted"]["branch_total"])
        self.assertFalse(result["passed"])

    def test_missing_mutation_requires_new_review(self):
        self.pit.remove(self.pit.find("mutation"))
        with self.assertRaisesRegex(ValueError, "inventory changed"):
            calculate(self.jacoco, self.pit, self.policy)

    def test_duplicate_exclusion_is_rejected(self):
        self.policy["equivalent_mutations"].append(copy.deepcopy(self.policy["equivalent_mutations"][0]))
        with self.assertRaisesRegex(ValueError, "Duplicate equivalent"):
            calculate(self.jacoco, self.pit, self.policy)


if __name__ == "__main__":
    unittest.main()
