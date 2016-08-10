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
import com.microsoft.intellij.ui.StorageAccountPanel;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.ui.util.JdkSrvConfig;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.WAHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.*;
import com.microsoftopentechnologies.azurecommons.roleoperations.JdkSrvConfigUtilMethods;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.xmlhandling.WebAppConfigOperations;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    private JComboBox storageNames;
    private JXHyperlink accLink;
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
    String ftpPath = "/site/wwwroot/";

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

        storageNames.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                storageComboListener();
            }
        });

        customUrl.getDocument().addDocumentListener(createServerUrlListener());

        accLink.setAction(new AccountsAction());

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
        storageNames.setEnabled(enable);
        if (!enable) {
            customUrl.setText("");
            String [] storageAccs = JdkSrvConfig.getStrgAccoNamesAsPerTab(null, false);
            if (storageAccs.length >= 1) {
                storageNames.setModel(new DefaultComboBoxModel(storageAccs));
                storageNames.setSelectedItem(storageAccs[0]);
            }
        }
    }

    private void storageComboListener() {
        int index = storageNames.getSelectedIndex();
        String url = customUrl.getText().trim();
        // check value is not none
        if (index > 0) {
            String newUrl = StorageAccountRegistry.
                    getStrgList().get(index - 1).getStrgUrl();
			/*
			 * If URL is blank and new storage account selected
			 * then auto generate with storage accounts URL.
			 */
            if (url.isEmpty()) {
                customUrl.setText(newUrl);
            } else {
				/*
				 * If storage account in combo box and URL
				 * are in sync then update
				 * corresponding portion of the URL
				 * with the URI of the newly selected storage account
				 * (leaving the container and blob name unchanged.
				 */
                String oldVal = StorageRegistryUtilMethods.
                        getSubStrAccNmSrvcUrlFrmUrl(url);
                String newVal = StorageRegistryUtilMethods.
                        getSubStrAccNmSrvcUrlFrmUrl(newUrl);
                if (oldVal.equalsIgnoreCase(url)) {
                    // old URL is not correct blob storage URL then set new url
                    customUrl.setText(newUrl);
                } else {
                    customUrl.setText(url.replaceFirst(oldVal, newVal));
                }
            }
        }
    }

    private DocumentListener createServerUrlListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleUpdate();
            }

            private void handleUpdate() {
                String url = customUrl.getText().trim();
                String nameInUrl = StorageRegistryUtilMethods.getAccNameFromUrl(url);
                storageNames.setSelectedItem(JdkSrvConfigUtilMethods.getNameToSet(
                        url, nameInUrl, StorageRegistryUtilMethods.getStorageAccountNames(false)));
            }
        };
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

        if (customJDKUser.isSelected() && !(WAEclipseHelperMethods.isBlobStorageUrl(customUrl.getText())
                && customUrl.getText().endsWith(".zip"))) {
            return new ValidationInfo(message("noURLMsg"), customUrl);
        }

        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
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
            if (customJDK.isSelected() || customJDKUser.isSelected()) {
                copyWebConfigForCustom(webSiteConfiguration);
            }
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

    private class AccountsAction extends AbstractAction {

        private AccountsAction() {
            super("Accounts...");
        }

        public void actionPerformed(ActionEvent e) {
            final DefaultDialogWrapper storageAccountDialog = new DefaultDialogWrapper(project, new StorageAccountPanel(project));
            storageAccountDialog.show();
            String cmbName = (String) storageNames.getSelectedItem();
            String accPgName = storageAccountDialog.getSelectedValue();
            String finalNameToSet;
			/*
			 * If row selected on preference page.
			 * set combo box to it always.
			 * Else keep combo box's previous value
			 * as it is.
			 */
            if (accPgName != JdkSrvConfig.NONE_TXT && accPgName != JdkSrvConfig.AUTO_TXT) {
                finalNameToSet = accPgName;
            } else {
                finalNameToSet = cmbName;
            }
            // update storage account combo box
            String [] storageAccs = JdkSrvConfig.getStrgAccoNamesAsPerTab(null, false);
            if (storageAccs.length >= 1) {
                storageNames.setModel(new DefaultComboBoxModel(storageAccs));
                storageNames.setSelectedItem(finalNameToSet);
            }
        }
    }

    private class CustomJdk extends Task.Modal {
        WebSiteConfiguration config;

        public CustomJdk(WebSiteConfiguration webSiteConfiguration) {
            super(project, "Configuring custom JDK", true);
            this.config = webSiteConfiguration;
        }

        @Override
        public void run(@org.jetbrains.annotations.NotNull final ProgressIndicator indicator) {
            try {
                indicator.setFraction(0.1);
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
                        final FTPClient ftp = new FTPClient();
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
                            indicator.setFraction(0.2);

                            // stop and restart web app
                            manager.stopWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                            Thread.sleep(5000);
                            // delete old jdk zip and folder if exists
                            ftp.deleteFile(ftpPath + "jdk.zip");
                            // copy files required for download
                            copyFilesForDownload(ftp);
                            Thread.sleep(5000);
                            manager.startWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                            // wait till zip files download
                            while (!WAHelper.isFilePresentOnFTPServer(ftp, "jdk.zip")) {
                                Thread.sleep(15000);
                            }
                            Thread.sleep(60000);
                            indicator.setFraction(0.5);
                            // copy files required for extraction
                            manager.stopWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                            Thread.sleep(5000);
                            copyFilesForExtract(ftp);
                            // delete old jdk folder if exists
                            if (WAHelper.isFilePresentOnFTPServer(ftp, "jdk")) {
                                AzureManagerImpl.removeFtpDirectory(ftp, ftpPath + "jdk", "");
                                Thread.sleep(60000);
                                while (WAHelper.isFilePresentOnFTPServer(ftp, "jdk")) {
                                    Thread.sleep(15000);
                                }
                            }
                            Thread.sleep(5000);
                            manager.startWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                            indicator.setFraction(0.6);
                            // wait till zip files starts extracting and jdk folder has been created
                            Thread.sleep(60000);
                            int timeout = 0;
                            while (!WAHelper.isFilePresentOnFTPServer(ftp, "jdk")) {
                                Thread.sleep(15000);
                                timeout = timeout + 15000;
                                if (timeout > 300000) {
                                    manager.restartWebSite(config.getSubscriptionId(), config.getWebSpaceName(), config.getWebSiteName());
                                    Thread.sleep(15000);
                                    timeout = 0;
                                }
                            }
                            String jdkPath = "/site/wwwroot/jdk/";
                            if (customJDKUser.isSelected()) {
                                String url = customUrl.getText().trim();
                                String jdkFolderName = url.substring(url.lastIndexOf("/") + 1, url.length());
                                jdkFolderName = jdkFolderName.substring(0, jdkFolderName.indexOf(".zip"));
                                jdkPath = jdkPath + jdkFolderName;
                            } else {
                                String cloudVal = WindowsAzureProjectManager.getCloudValue((String) jdkNames.getSelectedItem(), AzurePlugin.cmpntFile);
                                String jdkFolderName =  cloudVal.substring(cloudVal.indexOf("\\") + 1, cloudVal.length());
                                jdkPath = jdkPath + jdkFolderName;
                            }
                            // count number of files in extracted folder. Else extraction is not complete yet.
                            timeout = 0;
                            while (!WAHelper.checkFileCountOnFTPServer(ftp, jdkPath, 10) && timeout < 420000) {
                                timeout = timeout + 15000;
                                Thread.sleep(15000);
                            }
                            ftp.logout();
                            indicator.setFraction(1.0);
                        } catch (Exception e) {
                            AzurePlugin.log(e.getMessage(), e);
                        } finally {
                            if (ftp.isConnected()) {
                                try {
                                    ftp.disconnect();
                                } catch (IOException ignored) {
                                }
                            }
                        }
                    }
                }
            } catch(AzureCmdException ex) {
                AzurePlugin.log(ex.getMessage(), ex);
            }
        }

        // new methods
        private void copyFilesForDownload(FTPClient ftp) throws Exception {
            // wash.ps1
            InputStream input = new FileInputStream(WAHelper.getCustomJdkFile(".wash.ps1"));
            ftp.storeFile(ftpPath + ".wash.ps1", input);

            // download.vbs
            input = new FileInputStream(WAHelper.getCustomJdkFile("download.vbs"));
            ftp.storeFile(ftpPath + "download.vbs", input);

            // edmDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("Microsoft.Data.Edm.dll"));
            ftp.storeFile(ftpPath + "Microsoft.Data.Edm.dll", input);

            // odataDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("Microsoft.Data.OData.dll"));
            ftp.storeFile(ftpPath + "Microsoft.Data.OData.dll", input);

            // clientDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("Microsoft.Data.Services.Client.dll"));
            ftp.storeFile(ftpPath + "Microsoft.Data.Services.Client.dll", input);

            // configDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("Microsoft.WindowsAzure.Configuration.dll"));
            ftp.storeFile(ftpPath + "Microsoft.WindowsAzure.Configuration.dll", input);

            // storageDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("Microsoft.WindowsAzure.Storage.dll"));
            ftp.storeFile(ftpPath + "Microsoft.WindowsAzure.Storage.dll", input);

            // jsonDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("Newtonsoft.Json.dll"));
            ftp.storeFile(ftpPath + "Newtonsoft.Json.dll", input);

            // spatialDll
            input = new FileInputStream(WAHelper.getCustomJdkFile("System.Spatial.dll"));
            ftp.storeFile(ftpPath + "System.Spatial.dll", input);

            // washCmd
            input = new FileInputStream(WAHelper.getCustomJdkFile("wash.cmd"));
            ftp.storeFile(ftpPath + "wash.cmd", input);

            // psConfig
            input = new FileInputStream(WAHelper.getCustomJdkFile("powershell.exe.activation_config"));
            ftp.storeFile(ftpPath + "powershell.exe.activation_config", input);

            // download.aspx
            String tmpDownloadPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, message("downloadAspx"));
            File file = new File(tmpDownloadPath);
            if (file.exists()) {
                file.delete();
            }
            if (customJDKUser.isSelected()) {
                String finalKey = "";
                int strgAccIndex = storageNames.getSelectedIndex();
                if (strgAccIndex > 0 && !((String) storageNames.getSelectedItem()).isEmpty()) {
                    finalKey = StorageAccountRegistry.getStrgList().get(strgAccIndex - 1).getStrgKey();
                }
                WebAppConfigOperations.prepareDownloadAspx(tmpDownloadPath,
                        customUrl.getText().trim(), finalKey, true);
            } else {
                WebAppConfigOperations.prepareDownloadAspx(tmpDownloadPath,
                        WindowsAzureProjectManager.getCloudAltSrc((String) jdkNames.getSelectedItem(), AzurePlugin.cmpntFile), "", true);
            }
            input = new FileInputStream(tmpDownloadPath);
            ftp.storeFile(ftpPath + message("downloadAspx"), input);

            // web.config for custom download
            String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, message("configName"));
            file = new File(tmpPath);
            if (file.exists()) {
                file.delete();
            }
            WAEclipseHelperMethods.copyFile(WAHelper.getCustomJdkFile(message("configName")), tmpPath);
            String[] pages = {message("downloadAspx")};
            WebAppConfigOperations.prepareWebConfigForAppInit(tmpPath, pages);
            input = new FileInputStream(tmpPath);
            ftp.storeFile(ftpPath + message("configName"), input);
        }

        private void copyFilesForExtract(FTPClient ftp) throws Exception {
            // extract.aspx
            String tmpExtractPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, message("extractAspx"));
            File file = new File(tmpExtractPath);
            if (file.exists()) {
                file.delete();
            }
            WebAppConfigOperations.prepareExtractAspx(tmpExtractPath, true);
            InputStream input = new FileInputStream(tmpExtractPath);
            ftp.storeFile(ftpPath + message("extractAspx"), input);

            // web.config for custom download
            ftp.deleteFile(ftpPath + message("configName"));
            String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, message("configName"));
            file = new File(tmpPath);
            if (file.exists()) {
                file.delete();
            }

            WAEclipseHelperMethods.copyFile(WAHelper.getCustomJdkFile(message("configName")), tmpPath);
            String[] pages = {message("extractAspx")};
            WebAppConfigOperations.prepareWebConfigForAppInit(tmpPath, pages);
            input = new FileInputStream(tmpPath);
            ftp.storeFile(ftpPath + message("configName"), input);
        }
    }

    private void copyWebConfigForCustom(WebSiteConfiguration config) throws AzureCmdException {
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
                    String tmpPath = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, message("configName"));
                    File file = new File(tmpPath);
                    if (file.exists()) {
                        file.delete();
                    }

                    WAEclipseHelperMethods.copyFile(WAHelper.getCustomJdkFile(message("configName")), tmpPath);
                    String jdkFolderName = "";
                    if (customJDKUser.isSelected()) {
                        String url = customUrl.getText();
                        jdkFolderName = url.substring(url.lastIndexOf("/") + 1, url.length());
                        jdkFolderName = jdkFolderName.substring(0, jdkFolderName.indexOf(".zip"));
                    } else {
                        String cloudVal = WindowsAzureProjectManager.getCloudValue((String) jdkNames.getSelectedItem(), AzurePlugin.cmpntFile);
                        jdkFolderName =  cloudVal.substring(cloudVal.indexOf("\\") + 1, cloudVal.length());
                    }
                    String jdkPath = "%HOME%\\site\\wwwroot\\jdk\\" + jdkFolderName;
                    String serverPath = "%programfiles(x86)%\\" +
                            WAHelper.generateServerFolderName(config.getJavaContainer(), config.getJavaContainerVersion());
                    WebAppConfigOperations.prepareWebConfigForCustomJDKServer(tmpPath, jdkPath, serverPath);
                    InputStream input = new FileInputStream(tmpPath);
                    ftp.storeFile(ftpPath + message("configName"), input);
                    cleanup(ftp);
                    ftp.logout();
                } catch (Exception e) {
                    AzurePlugin.log(e.getMessage(), e);
                } finally {
                    if (ftp.isConnected()) {
                        try {
                            ftp.disconnect();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    private void cleanup(FTPClient ftp) throws IOException {
        // wash.ps1
        ftp.deleteFile(ftpPath + ".wash.ps1");

        // download.vbs
        ftp.deleteFile(ftpPath + "download.vbs");

        // edmDll
        ftp.deleteFile(ftpPath + "Microsoft.Data.Edm.dll");

        // odataDll
        ftp.deleteFile(ftpPath + "Microsoft.Data.OData.dll");

        // clientDll
        ftp.deleteFile(ftpPath + "Microsoft.Data.Services.Client.dll");

        // configDll
        ftp.deleteFile(ftpPath + "Microsoft.WindowsAzure.Configuration.dll");

        // storageDll
        ftp.deleteFile(ftpPath + "Microsoft.WindowsAzure.Storage.dll");

        // jsonDll
        ftp.deleteFile(ftpPath + "Newtonsoft.Json.dll");

        // spatialDll
        ftp.deleteFile(ftpPath + "System.Spatial.dll");

        // washCmd
        ftp.deleteFile(ftpPath + "wash.cmd");

        // psConfig
        ftp.deleteFile(ftpPath + "powershell.exe.activation_config");

        // download.aspx
        ftp.deleteFile(ftpPath + message("downloadAspx"));

        //extract.aspx
        ftp.deleteFile(ftpPath + message("extractAspx"));
    }
}