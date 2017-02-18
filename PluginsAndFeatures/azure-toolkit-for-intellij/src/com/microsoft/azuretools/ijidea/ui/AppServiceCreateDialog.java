/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

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
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.*;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppServiceCreateDialog extends DialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(AppServiceCreateDialog.class);

    protected JPanel contentPane;
    protected JTextField textFieldWebappName;
    protected JComboBox comboBoxWebContainer;
    protected JComboBox comboBoxSubscription;
    protected JComboBox comboBoxResourceGroup;
    protected JComboBox comboBoxAppServicePlan;
    protected JComboBox comboBoxJDK3Party;
    protected JTextField textFieldJDKUrl;
    protected JTextField textFieldJDKAccountKey;
    protected JLabel labelJDKAccountKey;
    protected JTabbedPane tabbedPaneAppService;
    protected JTextField textFieldAppServicePlanName;
    protected JTabbedPane tabbedPaneResourceGroup;
    protected JTextField textFieldResourceGroupName;
    protected JComboBox comboBoxAppServicePlanLocation;
    protected JComboBox comboBoxAppServicePlanPricingTier;
    protected JTabbedPane tabbedPaneJdk;
    protected JXHyperlink linkPricing;
    protected JPanel panelResourceGroupUseExisting;
    protected JPanel panelResourceGroupCreateNew;
    protected JPanel panelAppServiceUseExisting;
    protected JPanel panelAppServiceCreateNew;
    protected JPanel panelJdkDefault;
    protected JPanel panelJdk3Party;
    protected JPanel panelJdkOwn;
    protected JXHyperlink linkLicense;
    protected JPanel panelResourceGroup;
    protected JPanel panelAppServicePlan;
    private JLabel labelFieldAppServicePlanLocation;
    private JLabel labelFieldAppServicePlanTier;
    private JTabbedPane tabbedPaneOptions;
    private JPanel panelTabAppServicePlan;
    private JRadioButton radioButtonAppServicePlanCreateNew;
    private JRadioButton radioButtonAppServicePlanUseExisting;
    private JRadioButton radioButtonResourceGroupCreatNew;
    private JPanel panelTabResourceGroup;
    private JRadioButton radioButtonResourceGroupUseExisting;
    private JPanel panelTabJdk;
    private JRadioButton radioButtonJdkDefault;
    private JRadioButton radioButtonJdk3rdParty;
    private JRadioButton radioButtonJdkOwn;
    private JLabel labelJdkDefault;
    private JLabel labelStorageAccountComment;
    private JLabel labelAppServicePlanLocation;
    private JLabel labelAppServicePlanPricingTier;

    protected Module module;

    protected static final String textNotAvailable = "N/A";

    private void createUIComponents() {
        // generate random name
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());

        textFieldWebappName = new HintTextField("<enter name>");
        textFieldWebappName.setText("webapp-" + date);
        textFieldAppServicePlanName = new HintTextField("<enter name>");
        textFieldAppServicePlanName.setText("asp-" + date);
        textFieldResourceGroupName = new HintTextField("<enter name>");
        textFieldResourceGroupName.setText("rg-webapp-" + date);
        textFieldJDKUrl = new HintTextField("<enter url>");
        textFieldJDKAccountKey = new HintTextField("<enter key>");
    }

    protected static abstract class AdapterBase<T> {
        protected T adapted;
        protected AdapterBase(T adapted) {
            this.adapted = adapted;
        }
        protected T getAdapted() {
            return adapted;
        }
    }

    protected static class ResourceGroupAdapter extends AdapterBase<ResourceGroup> {
        public ResourceGroupAdapter(ResourceGroup rg) {
            super(rg);
        }
        @Override
        public String toString() {
            return adapted.name();
        }
    }
    protected static class AppServicePlanAdapter extends AdapterBase<AppServicePlan> {
        public AppServicePlanAdapter(AppServicePlan asp) {
            super(asp);
        }
        @Override
        public String toString() {
            return adapted.name();
        }
    }

    protected static class LocationAdapter extends AdapterBase<Location> {
        public LocationAdapter(Location l) {
            super(l);
        }
        @Override
        public String toString() {
            return adapted.displayName();
        }
    }

    public static AppServiceCreateDialog go(Module module){
        AppServiceCreateDialog d = new AppServiceCreateDialog(module);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }
        return null;
    }

    protected AppServiceCreateDialog(Module module) {
        super(module.getProject(), true, IdeModalityType.PROJECT);
        this.module =  module;
        setModal(true);
        setTitle("Create App Service");

        setOKButtonText("Create");

        init();

        comboBoxSubscription.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    fillResourceGroups();
                    fillAppServicePlans();
                    fillAppServicePlansDetails();
                    fillAppServicePlanLocations();
                }
            }
        });

        comboBoxAppServicePlan.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    fillAppServicePlansDetails();
                }
            }
        });

        linkPricing.setURI(URI.create("https://azure.microsoft.com/en-us/pricing/details/app-service/"));
        linkPricing.setText("App Service Pricing Details");

        linkLicense.setURI(URI.create(AzulZuluModel.getLicenseUrl()));
        linkLicense.setText("License");

        fillWebContainers();
        fillSubscriptions();
        fillResourceGroups();
        fillAppServicePlans();
        fillAppServicePlansDetails();
        fillAppServicePlanLocations();
        fillAppServicePlanPricingTiers();
        fill3PartyJdk();

        radioButtonAppServicePlanCreateNew.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioAppServicePlanLogic();
                }
            }
        });
        radioButtonAppServicePlanUseExisting.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioAppServicePlanLogic();
                }

            }
        });
        radioButtonResourceGroupCreatNew.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioResourceGroupLogic();
                }
            }
        });
        radioButtonResourceGroupUseExisting.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioResourceGroupLogic();
                }
            }
        });
        radioButtonJdkDefault.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioJdkLogic();
                }
            }
        });
        radioButtonJdk3rdParty.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioJdkLogic();
                }
            }
        });
        radioButtonJdkOwn.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    radioJdkLogic();
                }
            }
        });
    }

    private void radioAppServicePlanLogic() {
        boolean enabled = radioButtonAppServicePlanCreateNew.isSelected();
        textFieldAppServicePlanName.setEnabled(enabled);
        comboBoxAppServicePlanLocation.setEnabled(enabled);
        comboBoxAppServicePlanPricingTier.setEnabled(enabled);
        comboBoxAppServicePlan.setEnabled(!enabled);
        labelAppServicePlanLocation.setEnabled(!enabled);
        labelFieldAppServicePlanLocation.setEnabled(!enabled);
        labelAppServicePlanPricingTier.setEnabled(!enabled);
        labelFieldAppServicePlanTier.setEnabled(!enabled);
    }

    private void radioResourceGroupLogic() {
        boolean enabled = radioButtonResourceGroupCreatNew.isSelected();
        textFieldResourceGroupName.setEnabled(enabled);
        comboBoxResourceGroup.setEnabled(!enabled);
    }

    private void radioJdkLogic() {
        boolean enabledDefault = radioButtonJdkDefault.isSelected();
        labelJdkDefault.setEnabled(enabledDefault);

        boolean enabled3Party = radioButtonJdk3rdParty.isSelected();
        comboBoxJDK3Party.setEnabled(enabled3Party);
        linkLicense.setEnabled(enabled3Party);

        boolean enabledOwn = radioButtonJdkOwn.isSelected();
        textFieldJDKUrl.setEnabled(enabledOwn);
        labelJDKAccountKey.setEnabled(enabledOwn);
        textFieldJDKAccountKey.setEnabled(enabledOwn);
        labelStorageAccountComment.setEnabled(enabledOwn);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "AppServiceCreateDialog";
    }

    protected void fillWebContainers() {
        try {
            DefaultComboBoxModel<WebContainer> cbModel = createComboboxModelFromClassFields(WebContainer.class);
            comboBoxWebContainer.setModel(cbModel);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("fillWebContainers", ex);
        }
    }

    protected void fillSubscriptions() {
        if (AzureModel.getInstance().getSubscriptionToResourceGroupMap() == null) {
            updateAndFillSubscriptions();
        } else {
            doFillSubscriptions();
        }
    }

    protected void updateAndFillSubscriptions() {
        ProgressManager.getInstance().run(new Task.Modal(module.getProject(), "Update Azure Local Cache Progress", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {

                progressIndicator.setIndeterminate(true);
                try {
                    if (progressIndicator.isCanceled()) {
                        AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                doCancelAction();
                            }
                        }, ModalityState.any());
                    }

                    AzureModelController.updateSubscriptionMaps(new UpdateProgressIndicator(progressIndicator));
                    AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            doFillSubscriptions();
                        }
                    }, ModalityState.any());


                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.error("updateAndFillSubscriptions", ex);
                }
            }
        });
    }

    protected void doFillSubscriptions() {
        try {
            // reset model
            Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
            DefaultComboBoxModel<SubscriptionDetail> cbModel = new DefaultComboBoxModel<SubscriptionDetail>();
            if (sdl == null) {
                System.out.println("sdl is null");
                return;
            }
            for (SubscriptionDetail sd : sdl) {
                if (!sd.isSelected()) continue;
                cbModel.addElement(sd);
            }
            comboBoxSubscription.setModel(cbModel);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("doFillSubscriptions", ex);
        }
    }

    protected void fillResourceGroups() {

        DefaultComboBoxModel<SubscriptionDetail> cbModelSub = (DefaultComboBoxModel<SubscriptionDetail>)comboBoxSubscription.getModel();
        SubscriptionDetail sd = (SubscriptionDetail)cbModelSub.getSelectedItem();
        if (sd == null) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        DefaultComboBoxModel<ResourceGroupAdapter> cbModel = new DefaultComboBoxModel<ResourceGroupAdapter>();
        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd);
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }

        for (ResourceGroup rg : rgl) {
            cbModel.addElement(new ResourceGroupAdapter(rg));
        }
        comboBoxResourceGroup.setModel(cbModel);

        if (cbModel.getSize() == 1)
            comboBoxResourceGroup.setSelectedItem(null);
    }

    protected void fillAppServicePlans() {
        DefaultComboBoxModel<SubscriptionDetail> cbModelSub = (DefaultComboBoxModel<SubscriptionDetail>)comboBoxSubscription.getModel();
        SubscriptionDetail sd = (SubscriptionDetail)cbModelSub.getSelectedItem();
        if (sd == null) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd);
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }

        DefaultComboBoxModel<AppServicePlanAdapter> cbModel = new DefaultComboBoxModel<AppServicePlanAdapter>();
        for (ResourceGroup rg : rgl) {
            List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
            for (AppServicePlan asp : aspl) {
                cbModel.addElement(new AppServicePlanAdapter(asp));
            }
        }
        comboBoxAppServicePlan.setModel(cbModel);
    }

    protected void fillAppServicePlansDetails() {
        DefaultComboBoxModel<AppServicePlanAdapter> cbModel =  (DefaultComboBoxModel<AppServicePlanAdapter>)comboBoxAppServicePlan.getModel();
        AppServicePlanAdapter aspa = (AppServicePlanAdapter)cbModel.getSelectedItem();
        if (aspa == null || aspa.getAdapted() == null) { // empty || <create new>
            labelFieldAppServicePlanLocation.setText(textNotAvailable);
            labelFieldAppServicePlanTier.setText(textNotAvailable);
            return;
        } else {
            AppServicePlan asp = aspa.getAdapted();
            labelFieldAppServicePlanLocation.setText(asp.region().label());
            labelFieldAppServicePlanTier.setText(asp.pricingTier().toString());
        }
    }

    protected void fillAppServicePlanLocations() {
        try {
            DefaultComboBoxModel<SubscriptionDetail> cbModelSub = (DefaultComboBoxModel<SubscriptionDetail>)comboBoxSubscription.getModel();
            SubscriptionDetail sd = (SubscriptionDetail)cbModelSub.getSelectedItem();
            if (sd == null) { // empty
                System.out.println("No subscription is selected");
                return;
            }

            Map<SubscriptionDetail, List<Location>> sdlocMap = AzureModel.getInstance().getSubscriptionToLocationMap();
            List<Location> locl = sdlocMap.get(sd);

            DefaultComboBoxModel<LocationAdapter> cbModel = new DefaultComboBoxModel<LocationAdapter>();
            for (Location l : locl) {
                cbModel.addElement(new LocationAdapter(l));
            }
            comboBoxAppServicePlanLocation.setModel(cbModel);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("fillAppServicePlanLocations", ex);
        }
    }

    protected void fillAppServicePlanPricingTiers() {
        try {
            DefaultComboBoxModel<AppServicePricingTier> cbModel = createComboboxModelFromClassFields(AppServicePricingTier.class);
            comboBoxAppServicePlanPricingTier.setModel(cbModel);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("fillAppServicePlanPricingTiers", ex);
        }
    }

    protected void fill3PartyJdk() {

        DefaultComboBoxModel<AzulZuluModel> cbModel = new DefaultComboBoxModel<AzulZuluModel>();
        for (AzulZuluModel jdk : AzulZuluModel.values()) {
            if (jdk.isDeprecated()) continue;
            cbModel.addElement(jdk);
        }
        comboBoxJDK3Party.setModel(cbModel);
    }

    protected static <T> List<T> createLisFromClassFields(Class c) throws IllegalAccessException {
        List<T> list = new LinkedList<T>();

        Field[] declaredFields = c.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && Modifier.isPublic(modifiers)) {
                T value = (T)field.get(null);
                list.add(value);
            }
        }
        return list;
    }

    protected static <T> DefaultComboBoxModel<T> createComboboxModelFromClassFields(Class c) throws IllegalAccessException {
        List<T> list = createLisFromClassFields(c);
        DefaultComboBoxModel<T> cbModel = new DefaultComboBoxModel<T>((T[]) list.toArray());
        return cbModel;
    }

    public enum JdkTab {
        Default,
        ThirdParty,
        Own;
    }

    protected class Model {
        public String webAppName;
        public WebContainer webContainer;
        public SubscriptionDetail subscriptionDetail;

        public boolean isResourceGroupCreateNew;
        public ResourceGroup resourceGroup;
        public String resourceGroupNameCreateNew;

        public boolean isAppServicePlanCreateNew;
        public AppServicePlan appServicePlan;
        public String appServicePlanNameCreateNew;
        public Location appServicePlanLocationCreateNew;
        public AppServicePricingTier appServicePricingTierCreateNew;

        public String jdk3PartyUrl;
        public String jdkOwnUrl;
        public String storageAccountKey;
        public JdkTab jdkTab;
        public String jdkDownloadUrl;

        public void collectData() {
            webAppName = textFieldWebappName.getText().trim();
            webContainer = (WebContainer)comboBoxWebContainer.getSelectedItem();
            subscriptionDetail = (SubscriptionDetail)comboBoxSubscription.getSelectedItem();

            //isResourceGroupCreateNew = tabbedPaneResourceGroup.getSelectedComponent() == panelResourceGroupCreateNew;
            isResourceGroupCreateNew = radioButtonResourceGroupCreatNew.isSelected();
            ResourceGroupAdapter rga = (ResourceGroupAdapter) comboBoxResourceGroup.getModel().getSelectedItem();
            resourceGroup = rga == null ? null : rga.getAdapted();
            resourceGroupNameCreateNew = textFieldResourceGroupName.getText().trim();

            //isAppServicePlanCreateNew = tabbedPaneAppService.getSelectedComponent() == panelAppServiceCreateNew;
            isAppServicePlanCreateNew = radioButtonAppServicePlanCreateNew.isSelected();
            AppServicePlanAdapter aspa = (AppServicePlanAdapter)comboBoxAppServicePlan.getModel().getSelectedItem();
            appServicePlan = aspa == null ? null : aspa.getAdapted();

            appServicePlanNameCreateNew = textFieldAppServicePlanName.getText().trim();

            AppServicePricingTier appServicePricingTier = (AppServicePricingTier) comboBoxAppServicePlanPricingTier.getModel().getSelectedItem();
            appServicePricingTierCreateNew = appServicePricingTier == null ? null : appServicePricingTier;

            LocationAdapter loca = (LocationAdapter) comboBoxAppServicePlanLocation.getModel().getSelectedItem();
            appServicePlanLocationCreateNew = loca == null ? null : loca.getAdapted();

            //Component selectedJdkPanel = tabbedPaneJdk.getSelectedComponent();
            jdkTab = (radioButtonJdkDefault.isSelected())
                ? JdkTab.Default
                : (radioButtonJdk3rdParty.isSelected())
                    ? JdkTab.ThirdParty
                    : (radioButtonJdkOwn.isSelected())
                        ? JdkTab.Own
                        : null;

            AzulZuluModel jdk3Party = (AzulZuluModel)comboBoxJDK3Party.getModel().getSelectedItem();
            jdk3PartyUrl = jdk3Party == null ? null : jdk3Party.getDownloadUrl();
            jdkOwnUrl = textFieldJDKUrl.getText().trim();
            storageAccountKey = textFieldJDKAccountKey.getText().trim();
            jdkDownloadUrl = null; // get value in validate phase
        }
    }

    protected Model model = new Model();


    @Nullable
    @Override
    protected ValidationInfo doValidate() {

        model.collectData();

        String webappName = model.webAppName;
        if (webappName.length() > 60 || !webappName.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$")) {
            StringBuilder builder = new StringBuilder();
            builder.append("The name can contain letters, numbers and hyphens but the first and last characters must be a letter or number. ");
            builder.append("The length can be between 2 and 60 characters. ");
            return new ValidationInfo(builder.toString(), textFieldWebappName);
        } else {
            for (List<WebApp> wal : AzureModel.getInstance().getResourceGroupToWebAppMap().values()) {
                for (WebApp wa : wal) {
                    if (wa.name().toLowerCase().equals(webappName.toLowerCase())) {
                        return new ValidationInfo("The name is already taken", textFieldWebappName);
                    }
                }
            }
        }
        if (model.subscriptionDetail == null) {
            return new ValidationInfo("Select a valid subscription.", comboBoxSubscription);
        }

        if (model.isAppServicePlanCreateNew) {
            if (model.appServicePlanNameCreateNew.isEmpty()) {
                return new ValidationInfo("Enter a valid App Service Plan name.", textFieldAppServicePlanName);
            } else {
                if (!model.appServicePlanNameCreateNew.matches("^[A-Za-z0-9-]*[A-Za-z0-9-]$")) {
                    return new ValidationInfo("App Service Plan name can only include alphanumeric characters and hyphens.", textFieldAppServicePlanName);
                }
                // App service plan name must be unuque in each subscription
                SubscriptionDetail sd = model.subscriptionDetail;
                List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd);
                for (ResourceGroup rg : rgl ) {
                    List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
                    for (AppServicePlan asp : aspl) {
                        if (asp.name().toLowerCase().equals(model.appServicePlanNameCreateNew.toLowerCase())) {
                            return new ValidationInfo("App service plan name must be unuque in each subscription.", textFieldAppServicePlanName);
                        }
                    }
                }
            }
        } else {
            if (model.appServicePlan == null ) {
                return new ValidationInfo("Select a valid App Service Plan.", comboBoxResourceGroup);
            }
        }

        if (model.isResourceGroupCreateNew) {
            if (model.resourceGroupNameCreateNew.isEmpty()) {
                return new ValidationInfo("Enter a valid resource group name", textFieldResourceGroupName);
            } else {
                if (!model.resourceGroupNameCreateNew.matches("^[A-Za-z0-9-_()\\.]*[A-Za-z0-9-_()]$")) {
                    return new ValidationInfo("Resounce group name can only include alphanumeric characters, periods, underscores, hyphens, and parenthesis and can't end in a period.", textFieldResourceGroupName);
                }

                for (List<ResourceGroup> rgl : AzureModel.getInstance().getSubscriptionToResourceGroupMap().values()) {
                    for (ResourceGroup rg : rgl) {
                        if (rg.name().toLowerCase().equals(model.resourceGroupNameCreateNew.toLowerCase())) {
                            return new ValidationInfo("The name is already taken", textFieldResourceGroupName);
                        }
                    }
                }
            }
        } else {
            if (model.resourceGroup == null ) {
                return new ValidationInfo("Select a valid resource group.", comboBoxResourceGroup);
            }
        }

        ValidationInfo res = volidateJdkTab();
        if (res != null) return res;

        return super.doValidate();
    }

    protected ValidationInfo superDoValidate() {
        return super.doValidate();
    }

    protected ValidationInfo volidateJdkTab() {
        try {
            switch (model.jdkTab) {
                case Default:
                    // do nothing
                    model.jdkDownloadUrl = null;
                    break;
                case ThirdParty:
                    if (!WebAppUtils.isUrlAccessible(model.jdk3PartyUrl)) {
                        return new ValidationInfo("Please check the URL is valid.", comboBoxJDK3Party);
                    }
                    model.jdkDownloadUrl = model.jdk3PartyUrl;
                    break;
                case Own:
                    if (model.jdkOwnUrl.isEmpty()) {
                        return new ValidationInfo("Enter a valid URL.", textFieldJDKUrl);
                    } else {
                        // first check the link is accessible as it is
                        if (!WebAppUtils.isUrlAccessible(model.jdkOwnUrl)) {
                            // create shared access signature url and check its accessibility
                            String sasUrl = StorageAccoutUtils.getBlobSasUri(model.jdkOwnUrl, model.storageAccountKey);
                            if (!WebAppUtils.isUrlAccessible(sasUrl)) {
                                return new ValidationInfo("Please check the storage account key and/or URL is valid.", textFieldJDKUrl);
                            } else {
                                model.jdkDownloadUrl = sasUrl;
                            }
                        } else {
                            model.jdkDownloadUrl = model.jdkOwnUrl;
                        }
                    }
                    // link to a zip file
                    // consider it's a Sas link
                    String urlPath = new URI(model.jdkOwnUrl).getPath();
                    if (!urlPath.endsWith(".zip")) {
                        return new ValidationInfo("link to a zip file is expected.", textFieldJDKUrl);
                    }

                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("volidateJdkTab", ex);
            //ErrorWindow.show(ex.getMessage(), "Form Data Validation Error", this.contentPane);
            return new ValidationInfo("Url validation exception:" + ex.getMessage(), tabbedPaneJdk);
        }
        return null;
    }

    protected WebApp createAppService(IProgressIndicator progressIndicator) throws Exception {

        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return null; }

        Azure azure = azureManager.getAzure(model.subscriptionDetail.getSubscriptionId());
        WebApp.DefinitionStages.Blank definitionStages = azure.webApps().define(model.webAppName);
        WebApp.DefinitionStages.WithAppServicePlan ds1;

        if (model.isResourceGroupCreateNew) {
            ds1 = definitionStages.withNewResourceGroup(model.resourceGroupNameCreateNew);
        } else {
            ds1 = definitionStages.withExistingResourceGroup(model.resourceGroup);
        }

        WebAppBase.DefinitionStages.WithCreate<WebApp> ds2;
        if (model.isAppServicePlanCreateNew) {
            ds2 = ds1.withNewAppServicePlan(model.appServicePlanNameCreateNew)
                    .withRegion(model.appServicePlanLocationCreateNew.name())
                    .withPricingTier(model.appServicePricingTierCreateNew);
        } else {
            ds2 = ds1.withExistingAppServicePlan(model.appServicePlan);
        }

        if (model.jdkDownloadUrl == null) { // no custom jdk
            ds2 = ds2.withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(model.webContainer);
        }

        WebApp myWebApp = ds2.create();

        if (model.jdkDownloadUrl != null ) {
            progressIndicator.setText("Deploying custom jdk...");
            WebAppUtils.deployCustomJdk(myWebApp, model.jdkDownloadUrl, model.webContainer, progressIndicator);
        }

        // update cache
        AzureModel azureModel = AzureModel.getInstance();

        if (model.isResourceGroupCreateNew) {
            ResourceGroup rg = azure.resourceGroups().getByName(model.resourceGroupNameCreateNew);
            AzureModelController.addNewResourceGroup(model.subscriptionDetail, rg);
            if (model.isAppServicePlanCreateNew) {
                AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                AzureModelController.addNewAppServicePlanToJustCreatedResourceGroup(rg, asp);
                AzureModelController.addNewWebAppToJustCreatedResourceGroup(rg, myWebApp);
            }
        } else {
            ResourceGroup rg = model.resourceGroup;
            AzureModelController.addNewWebAppToExistingResourceGroup(rg, myWebApp);
            if (model.isAppServicePlanCreateNew) {
                AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                AzureModelController.addNewAppServicePlanToExistingResourceGroup(rg, asp);
            }
        }

        return myWebApp;
    }

    protected WebApp webApp;

    public WebApp getWebApp() {
        return this.webApp;
    }

    public void superDoOKAction() {
        super.doOKAction();
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new Task.Modal(null,"Create App Service Progress", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    progressIndicator.setText("Creating Web App Service...");
                    webApp = createAppService(new UpdateProgressIndicator(progressIndicator));
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
                    ErrorWindow.show(ex.getMessage(), "Create App Service Error", AppServiceCreateDialog.this.contentPane);
                }
            }
        });
    }
}
