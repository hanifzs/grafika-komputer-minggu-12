$smoke = $args -contains '--smoke'
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
# Maven memilih JAVA_HOME (bila ada), yang mungkin menunjuk JDK 11 lama.
# Gunakan instalasi java terbaru dari PATH bila versinya >= 17.
$javaInfo = cmd /c 'java -XshowSettings:properties -version 2>&1'
$homeLine = $javaInfo | Where-Object { $_ -match '^\s*java\.home\s*=' } | Select-Object -First 1
if ($homeLine -and (($javaInfo | Select-String 'java.version = (\d+)' | Select-Object -First 1).Matches.Groups[1].Value -as [int]) -ge 17) {
    $env:JAVA_HOME = ($homeLine -split '=', 2)[1].Trim()
}
$maven = Get-Command mvn -ErrorAction SilentlyContinue
$goals = @('-f', "$project/pom.xml", 'compile', 'exec:java')
if ($smoke) { $goals += '-Dexec.args=--smoke' }
if ($maven) {
    & $maven.Source @goals
    exit $LASTEXITCODE
}
$version = '3.9.11'
$tools = Join-Path $project '.tools'
$exe = Join-Path $tools "apache-maven-$version/bin/mvn.cmd"
if (-not (Test-Path $exe)) {
    New-Item -ItemType Directory -Path $tools -Force | Out-Null
    $zip = Join-Path $tools "apache-maven-$version-bin.zip"
    Write-Host "Mengunduh Maven $version (hanya saat pertama kali)..."
    Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip" -OutFile $zip
    Expand-Archive $zip -DestinationPath $tools -Force
    Remove-Item $zip
}
& $exe @goals
exit $LASTEXITCODE
