<#
.SYNOPSIS
    Instala o OpenCode + kit de ferramentas (skills, agentes, MCPs, plugins) numa maquina Windows.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\instalar.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\instalar.ps1 -PixelLabToken "seu-token"

.NOTES
    Testado com: Windows 11, Node 26, OpenCode v2.2.5.
    Roda sem admin. Instala Node/Git via winget se faltarem.
#>
[CmdletBinding()]
param(
    # Token do PixelLab (opcional). Sem ele, o MCP pixellab nao e configurado.
    [string]$PixelLabToken = "",

    # Inclui o provider "Free Claude Code" (proxy local na porta 8082). So use se tiver o proxy rodando.
    [switch]$ComFCC
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$KitRoot  = Split-Path -Parent $MyInvocation.MyCommand.Path
$CfgDir   = Join-Path $env:USERPROFILE '.config\opencode'
$Stamp    = Get-Date -Format 'yyyy-MM-dd-HHmm'

function Write-Step($n, $text) { Write-Host ""; Write-Host "[$n] $text" -ForegroundColor Cyan }
function Write-Ok($text)       { Write-Host "    OK  $text" -ForegroundColor Green }
function Write-Warn2($text)    { Write-Host "    !   $text" -ForegroundColor Yellow }

function Update-Path {
    $machine = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $user    = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = "$machine;$user"
}

function Test-Cmd($name) { [bool](Get-Command $name -ErrorAction SilentlyContinue) }

function Ensure-Winget {
    if (-not (Test-Cmd winget)) {
        throw "winget nao encontrado. Instale Node.js LTS e Git manualmente e rode este script de novo."
    }
}

function Remove-MarkedBlock([string]$text, [string]$tag) {
    $pattern = "(?ms)^[ \t]*// <$tag>\r?\n.*?^[ \t]*// </$tag>\r?\n"
    return [regex]::Replace($text, $pattern, "")
}

Write-Host ""
Write-Host "  OpenCode Kit - instalacao" -ForegroundColor White

# ---------------------------------------------------------------- 1. Node.js
Write-Step 1 "Node.js 20+"
if (-not (Test-Cmd node)) {
    Ensure-Winget
    Write-Host "    instalando Node.js LTS via winget..."
    winget install --id OpenJS.NodeJS.LTS -e --accept-source-agreements --accept-package-agreements --silent | Out-Null
    Update-Path
}
$nodeVersion = (& node --version).TrimStart('v')
$nodeMajor   = [int]($nodeVersion -split '\.')[0]
if ($nodeMajor -lt 20) { throw "Node $nodeVersion e antigo. Instale Node 20+ e rode de novo." }
Write-Ok "Node $nodeVersion / npm $(& npm --version)"

# ---------------------------------------------------------------- 2. Git
Write-Step 2 "Git"
if (-not (Test-Cmd git)) {
    Ensure-Winget
    Write-Host "    instalando Git via winget..."
    winget install --id Git.Git -e --accept-source-agreements --accept-package-agreements --silent | Out-Null
    Update-Path
}
if (-not (Test-Cmd git)) { throw "Git nao encontrado depois da instalacao. Abra um terminal novo e rode de novo." }
Write-Ok (& git --version)

# ---------------------------------------------------------------- 3. OpenCode
Write-Step 3 "OpenCode CLI (@opencode/cli)"
if (Test-Cmd opencode) { Write-Host "    ja existe, atualizando para a ultima versao..." }

# npm 12+ bloqueia postinstall por padrao; sem ele o binario do OpenCode nao funciona.
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    & npm install -g @opencode/cli --allow-scripts=@opencode/cli --no-audit --no-fund 2>&1 |
        ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
    if ($LASTEXITCODE -ne 0) {
        Write-Warn2 "npm recusou --allow-scripts (versao antiga?), tentando sem o flag..."
        & npm install -g @opencode/cli --no-audit --no-fund 2>&1 |
            ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
    }
} finally {
    $ErrorActionPreference = $prevEap
}
Update-Path
if (-not (Test-Cmd opencode)) { throw "opencode nao entrou no PATH. Abra um terminal novo e rode de novo." }

$ocVersion = $null
try { $ocVersion = (& opencode --version 2>&1 | Out-String).Trim() } catch { $ocVersion = $null }
if (-not $ocVersion -or $ocVersion -notmatch '\d+\.\d+') {
    throw "opencode foi instalado mas nao executou. Rode: npm install -g @opencode/cli --allow-scripts=@opencode/cli"
}
Write-Ok $ocVersion

# O OpenCode decide onde fica a config; perguntamos a ele em vez de adivinhar.
try {
    $detected = (& opencode debug paths 2>&1 | Where-Object { $_ -match '^config\s+' })
    if ($detected) { $CfgDir = ($detected -replace '^config\s+', '').Trim() }
} catch { }

# ---------------------------------------------------------------- 4. Backup
Write-Step 4 "Backup da configuracao atual"
Write-Host "    destino: $CfgDir" -ForegroundColor DarkGray
if (Test-Path $CfgDir) {
    $backup = "$CfgDir.bak-$Stamp"
    Copy-Item $CfgDir $backup -Recurse -Force
    Write-Ok "backup em $backup"
} else {
    New-Item -ItemType Directory -Force $CfgDir | Out-Null
    Write-Ok "configuracao nova (nada para backup)"
}

# ---------------------------------------------------------------- 5. Arquivos
Write-Step 5 "Copiando skills, agentes, comandos, plugins e ferramentas"
foreach ($item in @('agents', 'command', 'skills', 'plugins', 'tools', 'cli.json', 'AGENTS.md', 'opencode.jsonc')) {
    $src = Join-Path $KitRoot "config\$item"
    if (Test-Path $src) { Copy-Item $src $CfgDir -Recurse -Force }
}
if (-not (Test-Path (Join-Path $CfgDir 'opencode.jsonc'))) { throw "opencode.jsonc nao foi copiado do kit." }
Write-Ok "$((Get-ChildItem (Join-Path $CfgDir 'skills') -Directory).Count) skills, $((Get-ChildItem (Join-Path $CfgDir 'agents') -File).Count) agentes, $((Get-ChildItem (Join-Path $CfgDir 'command') -Recurse -File).Count) comandos"

# ---------------------------------------------------------------- 6. Vendor
Write-Step 6 "Repositorios de skills (vendor)"
$repos = @{
    'claude-octopus'          = 'https://github.com/nyldn/claude-octopus.git'
    'andrej-karpathy-skills'  = 'https://github.com/multica-ai/andrej-karpathy-skills.git'
    'i-have-adhd'             = 'https://github.com/ayghri/i-have-adhd.git'
}
$vendorDir = Join-Path $CfgDir 'vendor'
New-Item -ItemType Directory -Force $vendorDir | Out-Null
foreach ($name in $repos.Keys) {
    $dest = Join-Path $vendorDir $name
    if (Test-Path $dest) { Write-Ok "$name (ja existe)"; continue }
    & git clone --depth 1 --quiet $repos[$name] $dest
    if ($LASTEXITCODE -ne 0) { throw "falhou clonar $name" }
    Write-Ok $name
}

# ---------------------------------------------------------------- 7. Config
Write-Step 7 "Ajustando opencode.jsonc"
$jsonPath = Join-Path $CfgDir 'opencode.jsonc'
$json = Get-Content $jsonPath -Raw

$json = $json.Replace('__OPENCODE_HOME__', ($CfgDir -replace '\\', '/'))

if ($PixelLabToken) {
    $json = $json.Replace('COLE_SEU_TOKEN_PIXELLAB_AQUI', $PixelLabToken)
    Write-Ok "MCP pixellab configurado"
} else {
    $json = Remove-MarkedBlock $json 'pixellab'
    Write-Warn2 "MCP pixellab desativado (sem token). Rode de novo com -PixelLabToken se quiser."
}

if (-not $ComFCC) {
    $json = Remove-MarkedBlock $json 'fcc'
    $json = [regex]::Replace($json, '(?m)^[ \t]*"providers"\s*:\s*\{\s*\},?[ \t]*\r?\n', '')
    Write-Warn2 "provider Free Claude Code desativado (use -ComFCC se tiver o proxy local)."
}

Set-Content $jsonPath $json -Encoding UTF8
Write-Ok "caminhos, MCPs e provider ajustados"

# ---------------------------------------------------------------- 8. Verificacao
Write-Step 8 "Verificacao"
try { Write-Ok (& opencode --version) } catch { Write-Warn2 "opencode nao respondeu: $($_.Exception.Message)" }
Write-Host "    checando MCPs (pode demorar na primeira vez, baixa pacotes npx)..."
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $mcpOut = (& opencode mcp list 2>&1 | Out-String).Trim()
    if ($mcpOut -match 'connected|✓|needs auth') {
        $mcpOut -split "`r?`n" | ForEach-Object { Write-Host "    $_" }
    } else {
        Write-Warn2 "checagem de MCP adiada. Rode depois: opencode mcp list"
    }
} catch {
    Write-Warn2 "nao consegui listar MCPs agora. Depois rode manualmente: opencode mcp list"
} finally {
    $ErrorActionPreference = $prevEap
}

# ---------------------------------------------------------------- Fim
Write-Host ""
Write-Host "  Instalacao concluida." -ForegroundColor Green
Write-Host ""
Write-Host "  Faltam 2 passos manuais:" -ForegroundColor White
Write-Host "   1. Login no provedor de modelos:  " -NoNewline; Write-Host "opencode auth login" -ForegroundColor Cyan
Write-Host "      Escolha OpenRouter (ou Anthropic/OpenAI). Sem isso o agente nao responde." -ForegroundColor DarkGray
Write-Host "   2. Abra uma pasta de projeto, rode " -NoNewline; Write-Host "opencode" -ForegroundColor Cyan -NoNewline; Write-Host " e cole o conteudo de PROMPT.md"
Write-Host ""
Write-Host "  Config: $CfgDir" -ForegroundColor DarkGray
Write-Host ""
