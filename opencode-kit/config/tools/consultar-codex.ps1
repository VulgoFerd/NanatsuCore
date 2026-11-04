[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Prompt,

    # Modelos tentados na ordem. Usa o primeiro que responder.
    # Descubra os modelos atuais da conta com: codex debug models
    [string[]]$Model = @('gpt-5.6-terra', 'gpt-5.6-luna', 'gpt-reserve'),

    [string]$WorkDir = $(Join-Path $env:TEMP 'opencode\codex-scratch'),

    [switch]$Ephemeral,

    [switch]$ShowLog
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)
try { [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false) } catch {}

function Get-CodexExe {
    $binRoot = Join-Path $env:LOCALAPPDATA 'OpenAI\Codex\bin'
    $candidates = @()
    if (Test-Path $binRoot) {
        # O diretorio tem nome com hash e muda quando o Codex atualiza: pega o mais recente.
        $candidates += Get-ChildItem $binRoot -Directory -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            ForEach-Object { Join-Path $_.FullName 'codex.exe' }
    }
    $candidates += (Join-Path $env:USERPROFILE '.codex\.sandbox-bin\codex.exe')
    $candidates += (Join-Path $env:USERPROFILE '.codex\plugins\.plugin-appserver\codex.exe')
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    $onPath = Get-Command codex -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    throw 'codex.exe nao encontrado. Instale o Codex CLI ou ajuste este script.'
}

$codexExe = Get-CodexExe
if (-not (Test-Path $WorkDir)) { New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null }

$tried    = @()
$answered = $false
$lastLog  = ''

foreach ($modelName in $Model) {

    $stamp    = [guid]::NewGuid().ToString('N')
    $respFile = Join-Path $env:TEMP "codex-msg-$stamp.txt"
    $logFile  = Join-Path $env:TEMP "codex-log-$stamp.txt"

    $codexArgs = @(
        'exec'
        '-m', $modelName
        '--sandbox', 'read-only'
        '--skip-git-repo-check'
        '--ignore-user-config'
        '-C', $WorkDir
        '-o', $respFile
    )
    if ($Ephemeral) { $codexArgs += '--ephemeral' }
    $codexArgs += $Prompt

    # O Codex escreve avisos no stderr; com ErrorActionPreference=Stop isso viraria excecao.
    $previousEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $codexExe @codexArgs > $logFile 2>&1 } catch { }
    finally { $ErrorActionPreference = $previousEap }

    $text = ''
    if (Test-Path $respFile) { $text = Get-Content $respFile -Raw -Encoding UTF8 }

    $log = ''
    if (Test-Path $logFile) { $log = Get-Content $logFile -Raw }

    if ($ShowLog) {
        Write-Host "----- log do modelo $modelName -----" -ForegroundColor DarkGray
        Write-Host $log
    }

    if ($text -and $text.Trim().Length -gt 0) {
        Write-Host $text.TrimEnd()
        $answered = $true
        Remove-Item $respFile, $logFile -Force -ErrorAction SilentlyContinue
        break
    }

    Remove-Item $respFile, $logFile -Force -ErrorAction SilentlyContinue
    $tried   += $modelName
    $lastLog  = $log

    # O log quebra linha no meio da mensagem, entao usamos \s+ em vez de espaco literal.
    if ($log -match '(?i)not\s+supported\s+when\s+using\s+Codex') {
        Write-Host "[consultar-codex] '$modelName' nao vale mais para conta ChatGPT; tentando o proximo..." -ForegroundColor DarkYellow
    } else {
        Write-Host "[consultar-codex] '$modelName' nao respondeu; tentando o proximo..." -ForegroundColor DarkYellow
    }
}

if (-not $answered) {
    Write-Host "[consultar-codex] nenhum modelo respondeu (tentei: $($tried -join ', '))." -ForegroundColor Red
    if ($lastLog) {
        Write-Host "[consultar-codex] ultimas linhas do log:" -ForegroundColor DarkGray
        Write-Host (($lastLog -split "`r?`n" | Select-Object -Last 12) -join [Environment]::NewLine)
    }
    Write-Host "[consultar-codex] rode 'codex debug models' para ver os modelos atuais da conta." -ForegroundColor DarkGray
}
