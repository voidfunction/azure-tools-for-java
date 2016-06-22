package com.microsoft.azureexplorer.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azureexplorer.Activator;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;


public class JobViewEditor extends EditorPart {

    private IClusterDetail clusterDetail;

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
        clusterDetail =  ((JobViewInput) input).getClusterDetail();
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
        composite.setLayout(new GridLayout());
    }

    @Override
    public void setFocus() {
    }

}