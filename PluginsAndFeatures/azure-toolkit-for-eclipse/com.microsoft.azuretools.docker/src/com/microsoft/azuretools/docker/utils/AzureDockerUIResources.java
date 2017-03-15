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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;

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
						
						monitor.subTask("Retrieving the key vault...");
						dockerManager.refreshDockerVaults();
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}
						
						monitor.subTask("Retrieving the key vault details...");
						dockerManager.refreshDockerVaultDetails();
						if (monitor.isCanceled()) {
							CANCELED = true;
							monitor.done();
							return;
						}
						
						monitor.subTask("Retrieving the network details...");
						dockerManager.refreshDockerVnetDetails();
						if (monitor.isCanceled()) {
							monitor.done();
							return;
						}
						
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
}
