package com.microsoft.azuretools.eclipse.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.eclipse.ui.SubscriptionsDialog;
import com.microsoft.azuretools.sdkmanage.AzureManager;

public class SelectSubsriptionsCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        onSelectSubscriptions(window.getShell());
        return null;
    }
    
    public static void onSelectSubscriptions(Shell parentShell) {
        try {
            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) {
                return;
            }
            
            SubscriptionManager sm = manager.getSubscriptionManager();
            IRunnableWithProgress op = new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Getting Subscription List...", IProgressMonitor.UNKNOWN);
                    try {
                        sm.getSubscriptionDetails();
                    } catch (Exception ex) {
                        System.out.println("onSelectSubscriptions ex: " + ex.getMessage());
                    }
                }
            };
            new ProgressMonitorDialog(parentShell.getShell()).run(true, false, op);
            
            SubscriptionsDialog d = SubscriptionsDialog.go(parentShell, sm.getSubscriptionDetails());
            if (d != null) {
                List<SubscriptionDetail> sdlUpdated = d.getSubscriptionDetails();
                sm.setSubscriptionDetails(sdlUpdated);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
