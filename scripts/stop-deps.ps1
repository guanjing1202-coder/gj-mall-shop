$ErrorActionPreference = 'Stop'

function Stop-PortProcess {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][int]$Port
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $connections) {
        Write-Host "$Name is not listening on port $Port"
        return
    }

    foreach ($processId in ($connections | Select-Object -ExpandProperty OwningProcess -Unique)) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping $Name process $($process.ProcessName) ($processId)"
            Stop-Process -Id $processId -Force
        }
    }
}

Write-Warning 'MySQL is not stopped by this script because its local installation/service path is not managed by the repository.'
Stop-PortProcess 'Redis' 6379
Stop-PortProcess 'MongoDB' 27017
Stop-PortProcess 'RabbitMQ' 5672

& (Join-Path $PSScriptRoot 'status-deps.ps1')
