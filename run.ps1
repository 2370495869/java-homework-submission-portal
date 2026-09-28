$env:LANG = 'en_US.UTF-8'
$env:LC_ALL = 'en_US.UTF-8'
$env:PYTHONIOENCODING = 'utf-8'
Set-Location $PSScriptRoot
& "$PSScriptRoot\mvnw.cmd" -B -ntp -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -jar "$PSScriptRoot\target\homework-submission-portal.jar" @args
exit $LASTEXITCODE
