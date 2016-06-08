package com.microsoft.azure.hdinsight.spark.actions;

import java.util.HashSet;
import java.util.List;

import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionExDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common2.HDInsightUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class SubmitHandler extends AbstractHandler {
    private List<IClusterDetail> cachedClusterDetails = null;
    private static final HashSet<IProject> isActionPerformedSet = new HashSet<>();
    
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		synchronized (SubmitHandler.class) {
//            final Project project = anActionEvent.getProject();
//            if(isActionPerformedSet.contains(project)) {
//                return;
//            }
//
//            isActionPerformedSet.add(project);
//            TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionRightClickProject, null, null);

//            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
//                @Override
//                public void run() {
                    HDInsightUtil.showInfoOnSubmissionMessageWindow("List spark clusters ...", true);
//
                    cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(true, null);
                    if(!ClusterManagerEx.getInstance().isSelectedSubscriptionExist()) {
                        HDInsightUtil.showWarningMessageOnSubmissionMessageWindow("No selected subscription(s), Please go to HDInsight Explorer to sign in....");
                    }

                    if (ClusterManagerEx.getInstance().isListClusterSuccess()) {
                        HDInsightUtil.showInfoOnSubmissionMessageWindow("List spark clusters successfully");
                    } else {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error : Failed to list spark clusters.");
                    }
                    if (ClusterManagerEx.getInstance().isLIstAdditionalClusterSuccess()) {
                        HDInsightUtil.showInfoOnSubmissionMessageWindow("List additional spark clusters successfully");
                    } else {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error: Failed to list additional cluster");
                    }
//
                    SparkSubmissionExDialog dialog = new SparkSubmissionExDialog(PluginUtil.getParentShell(), cachedClusterDetails, new CallBack() {
                        @Override
                        public void run() {
//                            isActionPerformedSet.remove(anActionEvent.getProject());
                        }
                    });
                    dialog.open();
//                }
//            });
            return null;
        }

	}
}
