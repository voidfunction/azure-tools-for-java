/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.utils;


import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
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
    private static String javaOptsString = "-Djava.net.preferIPv4Stack=true -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT%";
    private static String catalinaOpts = "-Dport.http=%HTTP_PLATFORM_PORT%";
    private static String aspScriptName = "getjdk.aspx";

    public static void deployArtifact(String artifactName, String artifactPath, PublishingProfile pp, boolean toRoot) throws Exception {
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(pp.ftpUrl());
            final int replyCode = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(replyCode)) {
                throw new ConnectException("Unable to connect to FTP server");
            }

            if (!ftp.login(pp.ftpUsername(), pp.ftpPassword())) {
                throw new ConnectException("Unable to login to FTP server");
            }

            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            InputStream input = new FileInputStream(artifactPath);
            if (toRoot) {
                WebAppUtils.removeFtpDirectory(ftp, ftpRootPath + "ROOT", "");
                ftp.storeFile(ftpRootPath + "ROOT.war", input);
            } else {
                WebAppUtils.removeFtpDirectory(ftp, ftpRootPath + artifactName, "");
                ftp.storeFile(ftpRootPath + artifactName + ".war", input);
            }
            input.close();
            ftp.logout();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }



    public static void removeFtpDirectory(FTPClient ftpClient, String parentDir,
                                          String currentDir) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }
        FTPFile[] subFiles = ftpClient.listFiles(dirToList);
        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile ftpFile : subFiles) {
                String currentFileName = ftpFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    continue;
                }
                String filePath = parentDir + "/" + currentDir + "/" + currentFileName;
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName;
                }

                if (ftpFile.isDirectory()) {
                    // remove the sub directory
                    removeFtpDirectory(ftpClient, dirToList, currentFileName);
                } else {
                    // delete the file
                    ftpClient.deleteFile(filePath);
                }
            }
        } else {
            // remove the empty directory
            ftpClient.removeDirectory(dirToList);
        }
        ftpClient.removeDirectory(dirToList);
    }

    public static String getAbsolutePath(String dir) {
        return "/" + dir.trim().replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public static void uploadJdkDownloadScript(FTPClient ftp, String jdkDownloadUrl) throws Exception {

        String aspxPageName = aspScriptName;

        byte[] aspxPageData = generateAspxScriptForCustomJdk(jdkDownloadUrl);
        ftp.storeFile(ftpRootPath + aspxPageName, new ByteArrayInputStream(aspxPageData));

        byte[] webXmlData = generateWebXmlForCustomJdk(aspxPageName, null);
        ftp.storeFile(ftpRootPath + "web.config", new ByteArrayInputStream(webXmlData));
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

    public static int sendGet(String sitePath) throws Exception {
        URL url = new URL(sitePath);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        //con.setRequestProperty("User-Agent", "AzureTools for Intellij");
        return con.getResponseCode();
    }

    private static void cleanupWorkerData(FTPClient ftp) {
        try {
            ftp.deleteFile(ftpRootPath + aspScriptName);
            ftp.deleteFile(ftpRootPath + "jdk.zip");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void cleanupJdk(FTPClient ftp, String customJdkFolderName) {
        try {
            if (customJdkFolderName != null) {
                removeFtpDirectory(ftp, ftpRootPath, "jdk");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class WebAppException extends Exception {
        WebAppException(String message) {
            super(message);
        }
    }

    public static void uploadJdk(WebApp webApp, String jdkDownloadUrl, IProgressIndicator indicator) throws Exception {
        indicator.setText("Initializing FTP client...");
        final FTPClient ftp = new FTPClient();
        String customJdkFolderName =  null;
        try {

            PublishingProfile pp = webApp.getPublishingProfile();
            try {
                indicator.setText("Logging in...");
                ftp.connect(pp.ftpUrl());
                final int replyCode = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    ftp.disconnect();
                }
                if (!ftp.login(pp.ftpUsername(), pp.ftpPassword())) {
                    ftp.logout();
                }
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.setControlKeepAliveTimeout(3000);

                indicator.setFraction(0.2);

                // {{ debug only
                System.out.println("\t\t" + pp.ftpUrl());
                System.out.println("\t\t" + pp.ftpUsername());
                System.out.println("\t\t" + pp.ftpPassword());
                // }}

                final String siteUrl = webApp.defaultHostName();

                // stop and restart web app
                indicator.setText("Stopping the site...");
                webApp.stop();
                indicator.setFraction(0.3);

                indicator.setText("Uploading scripts...");
                uploadJdkDownloadScript(ftp, jdkDownloadUrl);
                indicator.setFraction(0.4);

                indicator.setText("Starting the site...");
                webApp.start();
                indicator.setFraction(0.5);

                // Polling report.txt...
                indicator.setText("Checking the JDK gets downloaded and unpacked...");
                int step = 0;
                while (!doesRemoteFileExist(ftp, ftpRootPath, "report.txt")) {
                    if (indicator.isCanceled()) throw new CancellationException("Canceled by user.");
                    //if (step++ > 3) checkFreeSpaceAvailability(ftp);
                    Thread.sleep(5000);
                    sendGet(siteUrl);
                }
                indicator.setFraction(0.7);

                indicator.setText("Checking status...");
                OutputStream reportFileStream = new ByteArrayOutputStream();
                ftp.retrieveFile("report.txt", reportFileStream);
                String reportFileString = reportFileStream.toString();
                if (reportFileString.startsWith("FAIL")) {
                    String err = reportFileString.substring(reportFileString.indexOf(":"+1));
                    throw new WebAppException(err);
                }

                // get top level jdk folder name (under jdk folder)
                String jdkPath = ftpRootPath + "jdk/";
                FTPFile[] ftpDirs = ftp.listDirectories(jdkPath);
                if (ftpDirs.length != 1) {
                    String err = "Bad JDK archive. Please make sure the JDK archive contains a single JDK folder. For example, 'my-jdk1.7.0_79.zip' archive should contain 'jdk1.7.0_79' folder only";
                    throw new WebAppException(err);
                }

                customJdkFolderName = ftpDirs[0].getName();

                //customJdkFolderName = jdkFolderName;

                indicator.setFraction(1.0);
            } finally {
                cleanupWorkerData(ftp);
            }
        } finally {
            if (customJdkFolderName != null) {
                cleanupJdk(ftp, customJdkFolderName);
            }
            if (ftp != null && ftp.isConnected()) {
                try {
                    ftp.logout();
                    ftp.disconnect();
                } catch (IOException ignored) {
                    // go nothing
                }
            }
        }
    }

    public static byte[] prepareWebConfigForCustomJDKServer(String jdkPath, String serverPath) {
        String arg = "-Djava.net.preferIPv4Stack=true  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT% -Djetty.port=%HTTP_PLATFORM_PORT% -Djetty.base=\"" +
                serverPath + "\" -Djetty.webapps=\"d:\\home\\site\\wwwroot\\webapps\"  -jar \"" + serverPath + "\\start.jar\" etc\\jetty-logging.xml";
        String jdkProcessPath = jdkPath.isEmpty() ? "%JAVA_HOME%\\bin\\java.exe" : jdkPath + "\\bin\\java.exe";

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
        sb.append("<configuration>\n");
        sb.append("    <system.webServer>\n");
        sb.append("        <applicationInitialization remapManagedRequestsTo='/hostingstart.html'>\n");
        sb.append("        </applicationInitialization>\n");

        if (serverPath.contains("tomcat")) {
            sb.append("        <httpPlatform processPath='" + serverPath + "\\bin\\startup.bat" + "'>\n");
            sb.append("            <environmentVariables>\n");
            sb.append("                <environmentVariable name='JRE_HOME' value='"+ jdkPath +"'/>\n");
            sb.append("                <environmentVariable name='JAVA_OPTS' value='"+ javaOptsString +"'/>\n");
            sb.append("                <environmentVariable name='CATALINA_OPTS' value='"+ catalinaOpts +"'/>\n");
            sb.append("                <environmentVariable name='CATALINA_HOME' value='" + serverPath + "'/>\n");
            sb.append("            </environmentVariables>\n");
            sb.append("        </httpPlatform>\n");
        } else {
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

    public static byte[] generateAspxScriptForCustomJdk(String downloadPath) throws IOException {

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
        sb.append("         const string downloadSrc = @\"" + downloadPath + "\";\n");
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

    public static byte[] generateWebXmlForCustomJdk(String initializationPage, String[] assemblies) throws IOException {

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


}
