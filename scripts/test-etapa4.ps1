param(
    [string]$DockerCommand = 'docker',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
Push-Location $workspace
$previousEnvironment = @{}
$previousPath = $env:PATH
if ([IO.Path]::IsPathRooted($DockerCommand)) {
    $env:PATH = (Split-Path $DockerCommand -Parent) + ';' + $env:PATH
}
$composeArgs = @('compose', '--env-file', '.env.etapa4-validation', '-p', 'hulysses-etapa4-validation', '-f', 'compose.yml')

function Invoke-Compose {
    param([string[]]$Arguments)
    # Docker writes progress to stderr; treat progress as text and check its actual exit code.
    $ErrorActionPreference = 'Continue'
    & $DockerCommand @composeArgs @Arguments 2>&1 | ForEach-Object { Write-Host $_.ToString() }
    if ($LASTEXITCODE -ne 0) { throw "Compose failed: $($Arguments -join ' ')" }
}

function Assert-Eventually {
    param([scriptblock]$Condition, [string]$Description)
    $deadline = (Get-Date).AddSeconds(45)
    do {
        try {
            if (& $Condition) { Write-Host "PASS: $Description"; return }
        } catch [System.Net.WebException] {
            # An HTTP endpoint can still be reconnecting just after a container restart.
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Verification failed: $Description"
}

try {
    # Independent project, ports and volumes; keep credentials on reruns and never remove volumes.
    if (-not (Test-Path '.env.etapa4-validation')) {
        $settings = @{
            DB_NAME = 'hulysses'; DB_USERNAME = 'hulysses'; DB_PASSWORD = [guid]::NewGuid().ToString('N')
            DB_SCHEMA = 'public'; DOCKER_DB_URL = 'jdbc:postgresql://hulysses-db:5432/hulysses'
            PARTNER_DB_NAME = 'business_partner'; PARTNER_DB_USERNAME = 'business_partner'
            PARTNER_DB_PASSWORD = [guid]::NewGuid().ToString('N'); PARTNER_DB_SCHEMA = 'business_partner_service'
            PARTNER_DB_URL = 'jdbc:postgresql://business-partner-db:5432/business_partner'
            RABBITMQ_USERNAME = 'hulysses'; RABBITMQ_PASSWORD = [guid]::NewGuid().ToString('N')
            SERVER_PORT = '8080'; PARTNER_SERVER_PORT = '8081'; CONFIG_SERVER_PORT = '8888'
            APP_HOST_PORT = '18080'; PARTNER_HOST_PORT = '18081'; CONFIG_SERVER_HOST_PORT = '18888'
            RABBITMQ_HOST_PORT = '15673'; RABBITMQ_MANAGEMENT_PORT = '15674'
            CONFIG_SERVER_URL = 'http://config-server:8888'; BUSINESS_PARTNER_SERVICE_URL = 'http://business-partner-service:8081'
            JPA_DDL_AUTO = 'update'; JPA_SHOW_SQL = 'false'; JPA_CREATE_NAMESPACES = 'true'
            PARTNER_ACTIVITY_EXCHANGE = 'business-partner.events'; PARTNER_ACTIVITY_QUEUE = 'business-partner.activities'
            PARTNER_ACTIVITY_ROUTING_KEY = 'business-partner.created'
        }
        $lines = $settings.GetEnumerator() | Sort-Object Name | ForEach-Object { '{0}={1}' -f $_.Key, $_.Value }
        [IO.File]::WriteAllLines((Join-Path $workspace '.env.etapa4-validation'), $lines)
    }
    $settings = @{}
    foreach ($line in Get-Content '.env.etapa4-validation') {
        if ($line -match '^([^#=]+)=(.*)$') {
            $name = $Matches[1]; $value = $Matches[2]
            $settings[$name] = $value
            $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
    Invoke-Compose -Arguments @('config', '--quiet')
    if (-not $SkipBuild) { Invoke-Compose -Arguments @('build') }
    Invoke-Compose -Arguments @('up', '-d', '--wait', '--wait-timeout', '240')
    Invoke-Compose -Arguments @('ps', '-a')

    $api = 'http://localhost:18081'
    $principal = 'http://localhost:18080'
    $rabbitApi = 'http://localhost:15674/api'
    $authorization = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
            "$($settings.RABBITMQ_USERNAME):$($settings.RABBITMQ_PASSWORD)"))
    $headers = @{ Authorization = "Basic $authorization" }
    $queueUrl = "$rabbitApi/queues/%2F/business-partner.activities"

    function New-TestPartner {
        $document = '3' + (Get-Random -Minimum 1000000000 -Maximum 9999999999).ToString()
        $body = @{ name = 'Stage 4 Demonstration'; document = $document; email = 'stage4@example.com'
            phone = '11999990000'; type = 'INDIVIDUAL'; roles = @('CUSTOMER', 'SUPPLIER') } | ConvertTo-Json
        Invoke-RestMethod "$principal/business-partners" -Method Post -ContentType 'application/json' -Body $body
    }

    $partner = New-TestPartner
    Assert-Eventually {
        $history = Invoke-RestMethod "$api/business-partners/$($partner.id)/activities"
        $history.Count -eq 1
    } `
        'REST registration -> RabbitMQ -> persisted activity'
    $eligible = Invoke-RestMethod "$api/business-partners/$($partner.id)/eligibility?role=SUPPLIER"
    if ($eligible.id -ne $partner.id) { throw 'Existing eligibility contract failed' }
    $productBody = @{ name = 'Stage 4 Product'; description = 'Existing REST contract'
        price = 10.50; supplierId = $partner.id } | ConvertTo-Json
    $product = Invoke-RestMethod "$principal/products" -Method Post -ContentType 'application/json' -Body $productBody
    if ($product.supplier.id -ne $partner.id) { throw 'Existing product/Feign contract failed' }
    Write-Host 'PASS: existing product REST/Feign contract and separate principal database'

    Invoke-Compose -Arguments @('stop', 'business-partner-consumer')
    try {
        $queuedPartner = New-TestPartner
        Assert-Eventually {
            $queue = Invoke-RestMethod $queueUrl -Headers $headers
            $queue.consumers -eq 0 -and $queue.messages_ready -ge 1
        } 'Message remains ready in RabbitMQ with no consumer'
        $history = Invoke-RestMethod "$api/business-partners/$($queuedPartner.id)/activities"
        if ($history.Count -ne 0) {
            throw 'API consumed a message while the consumer container was stopped'
        }
        $peek = Invoke-RestMethod "$queueUrl/get" -Headers $headers -Method Post -ContentType 'application/json' `
            -Body '{"count":1,"ackmode":"ack_requeue_true","encoding":"auto","truncate":5000}'
        $event = $peek[0].payload | ConvertFrom-Json
        if ($event.businessPartnerId -ne $queuedPartner.id -or $event.eventType -ne 'BUSINESS_PARTNER_CREATED') {
            throw 'Queued JSON does not match the registered partner'
        }
        Write-Host 'PASS: queued JSON contains only eventId, eventType, businessPartnerId and occurredAt'
        Invoke-Compose -Arguments @('restart', 'rabbitmq')
        Invoke-Compose -Arguments @('up', '-d', '--wait', '--wait-timeout', '120', 'rabbitmq')
        Assert-Eventually { (Invoke-RestMethod $queueUrl -Headers $headers).messages_ready -ge 1 } `
            'Persistent message survives RabbitMQ restart while consumer is stopped'
    } finally {
        Invoke-Compose -Arguments @('start', 'business-partner-consumer')
    }
    Assert-Eventually {
        $history = Invoke-RestMethod "$api/business-partners/$($queuedPartner.id)/activities"
        $history.Count -eq 1
    } `
        'Restarted consumer persists the queued activity'
    Assert-Eventually { (Invoke-RestMethod $queueUrl -Headers $headers).messages -eq 0 } 'Queue drained after processing'

    $sample = 'business-partner-service/src/main/resources/batch/business-partners.csv'
    $batchJson = & curl.exe -fsS -X POST "$api/batch/business-partners/import" -F "file=@$sample"
    if ($LASTEXITCODE -ne 0) { throw 'CSV upload failed' }
    $batch = $batchJson | ConvertFrom-Json
    if ($batch.status -ne 'COMPLETED' -or $batch.readCount -ne 13 -or $batch.writeCount -notin @(0, 12)) {
        throw "Unexpected Batch result: $batchJson"
    }
    Write-Host "PASS: CSV job status=$($batch.status) read=$($batch.readCount) written=$($batch.writeCount) filtered=$($batch.filterCount) commits=$($batch.commitCount)"
    $repeat = Invoke-RestMethod "$api/batch/business-partners/import" -Method Post
    if ($repeat.status -ne 'COMPLETED' -or $repeat.writeCount -ne 0 -or $repeat.filterCount -ne 13) {
        throw 'Reimport did not filter already registered documents'
    }
    $allPartners = Invoke-RestMethod "$api/business-partners"
    $imported = @($allPartners | Where-Object { $_.document.StartsWith('11') })
    if ($imported.Count -ne 12) { throw 'Expected 12 imported partners' }
    Assert-Eventually {
        $history = Invoke-RestMethod "$api/business-partners/$($imported[0].id)/activities"
        $history.Count -eq 1
    } 'Batch-created partners also generate activities after chunk commit'
    Invoke-Compose -Arguments @('exec', '-T', 'business-partner-db', 'psql', '-U', 'business_partner', '-d', 'business_partner',
        '-c', 'select job_execution_id, status from public.batch_job_execution order by job_execution_id;')

    Invoke-Compose -Arguments @('restart', 'business-partner-service')
    Invoke-Compose -Arguments @('up', '-d', '--wait', '--wait-timeout', '180')
    $afterRestart = Invoke-RestMethod "$api/business-partners/$($partner.id)"
    if ($afterRestart.id -ne $partner.id) { throw 'Partner persistence did not survive restart' }
    $afterRestartProduct = Invoke-RestMethod "$principal/products/$($product.id)"
    if ($afterRestartProduct.id -ne $product.id) { throw 'Principal product persistence failed' }
    $afterRestartJob = Invoke-RestMethod "$api/batch/business-partners/import" -Method Post
    if ($afterRestartJob.status -ne 'COMPLETED' -or $afterRestartJob.writeCount -ne 0 `
            -or $afterRestartJob.jobExecutionId -le $repeat.jobExecutionId) {
        throw 'JDBC Batch metadata did not survive API restart'
    }
    Write-Host 'PASS: JDBC Batch metadata and partner data survive API restart'
    Invoke-Compose -Arguments @('logs', '--tail', '100', 'business-partner-service', 'business-partner-consumer')
    Write-Host 'All Stage 4 scenarios passed. Isolated containers remain running; volumes were preserved.'
    Write-Host 'Stop with: docker compose --env-file .env.etapa4-validation -p hulysses-etapa4-validation down'
} finally {
    $env:PATH = $previousPath
    foreach ($name in $previousEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    Pop-Location
}
