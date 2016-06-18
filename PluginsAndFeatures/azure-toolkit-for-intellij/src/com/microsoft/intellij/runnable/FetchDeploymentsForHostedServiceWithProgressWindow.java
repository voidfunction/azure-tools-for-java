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

package com.microsoft.intellij.runnable;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.windowsazure.management.compute.models.HostedServiceGetDetailedResponse;
import com.microsoftopentechnologies.azurecommons.deploy.tasks.AccountCachingExceptionEvent;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FetchDeploymentsForHostedServiceWithProgressWindow extends AccountActionRunnable implements Runnable {

    private CloudService hostedService;
    private HostedServiceGetDetailedResponse hostedServiceDetailed;
    private Project myProject;

    public FetchDeploymentsForHostedServiceWithProgressWindow(PublishData data, CloudService hostedService, Project project) {
        super(data);
        this.hostedService = hostedService;
        this.myProject = project;
    }

    void setIndicatorText() {
        progressIndicator.setText("Fetching Deployments For " + hostedService.getName());
    }

    @Override
    public void doTask() {
        try {
            this.hostedService = WizardCacheManager.getHostedServiceWithDeployments(hostedService, myProject);
        } catch (Exception e) {
            AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
            event.setException(e);
            event.setMessage(e.getMessage());
            onRestAPIError(event);
            log(message("fetchingDeploymentsTitle"), e);
        }
    }

    public CloudService getHostedServiceDetailed() {
        return hostedService;
    }
}
