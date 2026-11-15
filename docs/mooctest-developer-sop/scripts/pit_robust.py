"""Inspect PIT XML. Fragment recovery is diagnostic only, never a final score."""
import argparse
import collections
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

MUT = re.compile(r"<mutation\b[^>]*>.*?</mutation>", re.S)
STATUSES = {'KILLED', 'SURVIVED', 'TIMED_OUT', 'NON_VIABLE', 'MEMORY_ERROR',
            'NOT_STARTED', 'STARTED', 'RUN_ERROR', 'NO_COVERAGE'}


def mutation_row(element):
    def field(name):
        nodes = element.findall(name)
        if len(nodes) != 1 or not (nodes[0].text or '').strip():
            raise ValueError('missing/duplicate/empty mutation field: ' + name)
        return nodes[0].text.strip()

    status = element.get('status')
    if status not in STATUSES:
        raise ValueError('unknown mutation status: ' + str(status))
    indexes = tuple(n.text for n in element.findall('.//index'))
    blocks = tuple(n.text for n in element.findall('.//block'))
    if not indexes or any(v is None or not v.isdigit() for v in indexes + blocks):
        raise ValueError('missing/invalid mutation indexes or blocks')
    mutator = field('mutator')
    line = field('lineNumber')
    if not line.isdigit():
        raise ValueError('invalid lineNumber: ' + line)
    return {
        'status': status, 'class': field('mutatedClass'),
        'method': field('mutatedMethod'), 'signature': field('methodDescription'),
        'line': line, 'mutator': mutator.rsplit('.', 1)[-1].replace('Mutator', ''),
        'desc': field('description'), 'index': ','.join(indexes),
        'block': ','.join(blocks) or '?', 'full_mutator': mutator,
    }


def parse(path):
    """Parse one complete XML document, rejecting ambiguous/unfinished records."""
    data = Path(path).read_text(encoding='utf-8')
    if re.search(r'<!\s*(?:DOCTYPE|ENTITY)\b', data, re.I):
        raise ValueError('DTD/entity declarations are not supported')
    root = ET.fromstring(data)
    if root.tag != 'mutations' or (root.text or '').strip():
        raise ValueError('expected a mutations root containing mutation records')
    rows, seen = [], set()
    for element in root:
        if element.tag != 'mutation' or (element.tail or '').strip():
            raise ValueError('unexpected content inside mutations')
        row = mutation_row(element)
        if row['status'] in {'NOT_STARTED', 'STARTED'}:
            raise ValueError('unfinished mutation status: ' + row['status'])
        # Include descriptors and full mutator names to distinguish overloads/operators.
        key = (row['class'], row['method'], row['signature'], row['full_mutator'], row['index'])
        if key in seen:
            raise ValueError('duplicate mutation identity; reports must not be concatenated/deduped')
        seen.add(key)
        rows.append(row)
    if not rows:
        raise ValueError('empty mutation report; no final denominator is available')
    return rows


def diagnostic_rows(path):
    data = Path(path).read_text(encoding='utf-8', errors='replace')
    rows = []
    for fragment in MUT.findall(data):
        try:
            rows.append(mutation_row(ET.fromstring(fragment)))
        except (ET.ParseError, ValueError):
            continue
    return rows


def display(rows, mode, diagnostic=False):
    st = collections.Counter(r['status'] for r in rows)
    print(f'{"recovered fragments (duplicates retained)" if diagnostic else "parsed mutations (no deduplication)"}: {len(rows)}')
    print(' '.join(f'{k}={v}' for k, v in sorted(st.items())))
    if not diagnostic:
        print(f"KILLED = {st.get('KILLED', 0)}/{len(rows)} = {st.get('KILLED', 0)/len(rows)*100:.2f}%")
        print('XML structure checked only; confirm build completion, source/report version and scoring rules separately.')
    if mode == 'classes':
        agg = collections.defaultdict(collections.Counter)
        for r in rows:
            if r['status'] in ('SURVIVED', 'NO_COVERAGE'):
                agg[r['class']][r['status']] += 1
        for cls, cc in sorted(agg.items(), key=lambda kv: -(kv[1]['SURVIVED'] + kv[1]['NO_COVERAGE']))[:40]:
            print(f"  {cls:34s} SURVIVED={cc['SURVIVED']:3d} NO_COVERAGE={cc['NO_COVERAGE']:3d}")
    elif mode == 'list':
        for r in rows:
            if r['status'] in ('SURVIVED', 'NO_COVERAGE'):
                print(f"{r['status']:12s} {r['class']}.{r['method']}{r['signature']} L{r['line']} idx={r['index']} {r['mutator']} :: {r['desc'][:90]}")
    elif mode == 'mutators':
        c = collections.Counter((r['mutator'], r['status']) for r in rows if r['status'] in ('SURVIVED', 'NO_COVERAGE'))
        for (mut, status), n in c.most_common(30):
            print(f'  {n:4d} {status:12s} {mut}')


def main(path, mode='summary', diagnostic=False):
    if diagnostic:
        print('DIAGNOSTIC ONLY: incomplete/unverified report; NOT a final result. No score/denominator is certified.')
        try:
            parse(path)
        except (OSError, UnicodeError, ET.ParseError, ValueError) as exc:
            print('strict validation failed:', exc)
        display(diagnostic_rows(path), mode, diagnostic=True)
        return 2
    display(parse(path), mode)
    return 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('path')
    parser.add_argument('mode', nargs='?', default='summary', choices=('summary', 'classes', 'list', 'mutators'))
    parser.add_argument('--diagnostic', action='store_true', help='recover closed fragments; always exit 2, never output a final rate')
    args = parser.parse_args()
    try:
        sys.exit(main(args.path, args.mode, args.diagnostic))
    except (OSError, UnicodeError, ET.ParseError, ValueError) as exc:
        print('ERROR: report rejected; no final result:', exc, file=sys.stderr)
        sys.exit(1)
