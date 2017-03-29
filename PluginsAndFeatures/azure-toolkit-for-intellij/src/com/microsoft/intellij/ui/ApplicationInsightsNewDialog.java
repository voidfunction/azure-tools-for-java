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
package com.microsoft.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsNewDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JComboBox comboSub;
    private JComboBox comboGrp;
    private JComboBox comboReg;
    private JRadioButton createNewBtn;
    private JRadioButton useExistingBtn;
    private JTextField textGrp;
    Map<String, SubscriptionDetail> subMap = new HashMap<String, SubscriptionDetail>();
    private SubscriptionDetail currentSub;
    static ApplicationInsightsResource resourceToAdd;
    private AzureManager azureManager;

    public ApplicationInsightsNewDialog() {
        super(true);
        setTitle(message("aiErrTtl"));
        try {
            azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                AzurePlugin.log("Not signed in");
            }
        } catch (Exception ex) {
            AzurePlugin.log("Not signed in", ex);
        }
        init();
    }

    protected void init() {
        super.init();
        comboSub.addItemListener(subscriptionListener());
        final ButtonGroup resourceGroup = new ButtonGroup();
        resourceGroup.add(createNewBtn);
        resourceGroup.add(useExistingBtn);
        final ItemListener updateListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean isNewGroup = createNewBtn.isSelected();
                textGrp.setEnabled(isNewGroup);
                comboGrp.setEnabled(!isNewGroup);
            }
        };
        createNewBtn.addItemListener(updateListener);
        useExistingBtn.addItemListener(updateListener);
        comboReg.setRenderer(new ListCellRendererWrapper<Object>() {

            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o != null && (o instanceof Location)) {
                    setText("  " + ((Location)o).displayName());
                }
            }
        });
        createNewBtn.setSelected(true);
        populateValues();
    }

    private ItemListener subscriptionListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SubscriptionDetail newSub = (SubscriptionDetail) comboSub.getSelectedItem();
                String prevResGrpVal = (String) comboGrp.getSelectedItem();
                if (currentSub.equals(newSub)) {
                    populateResourceGroupValues(currentSub.getSubscriptionId(), prevResGrpVal);
                } else {
                    populateResourceGroupValues(currentSub.getSubscriptionId(), "");
                }
                currentSub = newSub;
            }
        };
    }

    private void populateValues() {
        try {
            List<SubscriptionDetail> subList = azureManager.getSubscriptionManager().getSubscriptionDetails();
            // check at least single subscription is associated with the account
            if (subList.size() > 0) {
                for (SubscriptionDetail sub : subList) {
                    subMap.put(sub.getSubscriptionId(), sub);
                }
                comboSub.setModel(new DefaultComboBoxModel(subList.toArray()));
                comboSub.setSelectedIndex(0);
                currentSub = subList.get(0);

                populateResourceGroupValues(currentSub.getSubscriptionId(), "");

                Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
                if (subscription2Location == null || subscription2Location.get(currentSub) == null) {
                    final DefaultComboBoxModel<String> loadingModel = new DefaultComboBoxModel<>(new String[]{"<Loading...>"});
                    comboReg.setModel(loadingModel);
                    DefaultLoader.getIdeHelper().runInBackground(null, "Loading Available Locations...", false, true, "Loading Available Locations...", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AzureModelController.updateSubscriptionMaps(null);
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        fillRegions();
                                    }
                                });
                            } catch (Exception ex) {
                                PluginUtil.displayErrorDialogInAWTAndLog("Error", "Error loading locations", ex);
                            }
                        }
                    });
                } else {
                    fillRegions();
                }
                comboReg.setSelectedIndex(0);
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    private void fillRegions() {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(currentSub)
                .stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
        comboReg.setModel(new DefaultComboBoxModel(locations.toArray()));
        if (locations.size() > 0) {
//            selectRegion();
        }
    }

    private void populateResourceGroupValues(String subscriptionId, String valtoSet) {
        try {
            com.microsoft.azuretools.sdkmanage.AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            Azure azure = azureManager.getAzure(subscriptionId);
            List<com.microsoft.azure.management.resources.ResourceGroup> groups = azure.resourceGroups().list();
            List<String> groupStringList = groups.stream().map(com.microsoft.azure.management.resources.ResourceGroup::name).collect(Collectors.toList());
            if (groupStringList.size() > 0) {
                String[] groupArray = groupStringList.toArray(new String[groupStringList.size()]);
                comboGrp.removeAllItems();
                comboGrp.setModel(new DefaultComboBoxModel(groupArray));
                if (valtoSet.isEmpty() || !groupStringList.contains(valtoSet)) {
                    comboGrp.setSelectedItem(groupArray[0]);
                } else {
                    comboGrp.setSelectedItem(valtoSet);
                }
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("newKeyTtl"), message("newKeyMsg"));
    }

    @Override
    protected void doOKAction() {
        boolean isValid = false;
        if (txtName.getText().trim().isEmpty()
                || ((String) comboSub.getSelectedItem()).isEmpty()
                || ((String) comboGrp.getSelectedItem()).isEmpty()
                || ((String) comboReg.getSelectedItem()).isEmpty()) {
            if (((String) comboSub.getSelectedItem()).isEmpty() || comboSub.getItemCount() <= 0) {
                PluginUtil.displayErrorDialog(message("aiErrTtl"), message("noSubErrMsg"));
            } else if (((String) comboGrp.getSelectedItem()).isEmpty() || comboGrp.getItemCount() <= 0) {
                PluginUtil.displayErrorDialog(message("aiErrTtl"), message("noResGrpErrMsg"));
            } else {
                PluginUtil.displayErrorDialog(message("aiErrTtl"), message("nameEmptyMsg"));
            }
        } else {
            try {
                Resource resource = AzureSDKManager.createApplicationInsightsResource(currentSub, (String) comboGrp.getSelectedItem(),
                        txtName.getText(), (String) comboReg.getSelectedItem());
                resourceToAdd = new ApplicationInsightsResource(resource.getName(), resource.getInstrumentationKey(),
                        (String) comboSub.getSelectedItem(), currentSub.getSubscriptionId(), resource.getLocation(),
                        resource.getResourceGroup(), true);
                isValid = true;
            } catch (Exception ex) {
                PluginUtil.displayErrorDialogAndLog(message("aiErrTtl"), message("resCreateErrMsg"), ex);
            }
        }
        if (isValid) {
            super.doOKAction();
        }
    }

    public static ApplicationInsightsResource getResource() {
        return resourceToAdd;
    }
}
