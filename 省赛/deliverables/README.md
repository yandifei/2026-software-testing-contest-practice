# deliverables —— 交付件与提交记录

`_submit/` 与 `deliverables/` 已合并到本目录，`_tools/` 已移入 `docs/tools/`。

```
deliverables/
├─ delivery-record.md            完整交付记录（要求、基线、验证、回执明细）
├─ card-approval/                最终交付包
│   ├─ card-approval.zip
│   └─ StudentTest.java
├─ math-assessment/
│   ├─ math-assessment.zip
│   └─ StudentTest.java
└─ submitted/                    平台提交原件（原 _submit/）
    ├─ 提交回执.md
    ├─ card-approval.zip / math-assessment.zip
    ├─ card-approval-StudentTest.java / math-assessment-StudentTest.java
    └─ card-approval/StudentTest.java、math-assessment/StudentTest.java
```

## 为什么同名文件没有去重

`StudentTest.java` 与两个 zip 在各处内容**互不相同**（SHA-256 不同），属于不同版本的阶段产物，删掉会丢失取证依据，因此按来源保留：

| 题号 | 最终交付版 | 提交原件（单文件） | 提交原件（目录内） |
| --- | --- | --- | --- |
| card-approval | 9267 行 | 9296 行 | 11255 行 |
| math-assessment | 13278 行 | 13431 行 | 18391 行 |

判分以平台最后一次提交的 `.java` 为准，`submitted/` 保留的就是提交时上传的文件原件；`card-approval/`、`math-assessment/` 下为随后整理归档的最终交付包。两者不一致时应以 `submitted/` 为准。

各数字可由 `docs/tools/` 下的脚本从对应报告复核。
