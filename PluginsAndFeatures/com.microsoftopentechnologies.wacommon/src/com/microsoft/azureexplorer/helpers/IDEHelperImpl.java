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
package com.microsoft.azureexplorer.helpers;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask.CancellableTaskHandle;

public class IDEHelperImpl implements IDEHelper {

    @Override
    public void runInBackground(Object project, String name, boolean canBeCancelled, boolean isIndeterminate, final String indicatorText, final Runnable runnable) {
        Job job = new Job(name) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(indicatorText, IProgressMonitor.UNKNOWN);
                try {
                    runnable.run();
                } catch (Exception ex) {
                    monitor.done();
                    return Status.CANCEL_STATUS;
                }
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    @Override
    public void closeFile(Object projectObject, Object openedFile) {
//        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//        page.closeEditor((IEditorPart) openedFile, false);
    }

    @Override
    public void invokeLater(Runnable runnable) {
        Display.getDefault().asyncExec(runnable);
    }

    @Override
    public void invokeAndWait(Runnable runnable) {
        Display.getDefault().syncExec(runnable);
    }

    @Override
    public void executeOnPooledThread(final Runnable runnable) {
        Job job = new Job("Loading...") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("", IProgressMonitor.UNKNOWN);
                try {
                    runnable.run();
                } catch (Exception ex) {
                    monitor.done();
                    return Status.CANCEL_STATUS;
                }
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    @Override
    public String getProperty(String name, Object projectObject) {
        return getProperty(name);
    }

    public String getProperty(Object projectObject, String name, String defaultValue) {
        return null;
    }

    @Override
    public void setProperty(String name, String value, Object projectObject) {
        setProperty(name, value);
    }

    @Override
    public void unsetProperty(String name, Object projectObject) {
        unsetProperty(name);
    }

    public boolean isPropertySet(Object projectObject, String name) {
        return false;
    }

    @Override
    public String getProperty(String name) {
        return PreferenceUtil.loadPreference(name);
    }

    @Override
    public String getPropertyWithDefault(String name, String defaultValue) {
        return PreferenceUtil.loadPreference(name, defaultValue);
    }

    @Override
    public void setProperty(String name, String value) {
         PreferenceUtil.savePreference(name, value);
    }

    @Override
    public void unsetProperty(String name) {
        PreferenceUtil.unsetPreference(name);
    }

    @Override
    public boolean isPropertySet(String name) {
        return false;
    }

    @Override
    public String[] getProperties(String name) {
        return PreferenceUtil.loadPreferences(name);
    }

    @Override
    public String[] getProperties(String name, Object project) {
        return getProperties(name);
    }

    @Override
    public void setProperties(String name, String[] value) {
        PreferenceUtil.savePreferences(name, value);
    }

	@Override
	public CancellableTaskHandle runInBackground(
			ProjectDescriptor projectDescriptor, String name,
			String indicatorText, CancellableTask cancellableTask)
			throws AzureCmdException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArtifactDescriptor> getArtifacts(
			ProjectDescriptor projectDescriptor) throws AzureCmdException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListenableFuture<String> buildArtifact(
			ProjectDescriptor projectDescriptor,
			ArtifactDescriptor artifactDescriptor) {
		// TODO Auto-generated method stub
		return null;
	}

    public Object getCurrentProject() {
        return AzureManagerImpl.DEFAULT_PROJECT;
    }
}
