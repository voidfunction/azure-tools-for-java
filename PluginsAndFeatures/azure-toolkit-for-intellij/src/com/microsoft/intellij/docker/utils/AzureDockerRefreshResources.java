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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;

public class AzureDockerRefreshResources {
  private static final Logger LOGGER = Logger.getInstance(AzureDockerRefreshResources.class);
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

          if (!dockerManager.isInitialized()) {

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

}
