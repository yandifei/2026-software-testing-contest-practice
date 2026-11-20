"""Regression checks using disposable fixtures only; does not run Java or Maven.

Run: python -B scripts/test_delivery_tools.py
"""
import contextlib
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import warnings
import zipfile

import package_project

SCRIPTS = Path(__file__).resolve().parent
ROW = '<mutation detected="true" status="KILLED"><mutatedClass>net.mooctest.A</mutatedClass><mutatedMethod>f</mutatedMethod><methodDescription>()I</methodDescription><lineNumber>1</lineNumber><mutator>x.ReturnMutator</mutator><indexes><index>1</index></indexes><blocks><block>0</block></blocks><description>return &lt; 2 &amp; 3</description></mutation>'


class DeliveryChecks(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='mooctest-tools-test-')
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.project = self.root / 'project'
        self.test_rel = 'src/test/java/net/mooctest/StudentTest.java'
        self.put('pom.xml', '<project/>')
        self.put('src/main/java/net/mooctest/A.java', 'class A {}')
        self.put(self.test_rel, 'class StudentTest {}')
        self.put('target/pit-reports/index.html', 'pit fixture')
        self.put('target/site/jacoco/index.html', 'jacoco fixture')
        self.put('target/classes/A.class', b'compiled fixture')
        self.java = self.root / 'StudentTest.java'
        self.java.write_bytes((self.project / self.test_rel).read_bytes())
        self.base = self.root / 'baseline.json'
        self.zip = self.root / 'delivery.zip'
        self.run_cli('baseline.py', self.project, self.base)

    def put(self, relative, data):
        path = self.project / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data if isinstance(data, bytes) else data.encode('utf-8'))
        return path

    def run_cli(self, script, *args, expect=0):
        env = dict(os.environ, PYTHONDONTWRITEBYTECODE='1', PYTHONIOENCODING='utf-8')
        result = subprocess.run([sys.executable, '-B', str(SCRIPTS / script), *map(str, args)], capture_output=True, text=True, encoding='utf-8', env=env)
        self.assertEqual(result.returncode, expect, result.stdout + result.stderr)
        return result.stdout + result.stderr

    def pack(self, *extra, expect=0):
        return self.run_cli('package_project.py', self.project, self.zip, '--java-file', self.java, '--max-bytes', 1000000, '--baseline', self.base, *extra, expect=expect)

    def pit(self, data, mode='summary', diagnostic=False, expect=0):
        path = self.root / 'mutations.xml'
        path.write_bytes(data if isinstance(data, bytes) else data.encode('utf-8'))
        return self.run_cli('pit_robust.py', path, mode, *(['--diagnostic'] if diagnostic else []), expect=expect)

    def test_pit_modes_and_xml_entities(self):
        survived = ROW.replace('KILLED', 'SURVIVED').replace('<index>1', '<index>2')
        for mode in ('summary', 'classes', 'list', 'mutators'):
            with self.subTest(mode=mode):
                out = self.pit('<mutations>' + ROW + survived + '</mutations>', mode)
                self.assertIn('KILLED = 1/2 = 50.00%', out)
                if mode == 'list':
                    self.assertIn('return < 2 & 3', out)
        self.pit('<mutations>' + ROW.replace('"', "'") + '</mutations>')

    def test_pit_rejects_bad_reports(self):
        complete = '<mutations>' + ROW + '</mutations>'
        cases = {
            'truncated': '<mutations>' + ROW,
            'concatenated': complete + complete,
            'duplicate': '<mutations>' + ROW + ROW + '</mutations>',
            'conflicting status': '<mutations>' + ROW + ROW.replace('KILLED', 'SURVIVED') + '</mutations>',
            'empty': '<mutations/>',
            'unfinished': complete.replace('KILLED', 'STARTED'),
            'missing field': complete.replace('<methodDescription>()I</methodDescription>', ''),
            'DTD': '<!DOCTYPE mutations [<!ENTITY x "foo">]>' + complete,
            'invalid UTF8': complete.encode('utf-8') + b'\xff',
        }
        for name, data in cases.items():
            with self.subTest(name=name):
                self.assertNotIn('KILLED =', self.pit(data, expect=1))

    def test_pit_diagnostic_never_certifies_score(self):
        data = '<mutations>' + ROW + ROW
        out = self.pit(data, diagnostic=True, expect=2)
        self.assertIn('NOT a final result', out)
        self.assertIn('duplicates retained): 2', out)
        self.assertNotIn('KILLED =', out)
        self.assertNotIn('%', out)

    def test_overloaded_methods_are_distinct(self):
        second = ROW.replace('()I', '(I)I')
        self.assertIn('2/2', self.pit('<mutations>' + ROW + second + '</mutations>'))

    def test_baseline_normal_and_changed_same_size(self):
        self.run_cli('verify_baseline.py', self.project, self.base)
        self.put('src/main/java/net/mooctest/A.java', 'class B {}')
        self.assertIn('CHANGED', self.run_cli('verify_baseline.py', self.project, self.base, expect=1))

    def test_baseline_added_missing_sources_and_missing_pom(self):
        source = self.project / 'src/main/java/net/mooctest/A.java'
        for case in ('added', 'missing', 'pom'):
            with self.subTest(case=case):
                if case == 'added':
                    extra = self.put('src/main/java/net/mooctest/B.java', 'class B {}')
                elif case == 'missing':
                    source.unlink()
                else:
                    (self.project / 'pom.xml').unlink()
                self.run_cli('verify_baseline.py', self.project, self.base, expect=1)
                if case == 'added':
                    extra.unlink()
                elif case == 'missing':
                    self.put('src/main/java/net/mooctest/A.java', 'class A {}')

    def test_baseline_empty_duplicate_unsafe_and_malformed(self):
        original = json.loads(self.base.read_text(encoding='utf-8'))
        cases = [ {'files': []}, {'files': [original['files'][0]]},
            {'files': original['files'] + [original['files'][0]]},
            {'files': [{**original['files'][0], 'path': '../outside'}]},
            {'files': [{**original['files'][0], 'sha256': 'invalid'}]},
            {'files': [None]}, {'files': 'bad'}]
        for data in cases:
            with self.subTest(data=data):
                self.base.write_text(json.dumps(data), encoding='utf-8')
                self.run_cli('verify_baseline.py', self.project, self.base, expect=1)
        self.base.write_text('{broken', encoding='utf-8')
        self.run_cli('verify_baseline.py', self.project, self.base, expect=1)

    def test_single_test_exists_across_entire_tree_and_utf8(self):
        test = self.project / self.test_rel
        test.unlink()
        self.run_cli('verify_baseline.py', self.project, self.base, expect=1)
        self.put(self.test_rel, 'class StudentTest {}')
        extra = self.put('src/test/java/other/HiddenTest.java', 'class HiddenTest {}')
        self.run_cli('verify_baseline.py', self.project, self.base, expect=1)
        extra.unlink()
        self.put(self.test_rel, b'\xff')
        self.run_cli('verify_baseline.py', self.project, self.base, expect=1)

    def test_task_specific_test_path(self):
        (self.project / self.test_rel).unlink()
        other = 'src/test/java/demo/Assessment.java'
        self.put(other, 'class Assessment {}')
        self.run_cli('verify_baseline.py', self.project, self.base, expect=1)
        self.run_cli('verify_baseline.py', self.project, self.base, '--test-path', other)
        self.java.write_bytes((self.project / other).read_bytes())
        self.pack('--test-path', other)

    def test_package_valid_and_required_flags(self):
        self.put('run.log', 'excluded')
        self.put('.idea/project.xml', 'excluded')
        self.pack()
        with zipfile.ZipFile(self.zip) as z:
            self.assertNotIn('run.log', z.namelist())
            self.assertIn('target/classes/A.class', z.namelist())
            self.assertEqual(z.read(self.test_rel), self.java.read_bytes())
        self.run_cli('package_project.py', self.project, self.zip, expect=2)

    def test_package_same_size_wrong_java_and_limit_preserve_existing(self):
        self.zip.write_bytes(b'previous good artifact')
        self.java.write_bytes(b'class ChangedTest {}')
        self.assertEqual(len(self.java.read_bytes()), len((self.project / self.test_rel).read_bytes()))
        self.pack(expect=1)
        self.assertEqual(self.zip.read_bytes(), b'previous good artifact')
        self.java.write_bytes((self.project / self.test_rel).read_bytes())
        self.pack('--max-bytes', 1, expect=1)
        self.assertEqual(self.zip.read_bytes(), b'previous good artifact')
        self.assertFalse(list(self.root.glob('mooctest-package-*.zip')))

    def test_package_inside_project_or_missing_report_rejected(self):
        self.zip = self.project / 'nested' / 'delivery.zip'
        self.pack(expect=1)
        self.zip = self.root / 'delivery.zip'
        self.java = self.project / self.test_rel
        self.pack(expect=1)
        self.java = self.root / 'StudentTest.java'
        (self.project / 'target/pit-reports/index.html').unlink()
        self.pack(expect=1)
        self.assertFalse(self.zip.exists())

    def test_archive_missing_extra_duplicate_corrupt_and_same_size_change(self):
        self.pack()
        with zipfile.ZipFile(self.zip) as z:
            original = [(i.filename, z.read(i.filename)) for i in z.infolist()]
        main = 'src/main/java/net/mooctest/A.java'
        cases = {
            'missing': [(n,b) for n,b in original if n != main],
            'extra': original + [('outside.txt', b'extra')],
            'traversal': original + [('../outside.txt', b'extra')],
            'duplicate': original + [original[0]],
            'same-size-change': [(n, b'class B {}' if n == main else b) for n,b in original],
        }
        for name, entries in cases.items():
            with self.subTest(name=name):
                with warnings.catch_warnings():
                    warnings.simplefilter('ignore', UserWarning)
                    with zipfile.ZipFile(self.zip, 'w', compression=zipfile.ZIP_STORED) as z:
                        for n,b in entries:
                            z.writestr(n,b)
                report = package_project.verify(self.project, self.zip, self.java, 1000000, self.base)
                self.assertTrue(report['errors'], name)
        self.pack()
        # Truncation must fail ZIP parsing; byte corruption must fail CRC/decompression.
        data = self.zip.read_bytes()
        self.zip.write_bytes(data[:-30])
        with self.assertRaises(zipfile.BadZipFile):
            package_project.verify(self.project, self.zip, self.java, 1000000, self.base)
        with zipfile.ZipFile(self.zip, 'w', compression=zipfile.ZIP_STORED) as z:
            for n,b in original:
                z.writestr(n,b)
        with zipfile.ZipFile(self.zip) as z:
            info = z.getinfo(main)
        raw = bytearray(self.zip.read_bytes())
        offset = info.header_offset + 30 + len(info.filename.encode('utf-8')) + len(info.extra)
        raw[offset] ^= 1
        self.zip.write_bytes(raw)
        with self.assertRaises(zipfile.BadZipFile):
            package_project.verify(self.project, self.zip, self.java, 1000000, self.base)

    def test_jacoco_reports_named_counters_without_statement_claim(self):
        xml = self.root / 'jacoco.xml'
        xml.write_text('<report><counter type="METHOD" missed="1" covered="3"/><counter type="LINE" missed="2" covered="8"/></report>', encoding='utf-8')
        out = self.run_cli('jacoco_report.py', xml)
        self.assertIn('METHOD: 3/4 = 75.00%', out)
        self.assertIn('LINE: 8/10 = 80.00%', out)
        self.assertNotIn('STATEMENT', out.upper())

    def test_member_conflicts_flags_only_real_collisions(self):
        main = self.root / 'main.java'
        frag = self.root / 'frag.java'
        # Braces inside a string and a comment must not shift the member scan.
        main.write_text('package net.mooctest;\nclass StudentTest {\n'
                        '    static final java.time.Instant T = java.time.Instant.parse("2026-01-01T00:00:00Z");\n'
                        '    private static Helper make() { return null; }\n'
                        '    static class Helper { }\n'
                        '    @Test void alpha() { String s = "}{"; /* } */ }\n'
                        '    @Test void beta() { }\n}\n', encoding='utf-8')
        frag.write_text('package net.mooctest;\nclass Other {\n'
                        '    static final java.time.Instant T = null;\n'
                        '    static class Helper { }\n'
                        '    @Test void alpha() { }\n'
                        '    @Test void gamma() { }\n}\n', encoding='utf-8')
        out = self.run_cli('member_conflicts.py', main, frag, expect=1)
        self.assertIn('CONFLICT T', out)
        self.assertIn('CONFLICT Helper', out)
        self.assertIn('CONFLICT alpha', out)
        self.assertNotIn('CONFLICT make', out)
        self.assertNotIn('CONFLICT beta', out)

        report = self.root / 'conflicts.json'
        self.run_cli('member_conflicts.py', frag, frag, '--json', report, expect=1)
        self.assertEqual(['Helper', 'T', 'alpha', 'gamma'],
                         json.loads(report.read_text(encoding='utf-8'))['conflicts'])

        same = self.root / 'clean.java'
        same.write_text('class Clean { @Test void onlyHere() { } }\n', encoding='utf-8')
        self.assertIn('CONFLICTS 0', self.run_cli('member_conflicts.py', main, same, expect=0))

    def test_merge_engine_wraps_container_and_refuses_conflicts(self):
        main = self.root / 'm_main.java'
        frag = self.root / 'm_frag.java'
        main.write_text('package net.mooctest;\nimport java.util.List;\nclass StudentTest {\n'
                        '    @Test void keep() { }\n}\n', encoding='utf-8')
        frag.write_text('package net.mooctest;\nimport java.util.Map;\nclass Fragment {\n'
                        '    @Test void added() { }\n}\n', encoding='utf-8')
        out = self.root / 'm_merged.java'
        text = self.run_cli('merge_engine.py', main, frag, out, '--nested-name', 'MergedExtra')
        self.assertIn('container mode', text)
        merged = out.read_text(encoding='utf-8')
        self.assertIn('@org.junit.jupiter.api.Nested', merged)
        self.assertIn('class MergedExtra', merged)
        self.assertIn('void added()', merged)
        self.assertIn('void keep()', merged)
        self.assertIn('import java.util.Map;', merged)
        self.assertEqual(merged.count('{'), merged.count('}'))

        clash = self.root / 'm_clash.java'
        clash.write_text('package net.mooctest;\nimport java.util.Set;\nclass Clash {\n'
                         '    @Test void keep() { }\n}\n', encoding='utf-8')
        text = self.run_cli('merge_engine.py', main, clash, '--check-conflicts', expect=1)
        self.assertIn('REFUSED', text)
        self.assertIn('keep', text)

        bare = self.root / 'm_bare.java'
        bare.write_text('class StudentTest { }\n', encoding='utf-8')
        text = self.run_cli('merge_engine.py', bare, frag, expect=1)
        self.assertIn('no imports block', text)

    def test_replace_section_only_touches_existing_container(self):
        main = self.root / 'r_main.java'
        main.write_text('package net.mooctest;\nimport java.util.List;\nclass StudentTest {\n'
                        '    @Nested\n    class Extra {\n        @Test void oldCase() { }\n    }\n}\n',
                        encoding='utf-8')
        section = self.root / 'r_section.java'
        section.write_text('package net.mooctest;\nimport java.util.Map;\nclass Extra {\n'
                           '    @Test void freshCase() { }\n}\n', encoding='utf-8')
        out = self.run_cli('replace_section.py', main, section, 'Extra')
        self.assertIn('re-run the suite', out)
        updated = main.read_text(encoding='utf-8')
        self.assertIn('freshCase', updated)
        self.assertNotIn('oldCase', updated)
        self.assertIn('import java.util.Map;', updated)
        self.assertEqual(updated.count('{'), updated.count('}'))

        text = self.run_cli('replace_section.py', main, section, 'Missing', expect=1)
        self.assertIn('flat mode', text)


if __name__ == '__main__':
    unittest.main(verbosity=2)
