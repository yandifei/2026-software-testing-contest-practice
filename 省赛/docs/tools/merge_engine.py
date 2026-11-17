import sys, re

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

if __name__ == '__main__':
    main_path, engine_path = sys.argv[1], sys.argv[2]
    out = sys.argv[3] if len(sys.argv) > 3 else None
    nested = sys.argv[4] if len(sys.argv) > 4 else 'ExpressionEngineTests'
    merged = merge(main_path, engine_path, nested_name=nested, out_path=out)
    print('merged chars:', len(merged), '->', out)
