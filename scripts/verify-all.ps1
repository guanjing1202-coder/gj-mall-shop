$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Name" -ForegroundColor Cyan
    & $Command
}

function Use-RepoJavaAndMaven {
    $jdkRoot = Join-Path $root '.codex-tools\jdk8'
    $mavenBin = Join-Path $root '.codex-tools\apache-maven-3.9.9\bin'
    if (Test-Path $jdkRoot) {
        $jdk = Get-ChildItem $jdkRoot -Directory | Select-Object -First 1
        if ($jdk) {
            $env:JAVA_HOME = $jdk.FullName
            $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        }
    }
    if (Test-Path $mavenBin) {
        $env:Path = "$mavenBin;$env:Path"
    }
}

Invoke-Step 'Check service status' {
    & (Join-Path $PSScriptRoot 'status-all.ps1')
}

Invoke-Step 'Run backend focused tests' {
    Use-RepoJavaAndMaven
    Push-Location (Join-Path $root 'gj-mall-server')
    try {
        mvn @(
            '-pl',
            'gj-mall-pay,gj-mall-order,gj-mall-admin-api',
            '-am',
            '-Dtest=PayServiceImplTest,AfterSaleServiceImplTest,AdminPaymentCallbackVOTest',
            '-DfailIfNoTests=false',
            'test'
        )
    } finally {
        Pop-Location
    }
}

Invoke-Step 'Package backend' {
    Use-RepoJavaAndMaven
    Push-Location (Join-Path $root 'gj-mall-server')
    try {
        mvn -DskipTests package
    } finally {
        Pop-Location
    }
}

Invoke-Step 'Run PC web tests' {
    Push-Location (Join-Path $root 'gj-mall-web')
    try {
        npm test
    } finally {
        Pop-Location
    }
}

Invoke-Step 'Run admin web tests' {
    Push-Location (Join-Path $root 'gj-mall-admin')
    try {
        npm test
    } finally {
        Pop-Location
    }
}

Invoke-Step 'Run Uni H5 tests and build' {
    Push-Location (Join-Path $root 'gj-mall-uni')
    try {
        npm test
        npm run build:h5
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host 'Verification completed.' -ForegroundColor Green
