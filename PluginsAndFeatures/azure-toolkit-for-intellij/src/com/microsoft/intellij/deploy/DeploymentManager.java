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
package com.microsoft.intellij.deploy;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.interopbridges.tools.windowsazure.*;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.activitylog.ActivityLogToolWindowFactory;
import com.microsoft.intellij.util.AppInsightsCustomEvent;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.wacommon.utils.WACommonException;
import com.microsoft.windowsazure.core.OperationStatus;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.compute.models.DeploymentSlot;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentEventArgs;
import com.microsoftopentechnologies.azurecommons.deploy.DeploymentManagerUtilMethods;
import com.microsoftopentechnologies.azurecommons.deploy.model.CertificateUpload;
import com.microsoftopentechnologies.azurecommons.deploy.model.DeployDescriptor;
import com.microsoftopentechnologies.azurecommons.exception.DeploymentException;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azuremanagementutil.model.InstanceStatus;
import com.microsoftopentechnologies.azuremanagementutil.model.Notifier;
import com.microsoftopentechnologies.azuremanagementutil.rest.WindowsAzureRestUtils;
import com.microsoftopentechnologies.azuremanagementutil.rest.WindowsAzureStorageServices;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public final class DeploymentManager {

    private final HashMap<String, DeployDescriptor> deployments = new HashMap<String, DeployDescriptor>();
    private Project myProject;

    public DeploymentManager(Project project) {
        this.myProject = project;
    }

    public void addDeployment(String name, DeployDescriptor deployment) {
        deployments.put(name, deployment);
    }

    public void removeDeployment(String name) {
        deployments.remove(name);
    }

    public HashMap<String, DeployDescriptor> getDeployments() {
        return deployments;
    }

    public void deploy(Module selectedModule) throws InterruptedException, DeploymentException {

        DeployDescriptor deploymentDesc = WizardCacheManager.collectConfiguration();
        deploymentDesc.setProject(selectedModule.getProject());
        String deployState = deploymentDesc.getDeployState();
        Date startDate = new Date();
        try {

            int conditionalProgress = 20;

            CloudService hostedService = deploymentDesc.getHostedService();
            addDeployment(hostedService.getName(), deploymentDesc);

            com.microsoft.tooling.msservices.model.storage.StorageAccount storageAccount = deploymentDesc.getStorageAccount();


//            WindowsAzureServiceManagement service = WizardCacheManager.createServiceManagementHelper();

            openWindowsAzureActivityLogView(deploymentDesc, selectedModule.getProject());

            if (deploymentDesc.getDeployMode() == WindowsAzurePackageType.LOCAL) {
                deployToLocalEmulator(selectedModule);
                notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 100, OperationStatus.Succeeded, message("deplCompleted"));
                return;
            }
            // Publish start event
            AppInsightsCustomEvent.create(message("startEvent"), "");

            // need to improve this check (maybe hostedSerivce.isExisting())?
            if (hostedService.getUri() == null || hostedService.getUri().toString().isEmpty()) { // the hosted service was not yet created.
                notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 5, OperationStatus.InProgress, String.format("%s - %s", message("createHostedService"), hostedService.getName()));
                createHostedService(hostedService.getName(), hostedService.getName(),
                        hostedService.getLocation(), hostedService.getDescription());
                conditionalProgress -= 5;
            }

            // same goes here
            if (storageAccount.getManagementUri() == null || storageAccount.getManagementUri().isEmpty()) { // the storage account was not yet created
                notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 10, OperationStatus.InProgress,
                        String.format("%s - %s", message("createStorageAccount"), storageAccount.getName()));
                createStorageAccount(storageAccount.getName(), storageAccount.getName(),
                        storageAccount.getLocation(), storageAccount.getDescription());
                conditionalProgress -= 10;
            }

            checkContainerExistance();

            // upload certificates
            if (deploymentDesc.getCertList() != null) {
                List<CertificateUpload> certList = deploymentDesc.getCertList().getList();
                if (certList != null && certList.size() > 0) {
                    for (int i = 0; i < certList.size(); i++) {
                        CertificateUpload cert = certList.get(i);
                        DeploymentManagerUtilMethods.uploadCertificateIfNeededGeneric(WizardCacheManager.getCurrentPublishData().getCurrentSubscription().getId(),
                                deploymentDesc, cert.getPfxPath(), cert.getPfxPwd());
                        notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 0, OperationStatus.InProgress,
                                String.format("%s%s", message("deplUploadCert"), cert.getName()));
                    }
                }
            }

            if (deploymentDesc.getRemoteDesktopDescriptor().isEnabled()) {

                notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, conditionalProgress, OperationStatus.InProgress, message("deplConfigRdp"));

                DeploymentManagerUtilMethods.configureRemoteDesktop(deploymentDesc, WizardCacheManager.getCurrentDeployConfigFile(),
                        String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, PluginUtil.PLUGIN_ID));
            } else {
                notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, conditionalProgress, OperationStatus.InProgress, message("deplConfigRdp"));
            }

            Notifier notifier = new NotifierImp();

            String targetCspckgName = createCspckTargetName(deploymentDesc);

            notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 20, OperationStatus.InProgress, message("uploadingServicePackage"));

            DeploymentManagerUtilMethods.uploadPackageService(
                    WizardCacheManager.createStorageServiceHelper(),
                    deploymentDesc.getCspkgFile(),
                    targetCspckgName,
                    message("eclipseDeployContainer").toLowerCase(),
                    deploymentDesc, notifier);

            notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 20, OperationStatus.InProgress, message("creatingDeployment"));

            String storageAccountURL = deploymentDesc.getStorageAccount().getBlobsUri();

            String cspkgUrl = String.format("%s%s/%s", storageAccountURL,
                    message("eclipseDeployContainer").toLowerCase(), targetCspckgName);
            /*
             * To make deployment name unique attach time stamp
			 * to the deployment name.
			 */
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String deploymentName = String.format("%s%s%s",
                    hostedService.getName(),
                    deployState,
                    dateFormat.format(new Date()));
            OperationStatusResponse operationStatusResponse = DeploymentManagerUtilMethods.createDeployment(deploymentDesc, cspkgUrl, deploymentName);
            OperationStatus status = AzureManagerImpl.getManager(myProject).waitForStatus(deploymentDesc.getSubscriptionId(), operationStatusResponse).getStatus();

            DeploymentManagerUtilMethods.deletePackage(WizardCacheManager.createStorageServiceHelper(),
                    message("eclipseDeployContainer").toLowerCase(),
                    targetCspckgName, notifier);
            notifyProgress(deploymentDesc.getDeploymentId(), startDate,
                    null, 0, OperationStatus.InProgress, message("deletePackage"));

            notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 20, OperationStatus.InProgress, message("waitingForDeployment"));

            DeploymentGetResponse deployment = waitForDeployment(
                    deploymentDesc.getSubscriptionId(),
                    hostedService.getName(),
                    deployState);

            boolean displayHttpsLink = deploymentDesc.getDisplayHttpsLink();
            WindowsAzureProjectManager waProjManager = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(selectedModule)));

            String serverAppName = null;
            for (WindowsAzureRole role : waProjManager.getRoles()) {
                if (role.getJDKSourcePath() != null && role.getServerCloudName() != null) {
                    List<WindowsAzureRoleComponent> serverAppComponents = role.getServerApplications();
                    // Get first server app component
                    if (serverAppComponents != null && serverAppComponents.size() > 0) {
                        String deployName = serverAppComponents.get(0).getDeployName();
                        serverAppName = deployName.substring(0, deployName.lastIndexOf("."));
                        break;
                    }
                }
            }
            String deploymentURL = displayHttpsLink ? deployment.getUri().toString().replaceAll("http://", "https://") : deployment.getUri().toString();
            if (serverAppName != null) {
                if (!deploymentURL.endsWith("/")) {
                    deploymentURL += "/";
                }
                deploymentURL += serverAppName + "/";
            }
            notifyProgress(deploymentDesc.getDeploymentId(), startDate, deploymentURL, 20, status, deployment.getStatus().toString());
            // publish success event
            AppInsightsCustomEvent.create(message("successEvent"), "");
            // RDP prompt will come only on windows
            if (deploymentDesc.isStartRdpOnDeploy() && AzurePlugin.IS_WINDOWS) {
                String pluginFolder = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, PluginUtil.PLUGIN_ID);
                WindowsAzureRestUtils.getInstance().launchRDP(deployment, deploymentDesc.getRemoteDesktopDescriptor().getUserName(), pluginFolder);
            }
        } catch (Throwable t) {
            if (t instanceof ProcessCanceledException) {
                PluginUtil.displayWarningDialogInAWT(message("interrupt"), message("deploymentInterrupted"));
            } else {
                // Publish failure event
                AppInsightsCustomEvent.create(message("failureEvent"), "");
                String msg = (t.getMessage() != null ? t.getMessage() : "");
                if (!msg.startsWith(OperationStatus.Failed.toString())) {
                    msg = OperationStatus.Failed.toString() + " : " + msg;
                }
                notifyProgress(deploymentDesc.getDeploymentId(), startDate, null, 100,
                        OperationStatus.Failed, msg,
                        deploymentDesc.getDeploymentId(),
                        deployState);
                if (t instanceof DeploymentException) {
                    throw (DeploymentException) t;
                }
                throw new DeploymentException(msg, t);
            }
        }
    }

    private void createStorageAccount(final String storageServiceName, final String label, final String location, final String description)
            throws Exception {
        com.microsoft.tooling.msservices.model.storage.StorageAccount storageService =
                WizardCacheManager.createStorageAccount(storageServiceName, label, location, description);
        /*
		 * Add newly created storage account
		 * in centralized storage account registry.
		 */
        StorageAccount storageAccount = new StorageAccount(storageService.getName(),
                storageService.getPrimaryKey(),
                storageService.getBlobsUri());
        StorageAccountRegistry.addAccount(storageAccount);
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).saveStorage();
    }

    private void createHostedService(final String hostedServiceName, final String label, final String location, final String description)
            throws Exception {
//        HostedServiceCreateParameters createHostedService = new HostedServiceCreateParameters();
//        createHostedService.setServiceName(hostedServiceName);
//        createHostedService.setLabel(label);
//        createHostedService.setLocation(location);
//        createHostedService.setDescription(description);

        WizardCacheManager.createHostedService(hostedServiceName, label, location, description);
    }

    private void checkContainerExistance() throws Exception {
        WindowsAzureStorageServices storageServices = WizardCacheManager.createStorageServiceHelper();
        storageServices.createContainer(message("eclipseDeployContainer").toLowerCase());
    }

    private DeploymentGetResponse waitForDeployment(String subscriptionId, String serviceName, String deployState)
            throws Exception {
        DeploymentGetResponse deployment = null;
        String status = null;
        DeploymentSlot deploymentSlot;
        if (DeploymentSlot.Staging.toString().equalsIgnoreCase(deployState)) {
            deploymentSlot = DeploymentSlot.Staging;
        } else if (DeploymentSlot.Production.toString().equalsIgnoreCase(deployState)) {
            deploymentSlot = DeploymentSlot.Production;
        } else {
            throw new Exception("Invalid deployment slot name");
        }
        do {
            Thread.sleep(5000);
            deployment = AzureManagerImpl.getManager(myProject).getDeploymentBySlot(subscriptionId, serviceName, deploymentSlot);

            for (RoleInstance instance : deployment.getRoleInstances()) {
                status = instance.getInstanceStatus();
                if (InstanceStatus.ReadyRole.getInstanceStatus().equals(status)
                        || InstanceStatus.CyclingRole.getInstanceStatus().equals(status)
                        || InstanceStatus.FailedStartingVM.getInstanceStatus().equals(status)
                        || InstanceStatus.UnresponsiveRole.getInstanceStatus().equals(status)) {
                    break;
                }
            }
        } while (status != null && !(InstanceStatus.ReadyRole.getInstanceStatus().equals(status)
                || InstanceStatus.CyclingRole.getInstanceStatus().equals(status)
                || InstanceStatus.FailedStartingVM.getInstanceStatus().equals(status)
                || InstanceStatus.UnresponsiveRole.getInstanceStatus().equals(status)));

        if (!InstanceStatus.ReadyRole.getInstanceStatus().equals(status)) {
            throw new DeploymentException(status);
        }
        // check deployment status. And let Transitioning phase to finish
        DeploymentStatus deploymentStatus = null;
        do {
            Thread.sleep(10000);
            deployment = AzureManagerImpl.getManager(myProject).getDeploymentBySlot(subscriptionId, serviceName, deploymentSlot);
            deploymentStatus = deployment.getStatus();
        } while (deploymentStatus != null
                && (deploymentStatus.equals(DeploymentStatus.RunningTransitioning)
                || deploymentStatus.equals(DeploymentStatus.SuspendedTransitioning)));
        return deployment;
    }

//    private OperationStatus waitForStatus(Configuration configuration, WindowsAzureServiceManagement service, String requestId)
//            throws Exception {
//        OperationStatusResponse op;
//        OperationStatus status = null;
//        do {
//            op = service.getOperationStatus(configuration, requestId);
//            status = op.getStatus();
//
//            log(message("deplId") + op.getId());
//            log(message("deplStatus") + op.getStatus());
//            log(message("deplHttpStatus") + op.getHttpStatusCode());
//            if (op.getError() != null) {
//                log(message("deplErrorMessage") + op.getError().getMessage());
//                throw new RestAPIException(op.getError().getMessage());
//            }
//
//            Thread.sleep(5000);
//
//        } while (status == OperationStatus.InProgress);
//
//        return status;
//    }

    private String createCspckTargetName(DeployDescriptor deploymentDesc) {
        String cspkgName = String.format(message("cspkgName"), deploymentDesc.getHostedService().getName(), deploymentDesc.getDeployState());
        return cspkgName;
    }

    private void deployToLocalEmulator(Module selectedModule) throws DeploymentException {
        WindowsAzureProjectManager waProjManager;
        try {
            waProjManager = WindowsAzureProjectManager.load(new File(PluginUtil.getModulePath(selectedModule)));
            waProjManager.deployToEmulator();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            throw new DeploymentException(e);
        }
    }

    /**
     * Unlike Eclipse plugin, here startDate is deployment start time, not the event timestamp
     */
    public void notifyProgress(String deploymentId, Date startDate,
                               String deploymentURL,
                               int progress,
                               OperationStatus inprogress, String message, Object... args) {

        DeploymentEventArgs arg = new DeploymentEventArgs(this);
        arg.setId(deploymentId);
        arg.setDeploymentURL(deploymentURL);
        arg.setDeployMessage(String.format(message, args));
        arg.setDeployCompleteness(progress);
        arg.setStartTime(startDate);
        arg.setStatus(inprogress);
        AzurePlugin.fireDeploymentEvent(arg);
    }

    private void openWindowsAzureActivityLogView(final DeployDescriptor descriptor, final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindowManager.getInstance(project).getToolWindow(ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW).activate(null);
            }
        });
    }

    public void undeploy(final String serviceName, final String deplymentName, final String deploymentState)
            throws WACommonException, RestAPIException, InterruptedException {
        String subscriptionId = WizardCacheManager.getCurrentPublishData().getCurrentSubscription().getId();
        int[] progressArr = new int[]{50, 50};
        unPublish(subscriptionId, serviceName, deplymentName, progressArr);
    }

    /**
     * Unpublish deployment without notifying user.
     * @param subscriptionId
     * @param serviceName
     * @param deplymentName
     * @param progressArr
     */
    public void unPublish(
            String subscriptionId,
            String serviceName,
            String deplymentName,
            int[] progressArr) {
        int retryCount = 0;
        boolean successfull = false;
        Date startDate = new Date();
        while (!successfull) {
            try {
                retryCount++;
                //          Commenting suspend deployment call since it is giving issues in china cloud.
                //			notifyProgress(deplymentName, null, progressArr[0], OperationStatus.InProgress,
                //					Messages.stoppingMsg, serviceName);
                //			requestId = service.updateDeploymentStatus(configuration,
                //					serviceName,
                //					deplymentName,
                //                    UpdatedDeploymentStatus.Suspended
                //            );
                //			waitForStatus(configuration, service, requestId);
                notifyProgress(deplymentName, startDate, null, progressArr[0], OperationStatus.InProgress, message("undeployProgressMsg"), deplymentName);
                OperationStatusResponse operationStatusResponse = AzureManagerImpl.getManager(myProject).deleteDeployment(subscriptionId, serviceName, deplymentName, true);
                AzureManagerImpl.getManager(myProject).waitForStatus(subscriptionId, operationStatusResponse);
                notifyProgress(deplymentName, startDate, null, progressArr[1], OperationStatus.Succeeded, message("undeployCompletedMsg"), serviceName);
                successfull = true;
            } catch (Exception e) {
                // Retry 5 times
                if (retryCount > AzurePlugin.REST_SERVICE_MAX_RETRY_COUNT) {
                    log(message("deplError"), e);
                    notifyProgress(deplymentName, startDate, null, 100, OperationStatus.Failed, e.getMessage(), serviceName);
                }
                notifyProgress(deplymentName, startDate, null, -progressArr[0], OperationStatus.InProgress, message("undeployProgressMsg"), deplymentName);
            }
        }
    }

    public void deployToWebApps(WebSite webSite, String url) {
        Date startDate = new Date();
        try {
            String msg = String.format(message("webAppDeployMsg"), webSite.getName());
            notifyProgress(webSite.getName(), startDate, null, 20, OperationStatus.InProgress, msg);
            Thread.sleep(2000);
            notifyProgress(webSite.getName(), startDate, null, 20, OperationStatus.InProgress, msg);
            notifyProgress(webSite.getName(), startDate, null, 20, OperationStatus.InProgress, msg);
            notifyProgress(webSite.getName(), startDate, null, 20, OperationStatus.InProgress, msg);
            Thread.sleep(2000);
            notifyProgress(webSite.getName(), startDate, url, 20, OperationStatus.Succeeded, message("runStatus"), webSite.getName());
        } catch (InterruptedException e) {
            notifyProgress(webSite.getName(), startDate, url, 100, OperationStatus.Succeeded, message("runStatus"), webSite.getName());
        }
    }
}
