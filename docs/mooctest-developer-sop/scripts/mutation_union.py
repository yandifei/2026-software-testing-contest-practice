#!/usr/bin/env python3
"""跨套件 PIT 变异身份对账。

用途：
  1) 多份同源套件（同 src/main、同 POM）各自跑过 PIT 后，按“变异身份集合”比较，
     而不是按包百分比或总数，回答三类问题：
       - 并集里有多少条被所有套件杀死、多少条无人杀死；
       - 以某套件为基线时，哪些变异“别的套件杀死了、基线没杀死”；
       - 这些差额对应的 killingTest 叫什么，用来反查是“等价判断错误”还是“用例被删”。
  2) 合并后回归排查：如果合并版声称某批剩余项“全部等价”，而另一套件的报告里
     同一身份是 KILLED，就说明合并丢了用例（本脚本直接列出 killingTest）。

身份口径（与 PIT 报告一致，缺一不可）：
  mutatedClass | mutatedMethod + methodDescription | lineNumber | mutator | indexes | blocks
  同一行可以有多个 index/block，对应不同字节码位置，不能按行合并。

损坏报告：PIT 并发写/中止会留下 NUL、截断、标签错乱。本脚本会给出
  nul 计数 / 是否以 </mutations> 收尾 / 解析条数，并在存在异常时以非零码退出，
  除非显式 --allow-damaged（此时结果仅供诊断，不得用于正式对比）。

用法：
  python -B scripts/mutation_union.py \
    --provider deepseek=path/to/mutations.xml \
    --provider glm=path/to/mutations.xml \
    --provider jiebei=path/to/mutations.xml \
    --baseline jiebei --json out.json

  python -B scripts/mutation_union.py --self-test   # 合成样例自验，不读真实报告

退出码：0 正常；1 报告损坏（未加 --allow-damaged）；2 参数错误。
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

MUTATION_RE = re.compile(r"<mutation\b([^>]*)>(.*?)</mutation>", re.S)
STATUS_RE = re.compile(r"status=['\"]([A-Z_]+)['\"]")
DETECTED_RE = re.compile(r"detected=['\"](true|false)['\"]")

FIELDS = (
    "mutatedClass",
    "mutatedMethod",
    "methodDescription",
    "lineNumber",
    "mutator",
    "killingTest",
)


def field(body: str, name: str) -> str:
    match = re.search(rf"<{name}>(.*?)</{name}>", body, re.S)
    if not match:
        return ""
    return match.group(1).strip()


def indexes(body: str, tag: str, item: str) -> str:
    block = re.search(rf"<{tag}>(.*?)</{tag}>", body, re.S)
    if not block:
        return ""
    values = re.findall(rf"<{item}>(.*?)</{item}>", block.group(1), re.S)
    return ",".join(value.strip() for value in values)


def identity(body: str) -> str:
    return "|".join(
        (
            field(body, "mutatedClass"),
            field(body, "mutatedMethod") + field(body, "methodDescription"),
            field(body, "lineNumber"),
            field(body, "mutator"),
            indexes(body, "indexes", "index"),
            indexes(body, "blocks", "block"),
        )
    )


def parse_report(path: Path) -> tuple[dict[str, dict], dict]:
    """返回 (identity -> {status, killingTest}, 报告健康信息)。"""
    raw = path.read_bytes()
    nul = raw.count(b"\x00")
    text = raw.decode("utf-8", "replace")
    closed = text.rstrip().endswith("</mutations>")
    records: dict[str, dict] = {}
    duplicates = 0
    for match in MUTATION_RE.finditer(text):
        attributes, body = match.group(1), match.group(2)
        status_match = STATUS_RE.search(attributes)
        status = status_match.group(1) if status_match else "UNKNOWN"
        key = identity(body)
        if key in records:
            duplicates += 1
            continue
        records[key] = {
            "status": status,
            "killingTest": field(body, "killingTest"),
            "detected": bool(DETECTED_RE.search(attributes) and DETECTED_RE.search(attributes).group(1) == "true"),
            "class": field(body, "mutatedClass"),
            "method": field(body, "mutatedMethod"),
            "line": field(body, "lineNumber"),
            "mutator": field(body, "mutator").rsplit(".", 1)[-1],
        }
    health = {
        "path": str(path),
        "bytes": len(raw),
        "nul_bytes": nul,
        "closed": closed,
        "mutations": len(records),
        "duplicates": duplicates,
        "damaged": bool(nul or not closed),
    }
    return records, health


SELF_TEST_TEMPLATE = """<mutations>
  <mutation detected='{detected}' status='{status}' numberOfTestsRun='1'>
    <sourceFile>Probe.java</sourceFile>
    <mutatedClass>net.mooctest.Probe</mutatedClass>
    <mutatedMethod>{method}</mutatedMethod>
    <methodDescription>()V</methodDescription>
    <lineNumber>{line}</lineNumber>
    <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
    <indexes><index>3</index></indexes>
    <blocks><block>1</block></blocks>
    <killingTest>{test}</killingTest>
    <description>{description}</description>
  </mutation>
</mutations>
"""


def self_test() -> int:
    """用两个合成报告验证：缺口识别、健康检查、损坏拒绝三条路径。"""
    import tempfile

    def write(directory: Path, name: str, entries: list[dict]) -> Path:
        def render(entry: dict) -> str:
            payload = dict(entry)
            test_value = payload.pop("test", "")
            return SELF_TEST_TEMPLATE.format(
                detected="true" if payload["status"] == "KILLED" else "false",
                test=test_value,
                **payload,
            )

        body = "".join(render(entry) for entry in entries)
        path = directory / name
        path.write_text(body, encoding="utf-8")
        return path

    with tempfile.TemporaryDirectory() as temp:
        temp_dir = Path(temp)
        base = write(temp_dir, "base.xml", [
            {"status": "SURVIVED", "method": "alpha", "line": "10", "description": "add -> sub"},
        ])
        other = write(temp_dir, "other.xml", [
            {"status": "KILLED", "method": "alpha", "line": "10",
             "description": "add -> sub", "test": "ProbeTest.killsAlpha"},
        ])
        records, health = parse_report(other)
        assert len(records) == 1 and not health["damaged"], health
        key = next(iter(records))
        assert records[key]["status"] == "KILLED"
        assert records[key]["killingTest"] == "ProbeTest.killsAlpha"

        # 截断（无 </mutations>）必须被标为损坏
        damaged_path = temp_dir / "damaged.xml"
        damaged_path.write_text(SELF_TEST_TEMPLATE.split("</mutations>")[0], encoding="utf-8")
        _, damaged_health = parse_report(damaged_path)
        assert damaged_health["damaged"], "截断报告未被识别"
        missing_records, _ = parse_report(damaged_path)
        assert len(missing_records) == 1, "截断报告仍应能诊断性地解出条目"

        # 正常报告不应带 NUL
        _, clean_health = parse_report(base)
        assert clean_health["nul_bytes"] == 0 and not clean_health["damaged"]

    print("self-test OK：身份对账、killingTest 提取、截断识别、NUL 计数")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="跨套件 PIT 变异身份对账")
    parser.add_argument("--self-test", action="store_true", help="运行内置样例，不读取真实报告")
    parser.add_argument("--provider", action="append", required=False,
                        metavar="LABEL=PATH", help="可重复，形如 deepseek=.../mutations.xml")
    parser.add_argument("--baseline", default=None, help="作为基线的 provider 标签")
    parser.add_argument("--json", default=None, help="把矩阵写到 JSON")
    parser.add_argument("--allow-damaged", action="store_true", help="允许使用损坏报告（仅诊断）")
    parser.add_argument("--max-gaps", type=int, default=80, help="最多打印多少条缺口")
    args = parser.parse_args()

    if args.self_test:
        return self_test()
    if not args.provider:
        parser.error("至少需要一个 --provider LABEL=PATH（或用 --self-test）")

    providers: list[tuple[str, Path]] = []
    for spec in args.provider:
        if "=" not in spec:
            print(f"参数错误：--provider 需要 LABEL=PATH，收到 {spec!r}", file=sys.stderr)
            return 2
        label, path_text = spec.split("=", 1)
        providers.append((label.strip(), Path(path_text)))

    reports: dict[str, dict[str, dict]] = {}
    health_rows = []
    damaged = False
    for label, path in providers:
        if not path.exists():
            print(f"参数错误：找不到 {path}", file=sys.stderr)
            return 2
        records, health = parse_report(path)
        reports[label] = records
        health_rows.append({"label": label, **health})
        damaged = damaged or health["damaged"]

    print("== 报告健康 ==")
    for row in health_rows:
        flag = "损坏" if row["damaged"] else "正常"
        print(f"  {row['label']:<14} {flag:<4} 变异={row['mutations']:<5} "
              f"NUL={row['nul_bytes']:<8} 收尾={'是' if row['closed'] else '否'} "
              f"重复={row['duplicates']}  {row['bytes']} 字节")
    if damaged and not args.allow_damaged:
        print("\n存在损坏报告：拒绝输出正式对比。修复或重跑后重试；"
              "仅诊断用途请加 --allow-damaged。", file=sys.stderr)
        return 1
    if damaged:
        print("\n[诊断模式] 损坏报告只用于定位，不可写入正式对比表。")

    labels = [label for label, _ in providers]
    all_keys = set()
    for records in reports.values():
        all_keys |= set(records)

    print(f"\n== 状态分布（并集 {len(all_keys)} 条）==")
    for label in labels:
        records = reports[label]
        counts: dict[str, int] = {}
        absent = 0
        for key in all_keys:
            if key not in records:
                absent += 1
                continue
            status = records[key]["status"]
            counts[status] = counts.get(status, 0) + 1
        summary = " ".join(f"{name}={counts[name]}" for name in sorted(counts))
        print(f"  {label:<14} {summary}  ABSENT={absent}")

    killed_by_all = 0
    killed_by_none = 0
    for key in all_keys:
        killed = [label for label in labels
                  if key in reports[label] and reports[label][key]["status"] == "KILLED"]
        if len(killed) == len(labels):
            killed_by_all += 1
        if not killed:
            killed_by_none += 1
    print(f"\n  全部套件都 KILLED：{killed_by_all}")
    print(f"  没有任何套件 KILLED：{killed_by_none}")

    if args.baseline:
        if args.baseline not in reports:
            print(f"参数错误：--baseline {args.baseline!r} 不在 provider 列表中", file=sys.stderr)
            return 2
        base = reports[args.baseline]
        gaps = []
        for key in all_keys:
            base_status = base[key]["status"] if key in base else "ABSENT"
            if base_status == "KILLED":
                continue
            killers = []
            for label in labels:
                if label == args.baseline:
                    continue
                record = reports[label].get(key)
                if record and record["status"] == "KILLED":
                    killers.append((label, record["killingTest"]))
            if killers:
                sample = base.get(key) or next(iter(reports[l][key] for l in labels
                                                     if key in reports[l]))
                gaps.append({"key": key, "base_status": base_status,
                             "sample": sample, "killers": killers})

        gaps.sort(key=lambda item: (item["sample"]["class"], int(item["sample"]["line"] or 0)))
        print(f"\n== 基线 {args.baseline} 未杀、但其它套件杀掉：{len(gaps)} 条 ==")
        print("（若基线自称这些项“等价/不可达”，说明合并时丢了用例；killingTest 可直接反查）")
        for gap in gaps[: args.max_gaps]:
            sample = gap["sample"]
            class_short = sample["class"].replace("net.mooctest.", "")
            killer_text = "；".join(
                f"{label}→{test or '(未记录 killingTest)'}" for label, test in gap["killers"]
            )
            print(f"  {class_short}.{sample['method']} L{sample['line']} "
                  f"{sample['mutator']}  base={gap['base_status']}  被 {killer_text} 杀死")
        if len(gaps) > args.max_gaps:
            print(f"  ... 其余 {len(gaps) - args.max_gaps} 条见 --json")

        if args.json:
            Path(args.json).write_text(
                json.dumps({"providers": health_rows, "baseline": args.baseline,
                            "gaps": gaps}, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print(f"\n矩阵已写入 {args.json}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
