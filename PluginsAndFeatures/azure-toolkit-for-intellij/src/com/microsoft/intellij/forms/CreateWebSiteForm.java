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
package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.ui.NewResourceGroupDialog;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.WAHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKHelper;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.*;
import com.microsoftopentechnologies.azurecommons.exception.AzureCommonsException;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.xmlhandling.WebAppConfigOperations;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.List;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateWebSiteForm extends DialogWrapper {
    private static final String createWebHostingPlanLabel = "<< Create new App Service Plan >>";
    private static final String createResGrpLabel = "<< Create new Resource Group >>";
    private JPanel mainPanel;
    private JComboBox subscriptionComboBox;
    private JTextField nameTextField;
    private JComboBox webHostingPlanComboBox;
    private JComboBox webContainerComboBox;
    private JLabel webContainerLabel;
    private JComboBox groupComboBox;
    private JLabel dnsWebsite;
    private JLabel servicePlanDetailsLocationLbl;
    private JLabel servicePlanDetailsPricingTierLbl;
    private JLabel servicePlanDetailsInstanceSizeLbl;
    private JXHyperlink linkPrice;
    private JTabbedPane tabbedPane1;
    private JRadioButton defaultJDK;
    private JRadioButton customJDK;
    private JComboBox jdkNames;
    private JRadioButton customJDKUser;
    private JTextField customUrl;
    private JTextField saKey;
    private Project project;
    private Subscription subscription;
    private WebHostingPlanCache webHostingPlan;
    private String resourceGroup;
    private String webAppCreated = "";
    List<String> webSiteNames = new ArrayList<String>();
    private CancellableTask.CancellableTaskHandle fillPlansAcrossSub;
    List<String> plansAcrossSub = new ArrayList<String>();
    HashMap<String, WebHostingPlanCache> hostingPlanMap = new HashMap<String, WebHostingPlanCache>();
    ItemListener webHostingPlanComboBoxItemListner;
    final String ftpPath = "/site/wwwroot/";

    public CreateWebSiteForm(@org.jetbrains.annotations.Nullable Project project, List<WebSite> webSiteList) {
        super(project, true, IdeModalityType.PROJECT);

        this.project = project;
        for (WebSite ws : webSiteList) {
            webSiteNames.add(ws.getName());
        }
        setTitle("New Web App Container");

        subscriptionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getItem() instanceof Subscription) {
                    subscription = (Subscription) itemEvent.getItem();
                    fillResourceGroups("");
                }
            }
        });

        groupComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    if (createResGrpLabel.equals(itemEvent.getItem())) {
                        resourceGroup = null;
                        showcreateResourceGroupForm();
                    } else if (itemEvent.getItem() instanceof String) {
                        resourceGroup = (String) itemEvent.getItem();
                        fillWebHostingPlans("");
                    }
                }
            }
        });

        webHostingPlanComboBoxItemListner = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    String selectedItem = (String) itemEvent.getItem();
                    if (selectedItem.equals(createWebHostingPlanLabel)) {
                        showCreateWebHostingPlanForm();
                    } else {
                        WebHostingPlanCache plan = hostingPlanMap.get(selectedItem);
                        pupulateServicePlanDetails(plan);
                    }
                }
            }
        };

        defaultJDK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableCustomJDK(false);
                enableCustomJDKUser(false);
                customJDK.setSelected(false);
                customJDKUser.setSelected(false);
            }
        });

        customJDK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableCustomJDK(true);
                enableCustomJDKUser(false);
                defaultJDK.setSelected(false);
                customJDKUser.setSelected(false);
            }
        });

        customJDKUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableCustomJDK(false);
                enableCustomJDKUser(true);
                defaultJDK.setSelected(false);
                customJDK.setSelected(false);
            }
        });

        defaultJDK.setSelected(true);
        enableCustomJDK(false);
        enableCustomJDKUser(false);

        List<String> containerList = new ArrayList<String>();
        for (WebAppsContainers type : WebAppsContainers.values()) {
            containerList.add(type.getName());
        }
        webContainerComboBox.setModel(new DefaultComboBoxModel(containerList.toArray()));
        linkPrice.setURI(URI.create(message("lnkWebAppPrice")));
        linkPrice.setText("Pricing");
        init();
        webAppCreated = "";
        fillSubscriptions();
    }

    private void enableCustomJDK(boolean enable) {
        if (!enable) {
            try {
                String[] thrdPrtJdkArr = WindowsAzureProjectManager.getThirdPartyJdkNames(AzurePlugin.cmpntFile, "");
                // check at least one element is present
                if (thrdPrtJdkArr.length >= 1) {
                    jdkNames.setModel(new DefaultComboBoxModel(thrdPrtJdkArr));
                    String valueToSet = "";
                    valueToSet = WindowsAzureProjectManager.getFirstDefaultThirdPartyJdkName(AzurePlugin.cmpntFile);
                    if (valueToSet.isEmpty()) {
                        valueToSet = thrdPrtJdkArr[0];
                    }
                    jdkNames.setSelectedItem(valueToSet);
                }
            } catch (WindowsAzureInvalidProjectOperationException ex) {
                log(ex.getMessage(), ex);
            }
        }
        jdkNames.setEnabled(enable);
    }

    private void enableCustomJDKUser(boolean enable) {
        customUrl.setEnabled(enable);
        saKey.setEnabled(enable);
    }


    @org.jetbrains.annotations.Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (subscription == null) {
            return new ValidationInfo("Select a valid subscription.", subscriptionComboBox);
        }

        if (resourceGroup == null || resourceGroup.isEmpty()) {
            return new ValidationInfo("Select a valid resource group.", groupComboBox);
        }

        String name = nameTextField.getText().trim();
        if (name.length() > 60 || !name.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$")) {
            StringBuilder builder = new StringBuilder();
            builder.append("The name can contain letters, numbers and hyphens but the first and last characters must be a letter or number. ");
            builder.append("The length can be between 2 and 60 characters. ");
            return new ValidationInfo(builder.toString(), nameTextField);
        } else if (webSiteNames.contains(name)) {
            return new ValidationInfo(message("inUseErrMsg"), nameTextField);
        }

        if (webHostingPlan == null) {
            return new ValidationInfo("Select a valid app service plan.", webHostingPlanComboBox);
        }

        if (customJDKUser.isSelected()) {
            String url = customUrl.getText();

            // url is valid
            // url is accessible
            try {
                if (saKey.getText().isEmpty()) {
                    if (!urlIsAccessabel(url)) {
                        return new ValidationInfo("Please check the URL is accessible ", customUrl);
                    }
                } else {
                    // first check the link is accessible as it is
                    if (!urlIsAccessabel(url)) {
                        // create shared access signature url and check its accessibility
                        String key = saKey.getText();
                        if (!urlIsAccessabel(AzureSDKHelper.getBlobSasUri(url, key))) {
                            return new ValidationInfo("Please check the storage account key and/or URL is valid ", customUrl);
                        }
                    }
                }
                // link to a zip file
                // consider it's a Sas link
                String urlPath = new URI(url).getPath();
                if (!urlPath.endsWith(".zip")) {
                    return new ValidationInfo("link to a zip file is expected ", customUrl);
                }
            }
            catch (Exception e) {
                return new ValidationInfo("Please check the URL is valid ", customUrl);
            }
        }
        return super.doValidate();
    }

    private static boolean urlIsAccessabel(String url) throws IOException {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("HEAD");
        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return false;
        }
        return true;
    }

    String customJdkErrorMessage = null;
    String customJdkFolderName = null;

    @Override
    protected void doOKAction() {
        java.util.logging.LogManager.getLogManager().reset();
        boolean isOK = true;
        AzureManager manager = AzureManagerImpl.getManager(project);
        mainPanel.getRootPane().getParent().setCursor(new Cursor(Cursor.WAIT_CURSOR));

        try {
            WebSite webSite = manager.createWebSite(subscription.getId(), webHostingPlan, nameTextField.getText().trim());
            WebSiteConfiguration webSiteConfiguration = manager.getWebSiteConfiguration(subscription.getId(),
                    webSite.getWebSpaceName(), webSite.getName());
            if (customJDK.isSelected() || customJDKUser.isSelected()) {
                CustomJdk task = new CustomJdk(webSiteConfiguration);
                task.queue();
            }

            webSiteConfiguration.setJavaVersion("1.8.0_73");
            String selectedContainer = (String) webContainerComboBox.getSelectedItem();
            if (selectedContainer.equalsIgnoreCase(WebAppsContainers.TOMCAT_8.getName())) {
                webSiteConfiguration.setJavaContainer("TOMCAT");
                webSiteConfiguration.setJavaContainerVersion(WebAppsContainers.TOMCAT_8.getValue());
            } else if (selectedContainer.equalsIgnoreCase(WebAppsContainers.TOMCAT_7.getName())) {
                webSiteConfiguration.setJavaContainer("TOMCAT");
                webSiteConfiguration.setJavaContainerVersion(WebAppsContainers.TOMCAT_7.getValue());
            } else if (selectedContainer.equalsIgnoreCase(WebAppsContainers.JETTY_9.getName())) {
                webSiteConfiguration.setJavaContainer("JETTY");
                webSiteConfiguration.setJavaContainerVersion(WebAppsContainers.JETTY_9.getValue());
            }
            manager.updateWebSiteConfiguration(subscription.getId(), webSite.getWebSpaceName(), webSite.getName(), webSite.getLocation(), webSiteConfiguration);
            webAppCreated = webSite.getName();
            Map<WebSite, WebSiteConfiguration> tempMap = AzureSettings.getSafeInstance(project).loadWebApps();
            tempMap.put(webSite, webSiteConfiguration);
            AzureSettings.getSafeInstance(project).saveWebApps(tempMap);

            // to not rewrite the default web config - throw an Exception here
            if(customJdkErrorMessage != null) {
                throw new AzureCommonsException(customJdkErrorMessage);
            }

            if (customJDK.isSelected() || customJDKUser.isSelected()) {
                copyWebConfigForCustom(webSiteConfiguration, customJdkFolderName);
            }
        } catch (AzureCommonsException e) {
            PluginUtil.displayErrorDialog("Error configuring custom jdk",  e.getMessage());
        } catch (AzureCmdException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains(message("nameConflict"))) {
                errorMessage = message("inUseErrMsg");
                isOK = false;
            }
            PluginUtil.displayErrorDialogAndLog(message("webAppErrTtl"), errorMessage, e);
        } finally {
            mainPanel.getRootPane().getParent().setCursor(Cursor.getDefaultCursor());
        }
        if (isOK) {
            super.doOKAction();
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected void dispose() {
        if (fillPlansAcrossSub != null && !fillPlansAcrossSub.isFinished()) {
            fillPlansAcrossSub.cancel();
        }
        super.dispose();
    }

    private void fillSubscriptions() {
//        try {
            List<Subscription> subscriptionList = AzureManagerImpl.getManager(project).getSubscriptionList();
            DefaultComboBoxModel subscriptionComboModel = new DefaultComboBoxModel(subscriptionList.toArray());
            subscriptionComboModel.setSelectedItem(null);
            subscriptionComboBox.setModel(subscriptionComboModel);
            if (!subscriptionList.isEmpty()) {
                subscriptionComboBox.setSelectedIndex(0);
            }
//        } catch (AzureCmdException e) {
//            String msg = "An error occurred while trying to load the subscriptions list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
//            PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
//        }
    }

    private void fillResourceGroups(String valToSet) {
        try {
            if (subscription != null) {
                final List<String> groupList = AzureManagerImpl.getManager(project).getResourceGroupNames(subscription.getId());
                DefaultComboBoxModel model = new DefaultComboBoxModel(groupList.toArray());
                model.insertElementAt(createResGrpLabel, 0);
                model.setSelectedItem(null);
                groupComboBox.setModel(model);
                if (!groupList.isEmpty()) {
                    if (valToSet != null && !valToSet.isEmpty()) {
                        groupComboBox.setSelectedItem(valToSet);
                    } else {
                        groupComboBox.setSelectedIndex(1);
                    }
                    // prepare list of App Service plans for selected subscription
                    if (fillPlansAcrossSub != null && !fillPlansAcrossSub.isFinished()) {
                        fillPlansAcrossSub.cancel();
                    }
                    IDEHelper.ProjectDescriptor projectDescriptor = new IDEHelper.ProjectDescriptor(project.getName(),
                            project.getBasePath() == null ? "" : project.getBasePath());
                    fillPlansAcrossSub = DefaultLoader.getIdeHelper().runInBackground(projectDescriptor, "Loading service plans...", null, new CancellableTask() {
                        @Override
                        public void onCancel() {
                        }

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(@NotNull Throwable throwable) {
                        }

                        @Override
                        public synchronized void run(final CancellationHandle cancellationHandle) throws Throwable {
                            plansAcrossSub = new ArrayList<String>();
                            for (String groupName : groupList) {
                                List<WebHostingPlanCache> plans = AzureManagerImpl.getManager(project).getWebHostingPlans(subscription.getId(), groupName);
                                for (WebHostingPlanCache plan : plans) {
                                    plansAcrossSub.add(plan.getName());
                                }
                            }
                        }
                    });
                }
            }
        } catch (AzureCmdException e) {
            String msg = "An error occurred while loading the resource groups." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
            PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
        }
    }

    private void fillWebHostingPlans(String valToSet) {
        try {
            if (resourceGroup != null) {
                // clean combobox and hashmap
                webHostingPlanComboBox.removeItemListener(webHostingPlanComboBoxItemListner); // remove listner to not get notification while adding items to the combobox
                webHostingPlanComboBox.removeAllItems();
                hostingPlanMap.clear();
                // add <<create new ...>> item
                webHostingPlanComboBox.addItem(createWebHostingPlanLabel);

                // get web hosting service plans from Azure
                List<WebHostingPlanCache> webHostingPlans = AzureManagerImpl.getManager(project).getWebHostingPlans(subscription.getId(), resourceGroup);
                if (webHostingPlans.size() > 0) {
                    // sort the list
                    Collections.sort(webHostingPlans, new Comparator<WebHostingPlanCache>() {
                        @Override
                        public int compare(WebHostingPlanCache o1, WebHostingPlanCache o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });

                    // populate the combobox and the hashmap
                    for (WebHostingPlanCache plan : webHostingPlans) {
                        webHostingPlanComboBox.addItem(plan.getName());
                        hostingPlanMap.put(plan.getName(), plan);
                    }

                    // set selested item
                    if (valToSet == null || valToSet.isEmpty()) {
                        webHostingPlanComboBox.setSelectedItem(webHostingPlanComboBox.getItemAt(1));
                    } else {
                        webHostingPlanComboBox.setSelectedItem(valToSet);;
                    }

                    // populate hosting service plan details
                    pupulateServicePlanDetails(hostingPlanMap.get((String) webHostingPlanComboBox.getSelectedItem()));
                }
                else {
                    // clear selected item if any
                    webHostingPlanComboBox.setSelectedItem(null);
                    // clean hosting service plan details
                    pupulateServicePlanDetails(null);
                }

                webHostingPlanComboBox.addItemListener(webHostingPlanComboBoxItemListner);
            }
        } catch (AzureCmdException e) {
            String msg = "An error occurred while loading the app service plans." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
            PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
        }
    }

    private void pupulateServicePlanDetails(WebHostingPlanCache plan){
        webHostingPlan = plan;
        servicePlanDetailsLocationLbl.setText(plan == null ? "-" : plan.getLocation());
        servicePlanDetailsPricingTierLbl.setText(plan == null ? "-" : plan.getSku().name());
        servicePlanDetailsInstanceSizeLbl.setText(plan == null ? "-" : plan.getWorkerSize().name());
    }

    private void showCreateWebHostingPlanForm() {
        final CreateWebHostingPlanForm form = new CreateWebHostingPlanForm(project, subscription.getId(), resourceGroup, plansAcrossSub);
        form.show();
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (form.isOK()) {
                    fillWebHostingPlans(form.getWebHostingPlan());
                } else {
                    fillWebHostingPlans("");
                }
            }
        });
    }

    private void showcreateResourceGroupForm() {
        NewResourceGroupDialog newResourceGroupDialog = new NewResourceGroupDialog(subscription.getName());
        newResourceGroupDialog.show();
        if (newResourceGroupDialog.isOK()) {
            final ResourceGroupExtended group = newResourceGroupDialog.getResourceGroup();
            if (group != null) {
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        fillResourceGroups(group.getName());
                    }
                });
            }
        } else {
            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                    fillResourceGroups("");
                }
            });
        }
    }

    public String getWebAppCreated() {
        return webAppCreated;
    }

    private class CustomJdk extends Task.Modal {
        WebSiteConfiguration config;

        public CustomJdk(WebSiteConfiguration webSiteConfiguration) {
            super(project, "Configuring custom JDK on Azure", true);
            this.config = webSiteConfiguration;
        }

        @Override
        public void run(@org.jetbrains.annotations.NotNull final ProgressIndicator indicator) {
            final FTPClient ftp = new FTPClient();
            try {
                if (config != null) {
                    indicator.setText("Initializing FTP client...");
                    AzureManager manager = AzureManagerImpl.getManager(project);
                    WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
                            config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                    // retrieve ftp publish profile
                    WebSitePublishSettings.FTPPublishProfile ftpProfile = null;
                    for (WebSitePublishSettings.PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
                        if (pp instanceof WebSitePublishSettings.FTPPublishProfile) {
                            ftpProfile = (WebSitePublishSettings.FTPPublishProfile) pp;
                            break;
                        }
                    }
                    indicator.setFraction(0.1);

                    if (ftpProfile != null) {
                        try {
                            indicator.setText("Logging in...");
                            URI uri = null;
                            uri = new URI(ftpProfile.getPublishUrl());
                            ftp.connect(uri.getHost());
                            final int replyCode = ftp.getReplyCode();
                            if (!FTPReply.isPositiveCompletion(replyCode)) {
                                ftp.disconnect();
                            }
                            if (!ftp.login(ftpProfile.getUserName(), ftpProfile.getPassword())) {
                                ftp.logout();
                            }
                            ftp.setFileType(FTP.BINARY_FILE_TYPE);
                            if (ftpProfile.isFtpPassiveMode()) {
                                ftp.enterLocalPassiveMode();
                            }
                            ftp.setControlKeepAliveTimeout(3000);

                            indicator.setFraction(0.2);

                            // {{ debug only
                            System.out.println("\t\t" + ftpProfile.getPublishUrl());
                            System.out.println("\t\t" + ftpProfile.getUserName());
                            System.out.println("\t\t" + ftpProfile.getPassword());
                            // }}

                            final String siteUrl = ftpProfile.getDestinationAppUrl();

                            // stop and restart web app
                            indicator.setText("Stopping the site...");
                            manager.stopWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                            WAHelper.checkSiteIsDown(siteUrl);
                            indicator.setFraction(0.3);

                            indicator.setText("Uploading scripts...");
                            uploadWorkerData(ftp);
                            indicator.setFraction(0.4);

                            indicator.setText("Starting the site...");
                            manager.startWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                            WAHelper.checkSiteIsUp(siteUrl);
                            indicator.setFraction(0.5);

                            // Polling report.txt...
                            indicator.setText("Checking the JDK gets downloaded and unpacked...");
                            int step = 0;
                            while (!WAHelper.isRemoteFileExist(ftp, "report.txt")) {
                                indicator.checkCanceled();
                                if (step++ > 3) checkFreeSpaceAvailability(ftp);
                                Thread.sleep(5000);
                                WAHelper.sendGet(siteUrl);
                            }
                            indicator.setFraction(0.7);

                            indicator.setText("Checking status...");
                            OutputStream reportFileStream = new ByteArrayOutputStream();
                            ftp.retrieveFile("report.txt", reportFileStream);
                            String reportFileString = reportFileStream.toString();
                            if (reportFileString.startsWith("FAIL")) {
                                String err = reportFileString.substring(reportFileString.indexOf(":"+1));
                                throw new AzureCommonsException(err);
                            }

                            // get top level jdk folder name (under jdk folder)
                            String jdkPath = "/site/wwwroot/jdk/";
                            FTPFile[] ftpDirs = ftp.listDirectories(jdkPath);
                            if (ftpDirs.length != 1) {
                                String err = "Bad JDK archive. Please make sure the JDK archive contains a single JDK folder. For example, 'my-jdk1.7.0_79.zip' archive should contain 'jdk1.7.0_79' folder only";
                                throw new AzureCommonsException(err);
                            }

                            String jdkFolderName = ftpDirs[0].getName();

                            customJdkFolderName = jdkFolderName;

                            indicator.setFraction(1.0);
                        } finally {
                            cleanupWorkerData(ftp);
                        }
                    }
                }
            } catch (ProcessCanceledException e) {
                cleanupJdk(ftp);
                customJdkErrorMessage = "CANCELED BY USER";
            } catch (Exception ex) {
                cleanupJdk(ftp);
                customJdkErrorMessage = ex.getMessage();
                AzurePlugin.log(ex.getMessage(), ex);
            } finally {
                if (ftp != null && ftp.isConnected()) {
                    try {
                        ftp.logout();
                        ftp.disconnect();
                    } catch (IOException ignored) {
                        // go nothing
                    }
                }
            }
        }

        private void checkFreeSpaceAvailability(FTPClient ftp) throws Exception {
            final String remoteFileName = "ping";
            final String message = "There is not enough space in App Service plan File System Storage to complete the operation.";
            try {
                // should throw an exception if the is no room
                boolean res = ftp.storeFile(ftpPath + remoteFileName, new ByteArrayInputStream(new byte[100000]));
                if (res == false) {
                    throw new AzureCommonsException(message);
                }
            } catch (IOException e) {
                throw new AzureCommonsException(message);
            } finally {
                try {
                    ftp.deleteFile(ftpPath + remoteFileName);
                } catch (IOException ex) {
                    AzurePlugin.log(ex.getMessage(), ex);
                }
            }
        }

        private void cleanupWorkerData(FTPClient ftp) {
            try {
                ftp.deleteFile(ftpPath + "getjdk.aspx");
                ftp.deleteFile(ftpPath + "jdk.zip");
            } catch (Exception ex) {
                AzurePlugin.log(ex.getMessage(), ex);
            }
        }

        private void cleanupJdk(FTPClient ftp) {
            try {
                if (customJdkFolderName != null) {
                    AzureManagerImpl.removeFtpDirectory(ftp, ftpPath, "jdk");
                }
            } catch (Exception ex) {
                AzurePlugin.log(ex.getMessage(), ex);
            }
        }

        private void uploadWorkerData(FTPClient ftp) throws Exception {
            String downloadUrl;
            if (customJDKUser.isSelected()) {
                String url = customUrl.getText().trim();
                String key = saKey.getText();

                downloadUrl = (!key.isEmpty())
                        ? AzureSDKHelper.getBlobSasUri(url, key)
                        : url;
            } else {
                downloadUrl = WindowsAzureProjectManager.getCloudAltSrc((String) jdkNames.getSelectedItem(), AzurePlugin.cmpntFile);
            }

            String aspxPageName = "getjdk.aspx";

            byte[] aspxPageData = WebAppConfigOperations.generateAspxScriptForCustomJdk(downloadUrl);
            ftp.storeFile(ftpPath + aspxPageName, new ByteArrayInputStream(aspxPageData));

            byte[] webXmlData = WebAppConfigOperations.generateWebXmlForCustomJdk(aspxPageName, null);
            ftp.storeFile(ftpPath + "web.config", new ByteArrayInputStream(webXmlData));
        }
    }

    private void copyWebConfigForCustom(WebSiteConfiguration config, String jdkFolderName) throws AzureCmdException {
        if  (jdkFolderName == null || jdkFolderName.isEmpty()) {
            throw new NullArgumentException("jdkFolderName is null or empty");
        }
        if (config != null) {
            AzureManager manager = AzureManagerImpl.getManager(project);
            WebSitePublishSettings webSitePublishSettings = manager.getWebSitePublishSettings(
                    config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
            // retrieve ftp publish profile
            WebSitePublishSettings.FTPPublishProfile ftpProfile = null;
            for (WebSitePublishSettings.PublishProfile pp : webSitePublishSettings.getPublishProfileList()) {
                if (pp instanceof WebSitePublishSettings.FTPPublishProfile) {
                    ftpProfile = (WebSitePublishSettings.FTPPublishProfile) pp;
                    break;
                }
            }

            if (ftpProfile != null) {
                FTPClient ftp = new FTPClient();
                try {
                    URI uri = null;
                    uri = new URI(ftpProfile.getPublishUrl());
                    ftp.connect(uri.getHost());
                    final int replyCode = ftp.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(replyCode)) {
                        ftp.disconnect();
                    }
                    if (!ftp.login(ftpProfile.getUserName(), ftpProfile.getPassword())) {
                        ftp.logout();
                    }
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    if (ftpProfile.isFtpPassiveMode()) {
                        ftp.enterLocalPassiveMode();
                    }
                    ftp.deleteFile(ftpPath + message("configName"));

                    String jdkPath = "%HOME%\\site\\wwwroot\\jdk\\" + jdkFolderName;
                    String serverPath = "%programfiles(x86)%\\" +
                            WAHelper.generateServerFolderName(config.getJavaContainer(), config.getJavaContainerVersion());
                    byte[] webXmlData = WebAppConfigOperations.prepareWebConfigForCustomJDKServer(jdkPath, serverPath);
                    ftp.storeFile(ftpPath + message("configName"),  new ByteArrayInputStream(webXmlData));
                    ftp.logout();
                } catch (Exception e) {
                    AzurePlugin.log(e.getMessage(), e);
                } finally {
                    if (ftp.isConnected()) {
                        try {
                            ftp.disconnect();
                        } catch (IOException ignored) {
                            // do nothing
                        }
                    }
                }
            }
        }
    }
}