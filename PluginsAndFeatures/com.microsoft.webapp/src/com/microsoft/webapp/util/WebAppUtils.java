/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.webapp.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.webapp.activator.Activator;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class WebAppUtils {
	public static Image getImage(String entry) {
		Image image = null;
		try {
			URL imgUrl = Activator.getDefault().getBundle().getEntry(entry);
			URL imgFileURL = FileLocator.toFileURL(imgUrl);
			URL path = FileLocator.resolve(imgFileURL);
			String imgpath = path.getFile();
			image = new Image(null, new FileInputStream(imgpath));
		} catch (Exception e){
			Activator.getDefault().log(e.getMessage(), e);
		}
		return image;
	}

	// HTTP GET request
	public static void sendGet(String sitePath) throws Exception {
		URL url = new URL(sitePath);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "AzureToolkit for Eclipse");
		con.getResponseCode();
	}
	
	public static void openDebugLaunchDialog(Object toSelect) {
		ILaunchGroup[] grp = DebugUITools.getLaunchGroups();
		DebugUITools.openLaunchConfigurationDialogOnGroup(PluginUtil.getParentShell(),
				new StructuredSelection(toSelect), grp[1].getIdentifier());
	}

	public static boolean isFilePresentOnFTPServer(FTPClient ftp, String fileName) throws IOException {
		boolean filePresent = false;
		FTPFile[] files = ftp.listFiles("/site/wwwroot");
		for (FTPFile file : files) {
			if (file.getName().equalsIgnoreCase(fileName)) {
				filePresent = true;
				break;
			}
		}
		return filePresent;
	}

	public static boolean checkFileCountOnFTPServer(FTPClient ftp, String path, int fileCount) throws IOException {
		boolean fileExtracted = false;
		FTPFile[] files = ftp.listFiles(path);
		if (files.length >= fileCount) {
			fileExtracted = true;
		}
		return fileExtracted;
	}

	public static String generateServerFolderName(String server, String version) {
		String serverFolder = "";
		if (server.equalsIgnoreCase("TOMCAT")) {
			if (version.equalsIgnoreCase(WebAppsContainers.TOMCAT_8.getValue())) {
				version = WebAppsContainers.TOMCAT_8.getCurrentVersion();
			} else if (version.equalsIgnoreCase(WebAppsContainers.TOMCAT_7.getValue())) {
				version = WebAppsContainers.TOMCAT_7.getCurrentVersion();
			}
			serverFolder = String.format("%s%s%s", "apache-tomcat", "-", version);
		} else {
			if (version.equalsIgnoreCase(WebAppsContainers.JETTY_9.getValue())) {
				version = WebAppsContainers.JETTY_9.getCurrentVersion();
			}
			String version1 = version.substring(0, version.lastIndexOf('.') + 1);
			String version2 = version.substring(version.lastIndexOf('.') + 1, version.length());
			serverFolder = String.format("%s%s%s%s%s", "jetty-distribution", "-", version1, "v", version2);
		}
		return serverFolder;
	}
}
