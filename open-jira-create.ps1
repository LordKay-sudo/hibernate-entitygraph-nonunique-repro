$ErrorActionPreference = "Stop"

$title = "getSingleResult() throws NonUniqueResultException when EntityGraph fetch-joins multiple collections for a single root entity"
$description = Get-Content -Raw -Path "$PSScriptRoot\JIRA-DESCRIPTION.txt"
$createUrl = "https://hibernate.atlassian.net/secure/CreateIssue.jspa?pid=10031"

Set-Clipboard -Value ($title + "`n`n" + ($description -replace 'h2\. ', '' -replace '\{\{', '' -replace '\}\}', ''))
Write-Host "Title and description copied to clipboard."
Write-Host "Opening Hibernate JIRA create form..."
Start-Process $createUrl
Write-Host ""
Write-Host "Project: HHH (Hibernate ORM)"
Write-Host "Issue type: Bug"
Write-Host "Summary: $title"
Write-Host "Description: paste from clipboard (wiki markup in JIRA-DESCRIPTION.txt)"
Write-Host "Link: https://github.com/LordKay-sudo/hibernate-entitygraph-nonunique-repro"
