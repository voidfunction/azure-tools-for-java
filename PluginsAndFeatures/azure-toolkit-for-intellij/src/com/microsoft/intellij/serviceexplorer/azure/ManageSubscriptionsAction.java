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
package com.microsoft.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.intellij.forms.ManageSubscriptionPanel;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureServiceModule;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@Name("Manage Subscriptions")
public class ManageSubscriptionsAction extends NodeActionListener {
    private AzureServiceModule azureServiceModule;

    public ManageSubscriptionsAction(AzureServiceModule azureServiceModule) {
        this.azureServiceModule = azureServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ManageSubscriptionPanel manageSubscriptionPanel = new ManageSubscriptionPanel((Project) azureServiceModule.getProject(), true);
        final DefaultDialogWrapper subscriptionsDialog = new DefaultDialogWrapper((Project) azureServiceModule.getProject(),
                manageSubscriptionPanel) {
            @Nullable
            @Override
            protected JComponent createSouthPanel() {
                return null;
            }
            @Override
            protected JComponent createTitlePane() {
                return null;
            }
        };
        manageSubscriptionPanel.setDialog(subscriptionsDialog);
        subscriptionsDialog.show();
    }
}