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
package com.microsoft.azuretools.docker.ui.wizards.publish;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;

import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class AzureSelectDockerWizard extends Wizard {
	private static final Logger log =  Logger.getLogger(AzureDockerUIResources.class.getName());
	
	private AzureSelectDockerHostPage azureSelectDockerHostPage;
	private AzureConfigureDockerContainerStep azureConfigureDockerContainerStep;

	private IProject project;
	private AzureDockerHostsManager dockerManager;
	private AzureDockerImageInstance dockerImageDescription;

	public AzureSelectDockerWizard(final IProject project, AzureDockerHostsManager dockerManager, AzureDockerImageInstance dockerImageDescription) {
	    this.project = project;
	    this.dockerManager = dockerManager;
	    this.dockerImageDescription = dockerImageDescription;

	    azureSelectDockerHostPage = new AzureSelectDockerHostPage(this);
	    azureConfigureDockerContainerStep = new AzureConfigureDockerContainerStep(this);

		setWindowTitle("Deploying Docker Container on Azure");
	}

	@Override
	public void addPages() {
		addPage(azureSelectDockerHostPage);
		addPage(azureConfigureDockerContainerStep);
	}

	@Override
	public boolean performFinish() {
		return doValidate();
	}
	
	public boolean doValidate() {
		return azureSelectDockerHostPage.doValidate() && azureConfigureDockerContainerStep.doValidate();
	}

	public AzureDockerImageInstance getDockerImageInstance() {
		return dockerImageDescription;
	}

	public IProject getProject() {
		return project;
	}

	public AzureDockerHostsManager getDockerManager() {
		return dockerManager;
	}

	public void setPredefinedDockerfileOptions(String artifactFileName) {
		if (azureConfigureDockerContainerStep != null) {
			azureConfigureDockerContainerStep.setPredefinedDockerfileOptions(artifactFileName);
		}
	}

	public void setDockerContainerName(String dockerContainerName) {
		if (azureConfigureDockerContainerStep != null) {
			azureConfigureDockerContainerStep.setDockerContainerName(dockerContainerName);
		}
	}

	public void selectDefaultDockerHost(DockerHost dockerHost, boolean selectOtherHosts) {
		if (azureSelectDockerHostPage != null) {
			azureSelectDockerHostPage.selectDefaultDockerHost(dockerHost, selectOtherHosts);
		}
	}

	public String deploy() {
		AzureDockerPreferredSettings dockerPreferredSettings = dockerManager.getDockerPreferredSettings();

		if (dockerPreferredSettings == null) {
			dockerPreferredSettings = new AzureDockerPreferredSettings();
		}
		dockerPreferredSettings.dockerApiName = dockerImageDescription.host.apiUrl;
		dockerPreferredSettings.dockerfileOption = dockerImageDescription.predefinedDockerfile;
		dockerManager.setDockerPreferredSettings(dockerPreferredSettings);

		DefaultLoader.getIdeHelper().runInBackground(project, "Deploying Docker Container on Azure", false, true, "Deploying Web app to a Docker host on Azure...", new Runnable() {
			@Override
			public void run() {
				try {
					DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
						@Override
						public void run() {
							if (!dockerImageDescription.hasNewDockerHost) {
								Session session = null;

								do {
									try {
										// check if the Docker host is accessible
										session = AzureDockerSSHOps.createLoginInstance(dockerImageDescription.host);
									} catch (Exception e) {
										session = null;
									}

									if (session == null) {
										EditableDockerHost editableDockerHost = new EditableDockerHost(dockerImageDescription.host);
										AzureInputDockerLoginCredsDialog loginCredsDialog = new AzureInputDockerLoginCredsDialog(PluginUtil.getParentShell(), project, editableDockerHost, dockerManager);

										if (loginCredsDialog.open() == Window.OK) {
											// Update Docker host log in credentials
											dockerImageDescription.host.certVault = editableDockerHost.updatedDockerHost.certVault;
											dockerImageDescription.host.hasSSHLogIn = editableDockerHost.updatedDockerHost.hasSSHLogIn;
											dockerImageDescription.host.hasPwdLogIn = editableDockerHost.updatedDockerHost.hasPwdLogIn;
//											AzureDockerVMOps.updateDockerHostVM(dockerManager.getSubscriptionsMap().get(dockerImageDescription.sid).azureClient, editableDockerHost.updatedDockerHost);
										} else {
											return;
										}
									}
								} while (session == null);
							}

							Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerImageDescription.sid).azureClient;
//							DockerContainerDeployTask task = new DockerContainerDeployTask(project, azureClient, dockerImageDescription);
//							task.queue();
						}
					});
				} catch (Exception e) {
					String msg = "An error occurred while attempting to deploy to the selected Docker host." + "\n" + e.getMessage();
					PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), "Failed to Deploy Web App as Docker Container", msg, e);
				}
			}
		});

		return String.format("%s://%s:%s/%s", (dockerImageDescription.isHttpsWebApp ? "https" : "http"),
				dockerImageDescription.host.hostVM.dnsName, dockerImageDescription.dockerPortSettings.split(":")[0], // "12345:80/tcp"
				dockerImageDescription.artifactName);
	}
	
}
