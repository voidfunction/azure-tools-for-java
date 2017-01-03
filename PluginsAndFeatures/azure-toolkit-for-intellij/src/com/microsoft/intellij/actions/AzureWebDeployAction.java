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
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.forms.WebSiteDeployForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tasks.WebSiteDeployTask;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;

import java.util.List;
import java.util.Set;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureWebDeployAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Module module = LangDataKeys.MODULE.getData(e.getDataContext());
        Module module1 = e.getData(LangDataKeys.MODULE);


        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }

            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            System.out.println("getting sid list...");
            Set<String> sidList = subscriptionManager.getAccountSidList();
            for (String sid : sidList) {
                System.out.println("sid : " + sid);
                Azure azure = azureManager.getAzure(sid);

//                System.out.println("Creating a vault...");
//                Vault vault =  azure.vaults().define("ShchKeyVault")
//                        .withRegion(Region.US_EAST)
//                        .withNewResourceGroup()
//                        .defineAccessPolicy()
//                        .forUser("vlashch@microsoft.com")
//                        .allowKeyAllPermissions()
//                        .attach()
//                        .create()
//                        ;
//
//                System.out.println(vault.name());
//                System.out.println("Deleting a vault...");
//                azure.vaults().deleteById(vault.id());

                List<ResourceGroup>  rgList = azure.resourceGroups().list();
                for (ResourceGroup rg : rgList) {
                    //System.out.println("rg : " + rg);
                    List<WebApp> waList = azure.webApps().listByGroup(rg.name());
                    for (WebApp wa : waList ) {
                        System.out.println("\twa : " + wa.name());
                    }
                }

            }


        } catch (Exception e1) {
            e1.printStackTrace();
        }



//        WebSiteDeployForm form = new WebSiteDeployForm(module);
//        form.show();
//        if (form.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
//            try {
//                String url = form.deploy();
//                WebSiteDeployTask task = new WebSiteDeployTask(e.getProject(), form.getSelectedWebSite(), url);
//                task.queue();
//            } catch (AzureCmdException ex) {
//                PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
//            }
//        }
    }

    @Override
    public void update(AnActionEvent e) {
        final Module module = e.getData(LangDataKeys.MODULE);
        e.getPresentation().setVisible(PlatformUtils.isIdeaUltimate());
        if (!PlatformUtils.isIdeaUltimate()) {
            return;
        }

        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            boolean isEnabled = isSignIn & module != null && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE));
            e.getPresentation().setEnabled(isEnabled);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}