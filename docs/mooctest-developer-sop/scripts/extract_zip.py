import sys, os, zipfile, pathlib

def fix_name(info):
    name = info.filename
    if info.flag_bits & 0x800:
        return name
    try:
        raw = name.encode('cp437')
    except UnicodeEncodeError:
        return name
    for enc in ('gbk', 'gb18030', 'utf-8'):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return name

def main(zip_path, out_dir):
    zip_path = os.path.abspath(zip_path)
    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    with zipfile.ZipFile(zip_path) as z:
        names = []
        for info in z.infolist():
            rel = fix_name(info)
            target = os.path.normpath(os.path.join(out_dir, rel))
            if not target.startswith(out_dir + os.sep) and target != out_dir:
                print('SKIP unsafe:', rel)
                continue
            if rel.endswith('/') or rel.endswith('\\'):
                os.makedirs(target, exist_ok=True)
                continue
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with z.open(info) as src, open(target, 'wb') as dst:
                dst.write(src.read())
            names.append(rel)
        print('extracted files:', len(names))

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
