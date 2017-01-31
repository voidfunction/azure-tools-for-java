package com.microsoft.azuretools.ijidea.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.SubscriptionsDialog;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.actions.AzureWebDeployAction;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by vlashch on 10/11/16.
 */
public class SelectSubscriptionsAction extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(SelectSubscriptionsAction.class);
    private static ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public void actionPerformed(AnActionEvent e) {
        onShowSubscriptions(e);
    }

    public static void onShowSubscriptions(AnActionEvent e) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
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

            updateSubscriptionWithProgressDialog(subscriptionManager, project, rwLock.writeLock());
            rwLock.readLock().lock();
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
        } finally {
            rwLock.readLock().unlock();
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

    public static void updateSubscriptionWithProgressDialog(final SubscriptionManager subscriptionManager, Project project, Lock wLock) {
        wLock.lock();
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
                } finally {
                    wLock.unlock();
                }
            }
        });

    }

}
