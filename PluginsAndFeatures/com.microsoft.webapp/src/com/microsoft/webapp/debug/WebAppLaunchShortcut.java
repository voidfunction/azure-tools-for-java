/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.webapp.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.config.Messages;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class WebAppLaunchShortcut implements ILaunchShortcut {

	@Override
	public void launch(ISelection selection, String arg1) {
		Object element = null;
		IResource resource;
		IProject selProject = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSel =
					(IStructuredSelection) selection;
			element = structuredSel.getFirstElement();
		}
		if (element instanceof IProject) {
			resource = (IResource) element;
			selProject = resource.getProject();
		} else if (element instanceof IJavaProject) {
			IJavaProject proj = ((IJavaElement) element).getJavaProject();
			selProject = proj.getProject();
		} else if (element instanceof IResource) {
			resource = (IResource) element;
			selProject = resource.getProject();
		}
		launchAsPerProject(selProject);
	}

	@Override
	public void launch(IEditorPart editorPart, String arg1) {
		IProject selProject = null;
		if (editorPart != null) {
			IFile file = (IFile) editorPart.getEditorInput().getAdapter(IFile.class);
			selProject = file.getProject();
		}
		launchAsPerProject(selProject);
	}

	private void launchAsPerProject(final IProject selProject) {
		if (selProject == null) {
			Display.getDefault().syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					PluginUtil.displayErrorDialog(PluginUtil.getParentShell(), Messages.errTtl, Messages.selProj);
				}
			});
		} else {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			final ILaunchConfigurationType type = manager.getLaunchConfigurationType(Messages.debugType);
			ILaunchConfiguration[] configurations;
			try {
				ILaunchConfiguration configToUse = null;
				configurations = manager.getLaunchConfigurations(type);
				for (ILaunchConfiguration configuration : configurations) {
					if (configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equalsIgnoreCase(selProject.getName())) {
						configToUse = configuration;
						break;
					}
				}
				if (configToUse == null) {
					Display.getDefault().syncExec(new Runnable()
					{
						@Override
						public void run()
						{
							WebAppUtils.openDebugLaunchDialog(type);
						}
					});
				} else {
					DebugUITools.launch(configToUse, ILaunchManager.DEBUG_MODE);
				}
			} catch (CoreException e) {
				Activator.getDefault().log(e.getMessage(), e);
			}
		}
	}
}
