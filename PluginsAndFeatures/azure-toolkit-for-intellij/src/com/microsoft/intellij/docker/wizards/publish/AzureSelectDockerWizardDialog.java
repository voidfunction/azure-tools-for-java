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
package com.microsoft.intellij.docker.wizards.publish;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.management.Azure;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tasks.DockerContainerDeployTask;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class AzureSelectDockerWizardDialog extends WizardDialog<AzureSelectDockerWizardModel> {
  private AzureSelectDockerWizardModel model;
  private Runnable onCreate;

  public AzureSelectDockerWizardDialog(AzureSelectDockerWizardModel model) {
    super(model.getProject(), true, model);
    this.model = model;
    model.setSelectDockerWizardDialog(this);
    onCreate = null;
  }

  @Override
  public void onWizardGoalAchieved() {
    if (model.finishedOK) {
      super.onWizardGoalAchieved();
    }
  }

  @Override
  public void onWizardGoalDropped() {
    if (model.finishedOK) {
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
//        validateInput();
    if (isOKActionEnabled()) {
      super.doOKAction();
    }
  }

//  @Override
//  public void doCancelAction() {
//    model.finishedOK = true;
//    super.doCancelAction();
//  }

  public void DialogShaker(ValidationInfo info) {
    PluginUtil.dialogShaker(info, this);
  }

  public String deploy() {
    AzureDockerImageInstance dockerImageInstance = model.getDockerImageDescription();

    performFinish();

    return String.format("%s://%s:%s/%s",
        (dockerImageInstance.isHttpsWebApp ? "https" : "http"),
        dockerImageInstance.host.hostVM.dnsName,
        dockerImageInstance.dockerPortSettings.split(":")[0],  // "12345:80/tcp" -> "12345"
        dockerImageInstance.artifactName);
  }

  public AzureDockerImageInstance getDockerImageInstance() {
    return model.getDockerImageDescription();
  }

  /**
   * This method gets called when wizard's finish button is clicked.
   *
   * @return True, if project gets created successfully; else false.
   */
  private void performFinish() {

    DefaultLoader.getIdeHelper().runInBackground(model.getProject(), "Deploying Web App to a Docker Host on Azure", false, true, "Deploying Web app to a Docker host on Azure...", new Runnable() {
      @Override
      public void run() {
        try {
          DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
              doFinish();

              if (onCreate != null) {
                onCreate.run();
              }
            }
          });
        } catch (Exception e) {
          String msg = "An error occurred while attempting to deploy to the selected Docker host." + "\n" + e.getMessage();
          PluginUtil.displayErrorDialogInAWTAndLog("Failed to deploy Web app to the selected Docker host", msg, e);
        }
      }
    });
  }

  private void doFinish() {
    try {
      Azure azureClient = model.getDockerHostsManager().getSubscriptionsMap().get(model.getDockerImageDescription().sid).azureClient;

      DockerContainerDeployTask task = new DockerContainerDeployTask(model.getProject(), azureClient, model.getDockerImageDescription());
      task.queue();

    } catch (Exception e) {
      // TODO: These strings should be retrieved from AzureBundle
      PluginUtil.displayErrorDialogAndLog("Error", "Error deploying to Docker", e);
    }
  }
}