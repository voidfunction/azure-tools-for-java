package com.microsoft.webapp.util;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IStartup;


public class WebAppStartup implements IStartup {

	@Override
	public void earlyStartup() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// register resource change listener
		WebAppResourceChangeListener listener = new WebAppResourceChangeListener();
		workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
	}

	private class WebAppResourceChangeListener implements IResourceChangeListener {

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					return true;
				}
			};
		}
	}
}
