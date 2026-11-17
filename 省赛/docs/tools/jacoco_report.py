import sys, collections, xml.etree.ElementTree as ET

def analyze(path, top=40, list_missed_methods=0):
    r = ET.parse(path).getroot()
    counters = {c.get('type'): (int(c.get('missed')), int(c.get('covered'))) for c in r.findall('counter')}
    def pct(t):
        m, c = counters.get(t, (0, 0))
        tot = m + c
        return f'{t}: {c}/{tot} = {c/tot*100:.2f}%' if tot else f'{t}: n/a'
    for t in ('INSTRUCTION', 'LINE', 'BRANCH', 'METHOD', 'CLASS'):
        print(pct(t))
    rows = []
    for pkg in r.findall('package'):
        for cls in pkg.findall('class'):
            name = cls.get('name')
            c = {x.get('type'): (int(x.get('missed')), int(x.get('covered'))) for x in cls.findall('counter')}
            m_miss, m_cov = c.get('METHOD', (0, 0))
            l_miss, l_cov = c.get('LINE', (0, 0))
            missing = []
            for meth in cls.findall('method'):
                mc = {x.get('type'): (int(x.get('missed')), int(x.get('covered'))) for x in meth.findall('counter')}
                mm, mc2 = mc.get('METHOD', (0, 0))
                ml, ml2 = mc.get('LINE', (0, 0))
                if mm or ml:
                    missing.append((meth.get('name'), meth.get('desc'), ml, mc2, mm))
            rows.append((l_miss, m_miss, name, l_cov, m_cov, missing))
    rows.sort(key=lambda x: (-x[0], -x[1]))
    print('\n-- classes with missed lines (top %d) --' % top)
    for l_miss, m_miss, name, l_cov, m_cov, missing in rows[:top]:
        if l_miss == 0 and m_miss == 0:
            continue
        print(f'{name}: LINE {l_cov}/{l_cov+l_miss}  METHOD {m_cov}/{m_cov+m_miss}')
        if list_missed_methods:
            for mname, desc, ml, mlcov, mm in missing[:list_missed_methods]:
                print(f'    missed {mname}{desc} lines_missed={ml}')
    return counters

if __name__ == '__main__':
    n = int(sys.argv[2]) if len(sys.argv) > 2 else 40
    lm = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    analyze(sys.argv[1], n, lm)
