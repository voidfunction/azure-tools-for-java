package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator;
import com.microsoft.azuretools.utils.*;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.net.URI;
import java.util.*;

/**
 * Created by vlashch on 1/30/17.
 */
public class AppServiceChangeSettingsDialog extends AppServiceCreateDialog {
    private static final Logger LOGGER = Logger.getInstance(AppServiceChangeSettingsDialog.class);

    public static AppServiceChangeSettingsDialog go(WebAppDeployDialog.WebAppDetails wad, Module module){
        AppServiceChangeSettingsDialog d = new AppServiceChangeSettingsDialog(module, wad);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }
        return null;
    }

    protected AppServiceChangeSettingsDialog(Module module, WebAppDeployDialog.WebAppDetails wad) {
        super(module);

        setTitle("Change App Service Settings");

        setOKButtonText("Change");

        webApp = wad.webApp;
        textFieldWebappName.setText(wad.webApp.name());
        textFieldWebappName.setEnabled(false);

        DefaultComboBoxModel<WebContainer> wcModel = (DefaultComboBoxModel<WebContainer>)comboBoxWebContainer.getModel();
        if (wad.webApp.javaVersion() != JavaVersion.OFF) {
            wcModel.setSelectedItem(new WebContainer(wad.webApp.javaContainer() + " " + wad.webApp.javaContainerVersion()));
        }

        comboBoxSubscription.setEnabled(false);

        DefaultComboBoxModel<ResourceGroupAdapter> rgModel = (DefaultComboBoxModel<ResourceGroupAdapter>)comboBoxResourceGroup.getModel();
        rgModel.setSelectedItem(new ResourceGroupAdapter(wad.resourceGroup));
        for (Component c : panelResourceGroupUseExisting.getComponents()) {
            c.setEnabled(false);
        }

        for (Component c : panelResourceGroup.getComponents()) {
            c.setEnabled(false);
        }

        DefaultComboBoxModel<AppServicePlanAdapter> aspModel = (DefaultComboBoxModel<AppServicePlanAdapter> )comboBoxAppServicePlan.getModel();
        aspModel.setSelectedItem(new AppServicePlanAdapter(wad.appServicePlan));
        //comboBoxAppServicePlan.setEnabled(false);
        for (Component c : panelAppServiceUseExisting.getComponents()) {
            c.setEnabled(false);
        }

        for (Component c : panelAppServicePlan.getComponents()) {
            c.setEnabled(false);
        }
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {

        model.collectData();

        ValidationInfo res = volidateJdkTab();
        if (res != null) return res;

        return super.superDoValidate();
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new Task.Modal(null,"Changing App Service Settings...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    webApp = editAppService(webApp, new UpdateProgressIndicator(progressIndicator));
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            superDoOKAction();
                        }
                    }, ModalityState.any());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // TODO: show error message
                    LOGGER.error("doOKAction :: Task.Modal", ex);
                    ErrorWindow.show(ex.getMessage(), "Create App Service Error", AppServiceChangeSettingsDialog.this.contentPane);
                }
            }
        });
    }

    protected WebApp editAppService(WebApp webApp, IProgressIndicator progressIndicator) throws Exception {
        if (model.jdkDownloadUrl != null ) {
            progressIndicator.setText("Turning App Service into .Net based...");

            //webApp.update().withNetFrameworkVersion(NetFrameworkVersion.V4_6).apply();
            //progressIndicator.setText("Deploying custom jdk...");
            //WebAppUtils.deployCustomJdk(webApp, model.jdkDownloadUrl, model.webContainer, progressIndicator);
        } else {
            FTPClient ftpClient = WebAppUtils.getFtpConnection(webApp.getPublishingProfile());
            progressIndicator.setText("Deleting custom jdk artifacts, if any (takes a while)...");
            WebAppUtils.removeCustomJdkArtifacts(ftpClient, progressIndicator);
            // TODO: make cancelable
            WebAppUtils.removeCustomJdkArtifacts(WebAppUtils.getFtpConnection(webApp.getPublishingProfile()), progressIndicator);
            progressIndicator.setText("Applying changes...");
            webApp.update().withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(model.webContainer).apply();
        }

        return webApp;
    }
}