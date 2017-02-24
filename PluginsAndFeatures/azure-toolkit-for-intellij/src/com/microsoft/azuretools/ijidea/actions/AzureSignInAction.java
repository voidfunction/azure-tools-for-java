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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.SignInWindow;

import javax.swing.*;

public class AzureSignInAction extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(AzureSignInAction.class);

    public AzureSignInAction() {
    }

    public AzureSignInAction(Icon icon) {
        super(icon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        onAzureSignIn(project);
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

    public static void onAzureSignIn(Project project) {
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
                SignInWindow w = SignInWindow.go(authMethodManager.getAuthMethodDetails(), project);
                if (w != null) {
                    AuthMethodDetails authMethodDetailsUpdated = w.getAuthMethodDetails();
                    authMethodManager.setAuthMethodDetails(authMethodDetailsUpdated);
                    SelectSubscriptionsAction.onShowSubscriptions(project);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("onAzureSignIn", ex);
            ErrorWindow.show(ex.getMessage(), "AzureSignIn Action Error", frame);
        }
    }
}
