"""Splice a probe/fragment class body into a main test class as a @Nested container.

Usage:
    python -B scripts/merge_engine.py <main.java> <fragment.java> [out.java] [container-name]
    python -B scripts/merge_engine.py <main.java> <fragment.java> [out.java] --nested-name NAME --check-conflicts

Container mode only: the fragment body becomes `@org.junit.jupiter.api.Nested class NAME { ... }`.
Do NOT use this when the target form must be flat (see references/suite-porting.md section 4):
flat merging has no script here, splice the fragment body and recompile.

`--check-conflicts` runs member_conflicts.py first and aborts (exit 1) when member names collide,
because a same-signature method or same-name nested class does not necessarily fail the build.

Exit codes: 0 = merged, 1 = refused (conflict / no imports block / unparsable), 2 = usage error.
"""
import argparse
import io
import re
import sys

import member_conflicts


def read(p):
    return open(p, encoding='utf-8').read()

def find_class_body(src):
    """Locate the top-level class declaration and return (imports, body) using a
    string/comment-aware brace matcher."""
    m = re.search(r'^[ \t]*(?:public\s+|final\s+|abstract\s+)*(?:class|record|enum)\s+\w+[^{]*\{', src, re.M)
    if not m:
        raise SystemExit('no class declaration found')
    start = m.end() - 1
    i = start
    depth = 0
    n = len(src)
    state = 'code'
    while i < n:
        ch = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
        if state == 'code':
            if ch == '/' and nxt == '/':
                state = 'line_comment'
                i += 2
                continue
            if ch == '/' and nxt == '*':
                state = 'block_comment'
                i += 2
                continue
            if ch == '"':
                if src.startswith('"""', i):
                    state = 'text_block'
                    i += 3
                    continue
                state = 'string'
                i += 1
                continue
            if ch == "'":
                state = 'char'
                i += 1
                continue
            if ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    break
            i += 1
            continue
        if state == 'line_comment':
            if ch == '\n':
                state = 'code'
            i += 1
            continue
        if state == 'block_comment':
            if ch == '*' and nxt == '/':
                state = 'code'
                i += 2
                continue
            i += 1
            continue
        if state == 'string':
            if ch == '\\':
                i += 2
                continue
            if ch == '"':
                state = 'code'
            i += 1
            continue
        if state == 'char':
            if ch == '\\':
                i += 2
                continue
            if ch == "'":
                state = 'code'
            i += 1
            continue
        if state == 'text_block':
            if src.startswith('"""', i):
                state = 'code'
                i += 3
                continue
            i += 1
            continue
    if depth != 0:
        raise SystemExit(f'unbalanced braces, depth={depth} at {i}')
    imports = re.findall(r'^(?:import\s+(?:static\s+)?[\w\.\*]+;)\s*$', src, re.M)
    return imports, src[start + 1:i]

def merge(main_path, engine_path, nested_name='ExpressionEngineTests', out_path=None):
    main = read(main_path)
    eng = read(engine_path)
    eng_imports, eng_body = find_class_body(eng)
    main_imports = re.findall(r'^(?:import\s+(?:static\s+)?[\w\.\*]+;)\s*$', main, re.M)
    all_imports = list(dict.fromkeys(main_imports + eng_imports))
    lines = main.splitlines()
    first = last = None
    for idx, ln in enumerate(lines):
        if re.match(r'^import\s', ln):
            if first is None:
                first = idx
            last = idx
    if first is None:
        raise SystemExit('main file has no imports block')
    main = '\n'.join(lines[:first] + all_imports + lines[last + 1:]) + '\n'
    _imports, main_body = find_class_body(main)
    # rebuild main from its own body plus the new nested class, guaranteeing balance
    head_end = main.index(main_body)
    head = main[:head_end]
    nested = f"\n    // ===== merged from {nested_name} probe =====\n    @org.junit.jupiter.api.Nested\n    class {nested_name} {{\n{eng_body}\n    }}\n"
    merged = head + main_body.rstrip() + '\n' + nested + '}\n'
    if out_path:
        with open(out_path, 'w', encoding='utf-8') as f:
            f.write(merged)
    return merged

def main(argv=None):
    parser = argparse.ArgumentParser(
        description='Splice a fragment class body into a main test class as a @Nested container.',
        epilog='Container mode only; for flat merging splice the body yourself (see suite-porting.md section 4).')
    parser.add_argument('main_file')
    parser.add_argument('fragment_file')
    parser.add_argument('out', nargs='?', default=None, help='output path; omit to only report the size')
    parser.add_argument('nested_name_positional', nargs='?', default=None, help='container name (legacy positional form)')
    parser.add_argument('--nested-name', dest='nested_name', default=None)
    parser.add_argument('--check-conflicts', action='store_true',
                        help='refuse to merge when the two files declare the same member name')
    args = parser.parse_args(argv)

    if args.check_conflicts:
        try:
            conflicts = sorted(set(member_conflicts.collect(args.main_file)) &
                               set(member_conflicts.collect(args.fragment_file)))
        except (OSError, ValueError) as error:
            print(f'PARSE_ERROR {error}')
            return 1
        if conflicts:
            print('REFUSED member name conflicts: ' + ', '.join(conflicts))
            print('rename or drop one side first; a same-name member may silently shadow, not fail the build')
            return 1

    nested = args.nested_name or args.nested_name_positional or 'ExpressionEngineTests'
    merged = merge(args.main_file, args.fragment_file, nested_name=nested, out_path=args.out)
    print(f'merged chars: {len(merged)} -> {args.out}')
    print(f'note: container mode (@Nested {nested}); flat merging is manual, see suite-porting.md section 4')
    return 0


if __name__ == '__main__':
    sys.exit(main())
