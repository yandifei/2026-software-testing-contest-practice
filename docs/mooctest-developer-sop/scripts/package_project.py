"""Package one project and verify the exact source snapshot and delivery Java file.

Report entry presence is checked, not freshness, test success, or platform scores.
"""
import argparse
import hashlib
import os
from pathlib import Path
import sys
import tempfile
import time
import zipfile
from baseline import digest, is_link, walk_error
from verify_baseline import check, test_file

SKIP_DIRS = {'__pycache__', '.idea', '.vscode', '.git', '.settings'}
SKIP_SUFFIX = ('.bak', '.tmp', '.orig', '.iml', '.log')
SKIP_NAMES = {'.DS_Store', 'Thumbs.db', 'out.txt', 'err.txt', '.classpath', '.project'}


def should_skip(rel, name):
    return name in SKIP_NAMES or name.endswith(SKIP_SUFFIX) or any(p in SKIP_DIRS for p in rel.split('/'))


def outside_project(project, path, label):
    root = Path(project).resolve(strict=True)
    target = Path(path).resolve()
    if target.is_relative_to(root):
        raise ValueError(label + ' must be outside the project (prevents self-inclusion)')
    return target


def inventory(project):
    root = Path(project).resolve(strict=True)
    entries = {}
    for dirpath, dirnames, filenames in os.walk(root, onerror=walk_error):
        dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS)
        for name in dirnames + filenames:
            full = Path(dirpath) / name
            rel = full.relative_to(root).as_posix()
            if should_skip(rel, name):
                continue
            if is_link(full):
                raise ValueError('linked project entry is not supported: ' + rel)
            if full.is_file():
                entries[rel] = (full.stat().st_size, digest(full))
    return entries


def build(project, out_zip):
    root = Path(project).resolve(strict=True)
    destination = outside_project(root, out_zip, 'ZIP output')
    entries = inventory(root)
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for rel in sorted(entries):
            z.write(root / rel, rel)
    return entries


def zip_digest(z, name):
    h = hashlib.sha256()
    with z.open(name) as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def verify(project, out_zip, java_file, max_bytes, baseline_json, test_path=None, expected=None):
    root = Path(project).resolve(strict=True)
    archive = outside_project(root, out_zip, 'ZIP output')
    standalone = outside_project(root, java_file, 'standalone Java')
    if max_bytes <= 0:
        raise ValueError('--max-bytes must be positive')
    report = {'count': 0, 'top': [], 'errors': [], 'bytes': archive.stat().st_size}
    errors = report['errors']
    if report['bytes'] > max_bytes:
        errors.append('ZIP exceeds --max-bytes: %d > %d' % (report['bytes'], max_bytes))
    if standalone.suffix != '.java' or not standalone.is_file():
        raise ValueError('--java-file must name the independent delivered .java file')
    standalone_bytes = standalone.read_bytes()
    standalone_bytes.decode('utf-8')
    inner = test_file(root, test_path)
    baseline_problems = check(root, baseline_json, test_path)
    errors.extend('baseline %s: %s' % p for p in baseline_problems)
    current = inventory(root)
    if expected is None:
        expected = current
    elif expected != current:
        errors.append('project file inventory/bytes changed while packaging')
    with zipfile.ZipFile(archive) as z:
        infos = z.infolist()
        names = [i.filename for i in infos]
        report['count'] = len(names)
        report['top'] = sorted({n.split('/')[0] for n in names})
        if len(set(names)) != len(names):
            errors.append('duplicate ZIP entries')
        bad = [n for n in names if n.startswith('/') or '\\' in n or ':' in n or any(p in ('', '.', '..') for p in n.split('/'))]
        if bad:
            errors.append('unsafe or unexpected ZIP entries: ' + repr(bad[:5]))
        if set(names) != set(expected):
            errors.append('ZIP file list differs from project inventory (missing/extra entries)')
        for needed in ('pom.xml', inner, 'target/pit-reports/index.html', 'target/site/jacoco/index.html'):
            if needed not in names:
                errors.append('missing ' + needed)
        if not any(n.startswith('src/main/') and n.endswith('.java') for n in names):
            errors.append('missing src/main Java sources')
        # Reading every member checks decompression and CRC, including non-test content.
        for info in infos:
            checksum = zip_digest(z, info.filename)
            if info.filename in expected and (info.file_size, checksum) != expected[info.filename]:
                errors.append('ZIP bytes differ from project snapshot: ' + info.filename)
        if inner in names and z.read(inner) != standalone_bytes:
            errors.append('ZIP test Java bytes differ from independent --java-file')
    report['MiB'] = round(report['bytes'] / 1048576, 2)
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('project')
    parser.add_argument('out_zip')
    parser.add_argument('--java-file', required=True, help='independent delivered Java file outside the project')
    parser.add_argument('--max-bytes', required=True, type=int, help='current ZIP upload size limit in bytes')
    parser.add_argument('--baseline', required=True, help='unchanged source/POM snapshot made from the trusted task archive')
    parser.add_argument('--test-path', help='exact project-relative test path required by this task')
    args = parser.parse_args()
    started = time.time()
    temporary = None
    try:
        destination = outside_project(args.project, args.out_zip, 'ZIP output')
        standalone = outside_project(args.project, args.java_file, 'standalone Java')
        if destination == standalone or destination == Path(args.baseline).resolve():
            raise ValueError('ZIP output must not overwrite Java delivery or baseline')
        if args.max_bytes <= 0:
            raise ValueError('--max-bytes must be positive')
        problems = check(args.project, args.baseline, args.test_path)
        if problems:
            raise ValueError('baseline/test validation failed: ' + repr(problems))
        # Validate the independent Java before doing the potentially expensive build.
        inner = test_file(args.project, args.test_path)
        if standalone.suffix != '.java' or standalone.read_bytes() != (Path(args.project) / inner).read_bytes():
            raise ValueError('independent --java-file must be a byte-identical .java copy of the test')
        handle, temporary = tempfile.mkstemp(prefix='mooctest-package-', suffix='.zip', dir=destination.parent)
        os.close(handle)
        expected = build(args.project, temporary)
        report = verify(args.project, temporary, standalone, args.max_bytes, args.baseline, args.test_path, expected)
        if report['errors']:
            raise ValueError('; '.join(report['errors']))
        os.replace(temporary, destination)
        temporary = None
        print('zip:', destination)
        print('entries:', report['count'], 'bytes:', report['bytes'], 'size MiB:', report['MiB'], 'top:', report['top'])
        print('elapsed %.1fs' % (time.time() - started))
        print('OK: ZIP CRC/file inventory/bytes, report entry presence, source baseline, single UTF-8 test, independent Java and size limit checked.')
        print('Not checked: report freshness, build/test success, class naming/main(), or platform scores.')
        return 0
    except (OSError, UnicodeError, ValueError, zipfile.BadZipFile, RuntimeError) as exc:
        print('ERROR:', exc, file=sys.stderr)
        return 1
    finally:
        if temporary is not None:
            Path(temporary).unlink(missing_ok=True)


if __name__ == '__main__':
    sys.exit(main())
