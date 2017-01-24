/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAppDeployDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTable table;
    private JButton createButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JList listWebAppDetails;
    private JCheckBox deployToRootCheckBox;

    private final Module module;
    private final Artifact artifact;

    private static class WebAppDetails {
        public SubscriptionDetail subscriptionDetail;
        public ResourceGroup resourceGroup;
        public AppServicePlan appServicePlan;
        public WebApp webApp;
    }

    private Map<String, WebAppDetails> webAppWebAppDetailsMap = new HashMap<>();

    public static WebAppDeployDialog go(Module module, Artifact artifact) {
        WebAppDeployDialog d = new WebAppDeployDialog(module, artifact);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    @Override
    public void show() {
        fillTable();
        super.show();
    }

    protected WebAppDeployDialog(Module module, Artifact artifact) {
        super(module.getProject(), true, IdeModalityType.PROJECT);
        this.module = module;
        this.artifact = artifact;

        setModal(true);
        setTitle("Select Web App");
        setOKButtonText("Deploy");

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createWebApp();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteWebApp();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cleanTable();
                AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                fillTable();
            }
        });

        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) return;
                //System.out.println("row : " + table.getValueAt(table.getSelectedRow(), 0).toString());
                fillWebAppDetails();
            }
        });

        class DisabledItemSelectionModel extends DefaultListSelectionModel {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        }

        listWebAppDetails.setSelectionModel(new DisabledItemSelectionModel());
        listWebAppDetails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        init();
    }

    private void cleanTable() {
        DefaultTableModel dm = (DefaultTableModel) table.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "WebAppDeployDialog";
    }

    private void fillTable() {
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillTable();
        } else {
            doFillTable();
        }
    }

    private void updateAndFillTable() {
        ProgressManager.getInstance().run(new Task.Modal(module.getProject(), "Getting Web Apps...", true) {
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

                    AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(progressIndicator));

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            doFillTable();
                        }
                    }, ModalityState.any());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void doFillTable() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
        Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgaspMap = AzureModel.getInstance().getResourceGroupToAppServicePlanMap();

        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Name");
        tableModel.addColumn("JDK");
        tableModel.addColumn("Web Container");
        tableModel.addColumn("Resource Group");
        webAppWebAppDetailsMap.clear();
        for (SubscriptionDetail sd : srgMap.keySet()) {
            if (!sd.isSelected()) continue;
            for (ResourceGroup rg : srgMap.get(sd)) {
                for (WebApp wa : rgwaMap.get(rg)) {
                    if (wa.javaVersion() != JavaVersion.OFF) {
                        tableModel.addRow(new String[]{
                                wa.name(),
                                wa.javaVersion().toString(),
                                wa.javaContainer() + " " + wa.javaContainerVersion(),
                                wa.resourceGroupName()
                        });
                        //tableModel.fireTableDataChanged();

                        WebAppDetails webAppDetails = new WebAppDetails();
                        webAppDetails.webApp = wa;
                        webAppDetails.subscriptionDetail = sd;
                        webAppDetails.resourceGroup = rg;
                        List<AppServicePlan> aspl = rgaspMap.get(rg);
                        for (AppServicePlan asp : aspl) {
                            if (asp.id().equals(wa.appServicePlanId())) {
                                webAppDetails.appServicePlan = asp;
                                break;
                            }
                        }
                        webAppWebAppDetailsMap.put(wa.name(), webAppDetails);
                    }
                }
            }
        }
        table.setModel(tableModel);
        if (tableModel.getRowCount() > 0)
            tableModel.fireTableDataChanged();
    }

    private void createWebApp() {
        WebAppCreateDialog d = WebAppCreateDialog.go(this.module);
        if (d == null) {
            // something went wrong - report an error!
            return;
        }
        WebApp wa = d.getWebApp();
        doFillTable();
        selectTableRowWithWebAppName(wa.name());
        //fillWebAppDetails();
    }

    private void selectTableRowWithWebAppName(String webAppName) {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        for (int ri = 0; ri < tableModel.getRowCount(); ++ri) {
            String waName = (String) tableModel.getValueAt(ri, 0);
            if (waName.equals(webAppName)) {
                table.setRowSelectionInterval(ri, ri);
                break;
            }
        }
    }

    private void deleteWebApp() {

    }

    private void fillWebAppDetails() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            WebAppDetails wad = webAppWebAppDetailsMap.get(tableModel.getValueAt(selectedRow, 0));
            SubscriptionDetail sd = wad.subscriptionDetail;
            listModel.addElement("Subsctiption Name: " + sd.getSubscriptionName() + "; ID: " + sd.getSubscriptionId());
            ResourceGroup rg = wad.resourceGroup;
            listModel.addElement("Resource Group: " + rg.name() + "; Region: " + rg.region().label());
            AppServicePlan asp = wad.appServicePlan;
            listModel.addElement("App Service Plan: " + asp.name() + "; Pricing Tier: " + asp.pricingTier().toString() + "; Region: " + asp.region().label());
        }
        listWebAppDetails.setModel(listModel);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return new ValidationInfo("Please select a Web App to deploy to", table);
        }

        return super.doValidate();
    }

    private void deploy() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int selectedRow = table.getSelectedRow();
        WebAppDetails wad = webAppWebAppDetailsMap.get(tableModel.getValueAt(selectedRow, 0));
        WebApp webApp = wad.webApp;
        try {
            PublishingProfile pp = webApp.getPublishingProfile();
            WebAppUtils.deployArtifact(artifact.getName(), artifact.getOutputFilePath(), pp, deployToRootCheckBox.isSelected());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doOKAction() {
        deploy();
        super.doOKAction();
    }
}