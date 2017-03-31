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
package com.microsoft.azuretools.docker.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AzureDockerUIResources {
	private static final Logger log =  Logger.getLogger(AzureDockerUIResources.class.getName());
	public static boolean CANCELED = false;

	public static void updateAzureResourcesWithProgressDialog(Shell shell, IProject project) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		try {
			dialog.run(true, true, new IRunnableWithProgress(){
				public void run(IProgressMonitor monitor) {
					monitor.beginTask("Loading Azure Resources", 100);
					try {
						monitor.subTask("Creating an Azure instance...");
						AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}
						// not signed in
						if (azureAuthManager == null) {
							monitor.done();
							return;
						}
						AzureDockerHostsManager dockerManager = AzureDockerHostsManager
								.getAzureDockerHostsManagerEmpty(azureAuthManager);
						monitor.worked(10);

						monitor.subTask("Retrieving the subscription details...");
						dockerManager.refreshDockerSubscriptions();
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}
						
						monitor.worked(10);
						monitor.subTask("Retrieving the key vault...");
						dockerManager.refreshDockerVaults();
						monitor.worked(10);
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}
						
						monitor.worked(10);
						monitor.subTask("Retrieving the key vault details...");
						dockerManager.refreshDockerVaultDetails();
						if (monitor.isCanceled()) {
							CANCELED = true;
							monitor.done();
							return;
						}
						
						monitor.worked(10);
						monitor.subTask("Retrieving the network details...");
						dockerManager.refreshDockerVnetDetails();
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}
						
						monitor.worked(10);
						monitor.subTask("Retrieving the storage account details...");
						dockerManager.refreshDockerStorageAccountDetails();
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}

						monitor.subTask("Retrieving the Docker virtual machines details...");
						dockerManager.refreshDockerHostDetails();
						CANCELED = false;
					} catch (Exception e) {
						CANCELED = true;
						log.log(Level.SEVERE, "updateAzureResourcesWithProgressDialog: " + e.getMessage(), e);
						e.printStackTrace();
					}
					
					monitor.done();
				}
			});
		} catch (Exception e) {
			CANCELED = true;
			log.log(Level.SEVERE, "updateAzureResourcesWithProgressDialog: " + e.getMessage(), e);
			e.printStackTrace();
		}
	  }
	
	public static void createDockerKeyVault(DockerHost dockerHost, AzureDockerHostsManager dockerManager) {
		Job createDockerHostJob = new Job(String.format("Creating Azure Key Vault %s for %s", dockerHost.certVault.name, dockerHost.name)) {
			@Override
			protected IStatus run(IProgressMonitor progressMonitor) {
				progressMonitor.beginTask("start task", 100);
		        try {
					progressMonitor.subTask(String.format("Reading subscription details for Docker host %s ...", dockerHost.apiUrl));
					progressMonitor.worked(5);
					Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
					KeyVaultClient keyVaultClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).keyVaultClient;
					if (progressMonitor.isCanceled()) {
						if (displayWarningOnCreateKeyVaultCancelAction() == 0) {
							progressMonitor.done();
							return Status.CANCEL_STATUS;
						}
					}
					
					String retryMsg = "Create";
					int retries = 5;
					AzureDockerCertVault certVault = null;
					do {
						progressMonitor.subTask(String.format("%s new key vault %s ...", retryMsg, dockerHost.certVault.name));
						progressMonitor.worked(15 + 15 * retries);
						if (AzureDockerUtils.DEBUG) System.out.println(retryMsg + " new Docker key vault: " + new Date().toString());
						AzureDockerCertVaultOps.createOrUpdateVault(azureClient, dockerHost.certVault, keyVaultClient);
						if (AzureDockerUtils.DEBUG) System.out.println("Done creating new key vault: " + new Date().toString());
						if (progressMonitor.isCanceled()) {
							if (displayWarningOnCreateKeyVaultCancelAction() == 0) {
								progressMonitor.done();
								return Status.CANCEL_STATUS;
							}
						}
						certVault = AzureDockerCertVaultOps.getVault(azureClient, dockerHost.certVault.name,
								dockerHost.certVault.resourceGroupName, keyVaultClient);
						retries++;
						retryMsg = "Retry creating";
					} while (retries < 5 && (certVault == null || certVault.vmUsername == null)); // Retry couple times

					progressMonitor.subTask("Updating key vaults ...");
					progressMonitor.worked(95);
					if (AzureDockerUtils.DEBUG) System.out.println("Refreshing key vaults: " + new Date().toString());
					dockerManager.refreshDockerVaults();
					dockerManager.refreshDockerVaultDetails();
					if (AzureDockerUtils.DEBUG) System.out.println("Done refreshing key vaults: " + new Date().toString());
					
//					progressMonitor.subTask("");
//					progressMonitor.worked(1);
//					if (progressMonitor.isCanceled()) {
//						if (displayWarningOnCreateKeyVaultCancelAction() == 0) {
//							progressMonitor.done();
//							return Status.CANCEL_STATUS;
//						}
//					}
//
		            progressMonitor.done();
					return Status.OK_STATUS;
				} catch (Exception e) {
					String msg = "An error occurred while attempting to create Docker host." + "\n" + e.getMessage();
					log.log(Level.SEVERE, "createHost: " + msg, e);
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}
		};
		
		createDockerHostJob.schedule();		
	}
	
	public static void createArtifact(Shell shell, IProject project) {
		if (project == null) {
			return;
		}
		
        String projectName = project.getName();
        String destinationPath = project.getLocation() + "/" + projectName + ".war";
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		try {
			dialog.run(true, true, new IRunnableWithProgress(){
				public void run(IProgressMonitor progressMonitor) {
					progressMonitor.beginTask("Creating WAR artifact", 100);
					try {
						progressMonitor.subTask(String.format("Building selected project: %s ...", project.getName()));
						progressMonitor.worked(35);
				        project.build(IncrementalProjectBuilder.FULL_BUILD, null);
						
						progressMonitor.subTask("Exporting to WAR ...");
						progressMonitor.worked(75);
				        IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());
				        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, projectName);
				        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, destinationPath);

				        dataModel.getDefaultOperation().execute(null, null);
						
//						progressMonitor.subTask("");
//						progressMonitor.worked(1);
//						if (progressMonitor.isCanceled()) {
//							if (displayWarningOnCreateKeyVaultCancelAction() == 0) {
//								progressMonitor.done();
//								return Status.CANCEL_STATUS;
//							}
//						}
			            progressMonitor.done();
					} catch (Exception e) {
						String msg = "An error occurred while attempting to create WAR artifact" + "\n" + e.getMessage();
						log.log(Level.SEVERE, "createArtifact: " + msg, e);
						e.printStackTrace();
					}
					
					progressMonitor.done();
				}
			});
		} catch (Exception e) {
			CANCELED = true;
			log.log(Level.SEVERE, "updateAzureResourcesWithProgressDialog: " + e.getMessage(), e);
			e.printStackTrace();
		}
	}

	private static int displayWarningOnCreateKeyVaultCancelAction(){
		Display currentDisplay = Display.getCurrent();
		Shell shell = currentDisplay.getActiveShell();
		
		if (shell != null) {
			MessageBox displayConfirmationDialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
			displayConfirmationDialog.setText("Stop Create Azure Key Vault");
			displayConfirmationDialog.setMessage("This action can leave the Docker virtual machine host in an partial setup state and which can cause publishing to a Docker container to fail!\n\n Are you sure you want this?");
			return displayConfirmationDialog.open();
		}
		
		return 1;
	}
	
	public static Color getColor(int systemColorID) {
		Display display = Display.getCurrent();
		return display.getSystemColor(systemColorID);
	}
	
	public static IProject getCurrentSelectedProject() {
		IProject project = null;
		ISelectionService selectionService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectionService.getSelection();

		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();

			if (element instanceof IResource) {
				project = ((IResource) element).getProject();
			} else if (element instanceof PackageFragmentRoot) {
				IJavaProject jProject = ((PackageFragmentRoot) element).getJavaProject();
				project = jProject.getProject();
			} else if (element instanceof IJavaElement) {
				IJavaProject jProject = ((IJavaElement) element).getJavaProject();
				project = jProject.getProject();
			}
		}
		
		if (project == null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			if (workspace.getRoot() != null && workspace.getRoot().getProjects().length > 0) {
				IProject[] projects = workspace.getRoot().getProjects();
				project = projects[projects.length - 1];
			} else {
				PluginUtil.displayErrorDialog(Display.getDefault().getActiveShell(), "No Active Project", "Must have a project first");
			}
		}

		return project;
	}

}
