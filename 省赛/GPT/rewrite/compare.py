from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET
import json, re, sys

root = Path(__file__).resolve().parent
name = sys.argv[1]

def result(project):
    target = project / "target"
    jacoco = ET.parse(target / "site/jacoco/jacoco.xml").getroot()
    stats = {counter.get("type"): [int(counter.get("covered")), int(counter.get("missed")) + int(counter.get("covered"))]
             for counter in jacoco.findall("counter")}
    text = (target / "pit-reports/mutations.xml").read_text("utf-8", errors="replace")
    text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f]", "\ufffd", text)
    mutations = {}
    for item in ET.fromstring(text.split("</mutations>", 1)[0] + "</mutations>"):
        identity = tuple(item.findtext(tag, "") for tag in
                         ("mutatedClass", "mutatedMethod", "methodDescription", "mutator", "index"))
        indexes = tuple(x.text for x in item.findall("./indexes/index"))
        key = identity + (indexes,)
        assert key not in mutations, key
        mutations[key] = item.get("status")
    return stats, mutations

old, baseline = result(root.parent / name)
new, current = result(root / name)
common = baseline.keys() & current.keys()
gained = [key for key in common if baseline[key] != "KILLED" and current[key] == "KILLED"]
lost = [key for key in common if baseline[key] == "KILLED" and current[key] != "KILLED"]
summary = {"old_jacoco": old, "new_jacoco": new,
           "old_pit": Counter(baseline.values()), "new_pit": Counter(current.values()),
           "same_mutants": baseline.keys() == current.keys(), "gained": len(gained), "lost": len(lost)}
print(json.dumps(summary, indent=2))

