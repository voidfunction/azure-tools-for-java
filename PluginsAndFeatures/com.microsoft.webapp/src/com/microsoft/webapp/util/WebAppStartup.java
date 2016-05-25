package com.microsoft.webapp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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
import org.eclipse.core.runtime.FileLocator;
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
		copyPluginComponents();
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
			if (key.endsWith(".webapps") && !key.equalsIgnoreCase(com.microsoftopentechnologies.wacommon.utils.Messages.prefFileName + ".webapps")) {
				String projName = key.substring(0, key.lastIndexOf("."));
				if (!webProjects.contains(projName)) {
					PreferenceUtil.unsetPreference(key);
				}
			}
		}
	}

	private void copyPluginComponents() {
		try {
			String pluginInstLoc = String.format("%s%s%s",
					PluginUtil.pluginFolder,
					File.separator, Messages.webAppPluginID);
			if (!new File(pluginInstLoc).exists()) {
				new File(pluginInstLoc).mkdir();
			}
			// JAR file
			String debugJarFile = String.format("%s%s%s", pluginInstLoc,
					File.separator, Messages.debugJarName);
			if (new File(debugJarFile).exists()) {
				new File(debugJarFile).delete();
			}
			copyResourceFile(Messages.debugJarEntry, debugJarFile);

			// Bat file
			String debugBatFile = String.format("%s%s%s", pluginInstLoc,
					File.separator, Messages.debugBatName);
			if (new File(debugBatFile).exists()) {
				new File(debugBatFile).delete();
			}
			copyResourceFile(Messages.debugBatEntry, debugBatFile);

			// web.config
			String configFile = String.format("%s%s%s", pluginInstLoc,
					File.separator, Messages.configName);
			if (new File(configFile).exists()) {
				new File(configFile).delete();
			}
			copyResourceFile(Messages.configEntry, configFile);
		} catch (Exception e) {
			Activator.getDefault().log(e.getMessage(), e);
		}
	}

	/**
	 * copy specified file to eclipse plugins folder
	 * @param name : Name of file
	 * @param entry : Location of file
	 */
	public static void copyResourceFile(String resourceFile , String destFile) {
		URL url = Activator.getDefault().getBundle()
				.getEntry(resourceFile);
		URL fileURL;
		try {
			fileURL = FileLocator.toFileURL(url);
			URL resolve = FileLocator.resolve(fileURL);
			File file = new File(resolve.getFile());
			FileInputStream fis = new FileInputStream(file);
			File outputFile = new File(destFile);
			FileOutputStream fos = new FileOutputStream(outputFile);
			com.microsoftopentechnologies.azurecommons.wacommonutil.FileUtil.writeFile(fis , fos);
		} catch (IOException e) {
			Activator.getDefault().log(e.getMessage(), e);
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
