package com.microsoft.azuretools.webapp.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.webapp.ui.WebAppDeployDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ISelectionService selectionService = window.getSelectionService();
		ISelection selection = selectionService.getSelection();
		
		if(selection instanceof IStructuredSelection) {
			 Object element = ((IStructuredSelection)selection).getFirstElement();
			
			System.out.print("Geting project: "); 
			if (element instanceof IResource) {
				System.out.println("IResource");
				IProject project = ((IResource)element).getProject();
				
				// TODO check the project is Dynamic Web Application
				
				WebAppDeployDialog d = WebAppDeployDialog.go(window.getShell(), project);
			}

		}
		
//		MessageDialog.openInformation(
//				window.getShell(),
//				"WebAppPlugin",
//				"Hello, Eclipse world");
		return null;
	}
}
