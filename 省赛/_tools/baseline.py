import sys, os, hashlib, json

def digest(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()

def manifest(root, subdirs):
    rows = []
    for sub in subdirs:
        base = os.path.join(root, sub)
        if not os.path.isdir(base):
            continue
        for dirpath, _dirnames, filenames in os.walk(base):
            for fn in sorted(filenames):
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, root).replace(os.sep, '/')
                rows.append({'path': rel, 'bytes': os.path.getsize(full), 'sha256': digest(full)})
    return rows

if __name__ == '__main__':
    proj = sys.argv[1]
    out = sys.argv[2]
    subs = sys.argv[3:] or ['src/main', 'src/test']
    rows = manifest(proj, subs)
    pom = os.path.join(proj, 'pom.xml')
    if os.path.isfile(pom):
        rows.append({'path': 'pom.xml', 'bytes': os.path.getsize(pom), 'sha256': digest(pom)})
    rows.sort(key=lambda r: r['path'])
    with open(out, 'w', encoding='utf-8') as f:
        json.dump({'root': os.path.abspath(proj), 'count': len(rows), 'files': rows}, f, ensure_ascii=False, indent=1)
    print(proj, 'files:', len(rows))
