from pathlib import Path
import sys, hashlib, json, zipfile, shutil, xml.etree.ElementTree as ET
key=sys.argv[1]
root=Path(r'B:\软件测试大赛')
out=root/'output'/key
p=out/'project'
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
baseline=json.loads((out/'source-baseline.json').read_text())
current={f.relative_to(p).as_posix():sha(f) for f in [p/'pom.xml',*sorted((p/'src/main').rglob('*'))] if f.is_file()}
assert current==baseline,'Source or POM changed'
tests=list((p/'src/test').rglob('*.java')); assert len(tests)==1
test=tests[0]; shutil.copy2(test,out/test.name)
suites=[ET.parse(f).getroot() for f in (p/'target/surefire-reports').glob('TEST-*.xml')]
counts={n:sum(int(s.attrib.get(n,0)) for s in suites) for n in ['tests','failures','errors','skipped']}
assert counts['tests']>0 and counts['failures']==counts['errors']==counts['skipped']==0,counts
jac=ET.parse(p/'target/site/jacoco/jacoco.xml').getroot()
coverage={c.attrib['type']:{k:int(c.attrib[k]) for k in ['missed','covered']} for c in jac.findall('counter')}
mut=ET.parse(p/'target/pit-reports/mutations.xml').getroot()
statuses={}
for m in mut: statuses[m.attrib['status']]=statuses.get(m.attrib['status'],0)+1
survivors=[{'status':m.attrib['status'],**{c.tag:c.text for c in m}} for m in mut if m.attrib['status']!='KILLED']
(out/'survivors.json').write_text(json.dumps(survivors,indent=2),encoding='utf-8')
name='Credit-Card-Validator' if key=='credit' else 'Research-Project-Management-System'
archive=out/(name+'.zip')
files=[f for f in p.rglob('*') if f.is_file() and not any(part in ['.idea','.git','.settings'] for part in f.relative_to(p).parts) and f.name not in ['.classpath','.project']]
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for f in files: z.write(f,name+'/'+f.relative_to(p).as_posix())
with zipfile.ZipFile(archive) as z:
 assert z.testzip() is None
 for f in files: assert z.read(name+'/'+f.relative_to(p).as_posix())==f.read_bytes()
 assert sum('/src/test/' in n and n.endswith('.java') for n in z.namelist())==1
assert archive.stat().st_size<20_000_000 and test.stat().st_size<2_000_000
manifest={'date':'2026-10-29','tests':counts,'jacoco':coverage,'pit':statuses,'protected_files':len(baseline),'source_pom_matches_official_archive':True,'zip_entries':len(files),'files':{f.name:{'bytes':f.stat().st_size,'sha256':sha(f)} for f in [out/test.name,archive]}}
(out/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
b=coverage['BRANCH']; l=coverage['LINE']; total=sum(statuses.values()); killed=statuses.get('KILLED',0)
text=f'''# {name} verification and delivery

Verified 2026-10-29 (Asia/Shanghai). Source: https://www.mooctest.net/student/contest/10
2026 developer individual registration practice; deadline 2026-10-30 20:00.
Java <= 2 MB, project ZIP <= 20 MB. ZIP includes fresh target reports.

- JUnit: {counts}.
- JaCoCo branches: {b['covered']}/{b['covered']+b['missed']} ({100*b['covered']/(b['covered']+b['missed']):.2f}%).
- JaCoCo lines: {l['covered']}/{l['covered']+l['missed']}.
- PIT DEFAULTS KILLED: {killed}/{total} ({100*killed/total:.2f}%); statuses: {statuses}.
- Source/POM: all {len(baseline)} protected files match official original ZIP, before and after execution.
- One UTF-8 test file; standalone file and ZIP contents verified byte for byte. Sizes and SHA-256 in manifest.json.
- Java 21.0.9 / Maven 3.9.11 / original POM / JUnit 5.10.2.
- Command: mvn clean test org.pitest:pitest-maven:mutationCoverage -DargLine=-javaagent:<local JaCoCo 0.8.11 runtime>=destfile=target/jacoco.exec -DoutputFormats=HTML,XML
- Relative JaCoCo destination avoids the documented non-ASCII absolute-path issue. No change to targets or mutators.
- Reports: project/target/site/jacoco/index.html and project/target/pit-reports/index.html, also included in ZIP.
- Existing single-file test assets reviewed and reused; all reported results are from this fresh run. No new platform submission performed.
- Local metrics are not platform scores. AI/reflection permissions were not explicitly stated on the page.
'''
(out/'README.md').write_text(text,encoding='utf-8')
print(json.dumps(manifest,indent=2))
