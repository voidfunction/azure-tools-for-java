package com.microsoft.azuretools.webapp.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.webapp.ui.WebAppDeployDialog;

public class DeployToAzureHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent ee) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(ee);
        ISelectionService selectionService = window.getSelectionService();
        ISelection selection = selectionService.getSelection();
        
        if(selection instanceof IStructuredSelection) {
             Object element = ((IStructuredSelection)selection).getFirstElement();
            
            System.out.print("Geting project: "); 
            if (element instanceof IResource) {
                System.out.println("IResource");
                IProject project = ((IResource)element).getProject();
                // TODO check the project is Dynamic Web Application
                if (!SignInCommandHandler.doSignIn( window.getShell())) return null;
                WebAppDeployDialog.go(window.getShell(), project);
            }

        }

        return null;
    }

}
