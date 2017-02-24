package com.microsoft.azuretools.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;


/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SignOutCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        try {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            String artifact = (authMethodManager.getAuthMethod() == AuthMethod.AD)
                    ? "Signed in as " + authMethodManager.getAuthMethodDetails().getAccountEmail()
                    : "Signed in using file \"" + authMethodManager.getAuthMethodDetails().getCredFilePath() + "\"";
            MessageBox messageBox = new MessageBox(
                    window.getShell(), 
                    SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setMessage(artifact + "\n"
                    + "Dou you really want to sign out?");
            messageBox.setText("Azure Sign Out");
            
            
            int response = messageBox.open();
            if (response == SWT.YES) {
                AdAuthManager adAuthManager = AdAuthManager.getInstance();
                if (adAuthManager.isSignedIn()) {
                    adAuthManager.signOut();
                }
                authMethodManager.cleanAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
