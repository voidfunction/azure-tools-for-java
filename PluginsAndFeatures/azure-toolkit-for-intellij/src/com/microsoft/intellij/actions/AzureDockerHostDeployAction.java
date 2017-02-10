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
package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.docker.utils.AzureDockerRefreshResources;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tasks.DockerContainerDeployTask;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.util.Arrays;
import java.util.List;

import static com.intellij.projectImport.ProjectImportBuilder.getCurrentProject;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzureDockerHostDeployAction extends AnAction {
  private static final Logger LOGGER = Logger.getInstance(AzureDockerHostDeployAction.class);

  public void actionPerformed(AnActionEvent actionEvent) {
    try {
      AzureDockerRefreshResources.CANCELED = false;

      Project project = getCurrentProject();
      Module module = PluginUtil.getSelectedModule();
      List<Module> modules = Arrays.asList(ModuleManager.getInstance(project).getModules());

      if (module == null && modules.isEmpty()) {
        Messages.showErrorDialog(message("noModule"), message("error"));
      } else if (module == null) {
        module = modules.iterator().next();
      }

      AzureDockerRefreshResources.updateAzureResourcesWithProgressDialog(project);
      if (AzureDockerRefreshResources.CANCELED) {
        return;
      }

      AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
      // not signed in
      if (azureAuthManager == null) {
        System.out.println("ERROR! Not signed in!");
        return;
      }


      AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);

      AzureDockerImageInstance dockerImageDescription = new AzureDockerImageInstance();
      dockerImageDescription.dockerImageName = AzureDockerUtils.getDefaultDockerImageName(project.getName()).toLowerCase();
      dockerImageDescription.dockerContainerName = AzureDockerUtils.getDefaultDockerContainerName(dockerImageDescription.dockerImageName);
      dockerImageDescription.artifactName = AzureDockerUtils.getDefaultArtifactName(project.getName());
      dockerImageDescription.host = dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName())));
      dockerImageDescription.hasNewDockerHost = false;
      AzureDockerCertVault certVault = AzureDockerCertVaultOps.getTLSCertsFromLocalFile("/Users/milis/temp/keys/tls/using_openssl");

      dockerImageDescription.host.certVault.tlsCACert = certVault.tlsCACert;
      dockerImageDescription.host.certVault.tlsCAKey = certVault.tlsCAKey;
      dockerImageDescription.host.certVault.tlsClientCert = certVault.tlsClientCert;
      dockerImageDescription.host.certVault.tlsClientKey = certVault.tlsClientKey;
      dockerImageDescription.host.certVault.tlsServerCert = certVault.tlsServerCert;
      dockerImageDescription.host.certVault.tlsServerKey = certVault.tlsServerKey;

      AzureSelectDockerWizardModel model = new AzureSelectDockerWizardModel(project, dockerManager, dockerImageDescription);
      AzureSelectDockerWizardDialog wizard = new AzureSelectDockerWizardDialog(model);
      wizard.setTitle("New Deployment of a Docker Container on Azure");
      wizard.show();

      if (wizard.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        try {
          String url = wizard.deploy();
          System.out.println("Web app published at: " + url);
        } catch (Exception ex) {
          PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void update(AnActionEvent actionEvent) {
    final Module module = actionEvent.getData(LangDataKeys.MODULE);
    actionEvent.getPresentation().setVisible(PlatformUtils.isIdeaUltimate());
    if (!PlatformUtils.isIdeaUltimate()) {
      return;
    }

    try {
      boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
      boolean isEnabled = isSignIn & module != null && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE));
      actionEvent.getPresentation().setEnabled(isEnabled);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}