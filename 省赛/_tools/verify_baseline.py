import sys, os, json, hashlib

def digest(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()

def check(project, baseline_json):
    base = json.load(open(baseline_json, encoding='utf-8'))
    want = {r['path']: r for r in base['files']}
    problems = []
    # 1) src/main + pom must be identical
    for rel, rec in sorted(want.items()):
        if not (rel == 'pom.xml' or rel.startswith('src/main')):
            continue
        full = os.path.join(project, rel.replace('/', os.sep))
        if not os.path.isfile(full):
            problems.append(('MISSING', rel))
        elif digest(full) != rec['sha256']:
            problems.append(('CHANGED', rel))
    # 2) no new file under src/main
    for dirpath, _d, files in os.walk(os.path.join(project, 'src', 'main')):
        for fn in files:
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, project).replace(os.sep, '/')
            if rel not in want:
                problems.append(('ADDED', rel))
    # 3) test dir must contain exactly one java file
    tdir = os.path.join(project, 'src', 'test', 'java', 'net', 'mooctest')
    java = sorted(f for f in os.listdir(tdir)) if os.path.isdir(tdir) else []
    others = [f for f in java if f != 'StudentTest.java']
    if others:
        problems.append(('EXTRA_TEST_FILES', ', '.join(others)))
    return problems

if __name__ == '__main__':
    print('=== baseline check:', sys.argv[1])
    probs = check(sys.argv[1], sys.argv[2])
    if not probs:
        print('OK: src/main 与 pom.xml 与基线完全一致；测试目录只有 StudentTest.java')
    else:
        for kind, rel in probs:
            print(f'{kind}: {rel}')
    sys.exit(1 if probs else 0)
