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
package com.microsoft.webapp.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.gigaspaces.azure.util.PreferenceWebAppUtil;
import com.gigaspaces.azure.views.WindowsAzureActivityLogView;
import com.gigaspaces.azure.wizards.WizardCacheManager;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoft.azureexplorer.helpers.PreferenceUtil;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.FTPPublishProfile;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings.PublishProfile;
import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoft.windowsazure.core.OperationStatus;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventListener;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.xmlhandling.WebAppConfigOperations;
import com.microsoftopentechnologies.wacommon.commoncontrols.ManageSubscriptionDialog;
import com.microsoftopentechnologies.wacommon.telemetry.AppInsightsCustomEvent;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;
import com.microsoftopentechnologies.wacommon.utils.WAExportWarEar;

public class WebAppDeployDialog extends TitleAreaDialog {
	org.eclipse.swt.widgets.List list;
	List<WebSite> webSiteList = new ArrayList<WebSite>();
	List<Subscription> subList = new ArrayList<Subscription>();
	Map<WebSite, WebSiteConfiguration> webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
	WebSite selectedWebSite;
	Button okButton;
	Button delBtn;
	Button deployToRoot;
	String webAppCreated = "";
	AzureCmdException exp = new AzureCmdException("");
	List<String> listToDisplay = new ArrayList<String>();
	File cmpntFile = new File(PluginUtil.getTemplateFile(Messages.cmpntFileName));

	public WebAppDeployDialog(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.webAppTtl);
		Image image = WebAppUtils.getImage(Messages.dlgImgPath);
		if (image != null) {
			setTitleImage(image);
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		okButton = getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(false);
		fillList(PreferenceUtil.loadPreference(String.format(Messages.webappKey, PluginUtil.getSelectedProject().getName())));
		return ctrl;
	}

	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.webAppTtl);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayout(gridLayout);
		container.setLayoutData(gridData);

		createWebAppLabel(container);
		createWebAppList(container);
		createDeployRootCheckBox(container);
		return super.createDialogArea(parent);
	}

	private void createWebAppLabel(Composite container) {
		Label label = new Label(container, SWT.LEFT);
		label.setText(Messages.webAppLbl);

		Link subLink = new Link(container, SWT.RIGHT);
		subLink.setText(Messages.linkLblSub);
		subLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (subList.isEmpty()) {
					createSubscriptionDialog(true);
				} else {
					createSubscriptionDialog(false);
				}
			}
		});
	}

	private void createWebAppList(Composite container) {
		list = new org.eclipse.swt.widgets.List(container, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.heightHint = 150;
		gridData.verticalIndent = 5;
		list.setLayoutData(gridData);

		list.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
			}

			@Override
			public void widgetSelected(SelectionEvent event) {
				int index = list.getSelectionIndex();
				if (index >= 0 && webSiteList.size() > index) {
					selectedWebSite = webSiteList.get(index);
					delBtn.setEnabled(true);
					if (webSiteConfigMap.get(webSiteList.get(index)).getJavaContainer().isEmpty()) {
						okButton.setEnabled(false);
					} else {
						okButton.setEnabled(true);
					}
				}
			}
		});

		createButtons(container);
	}

	private void createButtons(Composite container) {
		Composite containerButtons = new Composite(container, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.END;
		gridData.verticalAlignment = GridData.BEGINNING;
		containerButtons.setLayout(gridLayout);
		containerButtons.setLayoutData(gridData);

		Button newBtn = new Button(containerButtons, SWT.PUSH);
		newBtn.setText(Messages.newBtn);
		gridData = new GridData();
		gridData.widthHint = 70;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		newBtn.setLayoutData(gridData);
		newBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				CreateWebAppDialog dialog = new CreateWebAppDialog(getShell(), webSiteList);
				int result = dialog.open();
				if (result == Window.OK) {
					createWebApp(dialog);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		delBtn = new Button(containerButtons, SWT.PUSH);
		delBtn.setText(Messages.delBtn);
		gridData = new GridData();
		gridData.widthHint = 70;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		delBtn.setLayoutData(gridData);
		delBtn.setEnabled(false);
		delBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (selectedWebSite != null) {
					try {
						String name = selectedWebSite.getName();
						boolean choice = MessageDialog.openConfirm(getShell(), Messages.delTtl, String.format(Messages.delMsg, name));
						if (choice) {
							int index = list.getSelectionIndex();
							AzureManagerImpl.getManager().deleteWebSite(selectedWebSite.getSubscriptionId(),
									selectedWebSite.getWebSpaceName(), name);
							// update cached data as well
							webSiteList.remove(index);
							listToDisplay.remove(index);
							list.setItems(listToDisplay.toArray(new String[listToDisplay.size()]));
							webSiteConfigMap.remove(selectedWebSite);
							Activator.getDefault().getWebsiteDebugPrep().remove(selectedWebSite.getName());
							PreferenceWebAppUtil.save(webSiteConfigMap);
							if (webSiteConfigMap.isEmpty()) {
								setErrorMessage(Messages.noWebAppErrMsg);
							}
							// always disable button as after delete no entry is selected
							delBtn.setEnabled(false);
							selectedWebSite = null;
						}
					} catch (AzureCmdException e) {
						String msg = Messages.delErr + "\n" + String.format(Messages.webappExpMsg, e.getMessage());
						PluginUtil.displayErrorDialogAndLog(getShell(), Messages.errTtl, msg, e);
					}
				} else {
					PluginUtil.displayErrorDialog(getShell(), Messages.errTtl, "Select a web app container to delete.");
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}

	private void createDeployRootCheckBox(Composite container) {
		deployToRoot = new Button(container, SWT.CHECK);
		GridData groupGridData = new GridData();
		groupGridData.horizontalAlignment = SWT.FILL;
		groupGridData.horizontalSpan = 2;
		groupGridData.grabExcessHorizontalSpace = true;
		deployToRoot.setText(Messages.chkLbl);
		deployToRoot.setLayoutData(groupGridData);
	}

	private void validateAndFillList() {
		if (subList.isEmpty()) {
			setErrorMessage(Messages.noSubErrMsg);
			list.setItems(new String[]{""});
			selectedWebSite = null;
			createSubscriptionDialog(true);
		} else if (webSiteConfigMap.isEmpty()) {
			setErrorMessage(Messages.noWebAppErrMsg);
			list.setItems(new String[]{""});
			selectedWebSite = null;
		} else {
			setErrorMessage(null);
			setWebApps(webSiteConfigMap);
		}
	}

	private void createSubscriptionDialog(boolean invokeSignIn) {
		ManageSubscriptionDialog dialog = new ManageSubscriptionDialog(getShell(), false, invokeSignIn);
		dialog.create();
		dialog.open();
		subList = AzureManagerImpl.getManager().getSubscriptionList();
		if (subList.size() == 0) {
			setErrorMessage(Messages.noSubErrMsg);
			list.setItems(new String[]{""});
			selectedWebSite = null;
		} else {
			fillList(PreferenceUtil.loadPreference(String.format(Messages.webappKey, PluginUtil.getSelectedProject().getName())));
		}
	}

	private void setWebApps(Map<WebSite, WebSiteConfiguration> webSiteConfigMap) {
		webSiteList = new ArrayList<WebSite>(webSiteConfigMap.keySet());
		Collections.sort(webSiteList, new Comparator<WebSite>() {
			@Override
			public int compare(WebSite ws1, WebSite ws2) {
				return ws1.getName().compareTo(ws2.getName());
			}
		});
		// prepare list to display
		listToDisplay = WAEclipseHelperMethods.prepareListToDisplay(webSiteConfigMap, webSiteList);
		list.setItems(listToDisplay.toArray(new String[listToDisplay.size()]));
	}

	private class LoadWebAppsJob extends Job {
		public LoadWebAppsJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.loadWebApps, IProgressMonitor.UNKNOWN);
			try {
				Activator.getDefault().log(Messages.jobStart);
				webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
				AzureManager manager = AzureManagerImpl.getManager();
				subList = manager.getSubscriptionList();
				if (subList.size() > 0) {
					if (PreferenceWebAppUtil.isLoaded()) {
						webSiteConfigMap = PreferenceWebAppUtil.load();
					} else {
						if (manager.authenticated()) {
							// authenticated using AD. Proceed for Web Apps retrieval
							for (Subscription sub : subList) {
								List<String> resList = manager.getResourceGroupNames(sub.getId());
								for (String res : resList) {
									List<WebSite> webList = manager.getWebSites(sub.getId(), res);
									for (WebSite webSite : webList) {
										WebSiteConfiguration webSiteConfiguration = manager.
												getWebSiteConfiguration(webSite.getSubscriptionId(),
														webSite.getWebSpaceName(), webSite.getName());
										webSiteConfigMap.put(webSite, webSiteConfiguration);
									}
								}
							}
							PreferenceWebAppUtil.save(webSiteConfigMap);
							PreferenceWebAppUtil.setLoaded(true);
						} else {
							// imported publish settings file. Clear subscription
							manager.clearImportedPublishSettingsFiles();
							WizardCacheManager.clearSubscriptions();
							subList = manager.getSubscriptionList();
						}
					}
				}
			} catch(Exception ex) {
				Activator.getDefault().log(Messages.loadErrMsg, ex);
				super.setName("");
				monitor.done();
				return Status.CANCEL_STATUS;
			}
			super.setName("");
			monitor.done();
			return Status.OK_STATUS;
		}
	}

	private class CreateWebAppJob extends Job {
		CreateWebAppDialog dialog;
		WebSiteConfiguration config = null;
		AzureManager manager = AzureManagerImpl.getManager();
		String ftpPath = "/site/wwwroot/";
		String customFolderLoc = String.format("%s%s%s%s%s",
				PluginUtil.pluginFolder,
				File.separator, com.microsoft.webapp.util.Messages.webAppPluginID, File.separator, "customConfiguration");

		public CreateWebAppJob(String name) {
			super(name);
		}

		public void setDialog(CreateWebAppDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.createWebApps, IProgressMonitor.UNKNOWN);
			try {
				WebSite webSite = manager.createWebSite(dialog.getFinalSubId(), dialog.getFinalPlan(), dialog.getFinalName());
				WebSiteConfiguration webSiteConfiguration = manager.getWebSiteConfiguration(dialog.getFinalSubId(),
						webSite.getWebSpaceName(), webSite.getName());
				config = webSiteConfiguration;
				if (!dialog.getFinalJDK().isEmpty()) {
					enableCustomJDK();
				}
				webSiteConfiguration.setJavaVersion("1.8");
				String selectedContainer = dialog.getFinalContainer();
				if (selectedContainer.equalsIgnoreCase(WebAppsContainers.TOMCAT_8.getName())) {
					webSiteConfiguration.setJavaContainer("TOMCAT");
					webSiteConfiguration.setJavaContainerVersion(WebAppsContainers.TOMCAT_8.getValue());
				} else if (selectedContainer.equalsIgnoreCase(WebAppsContainers.TOMCAT_7.getName())) {
					webSiteConfiguration.setJavaContainer("TOMCAT");
					webSiteConfiguration.setJavaContainerVersion(WebAppsContainers.TOMCAT_7.getValue());
				} else if (selectedContainer.equalsIgnoreCase(WebAppsContainers.JETTY_9.getName())) {
					webSiteConfiguration.setJavaContainer("JETTY");
					webSiteConfiguration.setJavaContainerVersion(WebAppsContainers.JETTY_9.getValue());
				}
				config = manager.updateWebSiteConfiguration(dialog.getFinalSubId(), webSite.getWebSpaceName(), webSite.getName(),
						webSite.getLocation(), webSiteConfiguration);
				webAppCreated = webSite.getName();
				// update eclipse workspace preferences
				webSiteConfigMap.put(webSite, webSiteConfiguration);
				PreferenceWebAppUtil.save(webSiteConfigMap);
				if (!dialog.getFinalJDK().isEmpty()) {
					copyWebConfigForCustom();
				}
			} catch(AzureCmdException ex) {
				Activator.getDefault().log(Messages.createErrMsg, ex);
				super.setName("");
				monitor.done();
				exp = ex;
				return Status.CANCEL_STATUS;
			}
			super.setName("");
			monitor.done();
			return Status.OK_STATUS;
		}

		private void enableCustomJDK() throws AzureCmdException {
			if (config != null) {
				WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
						dialog.getFinalSubId(), config.getWebSpaceName(), dialog.getFinalName());
				// retrieve ftp publish profile
				WebSitePublishSettings.FTPPublishProfile ftpProfile = null;
				for (PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
					if (pp instanceof FTPPublishProfile) {
						ftpProfile = (FTPPublishProfile) pp;
						break;
					}
				}

				if (ftpProfile != null) {
					final FTPClient ftp = new FTPClient();
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
						// stop and restart web app
						manager.stopWebSite(dialog.getFinalSubId(), config.getWebSpaceName(), dialog.getFinalName());
						Thread.sleep(5000);
						// copy files required for download
						copyFilesForDownload(ftp);
						Thread.sleep(2000);
						manager.startWebSite(dialog.getFinalSubId(), config.getWebSpaceName(), dialog.getFinalName());
						// wait till zip files download
						while (!WebAppUtils.isFilePresentOnFTPServer(ftp, "jdk.zip")) {
							Thread.sleep(15000);
						}
						// copy files required for extraction
						manager.stopWebSite(dialog.getFinalSubId(), config.getWebSpaceName(), dialog.getFinalName());
						Thread.sleep(5000);
						copyFilesForExtract(ftp);
						Thread.sleep(2000);
						manager.startWebSite(dialog.getFinalSubId(), config.getWebSpaceName(), dialog.getFinalName());
						// wait till zip files extracted
						Thread.sleep(60000);
						while (!WebAppUtils.isFilePresentOnFTPServer(ftp, "jdk")) {
							Thread.sleep(15000);
						}
						String cloudVal = WindowsAzureProjectManager.getCloudValue(dialog.getFinalJDK(), cmpntFile);
						String jdkFolderName =  cloudVal.substring(cloudVal.indexOf("\\") + 1, cloudVal.length());
						String jdkPath = "/site/wwwroot/jdk/" + jdkFolderName;
						while (!WebAppUtils.checkFileCountOnFTPServer(ftp, jdkPath, 14)) {
							Thread.sleep(15000);
						}
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
				}
			}
		}

		private void copyFilesForDownload(FTPClient ftp) throws Exception {
			// wash.ps1
			String washPs1 = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.washName);
			InputStream input = new FileInputStream(washPs1);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.washName, input);

			// download.vbs
			String downloadName = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.downloadName);
			input = new FileInputStream(downloadName);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.downloadName, input);

			// edmDll
			String edmDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.edmDll);
			input = new FileInputStream(edmDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.edmDll, input);

			// odataDll
			String odataDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.odataDll);
			input = new FileInputStream(odataDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.odataDll, input);

			// clientDll
			String clientDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.clientDll);
			input = new FileInputStream(clientDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.clientDll, input);

			// configDll
			String configDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.configDll);
			input = new FileInputStream(configDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.configDll, input);

			// storageDll
			String storageDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.storageDll);
			input = new FileInputStream(storageDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.storageDll, input);

			// jsonDll
			String jsonDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.jsonDll);
			input = new FileInputStream(jsonDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.jsonDll, input);

			// spatialDll
			String spatialDll = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.spatialDll);
			input = new FileInputStream(spatialDll);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.spatialDll, input);

			// washCmd
			String washCmd = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.washCmd);
			input = new FileInputStream(washCmd);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.washCmd, input);

			// psConfig
			String psConfig = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.psConfig);
			input = new FileInputStream(psConfig);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.psConfig, input);

			// download.aspx
			String tmpDownloadPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, com.microsoft.webapp.util.Messages.downloadAspx);
			File file = new File(tmpDownloadPath);
			if (file.exists()) {
				file.delete();
			}
			WebAppConfigOperations.prepareDownloadAspx(tmpDownloadPath,
					WindowsAzureProjectManager.getCloudAltSrc(dialog.getFinalJDK(), cmpntFile), true);
			input = new FileInputStream(tmpDownloadPath);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.downloadAspx, input);

			// web.config for custom download
			String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, com.microsoft.webapp.util.Messages.configName);
			file = new File(tmpPath);
			if (file.exists()) {
				file.delete();
			}
			String configFile = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.configName);
			WAEclipseHelperMethods.copyFile(configFile, tmpPath);
			String[] pages = {com.microsoft.webapp.util.Messages.downloadAspx};
			WebAppConfigOperations.prepareWebConfigForAppInit(tmpPath, pages);
			input = new FileInputStream(tmpPath);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.configName, input);
		}

		private void copyFilesForExtract(FTPClient ftp) throws Exception {
			// extract.aspx
			String tmpExtractPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, com.microsoft.webapp.util.Messages.extractAspx);
			File file = new File(tmpExtractPath);
			if (file.exists()) {
				file.delete();
			}
			WebAppConfigOperations.prepareExtractAspx(tmpExtractPath, true);
			InputStream input = new FileInputStream(tmpExtractPath);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.extractAspx, input);
			

			// web.config for custom download
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.configName);
			String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, com.microsoft.webapp.util.Messages.configName);
			file = new File(tmpPath);
			if (file.exists()) {
				file.delete();
			}
			String configFile = String.format("%s%s%s", customFolderLoc, File.separator, com.microsoft.webapp.util.Messages.configName);
			WAEclipseHelperMethods.copyFile(configFile, tmpPath);
			String[] pages = {com.microsoft.webapp.util.Messages.extractAspx};
			WebAppConfigOperations.prepareWebConfigForAppInit(tmpPath, pages);
			input = new FileInputStream(tmpPath);
			ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.configName, input);
		}

		private void copyWebConfigForCustom() throws AzureCmdException {
			if (config != null) {
				WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
						dialog.getFinalSubId(), config.getWebSpaceName(), dialog.getFinalName());
				// retrieve ftp publish profile
				WebSitePublishSettings.FTPPublishProfile ftpProfile = null;
				for (PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
					if (pp instanceof FTPPublishProfile) {
						ftpProfile = (FTPPublishProfile) pp;
						break;
					}
				}

				if (ftpProfile != null) {
					FTPClient ftp = new FTPClient();
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
						ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.configName);
						String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, com.microsoft.webapp.util.Messages.configName);
						File file = new File(tmpPath);
						if (file.exists()) {
							file.delete();
						}
						String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator, com.microsoft.webapp.util.Messages.webAppPluginID);
						String configFile = String.format("%s%s%s", pluginInstLoc, File.separator, com.microsoft.webapp.util.Messages.configName);
						WAEclipseHelperMethods.copyFile(configFile, tmpPath);
						String cloudVal = WindowsAzureProjectManager.getCloudValue(dialog.getFinalJDK(), cmpntFile);
						String jdkFolderName =  cloudVal.substring(cloudVal.indexOf("\\") + 1, cloudVal.length());
						String jdkPath = "%HOME%\\site\\wwwroot\\jdk\\" + jdkFolderName;
						String serverPath = "%programfiles(x86)%\\" +
								WebAppUtils.generateServerFolderName(config.getJavaContainer(), config.getJavaContainerVersion());
						WebAppConfigOperations.prepareWebConfigForCustomJDKServer(tmpPath, jdkPath, serverPath);
						InputStream input = new FileInputStream(tmpPath);
						ftp.storeFile(ftpPath + com.microsoft.webapp.util.Messages.configName, input);
						cleanup(ftp);
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
				}
			}
		}

		private void cleanup(FTPClient ftp) throws IOException {
			// wash.ps1
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.washName);

			// download.vbs
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.downloadName);

			// edmDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.edmDll);

			// odataDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.odataDll);

			// clientDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.clientDll);

			// configDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.configDll);

			// storageDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.storageDll);

			// jsonDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.jsonDll);

			// spatialDll
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.spatialDll);

			// washCmd
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.washCmd);

			// psConfig
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.psConfig);

			// download.aspx
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.downloadAspx);

			//extract.aspx
			ftp.deleteFile(ftpPath + com.microsoft.webapp.util.Messages.extractAspx);
		}
	}

	private void fillList(final String nameToSelect) {
		list.setItems(new String[]{Messages.loadWebApps});
		PluginUtil.showBusy(true, getShell());
		LoadWebAppsJob job = new LoadWebAppsJob(Messages.loadWebApps);
		job.setPriority(Job.SHORT);
		job.schedule();
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							validateAndFillList();
							if (nameToSelect != null && !nameToSelect.isEmpty()) {
								for (int i = 0; i < webSiteList.size(); i++) {
									WebSite website = webSiteList.get(i);
									if (website.getName().equalsIgnoreCase(nameToSelect)) {
										list.select(i);
										selectedWebSite = webSiteList.get(i);
										okButton.setEnabled(true);
										delBtn.setEnabled(true);
										break;
									}
								}
							}
						}
					});
				} else {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							PluginUtil.displayErrorDialog(getShell(), Messages.errTtl, Messages.loadErrMsg);
							list.setItems(new String[]{""});
							selectedWebSite = null;
						}
					});
				}
				PluginUtil.showBusy(false, getShell());
			}
		});
	}

	private void createWebApp(CreateWebAppDialog dialog) {
		list.setItems(new String[]{Messages.createWebApps});
		PluginUtil.showBusy(true, getShell());
		CreateWebAppJob job = new CreateWebAppJob(Messages.createWebApps);
		job.setPriority(Job.SHORT);
		job.setDialog(dialog);
		job.schedule();
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							fillList(webAppCreated);
						}
					});
				} else {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							PluginUtil.showBusy(false, getShell());
							if (exp.getMessage().contains("Conflict: Website with given name")) {
								PluginUtil.displayErrorDialog(getShell(), Messages.errTtl, Messages.createErrMsg + " " + Messages.inUseErrMsg);
							} else {
								PluginUtil.displayErrorDialog(getShell(), Messages.errTtl, Messages.createErrMsg);
							}
							// if error while creating web app, display previous list
							list.setItems(listToDisplay.toArray(new String[listToDisplay.size()]));
						}
					});
				}
			}
		});
	}

	@Override
	protected void okPressed() {
		if (selectedWebSite != null) {
			final String selectedName =  selectedWebSite.getName();
			String selectedSubId = selectedWebSite.getSubscriptionId();
			String selectedWebSpace = selectedWebSite.getWebSpaceName();
			IProject project = PluginUtil.getSelectedProject();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						WindowsAzureActivityLogView waView = (WindowsAzureActivityLogView) PlatformUI
								.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(Messages.activityView);
						String desc = String.format(Messages.deplDesc, selectedName);
						waView.addDeployment(selectedName, desc, new Date());
						notifyProgress(selectedName, null, 20, OperationStatus.InProgress, desc);
					} catch (Exception e) {
						Activator.getDefault().log(e.getMessage(), e);
					}
				}
			});
			WebAppDeployJob job = new WebAppDeployJob(Messages.dplyWebApp, project, selectedName,
					selectedSubId, selectedWebSpace, deployToRoot.getSelection());
			job.setPriority(Job.SHORT);
			job.schedule();
			super.okPressed();
		} else {
			PluginUtil.displayErrorDialog(getShell(), Messages.errTtl, Messages.selWebAppMsg);
		}
	}

	public void notifyProgress(String deploymentId, String deploymentURL,
			int progress, OperationStatus inprogress, String message,
			Object... args) {
		DeploymentEventArgs arg = new DeploymentEventArgs(this);
		arg.setId(deploymentId);
		arg.setDeploymentURL(deploymentURL);
		arg.setDeployMessage(String.format(message, args));
		arg.setDeployCompleteness(progress);
		arg.setStartTime(new Date());
		arg.setStatus(inprogress);
		com.microsoftopentechnologies.wacommon.Activator.getDefault().fireDeploymentEvent(arg);
	}

	public class WebAppDeployJob extends Job {
		String name;
		IProject project;
		String selectedName;
		String selectedSubId;
		String selectedWebSpace;
		boolean isDeployToRoot;

		public WebAppDeployJob(String name, IProject project, String selectedName,
				String selectedSubId, String selectedWebSpace, boolean isDeployToRoot) {
			super(name);
			this.name = name;
			this.project = project;
			this.selectedName = selectedName;
			this.selectedSubId = selectedSubId;
			this.selectedWebSpace = selectedWebSpace;
			this.isDeployToRoot = isDeployToRoot;
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			MessageConsole console = com.microsoftopentechnologies.wacommon.Activator.findConsole(
					com.microsoftopentechnologies.wacommon.Activator.CONSOLE_NAME);
			console.clearConsole();
			final MessageConsoleStream out = console.newMessageStream();
			monitor.beginTask(name, IProgressMonitor.UNKNOWN);
			com.microsoftopentechnologies.wacommon.Activator.removeUnNecessaryListener();
			DeploymentEventListener undeployListnr = new DeploymentEventListener() {
				@Override
				public void onDeploymentStep(DeploymentEventArgs args) {
					monitor.subTask(args.toString());
					monitor.worked(args.getDeployCompleteness());
					out.println(args.toString());
				}
			};
			com.microsoftopentechnologies.wacommon.Activator.getDefault().addDeploymentEventListener(undeployListnr);
			com.microsoftopentechnologies.wacommon.Activator.depEveList.add(undeployListnr);

			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						notifyProgress(selectedName, null, 20, OperationStatus.InProgress, "");
						String impDestPath = String.format("%s%s%s%s", project.getLocation(), File.separator, project.getName(), ".war");
						WAExportWarEar.exportWarComponent(project.getName(), impDestPath);
						PluginUtil.refreshWorkspace();
						notifyProgress(selectedName, null, 20, OperationStatus.InProgress, "");
						AzureManager manager = AzureManagerImpl.getManager();
						manager.publishWebArchiveArtifact(selectedSubId, selectedWebSpace, selectedName,
								impDestPath, isDeployToRoot, project.getName());
						notifyProgress(selectedName, null, 20, OperationStatus.InProgress, "");
						WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(selectedSubId,
								selectedWebSpace, selectedName);
						WebSitePublishSettings.PublishProfile profile = webSitePublishSettings.getPublishProfileList().get(0);
						notifyProgress(selectedName, null, 20, OperationStatus.InProgress, "");
						String url = "";
						String destAppUrl = "";
						if (profile != null) {
							destAppUrl = profile.getDestinationAppUrl();
							url = destAppUrl;
							if (!isDeployToRoot) {
								String artifactName = project.getName().replaceAll("[^a-zA-Z0-9_-]+","");
								url = url + "/" + artifactName;
							}
						}

						final String sitePath = url;
						new Thread("Warm up the target site") {
							public void run() {
								try {
									WebAppUtils.sendGet(sitePath);
								}
								catch (Exception ex) {
									Activator.getDefault().log(ex.getMessage(), ex);
								}
							}
						}.start();

						Thread.sleep(2000);
						notifyProgress(selectedName, url, 50, OperationStatus.Succeeded, "Running");

						try {
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().
							showView("com.microsoft.azureexplorer.views.ServiceExplorerView");
						} catch (Exception ex) {
							// exception will occur if user do not install azure explorer plugin
							Activator.getDefault().log(ex.getMessage(), ex);
						}
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(Messages.activityView);
						// send telemetry event
						AppInsightsCustomEvent.createFTPEvent("WebAppFTP", destAppUrl, project.getName(), selectedSubId);
						// associate WebApp container with Java project for Republish functionality
						PreferenceUtil.savePreference(String.format(Messages.webappKey, project.getName()), selectedName);
					} catch (Exception e) {
						Activator.getDefault().log(e.getMessage(), e);
						notifyProgress(selectedName, null, 100, OperationStatus.Failed, e.getMessage());
					}
				}
			});

			super.setName("");
			monitor.done();
			super.done(Status.OK_STATUS);
			return Status.OK_STATUS;
		}
	}
}
