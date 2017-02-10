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
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.PluginUtil;
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
    private Subscription subscription;
    private Map<String, Subscription> subMap = new HashMap<String, Subscription>();
    private AzureManager azureManager;

    public NewResourceGroupDialog(Subscription subscription) {
        super(true);
        this.subscription = subscription;
        try {
            azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                AzurePlugin.log("Not signed in");
            }
        } catch (Exception ex) {
            AzurePlugin.log("Not signed in", ex);
        }
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
            List<Subscription> subscriptions = azureManager.getSubscriptions();
            // check at least single subscription is associated with the account
            if (subscriptions.size() > 0) {
                for (Subscription sub : subscriptions) {
                    subMap.put(sub.subscriptionId(), sub);
                }
                comboSub.setModel(new DefaultComboBoxModel(subMap.values().toArray()));

					/*
                     * If subscription name is there,
					 * dialog invoked from application insights/web sites dialog
					 * hence disable subscription combo.
					 */
                if (subscription != null) {
                    comboSub.setEnabled(false);
                    comboSub.setSelectedItem(subscription);
                } else {
                    comboSub.setSelectedIndex(0);
                }
                // Get list of locations available for subscription.
                populateLocations();
            }

        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    private void populateLocations() {
        try {
            List<Location> locationList = ((Subscription) comboSub.getSelectedItem()).listLocations();
            List<String> locationNameList = new ArrayList<String>();
            for (Location location : locationList) {
                locationNameList.add(location.displayName());
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
