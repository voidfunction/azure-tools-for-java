/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.utils;


import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CancellationException;

/**
 * Created by vlashch on 1/19/17.
 */
public class WebAppUtils {

    private static final String ftpRootPath = "/site/wwwroot/";
    private static final String ftpWebAppsPath = ftpRootPath + "webapps/";
    private static String jdkFolderName = "jdk";
    private static final String ftpJdkPath = ftpRootPath + jdkFolderName;
    private static String aspScriptName = "getjdk.aspx";
    private static String webConfigFilename = "web.config";
    private static String reportFilename = "report.txt";
    private static String statusFilename = "status.txt";

    public static FTPClient getFtpConnection(PublishingProfile pp) throws Exception {

        FTPClient ftp = new FTPClient();

        System.out.println("\t\t" + pp.ftpUrl());
        System.out.println("\t\t" + pp.ftpUsername());
        System.out.println("\t\t" + pp.ftpPassword());

        URI uri = URI.create("ftp://" + pp.ftpUrl());
        ftp.connect(uri.getHost(), 21);
        final int replyCode = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            throw new ConnectException("Unable to connect to FTP server");
        }

        if (!ftp.login(pp.ftpUsername(), pp.ftpPassword())) {
            throw new ConnectException("Unable to login to FTP server");
        }

        ftp.setControlKeepAliveTimeout(3000);

        return ftp;
    }

    public static void deployArtifact(String artifactName, String artifactPath, PublishingProfile pp, boolean toRoot, IProgressIndicator indicator) throws Exception {
        FTPClient ftp = null;
        try {
            if (indicator != null) indicator.setText("Connecting to FTP server...");

            ftp = getFtpConnection(pp);

            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            if (indicator != null) indicator.setText("Uploading the application...");
            InputStream input = new FileInputStream(artifactPath);
            if (toRoot) {
                WebAppUtils.removeFtpDirectory(ftp, ftpWebAppsPath + "ROOT", indicator);
                ftp.storeFile(ftpWebAppsPath + "ROOT.war", input);
            } else {
                WebAppUtils.removeFtpDirectory(ftp, ftpWebAppsPath + artifactName, indicator);
                ftp.storeFile(ftpWebAppsPath + artifactName + ".war", input);
            }
            input.close();
            if (indicator != null) indicator.setText("Logging out of FTP server...");
            ftp.logout();
        } finally {
            if (ftp != null && ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void removeFtpDirectory(FTPClient ftpClient, String path, IProgressIndicator pi) throws IOException {
        String prefix = "Removing from FTP server: ";
        FTPFile[] subFiles = ftpClient.listFiles(path);
        if (subFiles.length > 0) {
            for (FTPFile ftpFile : subFiles) {
                if (pi != null && pi.isCanceled()) break;
                String currentFileName = ftpFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    continue; // skip
                }

                String path1 = path + "/" + currentFileName;
                if (ftpFile.isDirectory()) {
                    // remove the sub directory
                    removeFtpDirectory(ftpClient, path1, pi);
                } else {
                    // delete the file
                    if (pi != null) pi.setText2(prefix + path1);
                    ftpClient.deleteFile(path1);
                }
            }
        }

        if (pi != null) pi.setText2(prefix + path);
        ftpClient.removeDirectory(path);
        if (pi != null) pi.setText2("");
    }

    private static void uploadJdkDownloadScript(FTPClient ftp, String jdkDownloadUrl) throws Exception {

        String aspxPageName = aspScriptName;

        byte[] aspxPageData = generateAspxScriptForCustomJdkDownload(jdkDownloadUrl);
        ftp.storeFile(ftpRootPath + aspxPageName, new ByteArrayInputStream(aspxPageData));

        byte[] webConfigData = generateWebConfigForCustomJdkDownload(aspxPageName, null);
        ftp.storeFile(ftpRootPath + webConfigFilename, new ByteArrayInputStream(webConfigData));
    }

    public static boolean doesRemoteFileExist(FTPClient ftp, String path, String fileName) throws IOException {
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile file : files) {
            if (file.isFile() && file.getName().equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doesRemoteFolderExist(FTPClient ftp, String path, String folderName) throws IOException {
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile file : files) {
            if (file.isDirectory() && file.getName().equalsIgnoreCase(folderName)) {
                return true;
            }
        }
        return false;
    }

    public static int sendGet(String sitePath) throws Exception {
        URL url = new URL(sitePath);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        //con.setRequestProperty("User-Agent", "AzureTools for Intellij");
        return con.getResponseCode();
    }

    public static boolean isUrlAccessible(String url) throws IOException {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("HEAD");
        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return false;
        }
        return true;
    }

    private static void cleanupWorkerData(FTPClient ftp) throws IOException {
        ftp.deleteFile(ftpRootPath + aspScriptName);
        ftp.deleteFile(ftpRootPath + "jdk.zip");
    }

    public static void removeCustomJdkArtifacts(FTPClient ftp, IProgressIndicator pi) throws IOException {
        if (doesRemoteFolderExist(ftp, ftpRootPath, jdkFolderName)) {
            removeFtpDirectory(ftp, ftpJdkPath, pi);
        }
        ftp.deleteFile(ftpRootPath + webConfigFilename);
        ftp.deleteFile(ftpRootPath + reportFilename);
        ftp.deleteFile(ftpRootPath + statusFilename);
    }

    private static class WebAppException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1352713295336034845L;

        WebAppException(String message) {
            super(message);
        }
    }

    public static void deployCustomJdk(WebApp webApp, String jdkDownloadUrl, WebContainer webContainer, IProgressIndicator indicator) throws Exception {
        FTPClient ftp = null;
        String customJdkFolderName =  null;
        try {

            PublishingProfile pp = webApp.getPublishingProfile();
            ftp = getFtpConnection(pp);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            // {{ debug only
            System.out.println("\t\t" + pp.ftpUrl());
            System.out.println("\t\t" + pp.ftpUsername());
            System.out.println("\t\t" + pp.ftpPassword());
            // }}

            // stop and restart web app
            if (indicator != null) indicator.setText("Stopping the service...");
            webApp.stop();

            if (indicator != null) indicator.setText("Deleting custom jdk artifacts, if any (takes a while)...");
            removeCustomJdkArtifacts(ftp, indicator);

            if (indicator != null) indicator.setText("Uploading scripts...");
            uploadJdkDownloadScript(ftp, jdkDownloadUrl);

            if (indicator != null) indicator.setText("Starting the service...");
            webApp.start();

            // Polling report.txt...
            final String siteUrl = "https://" + webApp.defaultHostName();
            if (indicator != null) indicator.setText("Checking the JDK gets downloaded and unpacked...");
            //int step = 0;
            while (!doesRemoteFileExist(ftp, ftpRootPath, reportFilename)) {
                if (indicator != null && indicator.isCanceled()) throw new CancellationException("Canceled by user.");
                //if (step++ > 3) checkFreeSpaceAvailability(ftp);
                Thread.sleep(5000);
                sendGet(siteUrl);
            }

            if (indicator != null) indicator.setText("Checking status...");
            OutputStream reportFileStream = new ByteArrayOutputStream();
            ftp.retrieveFile("report.txt", reportFileStream);
            String reportFileString = reportFileStream.toString();
            if (reportFileString.startsWith("FAIL")) {
                String err = reportFileString.substring(reportFileString.indexOf(":"+1));
                throw new WebAppException(err);
            }

            // get top level jdk folder name (under jdk folder)
            FTPFile[] ftpDirs = ftp.listDirectories(ftpJdkPath);
            if (ftpDirs.length != 1) {
                String err = "Bad JDK archive. Please make sure the JDK archive contains a single JDK folder. For example, 'my-jdk1.7.0_79.zip' archive should contain 'jdk1.7.0_79' folder only";
                throw new WebAppException(err);
            }

            customJdkFolderName = ftpDirs[0].getName();

            uploadWebConfigForCustomJdk(ftp, webApp, customJdkFolderName, webContainer, indicator);
        } catch (Exception ex){
            if (doesRemoteFolderExist(ftp, ftpRootPath, jdkFolderName)) {
                indicator.setText("Error happened. Cleaning up...");
                removeFtpDirectory(ftp, ftpJdkPath, indicator);
            }
            throw ex;
        } finally {
            indicator.setText("Removing working data from server...");
            cleanupWorkerData(ftp);
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
    }

    private static void uploadWebConfigForCustomJdk(FTPClient ftp, WebApp webApp, String jdkFolderName, WebContainer webContainer, IProgressIndicator indicator) throws Exception {
        if  (jdkFolderName == null || jdkFolderName.isEmpty()) {
            throw new Exception("jdkFolderName is null or empty");
        }

        if(indicator != null) indicator.setText("Stopping the service...");
        webApp.stop();

        if(indicator != null) indicator.setText("Deleting "+ webConfigFilename + "...");
        ftp.deleteFile(ftpRootPath + webConfigFilename);

        if (indicator != null) indicator.setText("Turning the App Service into java based...");
        webApp.update().withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(webContainer).apply();

        if(indicator != null) indicator.setText("Generating " + webConfigFilename + "...");
        String jdkPath = "%HOME%\\site\\wwwroot\\jdk\\" + jdkFolderName;
        String webContainerPath = generateWebContainerPath(webContainer);
        byte[] webConfigData = generateWebConfigForCustomJDK(jdkPath, webContainerPath);

        if(indicator != null) indicator.setText("Uploading " + webConfigFilename + "...");
        ftp.storeFile(ftpRootPath + webConfigFilename,  new ByteArrayInputStream(webConfigData));

        if(indicator != null) indicator.setText("Starting the service...");
        webApp.start();
    }

    public static String generateWebContainerPath(WebContainer webContainer) {
        if (webContainer.equals(WebContainer.TOMCAT_7_0_NEWEST)) {
            return "%AZURE_TOMCAT7_HOME%";
        } else if (webContainer.equals(WebContainer.TOMCAT_8_0_NEWEST)) {
            return "%AZURE_TOMCAT8_HOME%";
        } else if (webContainer.equals(WebContainer.JETTY_9_1_NEWEST)) {
            return "%AZURE_JETTY9_HOME%";
        }
        String binPath = "%programfiles(x86)%\\";
        String wc = webContainer.toString();
        int verIdx = wc.indexOf(" ") + 1;
        String ver = wc.substring(verIdx);
        if (wc.startsWith("tomcat")) {
            return binPath + "apache-tomcat-" + ver;
        } else if (wc.startsWith("jetty")) {
            StringBuilder sbVer = new StringBuilder(ver);
            sbVer.insert(ver.lastIndexOf('.')+1, 'v');
            return binPath + "jetty-distribution-" + sbVer.toString();
        }

        return "%AZURE_TOMCAT8_HOME%";
    }

    public static byte[] generateWebConfigForCustomJDK(String jdkPath, String webContainerPath) {
        String jdkProcessPath = jdkPath.isEmpty() ? "%JAVA_HOME%\\bin\\java.exe" : jdkPath + "\\bin\\java.exe";

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
        sb.append("<configuration>\n");
        sb.append("    <system.webServer>\n");
        sb.append("        <applicationInitialization remapManagedRequestsTo='/hostingstart.html'>\n");
        sb.append("        </applicationInitialization>\n");

        if (webContainerPath.toUpperCase().contains("TOMCAT")) {
            String javaOptsString =
                    "-Djava.net.preferIPv4Stack=true -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT%";
            String catalinaOpts = "-Dport.http=%HTTP_PLATFORM_PORT%";

            sb.append("        <httpPlatform processPath='" + webContainerPath + "\\bin\\startup.bat" + "'>\n");
            sb.append("            <environmentVariables>\n");
            sb.append("                <environmentVariable name='JRE_HOME' value='"+ jdkPath +"'/>\n");
            sb.append("                <environmentVariable name='JAVA_OPTS' value='"+ javaOptsString +"'/>\n");
            sb.append("                <environmentVariable name='CATALINA_OPTS' value='"+ catalinaOpts +"'/>\n");
            sb.append("                <environmentVariable name='CATALINA_HOME' value='" + webContainerPath + "'/>\n");
            sb.append("            </environmentVariables>\n");
            sb.append("        </httpPlatform>\n");
        } else {
            String arg =
                    "-Djava.net.preferIPv4Stack=true  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT% -Djetty.port=%HTTP_PLATFORM_PORT% -Djetty.base=\"" +
                    webContainerPath + "\" -Djetty.webapps=\"d:\\home\\site\\wwwroot\\webapps\"  -jar \"" + webContainerPath + "\\start.jar\" etc\\jetty-logging.xml";

            sb.append("        <httpPlatform processPath='"+ jdkProcessPath +"' startupTimeLimit='30' startupRetryCount='10' arguments='"+ arg +"'/>\n");
        }
        sb.append("    </system.webServer>\n");
        sb.append("    <system.web>\n");
        sb.append("        <customErrors mode='Off'/>\n");
        sb.append("        <compilation debug='true' targetFramework='4.5'>\n");
        sb.append("            <assemblies>\n");
        sb.append("            </assemblies>\n");
        sb.append("        </compilation>\n");
        sb.append("        <httpRuntime targetFramework='4.5'/>\n");
        sb.append("    </system.web>\n");
        sb.append("</configuration>\n");

        return sb.toString().getBytes();
    }

    public static byte[] generateAspxScriptForCustomJdkDownload(String jdkDownloadUrl) throws IOException {

        StringBuilder sb = new StringBuilder();

        sb.append("<%@ Page Language=\"C#\" %>\n");
        sb.append("<%@ Import namespace=\"System.IO\" %>\n");
        sb.append("<%@ Import namespace=\"System.Net\" %>\n");
        sb.append("<%@ Import namespace=\"System.IO.Compression\" %>\n");
        sb.append("<script runat=server>\n");

        sb.append("const string baseDir = @\"d:\\home\\site\\wwwroot\";\n");
        sb.append("const string keySuccess = \"SUCCESS\";\n");
        sb.append("const string keyFail = \"FAIL\";\n");
        sb.append("const string reportPattern = \"{0}:{1}\";\n");
        sb.append("readonly static string pathReport = Path.Combine(baseDir, \"report.txt\");\n");
        sb.append("readonly static string pathStatus = Path.Combine(baseDir, \"status.txt\");\n");

        sb.append("string getTime() {\n");
        sb.append("    getJdk();\n");
        sb.append("    return DateTime.Now.ToString(\"t\");\n");
        sb.append("}\n");

        sb.append("static void getJdk() {\n");
        sb.append("    try {\n");
        sb.append("         const string downloadSrc = @\"" + jdkDownloadUrl + "\";\n");
        sb.append("         string downloadDst = Path.Combine(baseDir, \"jdk.zip\");\n");
        sb.append("         statusAdd(\"Deleting zip file, if any\");\n");
        sb.append("         if (File.Exists(downloadDst)) { File.Delete(downloadDst); }\n");
        sb.append("         statusAdd(\"Checking zip size for download\");\n");
        sb.append("         var req = WebRequest.Create(downloadSrc);\n");
        sb.append("         req.Method = \"HEAD\";\n");
        sb.append("         long contentLength;\n");
        sb.append("         using (WebResponse resp = req.GetResponse()) {\n");
        sb.append("             if (!long.TryParse(resp.Headers.Get(\"Content-Length\"), out contentLength)) {\n");
        sb.append("                 throw new Exception(\"Can't get file size\");\n");
        sb.append("             }\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"zip size is [\" + contentLength + \"] , disk size is [\" + getDiskFreeSpace() + \"]\" );\n");
        sb.append("         if (contentLength*2 > getDiskFreeSpace()) {\n");
        sb.append("             throw new Exception(\"There is not enough disk space to complete the operation.\");\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"Downloading zip\");\n");
        sb.append("         using (var client = new WebClient()) {\n");
        sb.append("             client.DownloadFile(downloadSrc, downloadDst);\n");
        sb.append("         }\n");
        sb.append("         string unpackDst = Path.Combine(baseDir, \"jdk\");\n");
        sb.append("         statusAdd(\"Deleting jdk dir, if any\");\n");
        sb.append("         if (Directory.Exists(unpackDst)) { Directory.Delete(unpackDst, true); }\n");
        sb.append("         string unpackSrc = Path.Combine(baseDir, \"jdk.zip\");\n");
        sb.append("         statusAdd(\"Checking expected upacked size\");\n");
        sb.append("         long expectedUnpackedSize;\n");
        sb.append("         using (ZipArchive archive = ZipFile.OpenRead(unpackSrc)) {\n");
        sb.append("             expectedUnpackedSize = archive.Entries.Sum(entry => entry.Length);\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"Expected upacked size is [\" + expectedUnpackedSize + \"] , disk size is [\" + getDiskFreeSpace() + \"]\");\n");
        sb.append("         if (expectedUnpackedSize*2 > getDiskFreeSpace()) {\n");
        sb.append("             throw new Exception(\"There is not enough disk space to complete the operation.\");\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"Unpacking zip\");\n");
        sb.append("         ZipFile.ExtractToDirectory(unpackSrc, unpackDst);\n");
        sb.append("         statusAdd(\"Done\");\n");
        sb.append("         reportOneLine(string.Format(reportPattern, keySuccess, string.Empty));\n");
        sb.append("     } catch (Exception e) {\n");
        sb.append("         statusAdd(\"Exception: \" + e.Message);\n");
        sb.append("         reportOneLine(string.Format(reportPattern, keyFail, e.Message));\n");
        sb.append("     }\n");
        sb.append("}\n");

        sb.append("static long getDiskFreeSpace() {\n");
        sb.append("     DriveInfo driveInfo = new DriveInfo(@\"d:\");\n");
        sb.append("     return driveInfo.AvailableFreeSpace;\n");
        sb.append("}\n");

        sb.append("static void reportOneLine(string message) {\n");
        sb.append("     if (File.Exists(pathReport)) File.Delete(pathReport);\n");
        sb.append("     using (StreamWriter sw = File.CreateText(pathReport)) {\n");
        sb.append("         sw.WriteLine(message);\n");
        sb.append("     }\n");
        sb.append("}\n");

        sb.append("static void statusAdd(string message) {\n");
        sb.append("     if (!File.Exists(pathStatus)) {\n");
        sb.append("         using (StreamWriter sw = File.CreateText(pathStatus)) {\n");
        sb.append("             sw.WriteLine(message);\n");
        sb.append("         }\n");
        sb.append("     } else {\n");
        sb.append("         using (StreamWriter sw = File.AppendText(pathStatus)) {\n");
        sb.append("             sw.WriteLine(message);\n");
        sb.append("         }\n");
        sb.append("     }\n");
        sb.append("}\n");

        sb.append("</script>\n");
        sb.append("<html>\n");
        sb.append("<body>\n");
        sb.append("<form id=\"form1\" runat=\"server\">\n");
        sb.append("Current server time is <% =getTime()%>\n");
        sb.append("</form>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString().getBytes();
    }

    public static byte[] generateWebConfigForCustomJdkDownload(String initializationPage, String[] assemblies) throws IOException {

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
        sb.append("<configuration>\n");
        sb.append("    <system.webServer>\n");
        sb.append("        <applicationInitialization remapManagedRequestsTo='/hostingstart.html'>\n");
        if(initializationPage!=null && !initializationPage.isEmpty())
            sb.append("        <add initializationPage='/" + initializationPage + "'/>\n");
        sb.append("    </applicationInitialization>\n");
        sb.append("    </system.webServer>\n");
        sb.append("    <system.web>\n");
        sb.append("        <customErrors mode='Off'/>\n");
        sb.append("        <compilation debug='true' targetFramework='4.5'>\n");
        sb.append("        <assemblies>\n");
        sb.append("            <add assembly='System.IO.Compression.FileSystem, Version=4.0.0.0, Culture=neutral, PublicKeyToken=B77A5C561934E089'/>\n");
        sb.append("            <add assembly='System.IO.Compression, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089'/>\n");

        if (assemblies != null) {
            for (String assembly : assemblies) {
                sb.append("            <add assembly='" + assembly + "'/>\n");
            }
        }

        sb.append("        </assemblies>\n");
        sb.append("        </compilation>\n");
        sb.append("        <httpRuntime targetFramework='4.5'/>\n");
        sb.append("    </system.web>\n");
        sb.append("</configuration>\n");
        return sb.toString().getBytes();
    }


    public static abstract class CreateAppServiceModel {
        public enum JdkTab {
            Default,
            ThirdParty,
            Own;
        }
        public String webAppName;
        public WebContainer webContainer;
        public SubscriptionDetail subscriptionDetail;

        public boolean isResourceGroupCreateNew;
        public ResourceGroup resourceGroup;
        public String resourceGroupNameCreateNew;

        public boolean isAppServicePlanCreateNew;
        public AppServicePlan appServicePlan;
        public String appServicePlanNameCreateNew;
        public Location appServicePlanLocationCreateNew;
        public AppServicePricingTier appServicePricingTierCreateNew;

        public String jdk3PartyUrl;
        public String jdkOwnUrl;
        public String storageAccountKey;
        public JdkTab jdkTab;
        public String jdkDownloadUrl;

        public abstract void collectData();

    }

    public static WebApp createAppService(IProgressIndicator progressIndicator, CreateAppServiceModel model) throws Exception {

        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return null; }

        Azure azure = azureManager.getAzure(model.subscriptionDetail.getSubscriptionId());
        WebApp.DefinitionStages.Blank definitionStages = azure.webApps().define(model.webAppName);
        WebApp.DefinitionStages.WithAppServicePlan ds1;

        if (model.isResourceGroupCreateNew) {
            ds1 = definitionStages.withNewResourceGroup(model.resourceGroupNameCreateNew);
        } else {
            ds1 = definitionStages.withExistingResourceGroup(model.resourceGroup);
        }

        WebAppBase.DefinitionStages.WithCreate<WebApp> ds2;
        if (model.isAppServicePlanCreateNew) {
            ds2 = ds1.withNewAppServicePlan(model.appServicePlanNameCreateNew)
                    .withRegion(model.appServicePlanLocationCreateNew.name())
                    .withPricingTier(model.appServicePricingTierCreateNew);
        } else {
            ds2 = ds1.withExistingAppServicePlan(model.appServicePlan);
        }

        if (model.jdkDownloadUrl == null) { // no custom jdk
            ds2 = ds2.withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(model.webContainer);
        }

        WebApp myWebApp = ds2.create();

        if (model.jdkDownloadUrl != null ) {
            progressIndicator.setText("Deploying custom jdk...");
            WebAppUtils.deployCustomJdk(myWebApp, model.jdkDownloadUrl, model.webContainer, progressIndicator);
        }

        // update cache
        AzureModel azureModel = AzureModel.getInstance();

        if (model.isResourceGroupCreateNew) {
            ResourceGroup rg = azure.resourceGroups().getByName(model.resourceGroupNameCreateNew);
            if (rg == null) {
                throw new AzureCmdException(String.format("azure.resourceGroups().getByName(%s) returned null"), model.resourceGroupNameCreateNew);
            }

            AzureModelController.addNewResourceGroup(model.subscriptionDetail, rg);
            AzureModelController.addNewWebAppToJustCreatedResourceGroup(rg, myWebApp);
            if (model.isAppServicePlanCreateNew) {
                AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                if (asp == null) {
                    throw new AzureCmdException(String.format("azure.appServices().appServicePlans().getById(%s) returned null"), myWebApp.appServicePlanId());
                }
                AzureModelController.addNewAppServicePlanToJustCreatedResourceGroup(rg, asp);
            } else {
                // add empty list
                AzureModelController.addNewAppServicePlanToJustCreatedResourceGroup(rg, null);
            }
        } else {
            ResourceGroup rg = model.resourceGroup;
            AzureModelController.addNewWebAppToExistingResourceGroup(rg, myWebApp);
            if (model.isAppServicePlanCreateNew) {
                AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                AzureModelController.addNewAppServicePlanToExistingResourceGroup(rg, asp);
            }
        }

        return myWebApp;
    }
}
