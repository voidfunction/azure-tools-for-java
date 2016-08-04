package com.microsoft.azureexplorer.editors;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azureexplorer.hdinsight.*;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class JobViewEditor extends EditorPart {

	private IClusterDetail clusterDetail;
	private String uuid;

	@Override
	public void doSave(IProgressMonitor iProgressMonitor) {
	}

	@Override
	public void doSaveAs() {
	}

	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		clusterDetail = ((JobViewInput) input).getClusterDetail();
		uuid = ((JobViewInput) input).getUuid();
		setPartName(clusterDetail.getName() + " Spark JobView");
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite composite) {
		composite.setLayout(new FillLayout());
		final String indexPath = PluginUtil.pluginFolder + "/com.microsoft.azure.hdinsight" + "/hdinsight/job/html/index.html";
		final String jobViewFxJarPath = PluginUtil.pluginFolder + "/com.microsoft.azure.hdinsight" + "/hdinsight-job-view.jar"; 
		final String queryString = "?projectid=" + uuid + "&engintype=javafx";
		final String webUrl = "file:///" + indexPath.replace("\\", "/") + queryString;
		FxClassLoader.loadJavaFxForJobView(composite, webUrl, jobViewFxJarPath);
	}

	@Override
	public void setFocus() {
	}

}