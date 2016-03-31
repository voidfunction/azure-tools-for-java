package com.microsoft.webapp.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;

import com.microsoft.azureexplorer.helpers.PreferenceUtil;
import com.microsoft.webapp.activator.Activator;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class WebAppStartup implements IStartup {

	@Override
	public void earlyStartup() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// register resource change listener
		WebAppResourceChangeListener listener = new WebAppResourceChangeListener();
		workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		/*
		 * Scan and gather open dynamic web projects in workspace
		 * Remove unnecessary webapps preference keys associated with non existing projects.
		 */
		IProject[] projects = workspace.getRoot().getProjects();
		List<String> webProjects = new ArrayList<String>();
		for (IProject iProject : projects) {
			try {
				if (WAPropertyTester.isWebProj(iProject)) {
					webProjects.add(iProject.getName());
				}
			} catch (CoreException e) {
				Activator.getDefault().log(e.getMessage(), e);
			}
		}
		String[] keys = PreferenceUtil.getPreferenceKeys();
		for (String key : keys) {
			if (key.endsWith(".webapps")) {
				String projName = key.substring(0, key.lastIndexOf("."));
				if (!webProjects.contains(projName)) {
					PreferenceUtil.unsetPreference(key);
				}
			}
		}
	}

	private class WebAppResourceChangeListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta resourcedelta = event.getDelta();
			IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					IProject project = resource.getProject();
					boolean isNature = false;
					//Check if project is of required nature
					if (project != null && project.isOpen()) {
						isNature = WAPropertyTester.isWebProj(project);
					}
					if (isNature) {
						handleResourceChange(delta);
					}
					return true;
				}
			};
			try {
				resourcedelta.accept(visitor);
				WorkspaceJob job = new WorkspaceJob(Messages.resCLJobName) {

					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor)
							throws CoreException {
						PluginUtil.refreshWorkspace();
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			} catch (CoreException e) {
				Activator.getDefault().log(e.getMessage(), e);
			}
		}

		private void handleResourceChange(IResourceDelta delta) {
			IResource resource = delta.getResource();
			IProject project = resource.getProject();
			if (resource.getType() == IResource.PROJECT
					&& (delta.getFlags()
							& IResourceDelta.MOVED_FROM) != 0) {
				// If project is renamed, rename preference key as well.
				String oldProjName = delta.getMovedFromPath().toString();
				oldProjName = oldProjName.substring(1, oldProjName.length());
				String oldKey = String.format(com.microsoft.webapp.config.Messages.webappKey, oldProjName);
				String oldValue = PreferenceUtil.loadPreference(oldKey);
				PreferenceUtil.unsetPreference(oldKey);
				String newKey = String.format(com.microsoft.webapp.config.Messages.webappKey, project.getName());
				PreferenceUtil.savePreference(newKey, oldValue);
			}
		}
	}
}
