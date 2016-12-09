package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.ui.SubscriptionsDialog;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import javax.swing.*;
import java.util.List;

/**
 * Created by vlashch on 10/11/16.
 */
public class SelectSubscriptionsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        onShowSubscriptions();
    }

    public static void onShowSubscriptions() {
        try {
            Project project = ProjectManager.getInstance().getDefaultProject();
            JFrame frame = WindowManager.getInstance().getFrame(project);

            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) {
                return;
            }

            final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();

            ProgressManager.getInstance().run(new Task.Modal(project, "Loading Subscriptions...", true) {
                @Override
                public void run(ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        subscriptionManager.getSubscriptionDetails();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();
            for (SubscriptionDetail sd : sdl) {
                System.out.println(sd.getSubscriptionName());
            }
            SubscriptionsDialog d = SubscriptionsDialog.go(subscriptionManager.getSubscriptionDetails(), frame);
            List<SubscriptionDetail> subscriptionDetailsUpdated;
            if (d.getResult() == JOptionPane.OK_OPTION) {
                subscriptionDetailsUpdated = d.getSubscriptionDetails();
                subscriptionManager.setSubscriptionDetails(subscriptionDetailsUpdated);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public  void update(AnActionEvent e) {
        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            e.getPresentation().setEnabled(isSignIn);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
