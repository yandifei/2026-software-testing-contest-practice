[CmdletBinding()]
param(
    [string] $Maven = 'mvn'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

Push-Location -LiteralPath $projectRoot
try {
    # PIT needs compiled production and test classes, including after a move or clean.
    & $Maven '-B' 'clean' 'test' 'org.pitest:pitest-maven:1.15.3:mutationCoverage'
    if ($LASTEXITCODE -ne 0) {
        throw "PricingCalculator verification failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
