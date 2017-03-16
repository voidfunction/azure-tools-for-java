package com.microsoft.azuretools.core.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.authmanage.interact.INotification;

public class Notification implements INotification {

    @Override
    public void deliver(String subject, String message) {
        Shell shell = getDisplay().getActiveShell();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(
                        shell,
                        subject,
                        message);
            }
        });
    }
    
    private Display getDisplay() {
        Display display = Display.getCurrent();
        //may be null if outside the UI thread
        if (display == null)
           display = Display.getDefault();
        return display;   
    }
}
