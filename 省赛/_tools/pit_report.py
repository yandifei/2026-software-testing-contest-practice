import sys, collections, xml.etree.ElementTree as ET

def txt(node, path, default=''):
    e = node.find(path)
    return e.text if e is not None and e.text is not None else default

def summarize(path, show_survivors=0):
    t = ET.parse(path).getroot()
    status = collections.Counter()
    per_class = collections.defaultdict(collections.Counter)
    survivors = []
    for m in t.iter('mutation'):
        st = m.get('status')
        status[st] += 1
        cls = txt(m, 'mutatedClass') or '?'
        per_class[cls][st] += 1
        if st in ('SURVIVED', 'NO_COVERAGE', 'TIMED_OUT', 'RUN_ERROR'):
            survivors.append({
                'status': st,
                'class': cls,
                'method': txt(m, 'mutatedMethod'),
                'line': txt(m, 'lineNumber', '?'),
                'index': txt(m, 'indexes/index', '?'),
                'block': txt(m, 'blocks/block', '?'),
                'mutator': (txt(m, 'mutator').rsplit('.', 1)[-1]),
                'desc': txt(m, 'description'),
                'killing': txt(m, 'killingTest'),
            })
    total = sum(status.values())
    print(f'== {path}')
    print(f'total={total} ' + ' '.join(f'{k}={v}' for k, v in sorted(status.items())))
    if total:
        killed = status.get('KILLED', 0)
        print(f'KILLED rate = {killed}/{total} = {killed/total*100:.2f}%')
    print('-- by class (survivors>0) --')
    for cls, c in sorted(per_class.items(), key=lambda kv: -(kv[1].get('SURVIVED', 0) + kv[1].get('NO_COVERAGE', 0))):
        s = c.get('SURVIVED', 0); n = c.get('NO_COVERAGE', 0); k = c.get('KILLED', 0)
        if s or n:
            print(f'  {cls}: KILLED={k} SURVIVED={s} NO_COVERAGE={n} TIMED_OUT={c.get("TIMED_OUT",0)}')
    if show_survivors:
        print(f'-- first {show_survivors} survivors --')
        for s in survivors[:show_survivors]:
            print(f'  {s["status"]} {s["class"]}.{s["method"]} L{s["line"]} idx={s["index"]} blk={s["block"]} {s["mutator"]} :: {s["desc"]}')
    return status, per_class, survivors

if __name__ == '__main__':
    n = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    summarize(sys.argv[1], n)
