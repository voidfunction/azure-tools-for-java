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
package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.intellij.forms.WebSiteDeployForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tasks.WebSiteDeployTask;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureWebDeployAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Module module = LangDataKeys.MODULE.getData(e.getDataContext());
        WebSiteDeployForm form = new WebSiteDeployForm(module);
        form.show();
        if (form.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            try {
                String url = form.deploy();
                WebSiteDeployTask task = new WebSiteDeployTask(e.getProject(), form.getSelectedWebSite(), url);
                task.queue();
            } catch (AzureCmdException ex) {
                PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final Module module = e.getData(LangDataKeys.MODULE);
        e.getPresentation().setEnabled(module != null && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}