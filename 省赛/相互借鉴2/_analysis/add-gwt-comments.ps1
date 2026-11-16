# 把每个测试方法改写成 Given/When/Then 注释风格（Javadoc + 方法体内分区标记）
param(
    [string]$Source,
    [string]$Output
)

$lines = [System.IO.File]::ReadAllLines($Source, [Text.Encoding]::UTF8)

function Test-IsDeclaration([string]$text) {
    # 形如：Type name = ...（含 final/泛型/数组）
    return ($text -match '^\s*(final\s+)?[A-Za-z_][\w\.<>,\[\]\s]*\s+\w+\s*=\s*')
}
function Test-IsAssertion([string]$text) {
    return ($text -match '\b(assert[A-Za-z]*|fail|verify)\s*\(')
}
function Test-IsCall([string]$text) {
    # 数据搭建类方法的黑名单：这些不是“调用被测方法”
    $builderPattern = '\.(put|putAll|add|addAll|set|build|append|of|asList|stream|forEach|ifPresent|apply|accept|reset|clear)\s*\('
    if ($text -match $builderPattern) { return $false }
    return ($text -match '\w+\s*\.\s*\w+\s*\(' -or $text -match '^\s*\w+\s*\(')
}

$methods = New-Object System.Collections.Generic.List[object]

# ---------- 第一遍：定位方法边界与三类语句 ----------
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i].Trim() -notmatch '^@(?:org\.junit\.jupiter\.api\.)?Test\b') { continue }

    $signatureIndex = -1
    for ($j = $i; $j -lt [Math]::Min($i + 8, $lines.Count); $j++) {
        if ($lines[$j] -match '\bvoid\s+(\w+)\s*\(') { $signatureIndex = $j; break }
    }
    if ($signatureIndex -lt 0) { continue }
    $methodName = [regex]::Match($lines[$signatureIndex], '\bvoid\s+(\w+)').Groups[1].Value

    $braceIndex = -1
    for ($j = $signatureIndex; $j -lt [Math]::Min($signatureIndex + 4, $lines.Count); $j++) {
        if ($lines[$j].Contains('{')) { $braceIndex = $j; break }
    }
    if ($braceIndex -lt 0) { continue }

    $depth = 0
    $endIndex = -1
    for ($j = $braceIndex; $j -lt $lines.Count; $j++) {
        $depth += ([regex]::Matches($lines[$j], '\{')).Count
        $depth -= ([regex]::Matches($lines[$j], '\}')).Count
        if ($depth -le 0) { $endIndex = $j; break }
    }
    if ($endIndex -lt 0) { continue }

    # 方法体里的“有效语句行”
    $statementIndexes = New-Object System.Collections.Generic.List[int]
    for ($j = $braceIndex + 1; $j -lt $endIndex; $j++) {
        $bodyTrim = $lines[$j].Trim()
        if ($bodyTrim -eq '') { continue }
        if ($bodyTrim.StartsWith('//') -or $bodyTrim.StartsWith('*') -or $bodyTrim.StartsWith('/*')) { continue }
        if ($bodyTrim -eq '{' -or $bodyTrim -eq '}') { continue }
        $statementIndexes.Add($j)
    }

    $firstAssertIndex = -1
    foreach ($j in $statementIndexes) {
        if (Test-IsAssertion $lines[$j]) { $firstAssertIndex = $j; break }
    }

    # Given：Then 之前的第一条“声明语句”
    $givenIndex = -1
    foreach ($j in $statementIndexes) {
        if ($firstAssertIndex -ge 0 -and $j -ge $firstAssertIndex) { break }
        if (Test-IsDeclaration $lines[$j]) { $givenIndex = $j; break }
    }

    # When：Then 之前第一条“非声明、非断言”的调用语句
    $whenIndex = -1
    foreach ($j in $statementIndexes) {
        if ($firstAssertIndex -ge 0 -and $j -ge $firstAssertIndex) { break }
        if (Test-IsDeclaration $lines[$j]) { continue }
        if (Test-IsAssertion $lines[$j]) { continue }
        if (Test-IsCall $lines[$j]) { $whenIndex = $j; break }
    }

    $displayName = $null
    for ($j = $i; $j -le $signatureIndex; $j++) {
        $m = [regex]::Match($lines[$j], '@DisplayName\("([^"]*)"\)')
        if ($m.Success) { $displayName = $m.Groups[1].Value; break }
    }

    $methods.Add([pscustomobject]@{
        TestLine      = $i
        EndLine       = $endIndex
        GivenLine     = $givenIndex
        WhenLine      = $whenIndex
        ThenLine      = $firstAssertIndex
        Intent        = if ($displayName) { $displayName } else { $methodName }
    })
}

# ---------- 第二遍：重建文件 ----------
$insertBefore = @{}
$skipLines = @{}
$indentByMethod = @{}

function Summarize([int]$lineIndex) {
    if ($lineIndex -lt 0) { return $null }
    $text = $lines[$lineIndex].Trim()
    if ($text.Length -gt 120) { $text = $text.Substring(0, 117) + '...' }
    return $text
}

foreach ($method in $methods) {
    $indent = [regex]::Match($lines[$method.TestLine], '^\s*').Value

    $givenText = Summarize $method.GivenLine
    $whenText = Summarize $method.WhenLine
    $thenText = Summarize $method.ThenLine
    if (-not $givenText) { $givenText = '无独立前置对象，输入在断言调用中直接给出' }
    if (-not $whenText) { $whenText = '调用被测方法（与断言同一语句）' }
    if (-not $thenText) { $thenText = '断言可观察结果（见方法体）' }

    # 上一版插入的“// 用例意图：...”整行替换为 Javadoc
    for ($k = $method.TestLine - 1; $k -ge [Math]::Max(0, $method.TestLine - 2); $k--) {
        if ($lines[$k].Trim().StartsWith('// 用例意图：')) { $skipLines[$k] = $true; break }
    }

    $javadoc = @()
    $javadoc += "$indent/**"
    $javadoc += "$indent * $($method.Intent)。"
    $javadoc += "$indent * Given: $givenText"
    $javadoc += "$indent * When:  $whenText"
    $javadoc += "$indent * Then:  $thenText"
    $javadoc += "$indent */"
    $insertBefore[$method.TestLine] = $javadoc

    if ($method.GivenLine -ge 0 -and -not $insertBefore.ContainsKey($method.GivenLine)) {
        $insertBefore[$method.GivenLine] = @("$indent    // Given：构造输入与期望值")
    }
    if ($method.WhenLine -ge 0 -and -not $insertBefore.ContainsKey($method.WhenLine)) {
        $insertBefore[$method.WhenLine] = @("$indent    // When：调用被测方法")
    }
    if ($method.ThenLine -ge 0 -and -not $insertBefore.ContainsKey($method.ThenLine)) {
        $insertBefore[$method.ThenLine] = @("$indent    // Then：断言可观察结果")
    }
}

$resultLines = New-Object System.Collections.Generic.List[string]
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($insertBefore.ContainsKey($i)) { foreach ($added in $insertBefore[$i]) { $resultLines.Add($added) } }
    if ($skipLines.ContainsKey($i)) { continue }
    $resultLines.Add($lines[$i])
}

[System.IO.File]::WriteAllLines($Output, $resultLines, (New-Object Text.UTF8Encoding($false)))
Write-Output ("{0}: 方法数={1} 原行={2} 新行={3}" -f (Split-Path $Source -Leaf), $methods.Count, $lines.Count, $resultLines.Count)
