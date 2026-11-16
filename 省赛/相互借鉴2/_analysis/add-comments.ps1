# 为 StudentTest.java 的每个嵌套类、每个测试方法补注释（幂等：已有注释则跳过）
param(
    [string]$Source,
    [string]$Output
)

$lines = [System.IO.File]::ReadAllLines($Source, [Text.Encoding]::UTF8)
$result = New-Object System.Collections.Generic.List[string]
$insertedClass = 0
$insertedTest = 0
$pendingClassDoc = $false

function Get-Indent([string]$line) {
    return ($line -replace '^(\s*).*$', '$1')
}

function Test-PrecedingComment($list, [int]$index) {
    # 向上找第一个非空行，判断它是不是注释
    for ($i = $index - 1; $i -ge 0; $i--) {
        $previous = $list[$i].Trim()
        if ($previous -eq '') { continue }
        return ($previous.StartsWith('//') -or $previous.StartsWith('*') -or $previous.StartsWith('/*'))
    }
    return $false
}

for ($index = 0; $index -lt $lines.Count; $index++) {
    $line = $lines[$index]
    $trimmed = $line.Trim()

    # ---------- 嵌套类：@Nested 或 class 声明 ----------
    $isNestedAnnotation = $trimmed -match '^@(?:org\.junit\.jupiter\.api\.)?Nested\b'
    $isClassDeclaration = $trimmed -match '^(?:private\s+|static\s+|final\s+|public\s+)*class\s+\w+' -and
                          $trimmed -notmatch '^class\s+StudentTest' -and
                          -not $trimmed.EndsWith(';')

    if ($isNestedAnnotation -and -not $pendingClassDoc -and -not (Test-PrecedingComment $result $result.Count)) {
        # @Nested 注解行：真正的测试组，类名在后面 3 行内
        $displayName = $null
        for ($j = $index - 1; $j -ge [Math]::Max(0, $index - 4); $j--) {
            $matchDisplay = [regex]::Match($lines[$j], '@DisplayName\("([^"]*)"\)')
            if ($matchDisplay.Success) { $displayName = $matchDisplay.Groups[1].Value; break }
        }
        $className = '该测试组'
        for ($k = $index + 1; $k -lt [Math]::Min($index + 4, $lines.Count); $k++) {
            if ($lines[$k] -match 'class\s+(\w+)') { $className = $Matches[1]; break }
        }
        $indent = Get-Indent $line
        $description = if ($displayName) { $displayName } else { "$className 测试组（覆盖对应模块的正常、边界与异常路径）" }
        $result.Add("$indent/**")
        $result.Add("$indent * 测试组：$description。")
        $result.Add("$indent * 组内每个用例都标注了用例意图与主断言，说明它固定了哪条业务规则、用什么输入、断言什么。")
        $result.Add("$indent */")
        $insertedClass++
        $pendingClassDoc = $true
    }
    elseif ($isClassDeclaration) {
        # 该类上方 4 行内若已有 Nested 注解，说明它是测试组且通常已带说明，不再追加
        $precededByNested = $false
        for ($m = $index - 1; $m -ge [Math]::Max(0, $index - 4); $m--) {
            if ($lines[$m] -match '@(?:org\.junit\.jupiter\.api\.)?Nested\b') { $precededByNested = $true; break }
        }
        if ($pendingClassDoc -or $precededByNested) {
            # 该类的 Javadoc 已在上方 @Nested 处写过，这里不重复
            $pendingClassDoc = $false
        }
        elseif (-not (Test-PrecedingComment $result $result.Count)) {
            # 普通内部类：测试夹具/构造器/替身，被多个用例复用
            $className = if ($trimmed -match 'class\s+(\w+)') { $Matches[1] } else { '辅助类型' }
            $indent = Get-Indent $line
            $result.Add("$indent/**")
            $result.Add("$indent * 测试辅助类型：$className —— 供本文件多个用例复用的构造器、可变夹具或测试替身。")
            $result.Add("$indent * 只承载测试数据搭建，不包含断言；修改时需回归引用它的全部用例。")
            $result.Add("$indent */")
            $insertedClass++
        }
    }

    # ---------- 测试方法：@Test ----------
    $isTest = $trimmed -match '^@(?:org\.junit\.jupiter\.api\.)?Test\b'
    if ($isTest -and -not (Test-PrecedingComment $result $result.Count)) {
        # 方法名与 @DisplayName：可能在同一行，也可能在后面几行
        $displayName = $null
        $methodName = $null
        for ($j = $index; $j -lt [Math]::Min($index + 4, $lines.Count); $j++) {
            if (-not $displayName) {
                $matchDisplay = [regex]::Match($lines[$j], '@DisplayName\("([^"]*)"\)')
                if ($matchDisplay.Success) { $displayName = $matchDisplay.Groups[1].Value }
            }
            if (-not $methodName) {
                $matchMethod = [regex]::Match($lines[$j], '\bvoid\s+(\w+)\s*\(')
                if ($matchMethod.Success) { $methodName = $matchMethod.Groups[1].Value }
            }
        }
        # 找不到 void 声明（可能是同行 @Test void xxx()），直接从当前行再试一次
        if (-not $methodName) {
            $matchInline = [regex]::Match($line, '\bvoid\s+(\w+)\s*\(')
            if ($matchInline.Success) { $methodName = $matchInline.Groups[1].Value }
        }
        # 主断言：方法体内第一条 assert* 语句
        $assertion = $null
        for ($j = $index; $j -lt [Math]::Min($index + 90, $lines.Count); $j++) {
            $matchAssert = [regex]::Match($lines[$j], '(assert\w+\(.*)')
            if ($matchAssert.Success) {
                $assertion = $matchAssert.Groups[1].Value.Trim()
                break
            }
        }
        $intent = if ($displayName) { $displayName } else { $methodName }
        if (-not $intent) { $intent = '本用例' }
        $indent = Get-Indent $line
        $text = if ($assertion) { "// 用例意图：$intent；主断言：$assertion" } else { "// 用例意图：$intent（断言见方法体）" }
        # 注释单行过长时截断，避免破坏排版
        if ($text.Length -gt 160) { $text = $text.Substring(0, 157) + '...' }
        $result.Add($indent + $text)
        $insertedTest++
    }

    $result.Add($line)
}

[System.IO.File]::WriteAllLines($Output, $result, (New-Object Text.UTF8Encoding($false)))
Write-Output ("{0} -> {1}" -f (Split-Path $Source -Leaf), $Output)
Write-Output ("  新增类注释={0} 新增方法注释={1} 原行数={2} 新行数={3}" -f $insertedClass, $insertedTest, $lines.Count, $result.Count)
