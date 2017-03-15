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
package com.microsoft.azuretools.docker.ui.wizards.createhost;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;

public class AzureNewDockerWizard extends Wizard {

	
	private AzureNewDockerConfigPage azureNewDockerConfigPage;
	private AzureNewDockerLoginPage azureNewDockerLoginPage;
	
	private IProject project;
	private AzureDockerHostsManager dockerManager;
	private DockerHost newHost;

	public AzureNewDockerWizard(final IProject project, AzureDockerHostsManager dockerManager) {
	    this.project = project;
	    this.dockerManager = dockerManager;

	    newHost = dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName())));

	    azureNewDockerConfigPage = new AzureNewDockerConfigPage(this);
	    azureNewDockerLoginPage = new AzureNewDockerLoginPage(this);

	    setWindowTitle("Create Docker Host");
	}

	@Override
	public void addPages() {
		addPage(azureNewDockerConfigPage);
		addPage(azureNewDockerLoginPage);
	}

	@Override
	public boolean performFinish() {
		return doValidate();
	}
	
	public boolean doValidate() {
		return azureNewDockerConfigPage.doValidate() && azureNewDockerLoginPage.doValidate();
	}

	public void setNewDockerHost(DockerHost dockerHost) {
		newHost = dockerHost;
	}

	public DockerHost getDockerHost() {
		return newHost;
	}

	public IProject getProject() {
		return project;
	}

	public AzureDockerHostsManager getDockerManager() {
		return dockerManager;
	}

	public void create() {		
	}

}
