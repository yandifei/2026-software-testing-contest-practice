# docs —— 省赛文档归档

本目录存放无代码依赖的说明性与证明性文档。项目源码与可执行产物仍在各自目录下。

| 文件 | 说明 |
| --- | --- |
| `AGENT-BRIEF.md` | 省赛“开发者测试”执行者共用指令：硬约束、命令与评分口径 |
| `card-approval-equivalence-appendix.md` | card-approval 变异存活项的等价 / 不可达证明（附录） |
| `card-approval-card2-branch-notes.md` | card-approval 分支覆盖补强（CardProbeTest2）的结果与结论 |
| `card-approval-card2-equivalence-notes.md` | card-approval 剩余未杀变异的等价 / 不可达证明（card2 / CardProbeTest） |
| `math-assessment-equivalence-appendix.md` | math-assessment 等价性 / 不可达性证明附录 |

相关文档位置（相对本目录）：

- 完整交付记录（要求、基线、验证、提交回执明细）：`../deliverables/delivery-record.md`
- 交付件与提交原件说明：`../deliverables/README.md`
- 平台提交回执：`../deliverables/submitted/提交回执.md`
- 各模型实现对比：`../相互借鉴/测试对比与覆盖率报告.md`、`../相互借鉴2/测试对比与达标报告.md`、`../GPT/rewrite/StudentTest-提升报告.md`
- 复核脚本（JaCoCo / PIT 报告解析、基线校验、打包）：`tools/`

> 目录曾用名：`_baseline/`（测试基线工作目录，日志与 JaCoCo / PIT 中间 XML 已删除）、`_submit/`（已并入 `deliverables/submitted/`）、`_tools/`（已并入 `docs/tools/`）、`mooctest-developer-sop/`（已删除，内容为通用赛项规程与方法论）。
