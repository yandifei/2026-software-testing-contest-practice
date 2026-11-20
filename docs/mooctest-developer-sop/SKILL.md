---
name: mooctest-developer-sop
description: 为慕测 Java 开发者测试赛题核对规则、设计和重写 JUnit 测试、复用或合并同源套件（含跨模型多套件对比与择优）、分析覆盖与变异缺口、验证打包并按用户要求提交；也用于比赛复盘、结论自查和测试方法研究。
---

# 慕测开发者测试

按用户指定的题目、目录与阶段执行。用户已下载、解压或登录时复用现状；仅要求复盘时更新方法与证据，不继续改题或提交。当前用户范围和当场题包/平台要求优先于本技能的历史案例；规则来源冲突时保留原文和适用范围，不能挑选有利条款凑分。

## 开局：先弄清本轮实验

1. 记录题目、阶段、截止时间/时区、原始题包、开发副本、允许修改范围、测试路径、上传限制及评分来源。参考 [赛事规则快照](references/competition-rules.md)，但不能沿用历史权重。未知权重标为未知，仍可开展真实行为测试。
2. 用户指定原始目录时只读；复制源码、POM、资源及说明到指定工作区。不带旧 target。读取适用 AGENTS.md、题包说明、POM 和示例。
3. 同时核对 `java -version` 与 `mvn -version`，在当前进程设置正确 JDK。用原题包建立源码/POM 基线；事后从已修改项目重新生成的基线不能证明未改。
4. 先列所有模块，再看旧套件覆盖了什么。新题即使沿用旧项目名，也可能增加大量零覆盖模块。默认允许审查后复用场景；用户要求“删掉重写”时走 [独立重做](references/independent-rebuild.md)，不得悄悄合并旧测试。手上有两份及以上同题测试时，先比对 `src/main` 与 `pom.xml` 哈希，逐字节相同才谈移植；谁更强用同口径三指标判断，不用文件大小（见 [套件移植](references/suite-porting.md)）。
5. 按本次需要读取下表。不要为了技能标签串行加载一长串其它技能；可借用独立预期值、根因定位、完成前验证等方法，不能以流程代替用例和实测。

| 当前任务 | 读取 |
| --- | --- |
| 写测试、运行、验包 | [工作流](references/workflow.md) |
| 弱断言、算法参照、状态污染 | [测试方法](references/testing-methodology.md) |
| 已有报告提分、分析存活项 | [覆盖与变异研究](references/coverage-mutation-research.md) |
| 反射、mock、私有状态或环境替换 | [白盒边界](references/whitebox-research.md) |
| 多执行者或跨版本对比 | [并行集成](references/multi-agent-integration.md) |
| 手上有两份同题测试、判断能否复用或合并 | [套件移植](references/suite-porting.md) |
| 三份以上同题套件、跨模型对比或选骨架 | [套件移植](references/suite-porting.md)、[三方合并案例](references/case-2026-11-16-cross-ai-merge.md) |
| 手上有合并版和其它同源 PIT 报告、要查是否丢击杀 | [跨套件对账案例](references/case-2026-11-15-cross-provider-diff.md)、`scripts/mutation_union.py` |
| 别家报告明显更高、想借它的用例 | [跨套件对账案例](references/case-2026-11-15-cross-provider-diff.md) 第 3–4 节（先审计 mock 命中区域，再做可达性证明） |
| 结论可能下早了、要自查推理 | [推理失误模式](references/ai-error-patterns.md) |
| 失败、低分或本轮复盘 | [防错手册](references/error-prevention.md)、[GPT 本轮复盘](references/case-2026-11-15-gpt-review.md)、[三方合并复盘](references/case-2026-11-16-cross-ai-merge.md) |
| 用户要求下载/提交 | [浏览器入口](references/browser-entry.md)、[提交](references/browser-submission.md)、[评分与回执](references/scoring-and-submission.md) |

## 执行中始终成立的约束

- **只改允许的测试材料。** 生产文件清单和 POM 哈希不变是必要条件；还要检查测试是否在运行时替换被测逻辑。依赖库已提供、源码没改、本地高分，都不等于赛事允许。
- **按行为判断白盒边界。** 外部接口的合法替身、调用真实方法、构造真实对象，与替换被测返回值/破坏构造器不变量不同。若题包禁止人为打开原实现不可达路径，不能用 mockStatic、mockConstruction、mock record/enum 等绕过；不能只禁两个 API 后放行同类行为。历史“官方认可”没有可核查原文时不作为许可。
- **覆盖与断言一起设计。** 每个场景写出独立预期及它应检出的错误。先处理大块零覆盖模块，再按当前缺口、真实权重和单位时间收益安排；不固定“方法做完才做分支、变异”。不得用实现输出直接生成 expected，或吞掉任意异常换取全绿。
- **一个测试主文件和一个 target 各有唯一写入者。** 并行用独立副本。合并按冻结文件哈希接收，集成后重新运行；测试修改后旧报告失效。
- **合并版的“剩余项等价”只是假设。** 用 `scripts/mutation_union.py` 按变异身份对账：同一身份在别家报告里是 KILLED 时，按 `killingTest` 回查是不是合并删了用例；借外部高分套件前先数 `mockStatic`/`mockConstruction`/连续 `thenReturn`，命中区域的击杀不计入可借集合。
- **报告先验真，再计数。** 构建结束、退出码成功、XML 严格完整、变异身份无重复、范围与版本可对应才用于最终统计。损坏 XML 的片段提取仅供诊断，不得静默截断、去重后宣布通过。
- **迭代与证据分层。** 秒级覆盖统计与副本内定向变异只用于决定下一批测试；正式分子分母只来自一次完整运行，且不与其它构建并行。汇报时必须标明实测值/下界/上界。
- **四项分开报告。** METHOD、LINE（仅行覆盖代理）、BRANCH、PIT KILLED/全部变异；TIMED_OUT 等其他状态独立列出。PIT 的 detected/控制台 Killed 不自动等于严格 KILLED，平台分数另列。
- **不虚构上限或成绩。** 未找到反例不等于等价；输入域、入口与观察范围必须写清。不缩分母、不改报告、不探测 mutant 身份。含规则冲突的实验高分不能当可提交版本。承诺“冲到 100%”前先量化不可达分支与等价变异的**合规上限**；差值已证明不可达时给证明，不用 mock 被测类或伪造状态换分。

## 验证、打包与提交

按 [工作流](references/workflow.md)冻结测试，保存“题包→测试→构建→原始报告→交付件”的哈希及命令/退出码，用 [交付记录](assets/delivery-record.md)记下实际状态。单文件命名、ZIP 根目录和报告要求以当前题包为准。

脚本只验证它声明的条件；结构正确不等于报告新鲜，也不等于平台评分通过。脚本本身有缺陷时应修正并用正常/失败样例验证，不能因为写在技能里就信任。

| 脚本 | 用途 |
| --- | --- |
| `scripts/extract_zip.py <zip> <dir>` | 安全解包及文件名编码处理 |
| `scripts/baseline.py <项目> <out.json>` | 从可信原题建立 src/main + POM 的 SHA-256 清单 |
| `scripts/verify_baseline.py <项目> <baseline.json> [--test-path <相对路径>]` | 核对生产边界与单一测试文件 |
| `scripts/pit_robust.py <xml> <summary\|classes\|list\|mutators>` | 严格校验并统计；`--diagnostic` 只诊断损坏片段，不产生可验收比例 |
| `scripts/report_freshness.py <xml> --test-file <StudentTest.java> [--expect-total N] [--console-log log]` | 检查 NUL/严格解析/总数/新鲜度四件套；`--self-test` 跑内置样例 |
| `scripts/jacoco_report.py <xml> [N] [M]` | 分开统计覆盖与方法缺口 |
| `scripts/branch_gaps.py <xml> [N] [1]` | 按类/方法定位分支缺口 |
| `scripts/package_project.py <项目> <out.zip> --java-file <独立Java> --max-bytes <ZIP上限字节> --baseline <基线JSON> [--test-path <相对路径>]` | 验证文件清单、ZIP 字节一致性与容量；输出放项目外 |
| `scripts/member_conflicts.py <主文件> <被移植文件> [--json out.json]` | 合并前扫描类体深度 1 的成员名冲突；比较名字不比较参数列表，同名重载算保守误报；返回码 1 表示有冲突 |
| `scripts/mutation_union.py --provider LABEL=PATH ... [--baseline LABEL] [--json out] [--self-test]` | 按变异身份集合对账多份同源报告：全部杀 / 无人杀 / 基线漏杀 + `killingTest`；NUL/截断/重复默认拒收，`--allow-damaged` 仅供诊断 |
| `scripts/merge_engine.py <主文件> <片段> [输出] [容器名] [--check-conflicts]` | 容器模式合并；默认生成 `@Nested`，平铺合并要自行拼接（见 [套件移植](references/suite-porting.md) 第 4 节） |
| `scripts/replace_section.py <主文件> <片段> <容器类名>` | 只替换已存在的 `@Nested` 容器体；没有容器时报错并提示平铺模式要重新生成 |
| `python -B scripts/test_delivery_tools.py` | 在临时目录检查报告拒绝条件、基线与打包一致性，不运行比赛构建 |

用户已有提交授权时无需重复索要确认。上传前核对赛事、题目和冻结材料；发现来源不明的更新提交时先核对其版本和收尾安排，不能为了声明“本会话版本”反复覆盖。平台文件名、大小和时间只提供辅助证据，不证明字节一致；没有本次上传回执不能把页面“2/2”称为本会话提交成功。

## 完成标准与学习方式

汇报已完成阶段、材料、实测分子分母、来源范围、未解决项和提交回执；没有平台成绩就明确未取得。明确区分“已修复”“有规避方案”“仍未验证”。

新增经验写成“症状→证据→原因及置信度→恢复→防止复发→验收”，而不是追加绝对命令。参见 [本轮 GPT 复盘](references/case-2026-11-15-gpt-review.md)、[合并增强线](references/case-2026-11-15-merge-sweep.md)、[跨套件对账线](references/case-2026-11-15-cross-provider-diff.md)、[三方合并复盘](references/case-2026-11-16-cross-ai-merge.md)、[其他执行线对照](references/case-2026-11-15-provincial.md) 和 [审计证据](references/evidence-2026-11-15.json)。不同执行线的高分不能拿来充当本会话成绩。
