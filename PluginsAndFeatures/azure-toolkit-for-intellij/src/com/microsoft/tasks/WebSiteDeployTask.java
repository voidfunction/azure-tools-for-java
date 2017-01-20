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
package com.microsoft.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.activitylog.ActivityLogToolWindowFactory;
import com.microsoft.intellij.components.ServerExplorerToolWindowFactory;
import com.microsoft.intellij.deploy.DeploymentManager;
import com.microsoft.intellij.util.WAHelper;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventListener;
import org.jetbrains.annotations.NotNull;


import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebSiteDeployTask extends Task.Backgroundable {
    WebApp webApp;
    String url;
    Project project;
    static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");

    public WebSiteDeployTask(Project project, WebApp webApp, String url) {
        super(project, message("deployingToAzure"), true, Backgroundable.DEAF);
        this.webApp = webApp;
        this.url = url;
        this.project = project;
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        openViews(project);
        AzurePlugin.removeUnNecessaryListener();
        DeploymentEventListener deployListnr = new DeploymentEventListener() {
            @Override
            public void onDeploymentStep(DeploymentEventArgs args) {
                indicator.setFraction(indicator.getFraction() + args.getDeployCompleteness() / 100.0);
                indicator.setText(message("deployingToAzure"));
                indicator.setText2(args.toString());
            }
        };
        AzurePlugin.addDeploymentEventListener(deployListnr);
        AzurePlugin.depEveList.add(deployListnr);
        new DeploymentManager(project).deployToWebApps(webApp, url);

        new Thread("Warm up the target site") {
            public void run() {
                try {

                    LOG.info("To warm the site up - implicitly trying to connect it");
                    WAHelper.sendGet(url);
                }
                catch (Exception ex) {
                    LOG.info(ex.getMessage(), ex);
                }
            }
        }.start();
    }

    private void openViews(final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindowManager.getInstance(project).getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW).activate(null);
                ToolWindowManager.getInstance(project).getToolWindow(ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW).activate(null);

            }
        });
    }
}
