#!/usr/bin/env python3
"""Merge/replace a @Nested section container inside StudentTest.java.

Usage: python merge-section.py <student-test> <section-file> <SectionClass> <ContainerClass> <banner-title>
Replaces the container marked by `// <banner-title>` banner if present, else appends.
"""
import re
import sys


def extract_body(path, class_name):
    src = open(path, encoding='utf-8').read()
    imports = re.findall(r'^import .*?;$', src, re.M)
    idx = src.index('class %s' % class_name)
    open_brace = src.index('{', idx)
    body = src[open_brace + 1:].rstrip()
    assert body.endswith('}'), f'{path}: outer brace not closed?'
    return imports, body[:-1].rstrip('\n')


def brace_match_end(text, open_idx):
    depth = 0
    i = open_idx
    while i < len(text):
        c = text[i]
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError('unbalanced braces')


def main():
    student_path, section_path, section_class, container_class, banner = sys.argv[1:6]
    student = open(student_path, encoding='utf-8').read()
    imports, body = extract_body(section_path, section_class)

    lines = student.split('\n')
    last_import = max(i for i, l in enumerate(lines) if l.startswith('import '))
    seen = set(l for l in lines[:last_import + 1])
    for imp in reversed(imports):
        if imp not in seen:
            lines.insert(last_import + 1, imp)
    student = '\n'.join(lines)

    container = ('    // ' + '-' * 66 + '\n'
                 '    // ' + banner + '\n'
                 '    // ' + '-' * 66 + '\n'
                 '    @Nested\n    class %s {' % container_class) + body + '}\n'

    marker = '    // ' + banner + '\n'
    pos = student.find(marker)
    if pos >= 0:
        open_idx = student.index('{', student.index('class %s' % container_class, pos))
        end_idx = brace_match_end(student, open_idx)
        # consume the trailing newline after the closing brace
        end_idx = end_idx + 1
        while end_idx < len(student) and student[end_idx] == '\n':
            end_idx += 1
        student = student[:pos] + container + '\n' + student[end_idx:]
        action = 'replaced'
    else:
        stripped = student.rstrip()
        assert stripped.endswith('}')
        student = stripped[:-1].rstrip('\n') + '\n\n' + container + '}\n'
        action = 'appended'

    open(student_path, 'w', encoding='utf-8').write(student)
    print('%s %s; total lines %d' % (action, container_class, student.count('\n') + 1))


if __name__ == '__main__':
    main()
