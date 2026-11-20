"""Replace an existing @Nested container's body inside the merged StudentTest.java
with the revised section file.

Use case (2026-11-15 provincial round, real incident): a probe/section executor
keeps refining expectations AFTER the integrator merged an earlier snapshot; the
merged suite then fails on the stale copy. Fix = re-extract the executor's latest
file and swap the container body in place. Initial merges should use
merge_engine.py; this tool only replaces an existing container.

Usage:
    python replace_section.py <main-StudentTest.java> <section-file.java> <ContainerClassName>

Steps:
  1. extract imports + class body from the section file (string/comment-aware
     brace matching, same state machine as merge_engine.py);
  2. locate `class <ContainerClassName> {` in the main file and match its braces;
  3. splice the new body inside the container, dedupe imports;
  4. ALWAYS recompile afterwards (`mvn -B test`) — a successful text splice is
     not a successful merge until the suite is green again.
"""
import re
import sys


def read(p):
    return open(p, encoding='utf-8').read()


def match_braces(src, start):
    """Return the index of the '}' matching the '{' at src[start], skipping
    string/char/comment/text-block content (state machine from merge_engine.py)."""
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
                    return i
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
    raise SystemExit(f'unbalanced braces, depth={depth}')


def extract_section(path):
    """Return (imports, body) of the first top-level class in the section file."""
    src = read(path)
    m = re.search(r'^[ \t]*(?:public\s+|final\s+|abstract\s+)*(?:class|record|enum)\s+\w+[^{]*\{', src, re.M)
    if not m:
        raise SystemExit(f'{path}: no class declaration found')
    imports = re.findall(r'^(?:import\s+(?:static\s+)?[\w\.\*]+;)\s*$', src, re.M)
    end = match_braces(src, m.end() - 1)
    return imports, src[m.end():end]


def main():
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    main_path, section_path, container = sys.argv[1], sys.argv[2], sys.argv[3]

    main = read(main_path)
    imports, body = extract_section(section_path)

    # locate the container class inside the main file
    m = re.search(r'^[ \t]*class\s+' + re.escape(container) + r'\s*\{', main, re.M)
    if not m:
        raise SystemExit(f'main file has no container class {container!r} — '
                         f'container mode needs merge_engine.py; flat mode has no container to replace, '
                         f'regenerate the file from the frozen main copy plus the frozen fragment instead')
    open_idx = m.end() - 1
    close_idx = match_braces(main, open_idx)

    # splice the revised body inside the container braces
    replaced = main[:open_idx + 1] + body + main[close_idx:]

    # dedupe imports: keep existing, append unseen section imports after the last import
    lines = replaced.split('\n')
    first = last = None
    for idx, ln in enumerate(lines):
        if re.match(r'^import\s', ln):
            if first is None:
                first = idx
            last = idx
    if first is None:
        raise SystemExit('main file has no imports block')
    existing = set(lines[first:last + 1])
    fresh = [imp for imp in imports if imp not in existing]
    lines = lines[:last + 1] + fresh + lines[last + 1:]

    out = '\n'.join(lines)
    with open(main_path, 'w', encoding='utf-8', newline='') as f:
        f.write(out)
    print(f'replaced {container!r} body from {section_path}: '
          f'+{len(body.splitlines())} lines, +{len(fresh)} imports')
    print('now re-run the suite (mvn -B test) — a text splice is not a merge until it is green')


if __name__ == '__main__':
    main()
