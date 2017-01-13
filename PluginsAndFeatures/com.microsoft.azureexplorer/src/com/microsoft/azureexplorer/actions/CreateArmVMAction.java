/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azureexplorer.actions;

import org.eclipse.jface.wizard.WizardDialog;

import com.microsoft.azureexplorer.forms.createvm.VMWizard;
import com.microsoft.azureexplorer.forms.createvm.arm.CreateVMWizard;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

@Name("Create VM")
public class CreateArmVMAction extends NodeActionListener {
    public CreateArmVMAction(VMArmModule node) {
        super(node);
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        VMWizard createVMWizard = new CreateVMWizard((VMArmModule) e.getAction().getNode());
        WizardDialog dialog = new WizardDialog(PluginUtil.getParentShell(), createVMWizard);
        dialog.setTitle("Create new Virtual Machine");
        dialog.setMinimumPageSize(450, 500);
        dialog.create();
        dialog.open();
    }
}