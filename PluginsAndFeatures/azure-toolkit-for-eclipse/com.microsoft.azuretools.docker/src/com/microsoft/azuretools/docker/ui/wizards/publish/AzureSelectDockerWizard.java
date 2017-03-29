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
import org.eclipse.jface.wizard.Wizard;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;

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
	
}
