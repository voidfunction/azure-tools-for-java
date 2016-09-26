package com.microsoftopentechnologies.azurecommons.xmlhandling;

import java.io.*;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.interopbridges.tools.windowsazure.ParserXMLUtility;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;


public class WebAppConfigOperations {
    // String constants
    static String config = "configuration";
    static String webServer = "system.webServer";
    static String handlers = "handlers";
    static String platform = "httpPlatform";
    static String variables = "environmentVariables";
    static String variable = "environmentVariable";
    static String configExp = "/" + config;
    static String webServerExp = configExp + "/" + webServer;
    static String handlersExp = webServerExp + "/" + handlers;
    static String addExp = handlersExp + "/" + "add[@name='httppPlatformHandler']";
    static String platformExp = webServerExp + "/" + platform;
    static String varsExp = platformExp + "/" + variables;
    static String varExp = varsExp + "/" + variable + "[@name='%s']";
    static String javaOptsString = "-Djava.net.preferIPv4Stack=true -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT%";
    static String catalinaOpts = "-Dport.http=%HTTP_PLATFORM_PORT%";
    static String appInitExp =  webServerExp + "/" + "applicationInitialization";
    static String appInitAddExp = appInitExp + "/" + "add[@initializationPage='%s']";


    public static void createTag(Document doc, String parentTag, String tagName) throws WindowsAzureInvalidProjectOperationException {
        HashMap<String, String> nodeAttribites = new HashMap<String, String>();
        ParserXMLUtility.updateOrCreateElement(doc, null, parentTag, tagName, false, nodeAttribites);

    }

    public static void prepareWebConfigForDebug(String fileName, String server) throws WindowsAzureInvalidProjectOperationException, IOException {
        Document doc = ParserXMLUtility.parseXMLFile(fileName);
        String serverPath = "%programfiles(x86)%\\" + server;

        if (!ParserXMLUtility.doesNodeExists(doc, webServerExp)) { 
            createTag(doc, configExp, webServer);
        }

        if (!ParserXMLUtility.doesNodeExists(doc, handlersExp)) {
            createTag(doc, webServerExp, handlers);
        }

        HashMap<String, String> nodeAttribites = new HashMap<String, String>();
        nodeAttribites.put("name", "httppPlatformHandler");
        nodeAttribites.put("path", "*");
        nodeAttribites.put("verb", "*");
        nodeAttribites.put("modules", "httpPlatformHandler");
        nodeAttribites.put("resourceType", "Unspecified");
        ParserXMLUtility.updateOrCreateElement(doc, addExp, handlersExp, "add", false, nodeAttribites);

        nodeAttribites.clear();
        if (server.contains("tomcat")) {
            nodeAttribites.put("processPath", serverPath + "\\bin\\startup.bat");
            ParserXMLUtility.updateOrCreateElement(doc, platformExp, webServerExp, platform, false, nodeAttribites);
            if (!ParserXMLUtility.doesNodeExists(doc, varsExp)) {
                createTag(doc, platformExp, variables);
            }
            // update CATALINA_HOME
            updateVarValue(doc, "CATALINA_HOME", serverPath);
            // update CATALINA_OPTS
            updateVarValue(doc, "CATALINA_OPTS", catalinaOpts);
            // update JAVA_OPTS
            updateVarValue(doc, "JAVA_OPTS", javaOptsString);
        } else {
            nodeAttribites.put("processPath", "%JAVA_HOME%\\bin\\java.exe");
            nodeAttribites.put("startupTimeLimit", "30");
            nodeAttribites.put("startupRetryCount", "10");
            String arg = "-Djava.net.preferIPv4Stack=true  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT% -Djetty.port=%HTTP_PLATFORM_PORT% -Djetty.base=\"" + 
                    serverPath + "\" -Djetty.webapps=\"d:\\home\\site\\wwwroot\\webapps\"  -jar \"" + serverPath + "\\start.jar\" etc\\jetty-logging.xml";
            nodeAttribites.put("arguments", arg);
            ParserXMLUtility.updateOrCreateElement(doc, platformExp, webServerExp, platform, false, nodeAttribites);
        }
        ParserXMLUtility.saveXMLFile(fileName, doc);
    }

    public static void updateVarValue(Document doc, String propertyName, String value) throws WindowsAzureInvalidProjectOperationException {
        String nodeExpr = String.format(varExp, propertyName);
        HashMap<String, String> nodeAttribites = new HashMap<String, String>();
        nodeAttribites.put("name", propertyName);
        nodeAttribites.put("value", value);
        ParserXMLUtility.updateOrCreateElement(doc, nodeExpr, varsExp, variable, true, nodeAttribites);
    }

    public static boolean isWebConfigEditRequired(String fileName, String server) throws Exception {
        boolean editRequired = true;
        Document doc = ParserXMLUtility.parseXMLFile(fileName);
        String serverPath = "%programfiles(x86)%\\" + server;

        if (ParserXMLUtility.doesNodeExists(doc, webServerExp) && ParserXMLUtility.doesNodeExists(doc, handlersExp)) {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Element element = null;
            element = (Element) xPath.evaluate(addExp, doc, XPathConstants.NODE);
            if (element != null
                    && element.hasAttribute("name") && element.getAttribute("name").equals("httppPlatformHandler")
                    && element.hasAttribute("path") && element.getAttribute("path").equals("*")
                    && element.hasAttribute("verb") && element.getAttribute("verb").equals("*")
                    && element.hasAttribute("modules") && element.getAttribute("modules").equals("httpPlatformHandler")
                    && element.hasAttribute("resourceType") && element.getAttribute("resourceType").equals("Unspecified")) {
                element = (Element) xPath.evaluate(platformExp, doc, XPathConstants.NODE);
                if (server.contains("tomcat")) {
                    if (element != null
                            && element.hasAttribute("processPath") && element.getAttribute("processPath").equals(serverPath + "\\bin\\startup.bat")
                            && ParserXMLUtility.doesNodeExists(doc, varsExp)) {
                        // JAVA_OPTS
                        String nodeExpr = String.format(varExp, "JAVA_OPTS");
                        element = (Element) xPath.evaluate(nodeExpr, doc, XPathConstants.NODE);
                        if (element != null && element.hasAttribute("value") && element.getAttribute("value").equals(javaOptsString)) {
                            // CATALINA_HOME
                            nodeExpr = String.format(varExp, "CATALINA_HOME");
                            element = (Element) xPath.evaluate(nodeExpr, doc, XPathConstants.NODE);
                            if (element != null && element.hasAttribute("value") && element.getAttribute("value").equals(serverPath)) {
                                nodeExpr = String.format(varExp, "CATALINA_OPTS");
                                element = (Element) xPath.evaluate(nodeExpr, doc, XPathConstants.NODE);
                                if (element != null && element.hasAttribute("value") && element.getAttribute("value").equals(catalinaOpts)) {
                                    editRequired = false;
                                }
                            }
                        }
                    }
                } else {
                    String arg = "-Djava.net.preferIPv4Stack=true  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT% -Djetty.port=%HTTP_PLATFORM_PORT% -Djetty.base=\"" + 
                            serverPath + "\" -Djetty.webapps=\"d:\\home\\site\\wwwroot\\webapps\"  -jar \"" + serverPath + "\\start.jar\" etc\\jetty-logging.xml";
                    if (element != null
                            && element.hasAttribute("processPath")
                            && element.getAttribute("processPath").equals("%JAVA_HOME%\\bin\\java.exe")
                            && element.hasAttribute("startupTimeLimit")
                            && element.getAttribute("startupTimeLimit").equals("30")
                            && element.hasAttribute("startupRetryCount")
                            && element.getAttribute("startupRetryCount").equals("10")
                            && element.hasAttribute("arguments")
                            && element.getAttribute("arguments").equals(arg)) {
                        editRequired = false;
                    }
                }
            }
        }
        return editRequired;
    }
    
    public static void prepareDownloadAspx(String filePath, String url, String key, boolean isJDK) {
        String zipName = "server.zip";
        if (isJDK) {
            zipName = "jdk.zip";
        }
        String arguments = "";
        if (key.isEmpty()) {
            // public download
            arguments = "si.StartInfo.Arguments = \"/c wash.cmd file download \\\"" + url + "\\\" \\\"" + zipName + "\\\"\";";
        } else {
            // private download
            String jdkDir = url.substring(url.lastIndexOf("/") + 1, url.length());
            String container = url.split("/")[3];
            String accName = StorageRegistryUtilMethods.getAccNameFromUrl(url);
            String endpoint = StorageRegistryUtilMethods.getServiceEndpoint(url);
            endpoint = "http://" + endpoint.substring(endpoint.indexOf('.') + 1, endpoint.length());
            arguments = "si.StartInfo.Arguments = \"/c wash.cmd blob download \\\"" + zipName + "\\\" \\\"" + jdkDir + "\\\" "
                    + container + " " + accName + " \\\"" + key + "\\\" \\\"" + endpoint + "\\\"\";";
        }
        String[] aspxLines = {"<%@ Page Language=\"C#\" %>",
                "<script runat=server>",
                "protected String GetTime() {",
                "System.Diagnostics.Process si = new System.Diagnostics.Process();",
                "si.StartInfo.WorkingDirectory = @\"d:\\home\\site\\wwwroot\";",
                "si.StartInfo.UseShellExecute = false;",
                "si.StartInfo.FileName = \"cmd.exe\";",
                arguments,
                "si.StartInfo.CreateNoWindow = true;",
                "si.Start();",
                "si.Close();",
                "return DateTime.Now.ToString(\"t\");}",
                "</script>",
                "<html>",
                "<body>",
                "<form id=\"form1\" runat=\"server\">",
                "Current server time is <% =GetTime()%>.",
                "</form>",
                "</body>",
        "</html>"};

        File file = new File(filePath);
        try (FileOutputStream fop = new FileOutputStream(file)) {
            if (!file.exists()) {
                file.createNewFile();
            }
            for (int i = 0; i < aspxLines.length; i++) {
                byte[] contentInBytes = (aspxLines[i] + "\n").getBytes();
                fop.write(contentInBytes);
            }
            fop.flush();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void prepareExtractAspx(String filePath, boolean isJDK) {
        String zipName = "server.zip";
        if (isJDK) {
            zipName = "jdk.zip";
        }
        String[] aspxLines = {"<%@ Page Language=\"C#\" %>",
                "<script runat=server>",
                "private delegate void DoExtract();",
                "protected String GetTime() {",
                "DoExtract myAction = new DoExtract(Extraction);",
                "myAction.BeginInvoke(null, null);",
                "return DateTime.Now.ToString(\"t\");}",
                "private void Extraction() {",
                "var path1 = @\"d:\\home\\site\\wwwroot\\" + zipName + "\";",
                "var path2 = @\"d:\\home\\site\\wwwroot\\" + zipName.substring(0, zipName.lastIndexOf('.')) + "\";",
                "System.IO.Compression.ZipFile.ExtractToDirectory(path1, path2);}",
                "</script>",
                "<html>",
                "<body>",
                "<form id=\"form1\" runat=\"server\">",
                "Current server time is <% =GetTime()%>.",
                "</form>",
                "</body>",
        "</html>"};

        File file = new File(filePath);
        try (FileOutputStream fop = new FileOutputStream(file)) {
            if (!file.exists()) {
                file.createNewFile();
            }
            for (int i = 0; i < aspxLines.length; i++) {
                byte[] contentInBytes = (aspxLines[i] + "\n").getBytes();
                fop.write(contentInBytes);
            }
            fop.flush();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void prepareWebConfigForAppInit(String fileName, String[] pages)
            throws WindowsAzureInvalidProjectOperationException, IOException {
        Document doc = ParserXMLUtility.parseXMLFile(fileName);
        for (String page : pages) {
            HashMap<String, String> nodeAttribites = new HashMap<String, String>();
            String tmpPage = "/" + page;
            nodeAttribites.put("initializationPage", tmpPage);
            ParserXMLUtility.updateOrCreateElement(doc, String.format(appInitAddExp, tmpPage), appInitExp, "add", false, nodeAttribites);
        }
        ParserXMLUtility.saveXMLFile(fileName, doc);
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
