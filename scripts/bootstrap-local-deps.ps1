param(
    [string]$RepoPath = "C:\Users\ASUS\Documents\servers\remote\source\Pufferfish",
    [string]$DepsRoot = "C:\Users\ASUS\Documents\servers\remote\tmp\deps"
)

$ErrorActionPreference = "Stop"

$repo = (Resolve-Path $RepoPath).Path
$deps = (Resolve-Path $DepsRoot).Path
$localRepo = Join-Path $repo "local-maven-repo"
$simpleYamlRepo = Join-Path $deps "Simple-YAML"
$flareRepo = Join-Path $deps "Flare"

if (!(Test-Path $simpleYamlRepo) -or !(Test-Path $flareRepo)) {
    throw "Expected cloned dependency repos in $deps"
}

New-Item -ItemType Directory -Force -Path $localRepo | Out-Null

$fileRepoUrl = "file:///" + ($localRepo -replace "\\", "/")

Write-Output "[1/3] Deploying Simple-YAML modules to local repo"
& mvn -q -f $simpleYamlRepo "-DskipTests" "-Dmaven.javadoc.skip=true" "-DaltDeploymentRepository=local::default::$fileRepoUrl" deploy
if ($LASTEXITCODE -ne 0) {
    throw "Simple-YAML deploy failed"
}

Write-Output "[2/3] Building Flare shadow jar"
& "$flareRepo\gradlew.bat" -q -p $flareRepo jar
if ($LASTEXITCODE -ne 0) {
    throw "Flare jar build failed"
}

$flareJar = Join-Path $flareRepo "build\libs\Flare-3.2-SNAPSHOT.jar"
if (!(Test-Path $flareJar)) {
    throw "Expected Flare jar at $flareJar"
}

Write-Output "[3/3] Deploying Flare jar to local repo with JitPack-compatible coordinates"
& mvn -q deploy:deploy-file `
    "-Durl=$fileRepoUrl" `
    "-DrepositoryId=local" `
    "-DgroupId=com.github.technove" `
    "-DartifactId=Flare" `
    "-Dversion=34637f3f87" `
    "-Dpackaging=jar" `
    "-Dfile=$flareJar" `
    "-DgeneratePom=true"
if ($LASTEXITCODE -ne 0) {
    throw "Flare deploy failed"
}

Write-Output "Local dependency bootstrap completed: $localRepo"
