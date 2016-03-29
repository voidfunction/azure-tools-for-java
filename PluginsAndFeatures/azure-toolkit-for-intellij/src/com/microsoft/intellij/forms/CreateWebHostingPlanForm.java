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
import com.microsoft.azure.management.websites.models.SkuOptions;
import com.microsoft.azure.management.websites.models.WebHostingPlan;
import com.microsoft.azure.management.websites.models.WorkerSizeOptions;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateWebHostingPlanForm extends DialogWrapper {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JComboBox geoRegionComboBox;
    private JComboBox pricingComboBox;
    private JComboBox workerSizeComboBox;
    private Project project;
    private String subscriptionId;
    private String resourceGroup;
    private String geoRegion;
    static String webHostingPlan;
    List<String> plansAcrossSub = new ArrayList<String>();

    public CreateWebHostingPlanForm(@org.jetbrains.annotations.Nullable Project project, String subscriptionId, String resourceGroup, List<String> plansAcrossSub) {
        super(project, true, IdeModalityType.PROJECT);

        this.project = project;
        this.subscriptionId = subscriptionId;
        this.resourceGroup = resourceGroup;
        this.plansAcrossSub = plansAcrossSub;
        setTitle("New App Service Plan");

        geoRegionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getItem() instanceof String) {
                    geoRegion = (String) itemEvent.getItem();
                }
            }
        });

        pricingComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getItem() instanceof String) {
                    fillWorkerSize((String) pricingComboBox.getSelectedItem());
                }
            }
        });

        init();
        fillGeoRegions();
        fillPricingComboBox();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected ValidationInfo doValidate() {
        String name = nameTextField.getText().trim();
        if (name.length() > 60 || !name.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$")) {
            StringBuilder builder = new StringBuilder();
            builder.append("The name can contain letters, numbers and hyphens but the first and last characters must be a letter or number. ");
            builder.append("The length can be between 2 and 60 characters. ");
            return new ValidationInfo(builder.toString(), nameTextField);
        } else if (plansAcrossSub.contains(name)) {
            return new ValidationInfo(message("appUniErrMsg"), nameTextField);
        }

        if (geoRegion == null) {
            return new ValidationInfo("Select a valid location.", geoRegionComboBox);
        }

        if (pricingComboBox.getSelectedItem() == null) {
            return new ValidationInfo("Select a valid pricing tier.", pricingComboBox);
        }

        if (workerSizeComboBox.getSelectedItem() == null) {
            return new ValidationInfo("Select a valid instance size.", workerSizeComboBox);
        }

        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        boolean isOK = true;
        AzureManager manager = AzureManagerImpl.getManager();
        mainPanel.getRootPane().getParent().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            if (subscriptionId == null || subscriptionId.isEmpty()) {
                PluginUtil.displayErrorDialog(message("errTtl"), message("appPlanMsg") + " " + message("subscriptionIdIsNull"));
            } else {
                WebHostingPlanCache plan = new WebHostingPlanCache(nameTextField.getText().trim(), resourceGroup,
                        subscriptionId, (String) geoRegionComboBox.getSelectedItem(),
                        SkuOptions.valueOf((String) pricingComboBox.getSelectedItem()),
                        WorkerSizeOptions.valueOf((String) workerSizeComboBox.getSelectedItem()));
                manager.createWebHostingPlan(subscriptionId, plan);
                webHostingPlan = plan.getName();
            }
        } catch (Exception e) {
            isOK = false;
            String msg = message("appPlanMsg");
            if (e.getMessage().contains("MissingSubscriptionRegistration: The subscription is not registered to use namespace")) {
                msg = msg + " " + message("tierErrMsg");
            }
            PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
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
        super.dispose();
    }

    private void fillGeoRegions() {
        try {
            List<Location> locationList = AzureManagerImpl.getManager().getLocations(subscriptionId);
            List<String> locationNameList = new ArrayList<String>();
            for (Location location : locationList) {
                locationNameList.add(location.getName());
            }
            DefaultComboBoxModel geoRegionComboModel = new DefaultComboBoxModel(locationNameList.toArray());
            geoRegionComboModel.setSelectedItem(null);
            geoRegionComboBox.setModel(geoRegionComboModel);

            if (!locationNameList.isEmpty()) {
                geoRegionComboBox.setSelectedIndex(0);
            }
        } catch (AzureCmdException e) {
            DefaultLoader.getUIHelper().showException("An error occurred while trying to load the geo region list",
                    e, "Azure Services Explorer - Error Loading Geo Regions", false, true);
        }
    }

    private void fillPricingComboBox() {
        List<String> skuOptions = new ArrayList<String>();
        for (SkuOptions sku : SkuOptions.values()) {
            skuOptions.add(sku.toString());
        }
        DefaultComboBoxModel model = new DefaultComboBoxModel(skuOptions.toArray());
        model.setSelectedItem(null);
        pricingComboBox.setModel(model);

        if (!skuOptions.isEmpty()) {
            pricingComboBox.setSelectedIndex(0);
            fillWorkerSize((String) pricingComboBox.getSelectedItem());
        }
    }

    private void fillWorkerSize(String price) {
        List<String> sizeList = new ArrayList<String>();
        if (price.equalsIgnoreCase(SkuOptions.Free.name()) || price.equalsIgnoreCase(SkuOptions.Shared.name())) {
            sizeList.add(WorkerSizeOptions.Small.name());
        } else {
            for (WorkerSizeOptions size : WorkerSizeOptions.values()) {
                sizeList.add(size.toString());
            }
        }
        DefaultComboBoxModel model = new DefaultComboBoxModel(sizeList.toArray());
        model.setSelectedItem(null);
        workerSizeComboBox.setModel(model);

        if (!sizeList.isEmpty()) {
            workerSizeComboBox.setSelectedIndex(0);
        }
    }

    public static String getWebHostingPlan() {
        return webHostingPlan;
    }
}