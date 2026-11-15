"""Compare source/POM with a trusted baseline and check the single UTF-8 test file."""
import argparse
import json
from pathlib import Path, PurePosixPath
import re
import sys
from baseline import manifest, source_manifest


def test_file(project, test_path=None):
    root = Path(project).resolve(strict=True)
    rows = manifest(root, ['src/test'])
    java = [r['path'] for r in rows if r['path'].lower().endswith('.java')]
    if len(java) != 1:
        raise ValueError('expected exactly one Java file under src/test; found: ' + repr(java))
    actual = java[0]
    if test_path:
        if actual != test_path:
            raise ValueError('test path differs from required path: ' + test_path)
    elif not PurePosixPath(actual).name.endswith('Test.java'):
        raise ValueError('test filename must end in Test.java; use --test-path for a task-specific filename')
    (root / actual).read_bytes().decode('utf-8')
    return actual


def load_baseline(path):
    with open(path, encoding='utf-8-sig') as f:
        base = json.load(f)
    records = base.get('files') if isinstance(base, dict) else None
    if not isinstance(records, list) or not records:
        raise ValueError('baseline files must be a nonempty list')
    if 'count' in base and base['count'] != len(records):
        raise ValueError('baseline count does not match files')
    want = {}
    for rec in records:
        if not isinstance(rec, dict):
            raise ValueError('invalid baseline record')
        rel = rec.get('path')
        if (not isinstance(rel, str) or not rel or '\\' in rel or ':' in rel
                or rel.startswith('/') or any(p in ('', '.', '..') for p in rel.split('/'))):
            raise ValueError('unsafe baseline path: ' + str(rel))
        if rel in want:
            raise ValueError('duplicate baseline path: ' + rel)
        if not isinstance(rec.get('sha256'), str) or not re.fullmatch('[0-9a-fA-F]{64}', rec['sha256']):
            raise ValueError('invalid SHA-256: ' + rel)
        if type(rec.get('bytes')) is not int or rec['bytes'] < 0:
            raise ValueError('invalid byte count: ' + rel)
        want[rel] = rec
    if 'pom.xml' not in want or not any(p.startswith('src/main/') for p in want):
        raise ValueError('baseline must contain pom.xml and src/main files')
    return want


def check(project, baseline_json, test_path=None):
    problems = []
    try:
        want = load_baseline(baseline_json)
        actual = {r['path']: r for r in source_manifest(project)}
        protected = {p: r for p, r in want.items() if p == 'pom.xml' or p.startswith('src/main/')}
        for rel, rec in sorted(protected.items()):
            if rel not in actual:
                problems.append(('MISSING', rel))
            elif (actual[rel]['sha256'] != rec['sha256'].lower() or actual[rel]['bytes'] != rec['bytes']):
                problems.append(('CHANGED', rel))
        for rel in sorted(actual.keys() - protected.keys()):
            problems.append(('ADDED', rel))
        test_file(project, test_path)
    except (OSError, UnicodeError, ValueError) as exc:
        problems.append(('INVALID', str(exc)))
    return problems


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('project')
    parser.add_argument('baseline')
    parser.add_argument('--test-path', help='exact project-relative path required by the task')
    args = parser.parse_args()
    probs = check(args.project, args.baseline, args.test_path)
    if not probs:
        print('OK: src/main and pom.xml match baseline; exactly one UTF-8 test Java file.')
    else:
        for kind, rel in probs:
            print(f'{kind}: {rel}')
    sys.exit(1 if probs else 0)
