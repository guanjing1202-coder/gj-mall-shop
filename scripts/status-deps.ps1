$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'status-all.ps1') |
    Out-String |
    ForEach-Object {
        $_ -split "`r?`n" | Where-Object {
            $_ -match '^(Name|----|MySQL|Redis|MongoDB|RabbitMQ|\s*$)'
        }
    }
