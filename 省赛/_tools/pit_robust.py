import sys, re, collections

MUT = re.compile(r"<mutation\b([^>]*)>(.*?)</mutation>", re.S)
ATTR = re.compile(r"(\w+)='([^']*)'")
FIELD = re.compile(r"<(\w+)>(.*?)</\1>", re.S)

def parse(path):
    data = open(path, encoding='utf-8', errors='replace').read()
    rows = []
    for attrs, body in MUT.findall(data):
        a = dict(ATTR.findall(attrs))
        fields = dict(FIELD.findall(body))
        # nested index/block
        idx = re.search(r"<index>(\d+)</index>", body)
        blk = re.search(r"<block>(\d+)</block>", body)
        rows.append({
            'status': a.get('status', '?'),
            'class': fields.get('mutatedClass', '?'),
            'method': fields.get('mutatedMethod', '?'),
            'line': fields.get('lineNumber', '?'),
            'mutator': (fields.get('mutator', '?')).rsplit('.', 1)[-1].replace('Mutator', ''),
            'desc': fields.get('description', ''),
            'index': idx.group(1) if idx else '?',
            'block': blk.group(1) if blk else '?',
        })
    # dedupe identical mutations (concatenated report artifact)
    seen = set()
    uniq = []
    for r in rows:
        key = (r['class'], r['method'], r['line'], r['mutator'], r['index'], r['block'], r['desc'])
        if key in seen:
            continue
        seen.add(key)
        uniq.append(r)
    return uniq

def main(path, mode='summary'):
    rows = parse(path)
    st = collections.Counter(r['status'] for r in rows)
    print(f'parsed mutations (deduped): {len(rows)}')
    print(' '.join(f'{k}={v}' for k, v in sorted(st.items())))
    if st:
        print(f"KILLED = {st.get('KILLED',0)}/{len(rows)} = {st.get('KILLED',0)/len(rows)*100:.2f}%")
    if mode == 'classes':
        c = collections.Counter()
        for r in rows:
            if r['status'] in ('SURVIVED', 'NO_COVERAGE'):
                c[(r['class'].split('.')[-1], r['status'])] += 1
        agg = collections.defaultdict(collections.Counter)
        for (cls, s), n in c.items():
            agg[cls][s] += n
        for cls, cc in sorted(agg.items(), key=lambda kv: -(kv[1].get('SURVIVED', 0) + kv[1].get('NO_COVERAGE', 0)))[:40]:
            print(f"  {cls:34s} SURVIVED={cc.get('SURVIVED',0):3d} NO_COVERAGE={cc.get('NO_COVERAGE',0):3d}")
    elif mode == 'list':
        for r in rows:
            if r['status'] in ('SURVIVED', 'NO_COVERAGE'):
                print(f"{r['status']:12s} {r['class'].split('.')[-1]}.{r['method']} L{r['line']} idx={r['index']} {r['mutator']} :: {r['desc'][:90]}")
    elif mode == 'mutators':
        c = collections.Counter((r['mutator'], r['status']) for r in rows if r['status'] in ('SURVIVED', 'NO_COVERAGE'))
        for (mut, s), n in c.most_common(30):
            print(f'  {n:4d} {s:12s} {mut}')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else 'summary')
