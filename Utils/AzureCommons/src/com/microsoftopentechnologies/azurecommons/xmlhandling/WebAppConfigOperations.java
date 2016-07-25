package com.microsoftopentechnologies.azurecommons.xmlhandling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

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

	public static void prepareWebConfigForCustomJDKServer(String fileName, String jdkPath, String serverPath)
			throws WindowsAzureInvalidProjectOperationException, IOException {
		Document doc = ParserXMLUtility.parseXMLFile(fileName);
		HashMap<String, String> nodeAttribites = new HashMap<String, String>();
		if (serverPath.contains("tomcat")) {
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
			if (!jdkPath.isEmpty()) {
				updateVarValue(doc, "JRE_HOME", jdkPath);
			}
		} else {
			String jdkProcessPath = "%JAVA_HOME%\\bin\\java.exe";
			if (!jdkPath.isEmpty()) {
				jdkProcessPath = jdkPath + "\\bin\\java.exe";
			}
			nodeAttribites.put("processPath", jdkProcessPath);
			nodeAttribites.put("startupTimeLimit", "30");
			nodeAttribites.put("startupRetryCount", "10");
			String arg = "-Djava.net.preferIPv4Stack=true  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT% -Djetty.port=%HTTP_PLATFORM_PORT% -Djetty.base=\"" + 
					serverPath + "\" -Djetty.webapps=\"d:\\home\\site\\wwwroot\\webapps\"  -jar \"" + serverPath + "\\start.jar\" etc\\jetty-logging.xml";
			nodeAttribites.put("arguments", arg);
			ParserXMLUtility.updateOrCreateElement(doc, platformExp, webServerExp, platform, false, nodeAttribites);
		}
		ParserXMLUtility.saveXMLFile(fileName, doc);
	}
}
