$ErrorActionPreference = 'Stop'

function Test-Port {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][int]$Port,
        [string]$Url = ''
    )

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
    [PSCustomObject]@{
        Name = $Name
        Port = $Port
        Open = $open
        Url = $Url
    }
}

$services = @(
    Test-Port 'MySQL' 3306
    Test-Port 'Redis' 6379
    Test-Port 'MongoDB' 27017
    Test-Port 'RabbitMQ' 5672
    Test-Port 'Backend' 8080 'http://127.0.0.1:8080/api/v1/ping'
    Test-Port 'PC Web' 5173 'http://127.0.0.1:5173'
    Test-Port 'Admin Web' 5174 'http://127.0.0.1:5174'
    Test-Port 'Uni H5' 5175 'http://127.0.0.1:5175'
)

$services | Format-Table -AutoSize
