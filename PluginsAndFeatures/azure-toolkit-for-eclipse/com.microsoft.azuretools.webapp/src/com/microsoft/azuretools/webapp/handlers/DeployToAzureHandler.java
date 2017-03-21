package com.microsoft.azuretools.webapp.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.webapp.ui.WebAppDeployDialog;

public class DeployToAzureHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent ee) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(ee);
        IProject project = PluginUtil.getSelectedProject();
        if (!SignInCommandHandler.doSignIn( window.getShell())) return null;
        WebAppDeployDialog.go(window.getShell(), project);
        return null;
    }

}
