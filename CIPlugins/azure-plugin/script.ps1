Set-ExecutionPolicy Unrestricted
$jenkinsserverurl = $args[0]
$vmname = $args[1]


# Download the file to a specific location
Write-Output "Downloading zulu SDK "
$source = "http://azure.azulsystems.com/zulu/zulu1.7.0_51-7.3.0.4-win64.zip?eclipse"
mkdir c:\azurecsdir
$destination = "c:\azurecsdir\zuluJDK.zip"
$wc = New-Object System.Net.WebClient
$wc.DownloadFile($source, $destination)

Write-Output "Unzipping JDK "
# Unzip the file to specified location
$shell_app=new-object -com shell.application 
$zip_file = $shell_app.namespace($destination) 
mkdir c:\java
$destination = $shell_app.namespace("c:\java") 
$destination.Copyhere($zip_file.items())
Write-Output "Successfully downloaded and extracted JDK "

# Downloading jenkins slaves jar
Write-Output "Downloading jenkins slave jar "
$slaveSource = $jenkinsserverurl + "jnlpJars/slave.jar"
$destSource = "c:\java\slave.jar"
$wc = New-Object System.Net.WebClient
$wc.DownloadFile($slaveSource, $destSource)

# execute slave
Write-Output "Executing slave process "
$java="c:\java\zulu1.7.0_51-7.3.0.4-win64\bin\java.exe"
$jar="-jar"
$jnlpUrl="-jnlpUrl" 
$serverURL=$jenkinsserverurl+"computer/" + $vmname + "/slave-agent.jnlp"
& $java $jar $destSource $jnlpUrl $serverURL  


