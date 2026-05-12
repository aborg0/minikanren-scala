$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$distDir = Join-Path $repoRoot 'site-dist'
$websiteSource = Join-Path $repoRoot 'website'
$websiteTargetDir = Join-Path $repoRoot 'website-demo\target\scala-3.8.3'

if (Test-Path $distDir) {
  Remove-Item -Recurse -Force $distDir
}

New-Item -ItemType Directory -Path $distDir | Out-Null

Push-Location $repoRoot
$previousSbtOpts = $env:SBT_OPTS
$env:SBT_OPTS = '-Dsbt.server.autostart=false'
try {
  sbt -batch "++3.8.3" "miniKanrenExamplesJVM/mdoc" "miniKanrenScala3DSLCrossJVM/mdoc" "miniKanrenLangCrossJVM/mdoc" "miniKanrenWebsite/fullLinkJS"
} finally {
  $env:SBT_OPTS = $previousSbtOpts
  Pop-Location
}

Copy-Item (Join-Path $websiteSource '*') $distDir -Recurse -Force

$siteApp = Get-ChildItem -Path $websiteTargetDir -Filter 'main.js' -Recurse |
  Where-Object { $_.FullName -match 'opt' } |
  Select-Object -First 1

if (-not $siteApp) {
  throw 'Could not locate the linked website Scala.js output.'
}

Copy-Item $siteApp.FullName (Join-Path $distDir 'assets\site-app.js') -Force

$docs = @(
  @{ Source = Join-Path $repoRoot 'examples\jvm\target\mdoc'; Target = Join-Path $distDir 'docs\examples' },
  @{ Source = Join-Path $repoRoot 'scala3dsl\.jvm\target\mdoc'; Target = Join-Path $distDir 'docs\scala3dsl' },
  @{ Source = Join-Path $repoRoot 'lang\.jvm\target\mdoc'; Target = Join-Path $distDir 'docs\lang' }
)

foreach ($doc in $docs) {
  if (-not (Test-Path $doc.Source)) {
    throw "Missing generated docs directory: $($doc.Source)"
  }

  New-Item -ItemType Directory -Path $doc.Target -Force | Out-Null
  Copy-Item (Join-Path $doc.Source '*') $doc.Target -Recurse -Force
}

Write-Host "Built site artifact at $distDir"
