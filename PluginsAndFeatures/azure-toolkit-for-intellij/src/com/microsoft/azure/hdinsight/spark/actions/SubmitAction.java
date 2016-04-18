package com.microsoft.azure.hdinsight.spark.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.azure.hdinsight.toolwindow.JobStatusManager;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionExDialog;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.StringHelper;

import java.util.HashSet;
import java.util.List;

public class SubmitAction extends AnAction {
    private List<IClusterDetail> cachedClusterDetails = null;
    private static final HashSet<Project> isActionPerformedSet = new HashSet<>();

    @Override
    public void actionPerformed(final AnActionEvent anActionEvent) {
        synchronized (SubmitAction.class) {
            final Project project = anActionEvent.getProject();
            if(isActionPerformedSet.contains(project)) {
                return;
            }

            isActionPerformedSet.add(project);
            TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionRightClickProject, null, null);

            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    PluginUtil.showInfoOnSubmissionMessageWindow(project, "List spark clusters ...", true);

                    cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(true);
                    if(!ClusterManagerEx.getInstance().isSelectedSubscriptionExist()) {
                        PluginUtil.showWarningMessageOnSubmissionMessageWindow(project, "No selected subscription(s), Please go to HDInsight Explorer to sign in....");
                    }

                    if (ClusterManagerEx.getInstance().isListClusterSuccess()) {
                        PluginUtil.showInfoOnSubmissionMessageWindow(project, "List spark clusters successfully");
                    } else {
                        PluginUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error : Failed to list spark clusters.");
                    }
                    if (ClusterManagerEx.getInstance().isLIstAdditionalClusterSuccess()) {
                        PluginUtil.showInfoOnSubmissionMessageWindow(project, "List additional spark clusters successfully");
                    } else {
                        PluginUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error: Failed to list additional cluster");
                    }

                    SparkSubmissionExDialog dialog = new SparkSubmissionExDialog(anActionEvent.getProject(), cachedClusterDetails, new CallBack() {
                        @Override
                        public void run() {
                            isActionPerformedSet.remove(anActionEvent.getProject());
                        }
                    });

                    dialog.setVisible(true);
                }
            });
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());

        Presentation presentation = event.getPresentation();
        if(module == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        String uniqueValue = module.getOptionValue(HDInsightModuleBuilder.UniqueKeyName);
        boolean isVisible = !StringHelper.isNullOrWhiteSpace(uniqueValue) && uniqueValue.equals(HDInsightModuleBuilder.UniqueKeyValue);
        if(isVisible) {
            presentation.setVisible(isVisible);
            JobStatusManager manager = PluginUtil.getJobStatusManager(module.getProject());
            presentation.setEnabled(!isActionPerformedSet.contains(module.getProject()) && (manager == null || !manager.isJobRunning()));
        } else {
            presentation.setEnabledAndVisible(false);
        }
    }
}
