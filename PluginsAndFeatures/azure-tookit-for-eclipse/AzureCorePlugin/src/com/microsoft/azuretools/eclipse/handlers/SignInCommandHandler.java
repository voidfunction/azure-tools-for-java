package com.microsoft.azuretools.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.eclipse.ui.SignInDialog;


/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SignInCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		try {
			AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
			SignInDialog d = SignInDialog.go(window.getShell(), authMethodManager.getAuthMethodDetails());
			if (null != d) {
                AuthMethodDetails authMethodDetailsUpdated = d.getAuthMethodDetails();
                authMethodManager.setAuthMethodDetails(authMethodDetailsUpdated);
                SelectSubsriptionsCommandHandler.onSelectSubscriptions(window.getShell());;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
