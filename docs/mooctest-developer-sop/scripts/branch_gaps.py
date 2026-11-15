import sys, xml.etree.ElementTree as ET

def main(path, top=25, method_level=0):
    r = ET.parse(path).getroot()
    rows = []
    for pkg in r.findall('package'):
        for cls in pkg.findall('class'):
            c = {x.get('type'): (int(x.get('missed')), int(x.get('covered'))) for x in cls.findall('counter')}
            bm, bc = c.get('BRANCH', (0, 0))
            lm, lc = c.get('LINE', (0, 0))
            meths = []
            if method_level and bm:
                for m in cls.findall('method'):
                    mc = {x.get('type'): (int(x.get('missed')), int(x.get('covered'))) for x in m.findall('counter')}
                    mm, mcv = mc.get('BRANCH', (0, 0))
                    if mm:
                        meths.append((m.get('name'), m.get('line'), mm, mcv))
            rows.append((bm, cls.get('name'), bc, bc + bm, lm, meths))
    rows.sort(key=lambda x: -x[0])
    tot_m = sum(x[0] for x in rows)
    tot_c = sum(x[2] for x in rows)
    print(f'TOTAL BRANCH missed={tot_m} covered={tot_c} total={tot_m+tot_c}')
    for bm, name, bc, tot, lm, meths in rows[:top]:
        if bm == 0:
            continue
        print(f'{bm:4d} missed  {name}  (branch {bc}/{tot}, line missed {lm})')
        for mname, line, mm, mcv in meths:
            print(f'        - {mname} (L{line}) missed={mm} covered={mcv}')

if __name__ == '__main__':
    main(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 25, int(sys.argv[3]) if len(sys.argv) > 3 else 0)
