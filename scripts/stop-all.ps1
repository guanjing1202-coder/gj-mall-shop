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

    $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $pids) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping $Name process $($process.ProcessName) ($processId)"
            Stop-Process -Id $processId -Force
        }
    }
}

# Keep database middleware running by default. Stop only project app processes.
Stop-PortProcess 'Backend' 8080
Stop-PortProcess 'PC Web' 5173
Stop-PortProcess 'Admin Web' 5174
Stop-PortProcess 'Uni H5' 5175

& (Join-Path $PSScriptRoot 'status-all.ps1')
