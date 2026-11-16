# 生成“基线项目未杀、但其他项目杀掉”的缺口清单，并统计各状态分布
param(
    [string]$Project = "card-approval",
    [string]$Baseline = "相互借鉴"
)

$root = "B:\软件测试大赛\省赛"
$providers = @("GPT\rewrite", "deepseek", "glm", "相互借鉴")

# 记录每条变异的 provider -> 状态 / killingTest
$records = @{}

foreach ($provider in $providers) {
    $xmlPath = Join-Path $root "$provider\$Project\target\pit-reports\mutations.xml"
    if (-not (Test-Path $xmlPath)) { continue }
    $raw = Get-Content $xmlPath -Raw
    # 用正则逐条解析，避免个别未转义字符导致整份 XML 解析失败
    $pattern = '(?s)<mutation detected=.(true|false). status=.([A-Z_]+). numberOfTestsRun=.(\d+).>(.*?)</mutation>'
    foreach ($match in [regex]::Matches($raw, $pattern)) {
        $body = $match.Groups[4].Value
        function Field($name) {
            $m = [regex]::Match($body, "<$name>(.*?)</$name>")
            if ($m.Success) { return $m.Groups[1].Value } else { return "" }
        }
        $idx = [regex]::Match($body, '(?s)<indexes>(.*?)</indexes>').Groups[1].Value
        $idxList = ([regex]::Matches($idx, '<index>(\d+)</index>') | ForEach-Object { $_.Groups[1].Value }) -join ','
        $key = "$(Field 'mutatedClass')|$(Field 'mutatedMethod')$(Field 'methodDescription')|L$(Field 'lineNumber')|$(Field 'mutator' -replace '.*\.','')|i$idxList"
        if (-not $records.ContainsKey($key)) {
            $records[$key] = [pscustomobject]@{
                Key      = $key
                Class    = Field 'mutatedClass'
                Method   = Field 'mutatedMethod'
                Line     = [int](Field 'lineNumber')
                Source   = Field 'sourceFile'
                Mutator  = (Field 'mutator' -replace '.*\.', '')
                Desc     = Field 'description'
                Status   = @{}
                Killer   = @{}
            }
        }
        $records[$key].Status[$provider] = $match.Groups[2].Value
        $killer = Field 'killingTest'
        if ($killer -ne "") { $records[$key].Killer[$provider] = $killer }
    }
}

$statusCounts = @{}
foreach ($provider in $providers) {
    $counts = @{ KILLED = 0; SURVIVED = 0; NO_COVERAGE = 0; TIMED_OUT = 0; RUN_ERROR = 0; ABSENT = 0 }
    foreach ($r in $records.Values) {
        $s = if ($r.Status.ContainsKey($provider)) { $r.Status[$provider] } else { 'ABSENT' }
        $counts[$s]++
    }
    $statusCounts[$provider] = $counts
}

Write-Output "Project=$Project  TotalMutations=$($records.Count)"
Write-Output ""
Write-Output "=== 各 provider 状态分布 ==="
foreach ($provider in $providers) {
    $c = $statusCounts[$provider]
    Write-Output ("{0,-14} KILLED={1,4} SURVIVED={2,4} NO_COV={3,4} TIMEOUT={4,3} RUN_ERR={5,2}" -f $provider, $c.KILLED, $c.SURVIVED, $c.NO_COVERAGE, $c.TIMED_OUT, $c.RUN_ERROR)
}

$gapRows = foreach ($r in $records.Values) {
    $baseStatus = if ($r.Status.ContainsKey($Baseline)) { $r.Status[$Baseline] } else { 'ABSENT' }
    if ($baseStatus -eq 'KILLED') { continue }
    $others = @()
    foreach ($p in $providers) {
        if ($p -eq $Baseline) { continue }
        $s = if ($r.Status.ContainsKey($p)) { $r.Status[$p] } else { 'ABSENT' }
        if ($s -eq 'KILLED') { $others += $p }
    }
    [pscustomobject]@{
        Class       = $r.Class -replace 'net\.mooctest\.', ''
        Method      = $r.Method
        Line        = $r.Line
        Mutator     = $r.Mutator
        Desc        = $r.Desc
        BaseStatus  = $baseStatus
        KilledElse  = ($others -join ',')
        OtherKiller = (($others | ForEach-Object { "$_ => $($r.Killer[$_])" }) -join ' | ')
    }
}

$outDir = Join-Path $root "相互借鉴2\_analysis"
$gapRows | Sort-Object Class, Line | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $outDir "$Project-gaps.csv")

$freeWins = @($gapRows | Where-Object { $_.KilledElse -ne '' })
Write-Output ""
Write-Output "=== 基线($Baseline)未杀: $($gapRows.Count) 条; 其中其他项目能杀(免费可借鉴): $($freeWins.Count) 条 ==="
$freeWins | Sort-Object Class, Line | ForEach-Object {
    Write-Output ("{0,-42} {1,-32} L{2,-4} {3,-24} base={4,-12} by={5}" -f $_.Class, $_.Method, $_.Line, $_.Mutator, $_.BaseStatus, $_.KilledElse)
}
