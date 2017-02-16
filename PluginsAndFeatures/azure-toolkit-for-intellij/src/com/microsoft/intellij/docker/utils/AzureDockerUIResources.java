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
package com.microsoft.intellij.docker.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.awt.*;

public class AzureDockerUIResources {
  private static final Logger LOGGER = Logger.getInstance(AzureDockerUIResources.class);
  public static boolean CANCELED = false;


  public static void updateAzureResourcesWithProgressDialog(Project project) {
    ProgressManager.getInstance().run(new Task.Modal(project, "Loading Azure Resources...", true) {
      @Override
      public void run(ProgressIndicator progressIndicator) {
        try {
          progressIndicator.setFraction(.05);
          AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();

          if (progressIndicator.isCanceled()) {
            return;
          }

          // not signed in
          if (azureAuthManager == null) {
            return;
          }

          progressIndicator.setFraction(.1);
          AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.2);
          progressIndicator.setText2("Retrieving the subscription details...");
          dockerManager.refreshDockerSubscriptions();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.3);
          progressIndicator.setText2("Retrieving the key vault...");
          dockerManager.refreshDockerVaults();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.45);
          progressIndicator.setText2("Retrieving the key vault details...");
          dockerManager.refreshDockerVaultDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.7);
          progressIndicator.setText2("Retrieving the network details...");
          dockerManager.refreshDockerVnetDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setFraction(.8);
          progressIndicator.setText2("Retrieving the storage account details...");
          dockerManager.refreshDockerStorageAccountDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setIndeterminate(true);
          progressIndicator.setText2("Retrieving the Docker virtual machines details...");
          dockerManager.refreshDockerHostDetails();
          if (progressIndicator.isCanceled()) {
            return;
          }

          progressIndicator.setIndeterminate(true);

        } catch (Exception ex) {
          ex.printStackTrace();
          LOGGER.error("updateAzureResourcesWithProgressDialog", ex);
          CANCELED = true;
        }
      }

      @Override
      public void onCancel() {
        CANCELED = true;
        super.onCancel();
      }
    });
  }

  /*
   * Opens a confirmation dialog box for the user to chose the delete action
   * @return integer representing the user's choise
   *  -1 - action was canceled
   *   0 - action was canceled
   *   1 - delete VM only
   *   2 - delete VM and associated resources (vnet, publicIp, nic, nsg)
   */

  public static int deleteAzureDockerHostConfirmationDialog(Azure azureClient, DockerHost dockerHost) {
    String promptMessageDeleteAll = String.format("This operation will delete virtual machine %s and its resources:\n" +
            "\t - network interface: %s\n" +
            "\t - public IP: %s\n" +
            "\t - virtual network: %s\n" +
            "The associated disks and storage account will not be deleted\n",
        dockerHost.hostVM.name,
        dockerHost.hostVM.nicName,
        dockerHost.hostVM.publicIpName,
        dockerHost.hostVM.vnetName);

    String promptMessageDelete = String.format("This operation will delete virtual machine %s.\n" +
            "The associated disks and storage account will not be deleted\n\n" +
            "Are you sure you want to continue?\n",
        dockerHost.hostVM.name);

    String[] options;
    String promptMessage;

    if (AzureDockerVMOps.isDeletingDockerHostAllSafe(
        azureClient,
        dockerHost.hostVM.resourceGroupName,
        dockerHost.hostVM.name)) {
      promptMessage = promptMessageDeleteAll;
      options = new String[]{"Cancel", "Delete VM Only", "Delete All"};
    } else {
      promptMessage = promptMessageDelete;
      options = new String[]{"Cancel", "Delete"};
    }

    int dialogReturn = JOptionPane.showOptionDialog(null,
        promptMessage,
        "Delete Docker Host",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        PluginUtil.getIcon("/icons/logwarn.png"),
        options,
        null);

    switch (dialogReturn) {
      case 0:
        if (AzureDockerUtils.DEBUG) System.out.format("Delete Docker Host op was canceled %s\n", dialogReturn);
        break;
      case 1:
        if (AzureDockerUtils.DEBUG) System.out.println("Delete VM only: " + dockerHost.name);
        break;
      case 2:
        if (AzureDockerUtils.DEBUG) System.out.println("Delete VM and resources: " + dockerHost.name);
        break;
      default:
        if (AzureDockerUtils.DEBUG) System.out.format("Delete Docker Host op was canceled %s\n", dialogReturn);
    }

    return dialogReturn;
  }

  public static void deleteDockerHost(Project project, Azure azureClient, DockerHost dockerHost, int option, Runnable runnable) {
    String progressMsg = (option == 2) ? String.format("Deleting Virtual Machine %s and Its Resources...", dockerHost.name) :
        String.format("Deleting Docker Host %s...", dockerHost.name);

    DefaultLoader.getIdeHelper().runInBackground(project, "Deleting Docker Host", false, true, progressMsg, new Runnable() {
      @Override
      public void run() {
        try {
          if (option == 2) {
            AzureDockerVMOps.deleteDockerHostAll(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
          } else {
            AzureDockerVMOps.deleteDockerHost(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
          }
          // TODO: remove this
          Thread.sleep(10000);
        } catch (Exception e) {
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              if (AzureDockerUtils.DEBUG) e.printStackTrace();
              LOGGER.error("onRemoveDockerHostAction", e);
              PluginUtil.displayErrorDialog("Delete Docker Host Error", String.format("Unexpected error detected while deleting Docker host %s:\n\n%s", dockerHost.name, e.getMessage()));
            }
          });
        }

        if (runnable != null) {
          runnable.run();
        }
      }
    });
  }
}
