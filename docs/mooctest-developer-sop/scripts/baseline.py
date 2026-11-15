"""Create a source/POM snapshot. Generate it from a trusted, untouched task archive."""
import hashlib
import json
import os
from pathlib import Path
import sys


def digest(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def is_link(path):
    return os.path.islink(path) or getattr(os.path, 'isjunction', lambda p: False)(path)


def walk_error(exc):
    raise exc


def manifest(root, subdirs):
    root = Path(root).resolve(strict=True)
    rows = []
    for sub in subdirs:
        base = root / sub
        if not base.is_relative_to(root) or '..' in Path(sub).parts:
            raise ValueError('snapshot directory must be inside project: ' + sub)
        for part in (base, *base.parents):
            if part == root:
                break
            if is_link(part):
                raise ValueError('linked source directory: ' + str(part))
        if not base.is_dir():
            continue
        for dirpath, dirnames, filenames in os.walk(base, onerror=walk_error):
            for name in dirnames + filenames:
                full = Path(dirpath) / name
                if is_link(full):
                    raise ValueError('linked source entry: ' + str(full))
            for fn in sorted(filenames):
                full = Path(dirpath) / fn
                rel = full.relative_to(root).as_posix()
                rows.append({'path': rel, 'bytes': full.stat().st_size, 'sha256': digest(full)})
    return rows


def source_manifest(project):
    root = Path(project).resolve(strict=True)
    rows = manifest(root, ['src/main'])
    if not rows:
        raise ValueError('src/main is missing or empty')
    pom = root / 'pom.xml'
    if is_link(pom) or not pom.is_file():
        raise ValueError('pom.xml is missing or linked')
    rows.append({'path': 'pom.xml', 'bytes': pom.stat().st_size, 'sha256': digest(pom)})
    return sorted(rows, key=lambda r: r['path'])


if __name__ == '__main__':
    try:
        proj, out = sys.argv[1:3]
        rows = source_manifest(proj)
        # Preserve the legacy optional directory arguments; protected files are always included.
        if len(sys.argv) > 3:
            extra = manifest(proj, sys.argv[3:])
            rows = sorted({r['path']: r for r in rows + extra}.values(), key=lambda r: r['path'])
        if Path(out).resolve().is_relative_to(Path(proj).resolve()):
            raise ValueError('write the baseline outside the project')
        with open(out, 'w', encoding='utf-8') as f:
            json.dump({'root': str(Path(proj).resolve()), 'count': len(rows), 'files': rows}, f, ensure_ascii=False, indent=1)
        print(proj, 'files:', len(rows))
        print('Snapshot only: trust depends on the archive used to create it; keep this baseline unchanged.')
    except (OSError, ValueError) as exc:
        print('ERROR:', exc, file=sys.stderr)
        sys.exit(1)
