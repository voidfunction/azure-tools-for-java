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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TitlePanel;
//import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.vm.Location;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.swing.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class NewResourceGroupDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JComboBox comboSub;
    private JComboBox comboReg;
    String subscription;
    Map<String, String> subMap = new HashMap<String, String>();
//    static ResourceGroupExtended group;
    AzureManager manager;

    public NewResourceGroupDialog(String subscription) {
        super(true);
        manager = AzureManagerImpl.getManager();
        this.subscription = subscription;
        setTitle(message("newResGrpTtl"));
        comboSub.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getItem() instanceof String) {
                    populateLocations();
                }
            }
        });
        init();
    }

    protected void init() {
        super.init();
        populateValues();
    }

    private void populateValues() {
        try {
            if (manager != null) {
                List<Subscription> subList = manager.getSubscriptionList();
                // check at least single subscription is associated with the account
                if (subList.size() > 0) {
                    for (Subscription sub : subList) {
                        subMap.put(sub.getId(), sub.getName());
                    }
                    Collection<String> values = subMap.values();
                    String[] subNameArray = values.toArray(new String[values.size()]);
                    comboSub.setModel(new DefaultComboBoxModel(subNameArray));

					/*
					 * If subscription name is there,
					 * dialog invoked from application insights/web sites dialog
					 * hence disable subscription combo.
					 */
                    if (subscription != null && !subscription.isEmpty()) {
                        comboSub.setEnabled(false);
                        comboSub.setSelectedItem(subscription);
                    } else {
                        comboSub.setSelectedItem(subNameArray[0]);
                    }
                    // Get list of locations available for subscription.
                    populateLocations();
                }
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    private void populateLocations() {
        try {
            List<Location> locationList = manager.getLocations(findKeyAsPerValue((String) comboSub.getSelectedItem()));
            List<String> locationNameList = new ArrayList<String>();
            for (Location location : locationList) {
                locationNameList.add(location.getName());
            }
            String[] regionArray = locationNameList.toArray(new String[locationNameList.size()]);
            comboReg.setModel(new DefaultComboBoxModel(regionArray));
            comboReg.setSelectedItem(regionArray[0]);
        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected JComponent createTitlePane() {
        return new TitlePanel(message("newResGrpTtl"), message("newResGrpMsg"));
    }

    private String findKeyAsPerValue(String subName) {
        String key = "";
        for (Map.Entry<String, String> entry : subMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(subName)) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }

    @Override
    protected void doOKAction() {
        boolean isValid = false;
        if (txtName.getText().trim().isEmpty()
                || ((String) comboSub.getSelectedItem()).isEmpty()
                || ((String) comboReg.getSelectedItem()).isEmpty()) {
            if (((String) comboSub.getSelectedItem()).isEmpty() || comboSub.getItemCount() <= 0) {
                PluginUtil.displayErrorDialog(message("err"), message("noSubErrMsg"));
            } else {
                PluginUtil.displayErrorDialog(message("err"), message("nameEmptyMsg"));
            }
        } else {
            throw new NotImplementedException();
/*
            try {
                String subId = findKeyAsPerValue((String) comboSub.getSelectedItem());
                group = manager.createResourceGroup(subId,
                        txtName.getText().trim(), (String) comboReg.getSelectedItem());
                isValid = true;
            } catch (Exception ex) {
                PluginUtil.displayErrorDialogAndLog(message("newResGrpTtl"), message("newResErrMsg"), ex);
            }
*/
        }
        if (isValid) {
            super.doOKAction();
        }
    }
/*
    public static ResourceGroupExtended getResourceGroup() {
        return group;
    }
    */
}
