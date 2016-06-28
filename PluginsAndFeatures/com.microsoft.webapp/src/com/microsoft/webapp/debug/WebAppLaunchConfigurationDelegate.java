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
package com.microsoft.webapp.debug;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import com.gigaspaces.azure.util.PreferenceWebAppUtil;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.FTPPublishProfile;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.MSDeployPublishProfile;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.PublishProfile;
import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.config.Messages;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.wacommonutil.Utils;
import com.microsoftopentechnologies.azurecommons.xmlhandling.WebAppConfigOperations;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;


@SuppressWarnings("restriction")
public class WebAppLaunchConfigurationDelegate extends JavaRemoteApplicationLaunchConfigurationDelegate implements ILaunchConfigurationDelegate  {

	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		final ILaunchConfiguration configToUse = config;
		String port = "8000";
		Map<String, String> conMap = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, new HashMap<String,String>());
		for (java.util.Map.Entry<String, String> entry : conMap.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("port")) {
				port = entry.getValue();
			}
		}
		final String portToDisplayError = port;
		try {
			// check port availability
			if (Utils.isPortAvailable(Integer.parseInt(port))) {
				// get web app name to which user want to debug his application
				String website = config.getAttribute(AzureLaunchConfigurationAttributes.WEBSITE_DISPLAY, "");
				if (!website.isEmpty()) {
					website = website.substring(0, website.indexOf('(')).trim();
					Map<WebSite, WebSiteConfiguration> webSiteConfigMap = PreferenceWebAppUtil.load();
					// retrieve web apps configurations
					for (Entry<WebSite, WebSiteConfiguration> entry : webSiteConfigMap.entrySet()) {
						final WebSite websiteTemp = entry.getKey();
						if (websiteTemp.getName().equals(website)) {
							// check is there a need for preparation
							AzureManager manager = AzureManagerImpl.getManager();
							final WebSiteConfiguration webSiteConfiguration = entry.getValue();
							final WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
									webSiteConfiguration.getSubscriptionId(), webSiteConfiguration.getWebSpaceName(), website);
							// case - if user uses shortcut without going to Azure Tab
							Map<String, Boolean> mp = Activator.getDefault().getWebsiteDebugPrep();
							if (!mp.containsKey(website)) {
								mp.put(website, false);
							}
							Activator.getDefault().setWebsiteDebugPrep(mp);

							if (Activator.getDefault().getWebsiteDebugPrep().get(website).booleanValue()) {
								// already prepared. Just start debugSession.bat
								// retrieve MSDeploy publish profile
								WebSitePublishSettings.MSDeployPublishProfile msDeployProfile = null;
								for (PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
									if (pp instanceof MSDeployPublishProfile) {
										msDeployProfile = (MSDeployPublishProfile) pp;
										break;
									}
								}
								if (msDeployProfile != null) {
									ProcessBuilder pb = null;
									String os = System.getProperty("os.name").toLowerCase();
									String webAppDirPath = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator, com.microsoft.webapp.util.Messages.webAppPluginID);
									if (Activator.IS_WINDOWS) {
										String command = String.format(Messages.command, port, website,
												msDeployProfile.getUserName(), msDeployProfile.getPassword());
										pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", command);
									} else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0) {
										// escape $ for linux
										String userName = "\\" + msDeployProfile.getUserName();
										String command = String.format(Messages.commandSh, port, website,
												userName, msDeployProfile.getPassword());
										pb = new ProcessBuilder("/bin/bash", "-c", command);
									} else {
										String command = String.format(Messages.commandMac, webAppDirPath + "/", port, website,
												msDeployProfile.getUserName(), msDeployProfile.getPassword());
										String commandNext = "tell application \"Terminal\" to do script \"" + command + "\"";
										pb = new ProcessBuilder("osascript", "-e", commandNext);
									}
									pb.directory(new File(webAppDirPath));
									try {
										pb.start();
										Thread.sleep(10000);
									} catch (Exception e) {
										Activator.getDefault().log(Messages.errTtl, e);
									}
									super.launch(config, mode, launch, monitor);
								}
							} else {
								// start the process of preparing the web app, in a blocking way
								Display.getDefault().syncExec(new Runnable()
								{
									@Override
									public void run()
									{
										boolean choice = MessageDialog.openConfirm(PluginUtil.getParentShell(), Messages.title, Messages.remoteDebug);
										if (choice) {
											IRunnableWithProgress op = new PrepareForDebug(websiteTemp, webSiteConfiguration, webSitePublishSettings);
											try {
												new ProgressMonitorDialog(PluginUtil.getParentShell()).run(true, true, op);
												MessageDialog.openInformation(PluginUtil.getParentShell(), Messages.title, Messages.debugReady);
											} catch (Exception e) {
												Activator.getDefault().log(e.getMessage(), e);
											}
										}
										WebAppUtils.openDebugLaunchDialog(configToUse);
									}
								});
							}
							break;
						}
					}
				}

			} else {
				Display.getDefault().syncExec(new Runnable()
				{
					@Override
					public void run()
					{
						PluginUtil.displayErrorDialog(PluginUtil.getParentShell(), Messages.errTtl, String.format(Messages.portMsg, portToDisplayError));
						WebAppUtils.openDebugLaunchDialog(configToUse);
					}
				});
			}
		} catch(Exception ex) {
			Activator.getDefault().log(ex.getMessage(), ex);
		}
	}

	private class PrepareForDebug implements IRunnableWithProgress
	{
		private int workload = 100;
		WebSite webSite;
		WebSiteConfiguration webSiteConfiguration;
		WebSitePublishSettings webSitePublishSettings;

		PrepareForDebug(WebSite webSite, WebSiteConfiguration webSiteConfiguration, WebSitePublishSettings webSitePublishSettings) {
			this.webSite = webSite;
			this.webSiteConfiguration = webSiteConfiguration;
			this.webSitePublishSettings = webSitePublishSettings;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			String webSiteName = webSite.getName();
			monitor.beginTask(String.format(Messages.prepareDebug, webSiteName), workload);
			try {
				// retrieve web apps configurations
				monitor.worked(10);

				AzureManager manager = AzureManagerImpl.getManager();
				String subId = webSiteConfiguration.getSubscriptionId();
				String webSpace = webSiteConfiguration.getWebSpaceName();

				monitor.worked(20);

				// retrieve ftp publish profile
				WebSitePublishSettings.FTPPublishProfile ftpProfile = null;
				for (PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
					if (pp instanceof FTPPublishProfile) {
						ftpProfile = (FTPPublishProfile) pp;
						break;
					}
				}

				monitor.worked(30);

				if (ftpProfile != null) {
					final FTPClient ftp = new FTPClient();
					FTPFile[] directories = null;
					try {
						URI uri = null;
						uri = new URI(ftpProfile.getPublishUrl());
						ftp.connect(uri.getHost());
						final int replyCode = ftp.getReplyCode();
						if (!FTPReply.isPositiveCompletion(replyCode)) {
							ftp.disconnect();
						}
						if (!ftp.login(ftpProfile.getUserName(), ftpProfile.getPassword())) {
							ftp.logout();
						}
						ftp.setFileType(FTP.BINARY_FILE_TYPE);
						if (ftpProfile.isFtpPassiveMode()) {
							ftp.enterLocalPassiveMode();
						}
						boolean webConfigPresent = isWebConfigPresent(ftp);

						directories = ftp.listDirectories("/site/wwwroot/webapps");

						monitor.worked(40);

						// delete temporary file
						String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, com.microsoft.webapp.util.Messages.configName);
						String remoteFile = "/site/wwwroot/web.config";
						File tmpFile = new File(tmpPath);
						if (tmpFile.exists()) {
							tmpFile.delete();
						}

						monitor.worked(50);

						// prepare server directory path as per server configuration
						String server = webSiteConfiguration.getJavaContainer();
						String version = webSiteConfiguration.getJavaContainerVersion();
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

						boolean updateRequired = true;
						if (webConfigPresent) {
							// download from web app server
							OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
							ftp.retrieveFile(remoteFile, outputStream);
							outputStream.close();
							updateRequired = WebAppConfigOperations.isWebConfigEditRequired(tmpPath, serverFolder);
						} else {
							// copy file from plugin repository
							String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator, com.microsoft.webapp.util.Messages.webAppPluginID);
							String configFile = String.format("%s%s%s", pluginInstLoc, File.separator, com.microsoft.webapp.util.Messages.configName);
							WAEclipseHelperMethods.copyFile(configFile, tmpPath);
						}
						if (updateRequired) {
							// web app restart gives problem some times. So stop and start service
							manager.stopWebSite(subId, webSpace, webSiteName);
							Thread.sleep(5000);
							WebAppConfigOperations.prepareWebConfigForDebug(tmpPath, serverFolder);
							// delete old file and copy new file
							ftp.deleteFile(remoteFile);
							Thread.sleep(5000);
							InputStream input = new FileInputStream(tmpPath);
							ftp.storeFile("/site/wwwroot/web.config", input);
							int attempts = 0;
							while (!isWebConfigPresent(ftp) && attempts < 5) {
								attempts++;
								ftp.storeFile("/site/wwwroot/web.config", input);
								Activator.getDefault().log("Web.config copy attempt : " + attempts);
								Thread.sleep(2000);
							}
							input.close();
						}
						monitor.worked(60);
						ftp.logout();
					} catch (Exception e) {
						Activator.getDefault().log(e.getMessage(), e);
					} finally {
						if (ftp.isConnected()) {
							try {
								ftp.disconnect();
							} catch (IOException ignored) {
							}
						}
					}

					// Enable Web socket and start web app
					manager.enableWebSockets(subId, webSpace, webSiteName, webSite.getLocation(), true);
					// if web site is stopped we will require to start for debugging
					manager.startWebSite(subId, webSpace, webSiteName);
					Thread.sleep(10000);

					monitor.worked(70);

					for (FTPFile dir : directories) {
						String sitePath = ftpProfile.getDestinationAppUrl();
						if (!dir.getName().equalsIgnoreCase("ROOT")) {
							sitePath = ftpProfile.getDestinationAppUrl() + "/" + dir.getName();
						}
						final String sitePathFinal = sitePath;
						new Thread("Warm up the target site") {
							public void run() {
								try {
									WebAppUtils.sendGet(sitePathFinal);
								}
								catch (Exception ex) {
									Activator.getDefault().log(ex.getMessage(), ex);
								}
							}
						}.start();
						Thread.sleep(5000);
					}
					Activator.getDefault().getWebsiteDebugPrep().put(webSiteName, true);
					Thread.sleep(10000);
					monitor.worked(100);
				}
			} catch (Exception e) {
				Activator.getDefault().log(e.getMessage(), e);
			} finally {
				monitor.done();
			}
			// Check if the user pressed "cancel"
			if (monitor.isCanceled())
			{
				monitor.done();
				Activator.getDefault().getWebsiteDebugPrep().put(webSiteName, false);
				return;
			}
		}
	}

	private boolean isWebConfigPresent(FTPClient ftp) throws IOException {
		boolean webConfigPresent = false;
		FTPFile[] files = ftp.listFiles("/site/wwwroot");
		for (FTPFile file : files) {
			if (file.getName().equalsIgnoreCase(com.microsoft.webapp.util.Messages.configName)) {
				webConfigPresent = true;
				break;
			}
		}
		return webConfigPresent;
	}
}