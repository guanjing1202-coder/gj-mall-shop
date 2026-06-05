$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$runDir = Join-Path $root '.codex-run'
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

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

function Start-IfClosed {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$ArgumentList = @(),
        [string]$WorkingDirectory = (Split-Path -Parent $FilePath),
        [hashtable]$Environment = @{}
    )

    if (Test-PortOpen $Port) {
        Write-Host "$Name already running on port $Port"
        return
    }
    if (-not (Test-Path $FilePath)) {
        Write-Warning "$Name executable not found: $FilePath"
        return
    }

    foreach ($key in $Environment.Keys) {
        Set-Item -Path "Env:$key" -Value $Environment[$key]
    }

    Start-Process -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput (Join-Path $runDir "$Name.out.log") `
        -RedirectStandardError (Join-Path $runDir "$Name.err.log") `
        -WindowStyle Hidden
    Write-Host "Started $Name"
}

if (-not (Test-PortOpen 3306)) {
    Write-Warning 'MySQL is not listening on 127.0.0.1:3306. Start your local MySQL service manually, then rerun status.'
} else {
    Write-Host 'MySQL already running on port 3306'
}

Start-IfClosed 'redis' 6379 'D:\dev\redis\redis-server.exe' @('D:\dev\redis\redis.windows.conf') 'D:\dev\redis'
Start-IfClosed 'mongodb' 27017 'D:\dev\mongodb\bin\mongod.exe' @('--config', 'D:\dev\mongodb\mongod.cfg') 'D:\dev\mongodb'
Start-IfClosed 'rabbitmq' 5672 'D:\dev\rabbitmq\rabbitmq_server-3.13.7\sbin\rabbitmq-server.bat' @() 'D:\dev\rabbitmq\rabbitmq_server-3.13.7\sbin' @{
    ERLANG_HOME = 'D:\dev\erlang'
    RABBITMQ_BASE = 'D:\dev\rabbitmq\data'
}

& (Join-Path $PSScriptRoot 'status-deps.ps1')
