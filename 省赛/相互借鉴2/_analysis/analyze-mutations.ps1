# 汇总四个参考项目的 PIT 变异结果，找出所有项目都未杀死的变异体
param([string]$Project = "card-approval")

$root = "B:\软件测试大赛\省赛"
$providers = @("GPT\rewrite", "deepseek", "glm", "相互借鉴")
$rows = @{}
$projectOrder = @()

foreach ($provider in $providers) {
    $xmlPath = Join-Path $root "$provider\$Project\target\pit-reports\mutations.xml"
    if (-not (Test-Path $xmlPath)) { Write-Output "SKIP $provider : no mutations.xml"; continue }
    $doc = [xml](Get-Content $xmlPath -Raw)
    $projectOrder += $provider
    foreach ($m in $doc.mutations.mutation) {
        $key = "$($m.mutatedClass)|$($m.mutatedMethod)$($m.methodDescription)|line$($m.lineNumber)|$($m.mutator)|idx$(($m.indexes.index -join ','))|blk$(($m.blocks.block -join ','))"
        if (-not $rows.ContainsKey($key)) {
            $rows[$key] = [pscustomobject]@{
                Key        = $key
                Class      = $m.mutatedClass
                Method     = $m.mutatedMethod
                Line       = $m.lineNumber
                Source     = $m.sourceFile
                Mutator    = ($m.mutator -replace 'org\.pitest\.mutationtest\.engine\.gregor\.mutators\.', '')
                Desc       = $m.description
                Status     = @{}
            }
        }
        $rows[$key].Status[$provider] = $m.status
    }
}

$all = $rows.Values | Sort-Object Class, Line
$summary = foreach ($r in $all) {
    $statuses = foreach ($p in $projectOrder) { if ($r.Status.ContainsKey($p)) { $r.Status[$p] } else { "ABSENT" } }
    $killedCount = ($statuses | Where-Object { $_ -eq 'KILLED' }).Count
    $timedOut = ($statuses | Where-Object { $_ -eq 'TIMED_OUT' }).Count
    [pscustomobject]@{
        Class       = $r.Class
        Method      = $r.Method
        Line        = $r.Line
        Mutator     = $r.Mutator
        Desc        = $r.Desc
        KilledBy    = $killedCount
        TimedOutBy  = $timedOut
        Detail      = ($statuses -join '/')
    }
}

$outDir = Join-Path $root "相互借鉴2\_analysis"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$summary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $outDir "$Project-union.csv")

Write-Output "Project: $Project"
Write-Output "Providers: $($projectOrder -join ', ')"
Write-Output "Union mutations: $($summary.Count)"
Write-Output "Killed by all 4: $(($summary | Where-Object KilledBy -eq 4).Count)"
Write-Output "Killed by none: $(($summary | Where-Object KilledBy -eq 0).Count)"
Write-Output "Killed by exactly 1: $(($summary | Where-Object KilledBy -eq 1).Count)"
Write-Output ""
Write-Output "=== GROUPED BY MUTATOR (survivors in >=1 project) ==="
$summary | Group-Object Mutator | Sort-Object Count -Descending | Select-Object Count, Name | Format-Table -AutoSize
Write-Output "=== NOBODY KILLED ==="
$summary | Where-Object KilledBy -eq 0 | Select-Object Class, Method, Line, Mutator, Desc | Format-Table -AutoSize -Wrap
Write-Output "=== ONLY ONE KILLED ==="
$summary | Where-Object KilledBy -eq 1 | Select-Object Class, Method, Line, Mutator, Desc, Detail | Format-Table -AutoSize -Wrap
