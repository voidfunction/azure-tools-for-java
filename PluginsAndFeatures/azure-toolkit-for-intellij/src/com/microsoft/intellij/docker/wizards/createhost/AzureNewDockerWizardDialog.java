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
package com.microsoft.intellij.docker.wizards.createhost;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class AzureNewDockerWizardDialog extends WizardDialog<AzureNewDockerWizardModel> {
  private static final Logger LOGGER = Logger.getInstance(AzureNewDockerWizardDialog.class);
  private AzureNewDockerWizardModel model;
  private Runnable onCreate;

  public AzureNewDockerWizardDialog(AzureNewDockerWizardModel model) {
    super(model.getProject(), true, model);
    this.model = model;
    model.setNewDockerWizardDialog(this);
    this.onCreate = null;
  }

  public void DialogShaker(ValidationInfo info) {
    PluginUtil.dialogShaker(info, this);
  }

  @Override
  public void onWizardGoalAchieved() {
    if (model.canClose()) {
      super.onWizardGoalAchieved();
    }
  }

  @Override
  public void onWizardGoalDropped() {
    if (model.canClose()) {
      super.onWizardGoalDropped();
    }
  }

  @Override
  protected Dimension getWindowPreferredSize() {
    return new Dimension(600, 400);
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    return model.doValidate();
  }

  @Override
  protected void doOKAction() {
    if (isOKActionEnabled()) {
      super.doOKAction();
    }
  }

  public void create() {
    DockerHost dockerHost = model.getDockerHost();


    ProgressManager.getInstance().run(new Task.Backgroundable(model.getProject(), "Creating Docker Host on Azure...", true) {
      @Override
      public void run(ProgressIndicator progressIndicator) {
        try {
          progressIndicator.setFraction(.05);
          progressIndicator.setText2(String.format("Create Docker Host %s ...", dockerHost.apiUrl));
          AzureDockerHostsManager dockerManager = model.getDockerManager();
          Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
          KeyVaultClient keyVaultClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).keyVaultClient;
          if (progressIndicator.isCanceled()) {
            if (displayWarningOnCreateCancelAction() == 1) {
              return;
            }
          }

          progressIndicator.setFraction(.25);
          progressIndicator.setText2(String.format("Creating new virtual machine %s ...", dockerHost.name));
          System.out.println("Creating new virtual machine: " + new Date().toString());
          AzureDockerVMOps.createDockerHostVM(azureClient, dockerHost);
          System.out.println("Done creating new virtual machine: " + new Date().toString());
          if (progressIndicator.isCanceled()) {
            if (displayWarningOnCreateCancelAction() == 1) {
              return;
            }
          }

          progressIndicator.setFraction(.55);
          progressIndicator.setText2(String.format("Waiting for virtual machine %s to be up...", dockerHost.name));
          System.out.println("Waiting for virtual machine to be up: " + new Date().toString());
          AzureDockerVMOps.waitForVirtualMachineStartup(azureClient, dockerHost);
          System.out.println("Done Waiting for virtual machine to be up: " + new Date().toString());
          if (progressIndicator.isCanceled()) {
            if (displayWarningOnCreateCancelAction() == 1) {
              return;
            }
          }

          progressIndicator.setFraction(.65);
          progressIndicator.setText2(String.format("Configuring Docker service for %s ...", dockerHost.apiUrl));
          System.out.println("Configuring Docker host: " + new Date().toString());
          AzureDockerVMOps.installDocker(dockerHost);
          System.out.println("Done configuring Docker host: " + new Date().toString());
          System.out.println("Finished setting up Docker host");
          if (progressIndicator.isCanceled()) {
            if (displayWarningOnCreateCancelAction() == 1) {
              return;
            }
          }

          if (dockerHost.certVault.hostName != null) {
            progressIndicator.setFraction(.75);
            progressIndicator.setText2(String.format("Creating new key vault %s ...", dockerHost.certVault.name));
            System.out.println("Creating new Docker key vault: " + new Date().toString());
            AzureDockerCertVaultOps.createOrUpdateVault(azureClient, dockerHost.certVault, keyVaultClient);
            System.out.println("Done creating new key vault: " + new Date().toString());
            if (progressIndicator.isCanceled()) {
              if (displayWarningOnCreateCancelAction() == 1) {
                return;
              }
            }

            progressIndicator.setFraction(.90);
            progressIndicator.setText2("Updating key vaults ...");
            System.out.println("Refreshing key vaults: " + new Date().toString());
            dockerManager.refreshDockerVaults();
            dockerManager.refreshDockerVaultDetails();
            System.out.println("Done refreshing key vaults: " + new Date().toString());
            if (progressIndicator.isCanceled()) {
              if (displayWarningOnCreateCancelAction() == 1) {
                return;
              }
            }
          }

          progressIndicator.setIndeterminate(true);
          progressIndicator.setText2("Refreshing the Docker virtual machines details...");
          System.out.println("Refreshing Docker hosts details: " + new Date().toString());
          dockerManager.refreshDockerHostDetails();
          DockerHost updatedHost = dockerManager.getDockerHostForURL(dockerHost.apiUrl);
          if (updatedHost.certVault == null) {
            updatedHost.certVault = dockerHost.certVault;
            updatedHost.hasPwdLogIn = dockerHost.hasPwdLogIn;
            updatedHost.hasSSHLogIn = dockerHost.hasSSHLogIn;
            updatedHost.isTLSSecured = dockerHost.isTLSSecured;
          }
          System.out.println("Done refreshing Docker hosts details: " + new Date().toString());
          if (progressIndicator.isCanceled()) {
            if (displayWarningOnCreateCancelAction() == 1) {
              return;
            }
          }
          progressIndicator.setIndeterminate(true);

        } catch (Exception e) {
          String msg = "An error occurred while attempting to create Docker host." + "\n" + e.getMessage();
          PluginUtil.displayErrorDialogInAWTAndLog("Failed to Create Docker Host", msg, e);
        }
      }
    });

    //    performFinish(newHost);
    DefaultLoader.getIdeHelper().runInBackground(model.getProject(), "Creating Docker Host on Azure", true, true, "Deploying Web app to a Docker host on Azure...", new Runnable() {
      @Override
      public void run() {
        try {
          DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {

              // Update caches here
              if (onCreate != null) {
                onCreate.run();
              }
            }
          });
        } catch (Exception e) {
          String msg = "An error occurred while attempting to deploy to the selected Docker host." + "\n" + e.getMessage();
          PluginUtil.displayErrorDialogInAWTAndLog("Failed to Deploy Web App as Docker Container", msg, e);
        }
      }
    });
  }

  private int displayWarningOnCreateCancelAction(){
    return JOptionPane.showOptionDialog(null,
        "This action can result the Docker host virtual machine in an partial setup state and can not be later use for container deployment!\n\n Are you sure you want this?",
        "Stop Create Docker Host",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        PluginUtil.getIcon("/icons/logwarn.png"),
        new String[]{"Cancel", "OK"},
        null);
  }

}
