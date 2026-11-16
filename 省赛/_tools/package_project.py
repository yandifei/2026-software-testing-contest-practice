import sys, os, zipfile, time

SKIP_DIRS = {'__pycache__', '.idea', '.vscode', '.git', '.settings'}
SKIP_SUFFIX = ('.bak', '.tmp', '.orig', '.iml', '.log')
SKIP_NAMES = {'.DS_Store', 'Thumbs.db', 'out.txt', 'err.txt', '.classpath', '.project'}

def should_skip(rel, name):
    if name in SKIP_NAMES or name.endswith(SKIP_SUFFIX):
        return True
    parts = rel.split('/')
    if any(p in SKIP_DIRS for p in parts):
        return True
    if rel.startswith('.git/'):
        return True
    return False

def build(project, out_zip):
    project = os.path.abspath(project)
    out_zip = os.path.abspath(out_zip)
    if os.path.exists(out_zip):
        os.remove(out_zip)
    entries = []
    with zipfile.ZipFile(out_zip, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for dirpath, dirnames, filenames in os.walk(project):
            rel_dir = os.path.relpath(dirpath, project).replace(os.sep, '/')
            if rel_dir == '.':
                rel_dir = ''
            # prune
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and not (rel_dir == '' and d == '.git')]
            for fn in sorted(filenames):
                full = os.path.join(dirpath, fn)
                rel = (rel_dir + '/' + fn) if rel_dir else fn
                if should_skip(rel, fn):
                    continue
                if os.path.abspath(full) == out_zip:
                    continue
                z.write(full, rel)
                entries.append(rel)
    return entries

def verify(project, out_zip):
    project = os.path.abspath(project)
    report = {'count': 0, 'top': [], 'errors': [], 'bytes': os.path.getsize(out_zip)}
    with zipfile.ZipFile(out_zip) as z:
        names = z.namelist()
        report['count'] = len(names)
        report['top'] = sorted({n.split('/')[0] for n in names})
        bad = [n for n in names if n.startswith('/') or '..' in n.split('/') or ':' in n.split('/')[0]]
        if bad:
            report['errors'].append('unsafe entries: %s' % bad[:5])
        if 'pom.xml' not in names:
            report['errors'].append('missing pom.xml at root')
        if not any(n == 'src/test/java/net/mooctest/StudentTest.java' for n in names):
            report['errors'].append('missing src/test/java/net/mooctest/StudentTest.java')
        test_java = [n for n in names if n.startswith('src/test/java/') and n.endswith('.java')]
        if len(test_java) != 1:
            report['errors'].append('test java files: %s' % test_java)
        if not any(n == 'target/pit-reports/index.html' for n in names):
            report['errors'].append('missing target/pit-reports/index.html')
        if not any(n == 'target/site/jacoco/index.html' for n in names):
            report['errors'].append('missing target/site/jacoco/index.html')
        if not any(n.startswith('src/main/java/net/mooctest/') for n in names):
            report['errors'].append('missing src/main sources')
        # byte-identical StudentTest inside zip vs on disk
        inner = 'src/test/java/net/mooctest/StudentTest.java'
        if inner in names:
            disk = os.path.join(project, 'src', 'test', 'java', 'net', 'mooctest', 'StudentTest.java')
            if open(disk, 'rb').read() != z.read(inner):
                report['errors'].append('StudentTest.java bytes differ from disk')
    report['MiB'] = round(report['bytes'] / 1048576, 2)
    return report

if __name__ == '__main__':
    project, out_zip = sys.argv[1], sys.argv[2]
    t0 = time.time()
    entries = build(project, out_zip)
    rep = verify(project, out_zip)
    print('zip:', out_zip)
    print('entries:', rep['count'], 'size MiB:', rep['MiB'], 'top:', rep['top'])
    print('elapsed %.1fs' % (time.time() - t0))
    if rep['errors']:
        print('ERRORS:')
        for e in rep['errors']:
            print(' -', e)
        sys.exit(1)
    print('OK: zip 结构校验通过（pom.xml / src / target 报告 / 单测试文件 / 字节一致）')
