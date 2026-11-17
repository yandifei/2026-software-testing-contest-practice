$ErrorActionPreference = 'SilentlyContinue'
$base = "B:\软件测试大赛\省赛"
$snap = Join-Path $base "_snapshot"
New-Item -ItemType Directory -Path $snap -Force | Out-Null
$targets = @{
  "card-approval" = "deepseek\card-approval\src\test\java\net\mooctest\StudentTest.java"
  "math-assessment" = "deepseek\math-assessment\src\test\java\net\mooctest\StudentTest.java"
}
$lastHash = @{}
while ($true) {
  foreach ($k in $targets.Keys) {
    $f = Join-Path $base $targets[$k]
    if (-not (Test-Path $f)) { continue }
    $h = (Get-FileHash $f -Algorithm SHA256).Hash
    if ($lastHash[$k] -ne $h) {
      $dest = Join-Path $snap ("{0}-{1}.java" -f $k, (Get-Date -Format "HHmmss"))
      Copy-Item $f $dest -Force
      $lastHash[$k] = $h
      Write-Output ("{0} snapshot {1} bytes -> {2}" -f (Get-Date -Format "HH:mm:ss"), (Get-Item $f).Length, (Split-Path $dest -Leaf))
    }
  }
  Start-Sleep -Seconds 20
}
