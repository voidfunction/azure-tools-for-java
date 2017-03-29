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
package com.microsoft.azuretools.docker.handlers;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.wizards.createhost.AzureNewDockerWizard;
import com.microsoft.azuretools.docker.ui.wizards.publish.AzureSelectDockerWizard;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import java.util.logging.Level;
import java.util.logging.Logger;


public class AzureDockerHostDeployHandler extends AbstractHandler {
	private static final Logger log =  Logger.getLogger(AzureDockerHostDeployHandler.class.getName());

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ISelectionService selectionService = window.getSelectionService();
		ISelection selection = selectionService.getSelection();
		Shell shell = window.getShell();
		
		if(selection instanceof IStructuredSelection) {
			 Object element = ((IStructuredSelection)selection).getFirstElement();
			
			if (element instanceof IResource) {
				IProject project = ((IResource)element).getProject();
				
				// TODO check the project is Dynamic Web Application
				
//				WebAppDeployDialog d = WebAppDeployDialog.go(window.getShell(), project);
				
				try {
					AzureDockerUIResources.createArtifact(shell, project);
					
					AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();

					// not signed in
					if (azureAuthManager == null) {
						System.out.println("ERROR! Not signed in!");
						return null;
					}

					AzureDockerHostsManager dockerManager = AzureDockerHostsManager
							.getAzureDockerHostsManager(azureAuthManager);

					if (!dockerManager.isInitialized()) {
						AzureDockerUIResources.updateAzureResourcesWithProgressDialog(shell, project);
						if (AzureDockerUIResources.CANCELED) {
							return null;
						}
						dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
					}

					if (dockerManager.getSubscriptionsMap().isEmpty()) {
						PluginUtil.displayErrorDialog(shell, "Create Docker Host",
								"Must select an Azure subscription first");
						return null;
					}
					
					DockerHost dockerHost = (dockerManager.getDockerPreferredSettings() != null) ? dockerManager.getDockerHostForURL(dockerManager.getDockerPreferredSettings().dockerApiName) : null;
					AzureDockerImageInstance dockerImageDescription = dockerManager.getDefaultDockerImageDescription(project.getName(), dockerHost);
					AzureSelectDockerWizard selectDockerWizard = new AzureSelectDockerWizard(project, dockerManager, dockerImageDescription);
					WizardDialog selectDockerHostDialog = new WizardDialog(shell, selectDockerWizard);
					if (selectDockerHostDialog.open() == Window.OK) {

//					AzureNewDockerWizard newDockerWizard = new AzureNewDockerWizard(project, dockerManager);
//					WizardDialog createNewDockerHostDialog = new WizardDialog(shell, newDockerWizard);
//					if (createNewDockerHostDialog.open() == Window.OK) {
//						newDockerWizard.createHost();
//					} else {
//						MessageDialog.openInformation(
//								shell,
//								"WebAppPlugin",
//								"Canceled");
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "execute: " + e.getMessage(), e);
					e.printStackTrace();					
				}
				
			} else {
				MessageDialog.openInformation(
						shell,
						"Publish as Docker Container",
						"Please select a project first");
			}
		}
		
		return null;
	}
}
