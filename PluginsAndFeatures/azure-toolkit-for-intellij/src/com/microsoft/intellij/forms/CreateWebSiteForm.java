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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.ui.NewResourceGroupDialog;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URI;
import java.util.*;
import java.util.List;

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

        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        boolean isOK = true;
        AzureManager manager = AzureManagerImpl.getManager();
        mainPanel.getRootPane().getParent().setCursor(new Cursor(Cursor.WAIT_CURSOR));

        try {
            WebSite webSite = manager.createWebSite(subscription.getId(), webHostingPlan, nameTextField.getText().trim());
            WebSiteConfiguration webSiteConfiguration = manager.getWebSiteConfiguration(subscription.getId(),
                    webSite.getWebSpaceName(), webSite.getName());
            webSiteConfiguration.setJavaVersion("1.8");
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
            List<Subscription> subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
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
                final List<String> groupList = AzureManagerImpl.getManager().getResourceGroupNames(subscription.getId());
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
                List<WebHostingPlanCache> webHostingPlans = AzureManagerImpl.getManager().getWebHostingPlans(subscription.getId(), resourceGroup);
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
}