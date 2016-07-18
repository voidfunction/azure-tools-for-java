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

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.microsoft.azure.hdinsight.common.HDInsightHelper;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azureexplorer.Activator;
import com.microsoft.azureexplorer.editors.JobViewInput;

public class HDInsightHelperImpl implements HDInsightHelper {
	
	private static final String HDINSIHGT_BUNDLE_ID = "com.microsoft.hdinsights";
	private static boolean isHDInsightLoaded = false;
	
    public void openJobViewEditor(Object projectObject, String uuid) {
    	try {
        	loadHDInsightPlugin();
		} catch (BundleException bundleException) {
			Activator.getDefault().log("Error loading plugin com.microsoft.hdinsights", bundleException);
		}

        IClusterDetail clusterDetail = JobViewManager.getCluster(uuid);
        IWorkbench workbench = PlatformUI.getWorkbench();
        IEditorDescriptor editorDescriptor = workbench.getEditorRegistry().findEditor("com.microsoft.azure.hdinsight.jobview");
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IEditorPart newEditor = page.openEditor(new JobViewInput(clusterDetail, uuid), editorDescriptor.getId());
        } catch (PartInitException e2) {
            Activator.getDefault().log("Error opening " + clusterDetail.getName(), e2);
        }
    }
        
    private void loadHDInsightPlugin() throws BundleException{
		Bundle bundle = Platform.getBundle(HDINSIHGT_BUNDLE_ID);
		if(bundle == null || bundle.getState() == Bundle.ACTIVE) {
			return;
		} else {
			bundle.start();
		}
    }
}
