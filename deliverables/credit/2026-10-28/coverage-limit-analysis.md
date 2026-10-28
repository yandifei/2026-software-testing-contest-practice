# Credit 原始指标与缺口分析

本轮重新生成：JaCoCo 分支 233/236（98.73%）、行 494/497（99.40%）；PIT 行 488/491（99.39%）、KILLED 263/272（96.69%），SURVIVED 9。没有剔除分母，没有使用旧派生报告。

## 三个不可达分支

- TypeChecker:20 的 while false：type 初始 null，任何一次赋值都立即 break，无法以非 null 返回循环入口。
- TypeChecker:56 的 if false：前面四种识别一旦成功即 break，因此到达该 if 时 type 恒 null。61 行循环回跳也不可达。
- VerveValidator:62 第二轮扫描命中：只在第一轮未命中时执行，第二轮使用同一局部 IIN 和相同区间，无法出现新命中，因此 63/64 行不可达。

## 九个存活变异

完整类、方法、行号与算子见 survivors.json，并可对照本轮 target/pit-reports/mutations.xml。

- LuhnValidator:76 累加变累减：总和模 2^32 为原总和相反数；Java int 除 10 余数是否为零在取反下不变，MIN_VALUE 是自身相反数，两边同为不整除。因此校验结果相同。
- VerveValidator:53 上端点边界变化：第一轮漏掉的端点由第二轮相同闭区间接受。
- VerveValidator:61 边界变化：能到第二轮的 IIN 不属于整个扫描区间，改变端点不影响不命中结果。
- VerveValidator:54 移除第一轮命中：第二轮完整扫描相同区间，补回第一轮命中结果。
- VerveValidator:60 跳过第二轮：第二轮原本不会产生新结果。
- VerveValidator:62 移除第二轮命中：该命中原本不可达。
- VerveValidator:53 跳过第一轮：第二轮完整扫描相同区间。
- VerveValidator:61 跳过第二轮循环：冗余扫描对结果无影响。
- Validator:64 返回常量 true：false 结果此前已抛异常，能正常返回的值原本恒 true。

这些证明针对当前原源码和 DEFAULTS 变异集，不代表平台认可扣除，不将原始比例改成 100%。

## 当前行为中的业务风险

日期校验未先拒绝过去年份，也未检查月份范围；到期当月被拒绝是否符合业务需确认。Verve 漏掉注释中的第二号段；UATP 六位 IIN 与一位常量比较；MaestroUK 只接受注释区间端点；VisaElectron 无法匹配声明的六位前缀且可接受特殊字符组合。Luhn、CVV 的当前行为并不构成完整真实卡校验。测试以 KnownDefects 命名前缀明确标为现状刻画，生产代码没有修改。
