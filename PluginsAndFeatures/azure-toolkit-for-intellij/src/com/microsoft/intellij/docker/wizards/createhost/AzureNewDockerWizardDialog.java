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

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class AzureNewDockerWizardDialog extends WizardDialog<AzureNewDockerWizardModel> {
  private AzureNewDockerWizardModel model;

  public AzureNewDockerWizardDialog(AzureNewDockerWizardModel model) {
    super(model.getProject(), true, model);
    this.model = model;
    model.setNewDockerWizardDialog(this);
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
//        validateInput();
    if (isOKActionEnabled() && performFinish()) {
      super.doOKAction();
    }
  }

  /**
   * This method gets called when wizard's finish button is clicked.
   *
   * @return True, if project gets created successfully; else false.
   */
  private boolean performFinish() {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        // Do the heavy workload in a background thread
        doFinish();
      }
    };

    ProgressManager progressManager = null;
    try {
      progressManager = ProgressManager.getInstance();
    } catch (Exception e) {
      progressManager = null;
    }

    if (progressManager != null) {
      progressManager.runProcessWithProgressSynchronously(runnable, "Deploying...", true, model.getProject());
    } else {
      doFinish();
    }

    return true;
  }

  private void doFinish() {
    try {
      // do something interesting here
    } catch (Exception e) {
      // TODO: These strings should be retrieved from AzureBundle
      PluginUtil.displayErrorDialogAndLog("Error", "Error deploying to Docker", e);
    }
  }
}
