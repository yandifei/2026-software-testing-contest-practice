"""Detect member-name conflicts before splicing one JUnit file into another.

Both inputs are Java source files. The script extracts the members declared
directly in the outermost type body (methods, fields, nested types) and reports
names that appear in both files, plus the two unique-name counts.

Usage:
    python -B scripts/member_conflicts.py <main.java> <fragment.java> [--json out.json]

Exit codes: 0 = no conflict, 1 = conflicts found, 2 = usage/parse failure.

Why it exists: splicing a fragment whose member names collide with the target
does not necessarily fail the build. A same-signature method silently overrides
the target's copy, and a same-name nested class silently shadows it. Scan first.
"""
import argparse
import io
import json
import re
import sys

MODIFIERS = {
    'public', 'protected', 'private', 'static', 'final', 'abstract', 'sealed',
    'non-sealed', 'transient', 'volatile', 'synchronized', 'native', 'default',
    'strictfp',
}
TYPE_KEYWORDS = ('class', 'interface', 'enum', 'record')

IDENT = re.compile(r'[A-Za-z_$][A-Za-z0-9_$]*')


def strip_noise(text):
    """Remove comments, string/char literals and text blocks, keeping newlines."""
    out = []
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        two = text[i:i + 2]
        three = text[i:i + 3]
        if three == '"""':
            j = text.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append('\n' * text.count('\n', i, j))
            i = j
        elif two == '//':
            j = text.find('\n', i)
            j = n if j < 0 else j
            i = j
        elif two == '/*':
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append('\n' * text.count('\n', i, j))
            i = j
        elif ch == '"':
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == '"':
                    j += 1
                    break
                j += 1
            out.append('""')
            i = j
        elif ch == "'":
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == "'":
                    j += 1
                    break
                j += 1
            out.append("''")
            i = j
        else:
            out.append(ch)
            i += 1
    return ''.join(out)


def outermost_body(text):
    """Return (body_start, body_end) offsets of the first top-level type body."""
    depth = 0
    type_start = None
    for i, ch in enumerate(text):
        if ch == '{':
            if depth == 0 and type_start is None:
                return i, match_body_end(text, i)
            depth += 1
        elif ch == '}':
            depth = max(0, depth - 1)
    raise ValueError('no top-level type body found')


def match_body_end(text, open_idx):
    depth = 0
    for i in range(open_idx, len(text)):
        if text[i] == '{':
            depth += 1
        elif text[i] == '}':
            depth -= 1
            if depth == 0:
                return i
    raise ValueError('unbalanced type body')


def split_members(body):
    """Split a class body into depth-1 declaration segments."""
    segments, buf, depth = [], [], 0
    for ch in body:
        if ch == '{':
            depth += 1
            buf.append(ch)
            if depth > 1:
                continue
        elif ch == '}':
            depth -= 1
            buf.append(ch)
            if depth == 0:
                segments.append(''.join(buf))
                buf = []
            continue
        elif ch == ';' and depth == 0:
            segments.append(''.join(buf))
            buf = []
            continue
        buf.append(ch)
    if ''.join(buf).strip():
        segments.append(''.join(buf))
    return [s.strip() for s in segments if s.strip()]


def member_names(segment):
    """Names declared by one depth-1 segment (methods, fields, nested types)."""
    text = re.sub(r'@[\w.]+(\([^)]*\))?', ' ', segment)
    lowered = re.sub(r'[{}]', ' ', text)
    names = []
    for kw in TYPE_KEYWORDS:
        for m in re.finditer(r'\b' + kw + r'\s+([A-Za-z_$][\w$]*)', lowered):
            names.append(m.group(1))
    if names:
        return names
    paren_at = lowered.find('(')
    equals_at = lowered.find('=')
    if paren_at != -1 and (equals_at == -1 or paren_at < equals_at):
        head = lowered[:paren_at]
        idents = [w for w in IDENT.findall(head) if w not in MODIFIERS]
        return idents[-1:] if idents else []
    head = lowered.split('=', 1)[0]
    head = re.sub(r'\b(static|final|transient|volatile)\b', ' ', head)
    idents = [w for w in IDENT.findall(head) if w not in MODIFIERS]
    return idents[-1:] if len(idents) >= 2 else []


def collect(path):
    text = strip_noise(io.open(path, encoding='utf-8').read())
    start, end = outermost_body(text)
    found = []
    for segment in split_members(text[start + 1:end]):
        for name in member_names(segment):
            found.append(name)
    return found


def main(argv=None):
    parser = argparse.ArgumentParser(description='Compare depth-1 member names of two Java files.')
    parser.add_argument('main_file')
    parser.add_argument('fragment_file')
    parser.add_argument('--json', dest='json_out')
    args = parser.parse_args(argv)

    try:
        main_names = collect(args.main_file)
        frag_names = collect(args.fragment_file)
    except (OSError, ValueError) as error:
        print(f'PARSE_ERROR {error}')
        return 2

    main_set, frag_set = set(main_names), set(frag_names)
    conflicts = sorted(main_set & frag_set)
    report = {
        'main_members': len(main_set),
        'fragment_members': len(frag_set),
        'conflicts': conflicts,
        'main_only': sorted(main_set - frag_set),
        'fragment_only': sorted(frag_set - main_set),
    }
    for name in conflicts:
        print(f'CONFLICT {name}')
    print(f'MAIN_MEMBERS {len(main_set)} FRAGMENT_MEMBERS {len(frag_set)} CONFLICTS {len(conflicts)}')
    if args.json_out:
        with io.open(args.json_out, 'w', encoding='utf-8') as handle:
            json.dump(report, handle, ensure_ascii=False, indent=2)
    return 1 if conflicts else 0


if __name__ == '__main__':
    sys.exit(main())
