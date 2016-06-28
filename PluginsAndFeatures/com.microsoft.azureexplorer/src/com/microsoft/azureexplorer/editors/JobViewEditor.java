package com.microsoft.azureexplorer.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azure.hdinsight.jobs.JobUtils;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azureexplorer.helpers.HDInsightJobViewUtils;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

//import javafx.application.Platform;
//import javafx.beans.value.ChangeListener;
//import javafx.beans.value.ObservableValue;
//import javafx.concurrent.Worker;
//import javafx.embed.swt.FXCanvas;
//import javafx.scene.Scene;
//import javafx.scene.web.WebEngine;
//import javafx.scene.web.WebView;
//import netscape.javascript.JSObject;

public class JobViewEditor extends EditorPart {

	private IClusterDetail clusterDetail;
	private String uuid;

	private EventHelper.EventWaitHandle subscriptionsChanged;
	private boolean registeredSubscriptionsChanged;
	private final Object subscriptionsChangedSync = new Object();

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
		HDInsightJobViewUtils.checkInitlize();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	// TODO: refine code of createPartControl, share component with IntelliJ plugin jobivew component
	@Override
	public void createPartControl(Composite composite) {
		composite.setLayout(new FillLayout());
//		FXCanvas canvas = new FXCanvas(composite, SWT.FULL_SELECTION);
//		String indexPath = PluginUtil.pluginFolder  + "/com.microsoft.azure.hdinsight" + "/job/html/index.html";
//		final String queryString = "?projectid=" + uuid + "&engintype=javafx";
//		final String webUrl = "file:///" + indexPath.replace("\\", "") + queryString;
//		final WebView webView = new WebView();
//		Scene scene = new Scene(webView);
//		canvas.setScene(scene);
//		Platform.setImplicitExit(false);
//		Platform.runLater(new Runnable() {
//			
//			@Override
//			public void run() {
//				WebEngine webEngine = webView.getEngine();
//				webEngine.load(webUrl);
//				
//                webEngine.getLoadWorker().stateProperty().addListener(
//                        new ChangeListener<Worker.State>() {
//                            @Override
//                            public void changed(ObservableValue<? extends Worker.State> ov,
//                                                Worker.State oldState, Worker.State newState) {
//                                if (newState == Worker.State.SUCCEEDED) {
//                                    JSObject win = (JSObject) webEngine.executeScript("window");
//                                    win.setMember("JobUtils", new JobUtils());
//                                }
//                            }
//                        }
//                );
//			}
//		});
	}

	@Override
	public void setFocus() {
	}

}