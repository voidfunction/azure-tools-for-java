package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.SignInWindow;

import javax.swing.*;

/**
 * Created by vlashch on 11/14/16.
 */
public class AzureSignInAction extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(AzureSignInAction.class);
    @Override
    public void actionPerformed(AnActionEvent e) {
        onAzureSignIn(e);
    }

    @Override
    public  void update(AnActionEvent e) {
        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            if (isSignIn) {
                e.getPresentation().setText("Azure Sign Out...");
            } else {
                e.getPresentation().setText("Azure Sign In...");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    public static void onAzureSignIn(AnActionEvent e) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        JFrame frame = WindowManager.getInstance().getFrame(project);
        try {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            boolean isSignIn = authMethodManager.isSignedIn();
            if (isSignIn) {
                String artifact = (authMethodManager.getAuthMethod() == AuthMethod.AD)
                        ? "Signed in as " + authMethodManager.getAuthMethodDetails().getAccountEmail()
                        : "Signed in using file \"" + authMethodManager.getAuthMethodDetails().getCredFilePath() + "\"";
                int res = JOptionPane.showConfirmDialog(frame,
                        artifact + "\n"
                                + "Dou you really want to sign out?",
                        "Azure Sign Out",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        new ImageIcon("icons/azure.png"));
                if (res == JOptionPane.OK_OPTION) {
                    AdAuthManager adAuthManager = AdAuthManager.getInstance();
                    if (adAuthManager.isSignedIn())
                        adAuthManager.signOut();
                    authMethodManager.cleanAll();
                }
            } else {
                SignInWindow w = SignInWindow.go(authMethodManager.getAuthMethodDetails(), frame);
                if (w.getResult() == JOptionPane.OK_OPTION) {
                    AuthMethodDetails authMethodDetailsUpdated = w.getAuthMethodDetails();
                    authMethodManager.setAuthMethodDetails(authMethodDetailsUpdated);
                    SelectSubscriptionsAction.onShowSubscriptions(e);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("onAzureSignIn", ex);
            ErrorWindow.show(ex.getMessage(), "AzureSignIn Action Error", frame);

        }
    }
}
