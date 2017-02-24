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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.management.network.Network;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateVirtualNetworkForm extends DialogWrapper {
    private JPanel contentPane;
    private JTextField nameField;
    private JTextField addressSpaceField;
    private JTextField subnetNameField;
    private JTextField subnetAddressRangeField;
    private JLabel userInfoLabel;
    private JTextField regionField;

    private Runnable onCreate;
    private Network network;
    private String subscriptionId;
    private String region;
    private Project project;

    public CreateVirtualNetworkForm(Project project, String subscriptionId, String region) {
        super(project, true);

        this.project = project;
        this.subscriptionId = subscriptionId;
        this.region = region;

        setModal(true);
        setTitle("Create Virtual Network");

        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateFields();
            }
        };

        nameField.getDocument().addDocumentListener(docListener);
        addressSpaceField.getDocument().addDocumentListener(docListener);
        subnetNameField.getDocument().addDocumentListener(docListener);
        subnetAddressRangeField.getDocument().addDocumentListener(docListener);

        regionField.setText(region.toString());
// TODO
//            userInfoLabel.setText("Signed in as: " + (upn.contains("#") ? upn.split("#")[1] : upn));

        validateFields();
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(
                new Task.Modal(project, "Creating virtual network", true) {
                    @Override
                    public void run(@com.microsoft.tooling.msservices.helpers.NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        boolean success = createVirtualNetwork();
                        if (success) {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    close(DialogWrapper.OK_EXIT_CODE, true);
                                }
                            }, ModalityState.any());

                        }
                    }
                }
        );
    }

    private boolean createVirtualNetwork() {
//        try {
//            if (isNewGroup) {
//                return azure.networks().define(name)
//                        .withRegion(region)
//                        .withNewResourceGroup(groupName)
//                        .withAddressSpace(addressSpace)
//                        .create();
//            } else {
//                return azure.networks().define(name)
//                        .withRegion(region)
//                        .withExistingResourceGroup(groupName)
//                        .withAddressSpace(addressSpace)
//                        .create();
//            }



//            network = AzureArmManagerImpl.getManager(project).createVirtualNetwork(subscriptionId, nameField.getText().trim(), region,
//                    addressSpaceField.getText().trim(), "", false); //todo:
            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (onCreate != null) {
                        onCreate.run();
                    }
                }
            });
            return true;

//        } catch (AzureCmdException e) {
//            network = null;
//            String msg = "An error occurred while attempting to create the specified virtual network in subscription " + subscriptionId + ".<br>"
//                    + String.format(message("webappExpMsg"), e.getCause());
//            DefaultLoader.getUIHelper().showException(msg, e, message("errTtl"), false, true);
//            AzurePlugin.log(msg, e);
//        }
//        return false;
    }

    private void validateFields() {
        boolean allFieldsCompleted = !(
                nameField.getText().isEmpty() || addressSpaceField.getText().isEmpty()
                        || subnetNameField.getText().isEmpty() || subnetAddressRangeField.getText().isEmpty());
        setOKActionEnabled(allFieldsCompleted);
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public Network getNetwork() {
        return network;
    }
}
