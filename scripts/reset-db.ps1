#Requires -Version 5.1
$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$SchemaSql = Join-Path $ProjectRoot 'sql\schema.sql'
$SeedSql = Join-Path $ProjectRoot 'sql\seed.sql'

$DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { 'localhost' }
$DbPort = if ($env:DB_PORT) { $env:DB_PORT } else { '3306' }
$DbUser = if ($env:DB_USER) { $env:DB_USER } else { 'root' }
$DbPass = if ($env:DB_PASS) { $env:DB_PASS } else { '114514' }
$DbName = if ($env:DB_NAME) { $env:DB_NAME } else { 'ranking_system' }

function Get-MySqlExecutable {
    $fromPath = Get-Command mysql -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    $candidates = @(
        "$env:ProgramFiles\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "$env:ProgramFiles\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "$env:ProgramFiles\MySQL\MySQL Server 5.7\bin\mysql.exe",
        "${env:ProgramFiles(x86)}\MySQL\MySQL Server 8.0\bin\mysql.exe"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw 'Error: mysql command not found. Install MySQL client and add its bin folder to PATH.'
}

function Invoke-MySql {
    param(
        [Parameter(Mandatory = $true)]
        [string] $MySqlExe,
        [string] $SqlFile,
        [string] $Command
    )

    $args = @(
        "--host=$DbHost",
        "--port=$DbPort",
        "--user=$DbUser",
        '--protocol=TCP',
        '--default-character-set=utf8mb4'
    )

    $prevPwd = $env:MYSQL_PWD
    $env:MYSQL_PWD = $DbPass
    try {
        if ($Command) {
            & $MySqlExe @args '-e' $Command
        }
        else {
            Get-Content -Path $SqlFile -Raw -Encoding UTF8 | & $MySqlExe @args
        }
        if ($LASTEXITCODE -ne 0) {
            throw "mysql exited with code $LASTEXITCODE"
        }
    }
    finally {
        if ($null -eq $prevPwd) {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
        else {
            $env:MYSQL_PWD = $prevPwd
        }
    }
}

if (-not (Test-Path $SchemaSql) -or -not (Test-Path $SeedSql)) {
    throw 'Error: schema.sql or seed.sql not found.'
}

$mysqlExe = Get-MySqlExecutable

Write-Host "Resetting database '$DbName' on ${DbHost}:$DbPort ..."
Invoke-MySql -MySqlExe $mysqlExe -Command "DROP DATABASE IF EXISTS ``$DbName``;"

Write-Host 'Running schema.sql ...'
Invoke-MySql -MySqlExe $mysqlExe -SqlFile $SchemaSql

Write-Host 'Running seed.sql ...'
Invoke-MySql -MySqlExe $mysqlExe -SqlFile $SeedSql

Write-Host "Done. Database '$DbName' has been refreshed."
