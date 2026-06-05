$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$runDir = Join-Path $root '.codex-run'
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

& (Join-Path $PSScriptRoot 'start-deps.ps1')

function Test-PortOpen {
    param([int]$Port)
    $client = [System.Net.Sockets.TcpClient]::new()
    $async = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
    $open = $async.AsyncWaitHandle.WaitOne(500, $false)
    if ($open) {
        try {
            $client.EndConnect($async)
        } catch {
            $open = $false
        }
    }
    $client.Close()
    return $open
}

function Start-NodeApp {
    param(
        [string]$Name,
        [string]$Directory,
        [string[]]$Arguments,
        [int]$Port
    )
    if (Test-PortOpen $Port) {
        Write-Host "$Name already running on port $Port"
        return
    }
    Start-Process -FilePath 'npm.cmd' `
        -ArgumentList $Arguments `
        -WorkingDirectory $Directory `
        -RedirectStandardOutput (Join-Path $runDir "$Name.out.log") `
        -RedirectStandardError (Join-Path $runDir "$Name.err.log") `
        -WindowStyle Hidden
    Write-Host "Started $Name"
}

if (-not (Test-PortOpen 8080)) {
    $jar = Join-Path $root 'gj-mall-server\gj-mall-app\target\gj-mall-app.jar'
    if (-not (Test-Path $jar)) {
        Push-Location (Join-Path $root 'gj-mall-server')
        try {
            mvn -DskipTests package
        } finally {
            Pop-Location
        }
    }
    Start-Process -FilePath 'java' `
        -ArgumentList @('-jar', 'target\gj-mall-app.jar') `
        -WorkingDirectory (Join-Path $root 'gj-mall-server\gj-mall-app') `
        -RedirectStandardOutput (Join-Path $runDir 'backend.out.log') `
        -RedirectStandardError (Join-Path $runDir 'backend.err.log') `
        -WindowStyle Hidden
    Write-Host 'Started backend'
} else {
    Write-Host 'Backend already running on port 8080'
}

Start-NodeApp 'web' (Join-Path $root 'gj-mall-web') @('run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173') 5173
Start-NodeApp 'admin' (Join-Path $root 'gj-mall-admin') @('run', 'dev', '--', '--host', '127.0.0.1', '--port', '5174') 5174
Start-NodeApp 'uni' (Join-Path $root 'gj-mall-uni') @('run', 'dev:h5', '--', '--host', '127.0.0.1', '--port', '5175') 5175

& (Join-Path $PSScriptRoot 'status-all.ps1')
