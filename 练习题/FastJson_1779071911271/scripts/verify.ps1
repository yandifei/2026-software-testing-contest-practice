[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

function Invoke-Maven {
    param([string[]] $Arguments)
    & mvn @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed with exit code $LASTEXITCODE : $($Arguments -join ' ')"
    }
}

Push-Location -LiteralPath $projectRoot
try {
    Invoke-Maven -Arguments @('-B', 'clean', 'test')
    Invoke-Maven -Arguments @('-B', 'checkstyle:check')
    Invoke-Maven -Arguments @('-B', 'org.pitest:pitest-maven:mutationCoverage')

    [xml] $coverage = Get-Content -Raw -LiteralPath 'target/site/jacoco/jacoco.xml'
    [xml] $pit = Get-Content -Raw -LiteralPath 'target/pit-reports/mutations.xml'
    $mutants = @($pit.mutations.mutation)
    $killed = @($mutants | Where-Object status -eq 'KILLED').Count
    $detected = @($mutants | Where-Object detected -eq 'true').Count
    $suites = @(Get-ChildItem 'target/surefire-reports/TEST-*.xml' | ForEach-Object {
        [xml] $report = Get-Content -Raw -LiteralPath $_.FullName
        [pscustomobject]@{
            Name = $report.testsuite.name
            Tests = [int] $report.testsuite.tests
            Failures = [int] $report.testsuite.failures
            Errors = [int] $report.testsuite.errors
            Skipped = [int] $report.testsuite.skipped
            Seconds = [double] $report.testsuite.time
        }
    })
    $counters = @($coverage.SelectNodes('/report/counter') | ForEach-Object {
        [pscustomobject]@{
            Type = $_.type
            Covered = [int] $_.covered
            Missed = [int] $_.missed
            Percent = [math]::Round(100 * [int] $_.covered / ([int] $_.covered + [int] $_.missed), 4)
        }
    })
    $summary = [ordered]@{
        GeneratedAt = (Get-Date).ToString('o')
        Tests = ($suites | Measure-Object Tests -Sum).Sum
        Failures = ($suites | Measure-Object Failures -Sum).Sum
        Errors = ($suites | Measure-Object Errors -Sum).Sum
        Skipped = ($suites | Measure-Object Skipped -Sum).Sum
        TestSeconds = ($suites | Measure-Object Seconds -Sum).Sum
        Coverage = $counters
        Mutations = [ordered]@{
            Total = $mutants.Count
            Killed = $killed
            TimedOut = @($mutants | Where-Object status -eq 'TIMED_OUT').Count
            Survived = @($mutants | Where-Object status -eq 'SURVIVED').Count
            NoCoverage = @($mutants | Where-Object status -eq 'NO_COVERAGE').Count
            StrictKillPercent = [math]::Round(100 * $killed / $mutants.Count, 4)
            PitDetectedPercent = [math]::Round(100 * $detected / $mutants.Count, 4)
        }
        Suites = $suites
    }
    $summary | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 'target/verification-summary.json'
    $mutants | Where-Object status -ne 'KILLED' | Select-Object status, sourceFile,
        mutatedMethod, lineNumber, mutator, description |
        Export-Csv -NoTypeInformation -Encoding utf8 'target/mutation-review.csv'
    Get-ChildItem 'src/main/java/net/mooctest/*.java' | Get-FileHash -Algorithm SHA256 |
        Select-Object Path, Hash | Export-Csv -NoTypeInformation -Encoding utf8 'target/source-hashes.csv'
    $summary | ConvertTo-Json -Depth 6
} finally {
    Pop-Location
}
