/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.SubscriptionsDialog;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import javax.swing.*;
import java.util.List;

public class SelectSubscriptionsAction extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(SelectSubscriptionsAction.class);

    public SelectSubscriptionsAction() {
    }

    public SelectSubscriptionsAction(Icon icon) {
        super(icon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        onShowSubscriptions(project);
    }

    public static void onShowSubscriptions(Project project) {
        JFrame frame = WindowManager.getInstance().getFrame(project);

        try {
            //Project project = ProjectManager.getInstance().getDefaultProject();();

            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) {
                return;
            }

            final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();

//            ProgressManager.getInstance().run(new Task.Modal(project, "Loading Subscriptions...", true) {
//                @Override
//                public void run(ProgressIndicator progressIndicator) {
//                    try {
//                        progressIndicator.setIndeterminate(true);
//                        subscriptionManager.getSubscriptionDetails();
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            });
            updateSubscriptionWithProgressDialog(subscriptionManager, project);
            List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();
            for (SubscriptionDetail sd : sdl) {
                System.out.println(sd.getSubscriptionName());
            }

            //System.out.println("onShowSubscriptions: calling getSubscriptionDetails()");
            SubscriptionsDialog d = SubscriptionsDialog.go(subscriptionManager.getSubscriptionDetails(), frame);
            List<SubscriptionDetail> subscriptionDetailsUpdated;
            if (d.getResult() == JOptionPane.OK_OPTION) {
                subscriptionDetailsUpdated = d.getSubscriptionDetails();
                subscriptionManager.setSubscriptionDetails(subscriptionDetailsUpdated);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("onShowSubscriptions", ex);
            ErrorWindow.show(ex.getMessage(), "Select Subscriptions Action Error", frame);
        }
    }

    @Override
    public  void update(AnActionEvent e) {
        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            e.getPresentation().setEnabled(isSignIn);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    public static void updateSubscriptionWithProgressDialog(final SubscriptionManager subscriptionManager, Project project) {
        ProgressManager.getInstance().run(new Task.Modal(project, "Loading Subscriptions...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    //System.out.println("updateSubscriptionWithProgressDialog: calling getSubscriptionDetails()");
                    subscriptionManager.getSubscriptionDetails();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.error("updateSubscriptionWithProgressDialog", ex);
                }
            }
        });

    }

}
