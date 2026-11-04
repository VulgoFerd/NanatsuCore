<#
.SYNOPSIS
    Atualiza o kit a partir da sua configuracao atual, removendo segredos.

.DESCRIPTION
    Copia skills, agentes, comandos, plugins e ferramentas para .\config e regenera
    .\config\opencode.jsonc com:
      - token do PixelLab trocado por placeholder
      - caminho absoluto trocado por __OPENCODE_HOME__
      - blocos opcionais (pixellab / free-claude-code) marcados para o instalador decidir

    Nunca copia: service.json, backups, node_modules, .git.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\exportar.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$KitRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$SrcDir  = Join-Path $env:USERPROFILE '.config\opencode'
$OutCfg  = Join-Path $KitRoot 'config'

if (-not (Test-Path (Join-Path $SrcDir 'opencode.jsonc'))) {
    throw "Configuracao nao encontrada em $SrcDir"
}

# --- arquivos e pastas (sem segredos) --------------------------------------
New-Item -ItemType Directory -Force $OutCfg | Out-Null
foreach ($item in @('agents', 'command', 'skills', 'plugins', 'tools', 'cli.json', 'AGENTS.md')) {
    $src = Join-Path $SrcDir $item
    if (-not (Test-Path $src)) { continue }
    $dst = Join-Path $OutCfg $item
    if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
    Copy-Item $src $dst -Recurse -Force
}
Write-Host "arquivos copiados para $OutCfg" -ForegroundColor Green

# --- opencode.jsonc saneado ------------------------------------------------
function Find-BlockEnd([string]$text, [int]$openIndex) {
    $depth = 0
    for ($i = $openIndex; $i -lt $text.Length; $i++) {
        if ($text[$i] -eq '{') { $depth++ }
        elseif ($text[$i] -eq '}') {
            $depth--
            if ($depth -eq 0) { return $i }
        }
    }
    return -1
}

function Add-Markers([string]$text, [string]$key, [string]$tag, [string]$indent) {
    if ($text -match "// <$tag>") { return $text }

    $keyIndex = $text.IndexOf("`"$key`":")
    if ($keyIndex -lt 0) { return $text }

    $openIndex = $text.IndexOf('{', $keyIndex)
    $closeIndex = Find-BlockEnd $text $openIndex
    if ($openIndex -lt 0 -or $closeIndex -lt 0) { return $text }

    $lineStart = $text.LastIndexOf("`n", $keyIndex) + 1
    $text = $text.Insert($lineStart, "$indent// <$tag>`n")

    $closeIndex = Find-BlockEnd $text ($text.IndexOf('{', $text.IndexOf("`"$key`":")))
    $lineEnd = $text.IndexOf("`n", $closeIndex)
    if ($lineEnd -lt 0) { $lineEnd = $text.Length }
    return $text.Insert($lineEnd, "`n$indent// </$tag>")
}

$json = Get-Content (Join-Path $SrcDir 'opencode.jsonc') -Raw

$json = [regex]::Replace($json, 'Bearer\s+[0-9a-fA-F\-]{20,}', 'Bearer COLE_SEU_TOKEN_PIXELLAB_AQUI')
$json = $json.Replace(($SrcDir -replace '\\', '/'), '__OPENCODE_HOME__')
$json = $json.Replace(($SrcDir -replace '\\', '\\\\'), '__OPENCODE_HOME__')

$json = Add-Markers $json 'pixellab' 'pixellab' '      '
$json = Add-Markers $json 'free-claude-code' 'fcc' '    '

Set-Content (Join-Path $OutCfg 'opencode.jsonc') $json -Encoding UTF8
Write-Host "opencode.jsonc saneado (sem token, sem caminho absoluto)" -ForegroundColor Green

# --- checagem de vazamento ------------------------------------------------
$leaks = Select-String -Path (Join-Path $OutCfg 'opencode.jsonc') -Pattern 'Bearer (?!COLE_)' -AllMatches
if ($leaks) { Write-Host "ATENCAO: ainda existe um Bearer real no config!" -ForegroundColor Red }
Write-Host ""
Write-Host "Pronto. Compacte a pasta do kit e envie." -ForegroundColor White
