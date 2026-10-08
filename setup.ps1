# Windows PowerShell version of setup.sh
# Usage:  .\setup.ps1                       (Lucene 10, needs Java 21+)
#         $env:LUCENE_VERSION="9.12.3"; .\setup.ps1   (Java 17-20)
$V = if ($env:LUCENE_VERSION) { $env:LUCENE_VERSION } else { "10.5.2" }
$Base = "https://repo1.maven.org/maven2/org/apache/lucene"
New-Item -ItemType Directory -Force -Path lib | Out-Null
foreach ($m in "lucene-core","lucene-analysis-common","lucene-queryparser","lucene-queries","lucene-sandbox") {
  Write-Host "Downloading $m-$V.jar"
  Invoke-WebRequest -Uri "$Base/$m/$V/$m-$V.jar" -OutFile "lib/$m-$V.jar"
}
Get-ChildItem lib
java -version
