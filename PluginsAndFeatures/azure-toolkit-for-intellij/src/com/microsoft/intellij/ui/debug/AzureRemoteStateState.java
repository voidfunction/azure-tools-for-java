/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.ui.debug;

import com.intellij.debugger.engine.RemoteDebugProcessHandler;
import com.intellij.debugger.engine.RemoteStateState;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RemoteState;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.WAHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.wacommonutil.Utils;
import com.microsoftopentechnologies.azurecommons.xmlhandling.WebAppConfigOperations;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureRemoteStateState implements RemoteState {
    private final Project project;
    private final RemoteConnection myConnection;
    private final String webAppName;
    private final String socketPort;

    public AzureRemoteStateState(Project project,
                                 RemoteConnection connection,
                                 String webAppName,
                                 String socketPort) {
        this.project = project;
        this.myConnection = connection;
        this.webAppName = webAppName;
        this.socketPort = socketPort;
    }

    public ExecutionResult execute(final Executor executor, @NotNull final ProgramRunner runner) throws ExecutionException {
        try {
            // get web app name to which user want to debug his application
            String website = webAppName;
            if (!website.isEmpty()) {
                website = website.substring(0, website.indexOf('(')).trim();
                Map<WebSite, WebSiteConfiguration> webSiteConfigMap = AzureSettings.getSafeInstance(project).loadWebApps();
                // retrieve web apps configurations
                for (Map.Entry<WebSite, WebSiteConfiguration> entry : webSiteConfigMap.entrySet()) {
                    final WebSite websiteTemp = entry.getKey();
                    if (websiteTemp.getName().equals(website)) {
                        final WebSiteConfiguration webSiteConfiguration = entry.getValue();
                        // case - if user uses shortcut without going to Azure Tab
                        Map<String, Boolean> mp = AzureSettings.getSafeInstance(project).getWebsiteDebugPrep();
                        if (!mp.containsKey(website)) {
                            mp.put(website, false);
                        }
                        AzureSettings.getSafeInstance(project).setWebsiteDebugPrep(mp);
                        // check if web app prepared for debugging and process has started
                        if (AzureSettings.getSafeInstance(project).getWebsiteDebugPrep().get(website).booleanValue()
                                && !Utils.isPortAvailable(Integer.parseInt(socketPort))) {
                            ConsoleViewImpl consoleView = new ConsoleViewImpl(project, false);
                            RemoteDebugProcessHandler process = new RemoteDebugProcessHandler(project);
                            consoleView.attachToProcess(process);
                            return new DefaultExecutionResult(consoleView, process);
                        } else {
                            if (AzureSettings.getSafeInstance(project).getWebsiteDebugPrep().get(website).booleanValue()) {
                                // process not started
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        int choice = Messages.showOkCancelDialog(message("processDebug"), "Azure Web App", Messages.getQuestionIcon());
                                        if (choice == Messages.OK) {
                                            // check is there a need for preparation
                                            ProcessForDebug task = new ProcessForDebug(websiteTemp, webSiteConfiguration);
                                            try {
                                                task.queue();
                                                Messages.showInfoMessage(message("debugReady"), "Azure Web App");
                                            } catch (Exception e) {
                                                AzurePlugin.log(e.getMessage(), e);
                                            }
                                        }
                                    }
                                }, ModalityState.defaultModalityState());
                            } else {
                                // start the process of preparing the web app, in a blocking way
                                if (Utils.isPortAvailable(Integer.parseInt(socketPort))) {
                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            int choice = Messages.showOkCancelDialog(message("remoteDebug"), "Azure Web App", Messages.getQuestionIcon());
                                            if (choice == Messages.OK) {
                                                // check is there a need for preparation
                                                PrepareForDebug task = new PrepareForDebug(websiteTemp, webSiteConfiguration);
                                                try {
                                                    task.queue();
                                                    Messages.showInfoMessage(message("debugReady"), "Azure Web App");
                                                } catch (Exception e) {
                                                    AzurePlugin.log(e.getMessage(), e);
                                                }
                                            }
                                        }
                                    }, ModalityState.defaultModalityState());
                                } else {
                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            PluginUtil.displayErrorDialog(String.format(message("portMsg")), socketPort);
                                        }
                                    }, ModalityState.defaultModalityState());
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
        return null;
    }

    private class PrepareForDebug extends Task.Modal {
        WebSite webSite;
        WebSiteConfiguration webSiteConfiguration;

        public PrepareForDebug(WebSite webSite, WebSiteConfiguration webSiteConfiguration) {
            super(project, "Preparing web app for remote debugging", true);
            this.webSite = webSite;
            this.webSiteConfiguration = webSiteConfiguration;
        }

        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
            indicator.setFraction(0.1);
            String webSiteName = webSite.getName();
            String subId = webSiteConfiguration.getSubscriptionId();
            String webSpace = webSiteConfiguration.getWebSpaceName();
            try {
                // retrieve web apps configurations
                AzureManager manager = AzureManagerImpl.getManager(project);
                WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
                        webSiteConfiguration.getSubscriptionId(), webSiteConfiguration.getWebSpaceName(), webSiteName);
                indicator.setFraction(0.2);
                // retrieve ftp publish profile
                WebSitePublishSettings.FTPPublishProfile ftpProfile = null;
                for (WebSitePublishSettings.PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
                    if (pp instanceof WebSitePublishSettings.FTPPublishProfile) {
                        ftpProfile = (WebSitePublishSettings.FTPPublishProfile) pp;
                        break;
                    }
                }

                indicator.setFraction(0.3);

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
                        boolean webConfigPresent = false;
                        FTPFile[] files = ftp.listFiles("/site/wwwroot");
                        for (FTPFile file : files) {
                            if (file.getName().equalsIgnoreCase("web.config")) {
                                webConfigPresent = true;
                                break;
                            }
                        }

                        directories = ftp.listDirectories("/site/wwwroot/webapps");

                        indicator.setFraction(0.4);

                        // delete temporary file
                        String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, "web.config");
                        String remoteFile = "/site/wwwroot/web.config";
                        File tmpFile = new File(tmpPath);
                        if (tmpFile.exists()) {
                            tmpFile.delete();
                        }

                        indicator.setFraction(0.5);

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
                            String configFile = WAHelper.getDebugFile("web.config");
                            WAEclipseHelperMethods.copyFile(configFile, tmpPath);
                        }
                        if (updateRequired) {
                            // web app restart gives problem some times. So stop and start service
                            manager.stopWebSite(subId, webSpace, webSiteName);
                            WebAppConfigOperations.prepareWebConfigForDebug(tmpPath, serverFolder);
                            // delete old file and copy new file
                            ftp.deleteFile(remoteFile);
                            InputStream input = new FileInputStream(tmpPath);
                            ftp.storeFile("/site/wwwroot/web.config", input);
                            input.close();
                        }
                        indicator.setFraction(0.6);
                        ftp.logout();
                    } catch (Exception e) {
                        AzurePlugin.log(e.getMessage(), e);
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

                    indicator.setFraction(0.7);

                    for (FTPFile dir : directories) {
                        String sitePath = ftpProfile.getDestinationAppUrl();
                        if (!dir.getName().equalsIgnoreCase("ROOT")) {
                            sitePath = ftpProfile.getDestinationAppUrl() + "/" + dir.getName();
                        }
                        final String sitePathFinal = sitePath;
                        new Thread("Warm up the target site") {
                            public void run() {
                                try {
                                    WAHelper.sendGet(sitePathFinal);
                                }
                                catch (Exception ex) {
                                    AzurePlugin.log(ex.getMessage(), ex);
                                }
                            }
                        }.start();
                        Thread.sleep(5000);
                    }
                    AzureSettings.getSafeInstance(project).getWebsiteDebugPrep().put(webSiteName, true);
                    Thread.sleep(10000);
                    // already prepared. Just start debugSession.bat
                    // retrieve MSDeploy publish profile
                    WebSitePublishSettings.MSDeployPublishProfile msDeployProfile = null;
                    for (WebSitePublishSettings.PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
                        if (pp instanceof WebSitePublishSettings.MSDeployPublishProfile) {
                            msDeployProfile = (WebSitePublishSettings.MSDeployPublishProfile) pp;
                            break;
                        }
                    }
                    if (msDeployProfile != null) {
                        String command = String.format(message("debugCmd"), socketPort, webSiteName,
                                msDeployProfile.getUserName(), msDeployProfile.getPassword());
                        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", command);
                        pb.directory(new File(WAHelper.getTemplateFile("remotedebug")));
                        try {
                            pb.start();
                            Thread.sleep(30000);
                        } catch (Exception e) {
                            AzurePlugin.log(e.getMessage(), e);
                        }
                    }
                    indicator.setFraction(1.0);
                }
            } catch (Exception e) {
                AzurePlugin.log(e.getMessage(), e);
            } finally {
                indicator.setFraction(1.0);
            }
        }
    }

    private class ProcessForDebug extends Task.Modal {
        WebSite webSite;
        WebSiteConfiguration webSiteConfiguration;

        public ProcessForDebug(WebSite webSite, WebSiteConfiguration webSiteConfiguration) {
            super(project, "Starting debug process", true);
            this.webSite = webSite;
            this.webSiteConfiguration = webSiteConfiguration;
        }

        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
            try {
                indicator.setFraction(0.1);
                String webSiteName = webSite.getName();
                AzureManager manager = AzureManagerImpl.getManager(project);
                WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
                        webSiteConfiguration.getSubscriptionId(), webSiteConfiguration.getWebSpaceName(), webSiteName);
                // already prepared. Just start debugSession.bat
                // retrieve MSDeploy publish profile
                WebSitePublishSettings.MSDeployPublishProfile msDeployProfile = null;
                for (WebSitePublishSettings.PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
                    if (pp instanceof WebSitePublishSettings.MSDeployPublishProfile) {
                        msDeployProfile = (WebSitePublishSettings.MSDeployPublishProfile) pp;
                        break;
                    }
                }
                indicator.setFraction(0.5);
                if (msDeployProfile != null) {
                    String command = String.format(message("debugCmd"), socketPort, webSiteName,
                            msDeployProfile.getUserName(), msDeployProfile.getPassword());
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", command);
                    pb.directory(new File(WAHelper.getTemplateFile("remotedebug")));
                    try {
                        pb.start();
                        Thread.sleep(30000);
                    } catch (Exception e) {
                        AzurePlugin.log(e.getMessage(), e);
                    }
                }
                indicator.setFraction(1.0);
            } catch(AzureCmdException ex) {
                AzurePlugin.log(ex.getMessage(), ex);
            }
        }
    }

    public RemoteConnection getRemoteConnection() {
        return myConnection;
    }
}