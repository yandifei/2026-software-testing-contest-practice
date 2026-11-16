from pathlib import Path
import collections, hashlib, json, sys, zipfile
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parent
name = sys.argv[1]
project = root / name
original = root.parent / '原始项目' / name
baseline = json.loads((root / (name + '-baseline.json')).read_text('utf-8'))
def digest(data):
    return hashlib.sha256(data).hexdigest()
actual = {str(f.relative_to(project)): digest(f.read_bytes()) for f in project.rglob('*') if f.is_file() and ('src/main' in f.as_posix() or f.name == 'pom.xml')}
assert actual == baseline, 'Source or POM differs from original baseline'
assert len(list((project / 'src/test').rglob('*.java'))) == 1
script = project / 'src/test/java/net/mooctest/StudentTest.java'
reports = list((project / 'target/surefire-reports').glob('TEST-*.xml'))
assert reports, 'Missing JUnit reports'
totals = collections.Counter()
for f in reports:
    report = ET.parse(f).getroot()
    for key in ['tests', 'failures', 'errors', 'skipped']:
        totals[key] += int(report.get(key, 0))
assert totals['tests'] > 0 and totals['failures'] == totals['errors'] == totals['skipped'] == 0, totals
coverage = ET.parse(project / 'target/site/jacoco/jacoco.xml').getroot()
counters = {c.get('type'): {'covered': int(c.get('covered')), 'missed': int(c.get('missed'))} for c in coverage.findall('counter')}
mutations = ET.parse(project / 'target/pit-reports/mutations.xml').getroot()
statuses = collections.Counter(m.get('status') for m in mutations)
assert sum(statuses.values()) > 0
for path in ['target/site/jacoco/index.html', 'target/pit-reports/index.html']:
    assert (project / path).is_file(), path
output = root / 'deliverables' / name
output.mkdir(parents=True, exist_ok=True)
java = output / 'StudentTest.java'
java.write_bytes(script.read_bytes())
archive = output / (name + '.zip')
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for f in project.rglob('*'):
        if f.is_file() and not any(part in {'.git', '.idea'} for part in f.relative_to(project).parts):
            z.write(f, f.relative_to(project).as_posix())
assert java.stat().st_size <= 2000000
assert archive.stat().st_size <= 20000000
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert z.read('src/test/java/net/mooctest/StudentTest.java') == java.read_bytes()
    for relative, sha in baseline.items():
        assert digest(z.read(relative.replace('\\', '/'))) == sha, relative
    for entry in ['pom.xml', '开发者测试比赛要求.md', 'target/site/jacoco/index.html', 'target/pit-reports/index.html']:
        assert entry in z.namelist(), entry
record = {'project': name, 'contest': '2026软件测试大赛-开发者测试-个人-省赛', 'url': 'https://www.mooctest.net/student/contest/35', 'deadline': '2026-11-15 18:00 Asia/Shanghai', 'source_baseline_matches': True, 'junit': dict(totals), 'jacoco': counters, 'pit': dict(statuses), 'pit_total': sum(statuses.values()), 'submission': 'prepared', 'files': [{'name': f.name, 'bytes': f.stat().st_size, 'sha256': digest(f.read_bytes())} for f in [java, archive]]}
(output / 'verification.json').write_text(json.dumps(record, ensure_ascii=False, indent=2), 'utf-8')
print(json.dumps(record, ensure_ascii=False, indent=2))
