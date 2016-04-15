package com.microsoft.azure.hdinsight.spark.common;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.spark.UIHelper.InteractiveTableModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.IOException;
import java.util.*;

public class SparkSubmitModel {

    private static final String[] columns = {"Key", "Value", ""};
    private  static final String SparkYarnLogUrlFormat = "%s/yarnui/hn/cluster/app/%s";

    private static Map<Project, SparkSubmissionParameter> submissionParameterMap = new HashMap<>();

    private Project project;
    private List<IClusterDetail> cachedClusterDetails;

    private Map<String, IClusterDetail> mapClusterNameToClusterDetail = new HashMap<>();
    private Map<String, Artifact> artifactHashMap = new HashMap<>();

    private SparkSubmissionParameter submissionParameter;

    private DefaultComboBoxModel<String> clusterComboBoxModel;
    private DefaultComboBoxModel<String> artifactComboBoxModel;
    private SubmissionTableModel tableModel = new SubmissionTableModel(columns);
    private Map<String, String> postEventProperty = new HashMap<>();


    public SparkSubmitModel(@NotNull Project project, @NotNull List<IClusterDetail> cachedClusterDetails) {
        this.cachedClusterDetails = cachedClusterDetails;
        this.project = project;

        this.clusterComboBoxModel = new DefaultComboBoxModel<>();
        this.artifactComboBoxModel = new DefaultComboBoxModel<>();
        this.submissionParameter = submissionParameterMap.get(project);

        setClusterComboBoxModel(cachedClusterDetails);
        int index = submissionParameter != null ? clusterComboBoxModel.getIndexOf(submissionParameter.getClusterName()) : -1;
        if (index != -1) {
            clusterComboBoxModel.setSelectedItem(submissionParameter.getClusterName());
        }

        final List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(project);

        for (Artifact artifact : artifacts) {
            artifactHashMap.put(artifact.getName(), artifact);
            artifactComboBoxModel.addElement(artifact.getName());
            if (artifactComboBoxModel.getSize() == 0) {
                artifactComboBoxModel.setSelectedItem(artifact.getName());
            }
        }

        index = submissionParameter != null ? artifactComboBoxModel.getIndexOf(submissionParameter.getArtifactName()) : -1;
        if (index != -1) {
            artifactComboBoxModel.setSelectedItem(submissionParameter.getArtifactName());
        }

        initializeTableModel(tableModel);
    }

    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    @NotNull
    public IClusterDetail getSelectedClusterDetail() {
        return mapClusterNameToClusterDetail.get((String) clusterComboBoxModel.getSelectedItem());
    }

    public DefaultComboBoxModel getClusterComboBoxModel() {
        return clusterComboBoxModel;
    }

    public DefaultComboBoxModel getArtifactComboBoxModel() {
        return artifactComboBoxModel;
    }

    public boolean isLocalArtifact() {
        return submissionParameter.isLocalArtifact();
    }

    public Project getProject() {
        return project;
    }

    public InteractiveTableModel getTableModel() {
        return tableModel;
    }

    public void setClusterComboBoxModel(List<IClusterDetail> cachedClusterDetails) {
        clusterComboBoxModel.removeAllElements();
        mapClusterNameToClusterDetail.clear();

        for (IClusterDetail clusterDetail : cachedClusterDetails) {
            mapClusterNameToClusterDetail.put(clusterDetail.getName(), clusterDetail);
            clusterComboBoxModel.addElement(clusterDetail.getName());
            if (clusterComboBoxModel.getSize() == 0) {
                clusterComboBoxModel.setSelectedItem(clusterDetail.getName());
            }
        }
    }
    public void action(@NotNull SparkSubmissionParameter submissionParameter) {
        PluginUtil.getJobStatusManager(project).setJobRunningState(true);
        this.submissionParameter = submissionParameter;
        submissionParameterMap.put(project, submissionParameter);
        postEventAction();

        if (isLocalArtifact()) {
            submit();
        } else {
            List<Artifact> artifacts = new ArrayList<>();
            final Artifact artifact = artifactHashMap.get(submissionParameter.getArtifactName());
            artifacts.add(artifact);
            ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

            final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, true);

            CompilerManager.getInstance(project).make(scope, new CompileStatusNotification() {
                @Override
                public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                    if (aborted || errors != 0) {
                        postEventProperty.put("IsSubmitSucceed", "false");
                        postEventProperty.put("SubmitFailedReason", "CompileFailed");
                        TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
                        return;
                    } else {
                        CompilerManager.getInstance(project).make(new CompileStatusNotification() {
                            @Override
                            public void finished(boolean aborted1, int errors1, int warnings1, CompileContext compileContext1) {
                                PluginUtil.showInfoOnSubmissionMessageWindow(project, String.format("Info : Build %s successfully.", artifact.getOutputFile()));
                                submit();
                            }
                        });
                    }
                }
            });
        }
    }

    private void uploadFileToAzureBlob(@NotNull final IClusterDetail selectedClusterDetail, @NotNull final String selectedArtifactName) throws Exception{
        String buildJarPath = submissionParameter.isLocalArtifact() ?
                submissionParameter.getLocalArtifactPath() : ((artifactHashMap.get(selectedArtifactName).getOutputFilePath()));

        String fileOnBlobPath = SparkSubmitHelper.uploadFileToAzureBlob(project, selectedClusterDetail, buildJarPath);
        submissionParameter.setFilePath(fileOnBlobPath);
    }

    private void tryToCreateBatchSparkJob(@NotNull final IClusterDetail selectedClusterDetail) throws HDIException,IOException {
        SparkBatchSubmission.getInstance().setCredentialsProvider(selectedClusterDetail.getHttpUserName(), selectedClusterDetail.getHttpPassword());
        HttpResponse response = SparkBatchSubmission.getInstance().createBatchSparkJob(selectedClusterDetail.getConnectionUrl() + "/livy/batches", submissionParameter);

        if (response.getStatusCode() == 201 || response.getStatusCode() == 200) {
            PluginUtil.showInfoOnSubmissionMessageWindow(project, "Info : Submit to spark cluster successfully.");
            postEventProperty.put("IsSubmitSucceed", "true");

            String jobLink = String.format("%s/sparkhistory", selectedClusterDetail.getConnectionUrl());
            PluginUtil.getSparkSubmissionToolWindowManager(project).setHyperLinkWithText("See spark job view from ", jobLink, jobLink);
            SparkSubmitResponse sparkSubmitResponse = new Gson().fromJson(response.getMessage(), new TypeToken<SparkSubmitResponse>() {
            }.getType());

            // Set submitted spark application id and http request info for stopping running application
            PluginUtil.getSparkSubmissionToolWindowManager(project).setSparkApplicationStopInfo(selectedClusterDetail.getConnectionUrl(), sparkSubmitResponse.getId());
            PluginUtil.getSparkSubmissionToolWindowManager(project).setStopButtonState(true);
            PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().resetJobStateManager();
            SparkSubmitHelper.getInstance().printRunningLogStreamingly(project, sparkSubmitResponse.getId(), selectedClusterDetail, postEventProperty);
        } else {
            PluginUtil.showErrorMessageOnSubmissionMessageWindow(project,
                    String.format("Error : Failed to submit to spark cluster. error code : %d, reason :  %s.", response.getStatusCode(), response.getReason()));
            postEventProperty.put("IsSubmitSucceed", "false");
            postEventProperty.put("SubmitFailedReason", response.getReason());
            TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
        }
    }

    private void showFailedSubmitErrorMessage(Exception exception) {
        PluginUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error : Failed to submit application to spark cluster. Exception : " + exception.getMessage());
        postEventProperty.put("IsSubmitSucceed", "false");
        postEventProperty.put("SubmitFailedReason", exception.toString());
        TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
    }

    private void writeJobLogToLocal() {
        String path = null;
        try {
            path = SparkSubmitHelper.getInstance().writeLogToLocalFile(project);
        } catch (IOException e) {
            PluginUtil.showErrorMessageOnSubmissionMessageWindow(project, e.getMessage());
        }

        if (!StringHelper.isNullOrWhiteSpace(path)) {
            String urlPath = StringHelper.concat("file:", path);
            PluginUtil.getSparkSubmissionToolWindowManager(project).setHyperLinkWithText("See detailed job log from local:", urlPath, path);
        }
    }

    private IClusterDetail getClusterConfiguration(@NotNull final IClusterDetail selectedClusterDetail, @NotNull final boolean isFirstSubmit) {
        try {
            if (!selectedClusterDetail.isConfigInfoAvailable()) {
                selectedClusterDetail.getConfigurationInfo();
            }
        } catch (AuthenticationException authenticationException) {
            if (isFirstSubmit) {
                PluginUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error: Cluster Credentials Expired, Please sign in again...");
                //get new credentials by call getClusterDetails
                cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetails(project);

                for (IClusterDetail iClusterDetail : cachedClusterDetails) {
                    if (iClusterDetail.getName().equalsIgnoreCase(selectedClusterDetail.getName())) {
                        //retry get cluster info
                        return getClusterConfiguration(iClusterDetail, false);
                    }
                }
            } else {
                return null;
            }
        } catch (Exception exception) {
            showFailedSubmitErrorMessage(exception);
            return null;
        }

        return selectedClusterDetail;
    }

    private void submit() {
        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                IClusterDetail selectedClusterDetail = mapClusterNameToClusterDetail.get(clusterComboBoxModel.getSelectedItem());
                String selectedArtifactName = submissionParameter.getArtifactName();

                //may get a new clusterDetail reference if cluster credentials expired
                selectedClusterDetail = getClusterConfiguration(selectedClusterDetail, true);

                if (selectedClusterDetail == null) {
                    PluginUtil.showErrorMessageOnSubmissionMessageWindow(project, "Selected Cluster can not found. Please login in first in HDInsight Explorer and try submit job again");
                    return;
                }

                try {
                    uploadFileToAzureBlob(selectedClusterDetail, selectedArtifactName);
                    tryToCreateBatchSparkJob(selectedClusterDetail);
                } catch (Exception exception) {
                    showFailedSubmitErrorMessage(exception);
                } finally {
                    PluginUtil.getSparkSubmissionToolWindowManager(project).setStopButtonState(false);
                    PluginUtil.getSparkSubmissionToolWindowManager(project).setBrowserButtonState(false);

                    if (PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isApplicationGenerated()) {
                        String applicationId = PluginUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().getApplicationId();

                        // ApplicationYarnUrl example : https://sparklivylogtest.azurehdinsight.net/yarnui/hn/cluster/app/application_01_111
                        String applicationYarnUrl = String.format(SparkYarnLogUrlFormat, selectedClusterDetail.getConnectionUrl(), applicationId);
                        PluginUtil.getSparkSubmissionToolWindowManager(project).setHyperLinkWithText("See detailed job information from ", applicationYarnUrl, applicationYarnUrl);

                        writeJobLogToLocal();
                    }

                    PluginUtil.getJobStatusManager(project).setJobRunningState(false);
                }
            }
        });
    }

    private void postEventAction() {
        postEventProperty.clear();
        postEventProperty.put("ClusterName", submissionParameter.getClusterName());
        if (submissionParameter.getArgs() != null && submissionParameter.getArgs().size() > 0) {
            postEventProperty.put("HasCommandLine", "true");
        } else {
            postEventProperty.put("HasCommandLine", "false");
        }

        if (submissionParameter.getReferencedFiles() != null && submissionParameter.getReferencedFiles().size() > 0) {
            postEventProperty.put("HasReferencedFile", "true");
        } else {
            postEventProperty.put("HasReferencedFile", "false");
        }

        if (submissionParameter.getReferencedJars() != null && submissionParameter.getReferencedJars().size() > 0) {
            postEventProperty.put("HasReferencedJar", "true");
        } else {
            postEventProperty.put("HasReferencedJar", "false");
        }
    }

    public Map<String, Object> getJobConfigMap() {
        return tableModel.getJobConfigMap();
    }

    private void initializeTableModel(final InteractiveTableModel tableModel) {
        if (submissionParameter == null) {
            for (int i = 0; i < SparkSubmissionParameter.defaultParameters.length; ++i) {
                tableModel.addRow(SparkSubmissionParameter.defaultParameters[i].getFirst(), "");
            }
        } else {
            Map<String, Object> configs = submissionParameter.getJobConfig();
            for (int i = 0; i < SparkSubmissionParameter.parameterList.length; ++i) {
                tableModel.addRow(SparkSubmissionParameter.parameterList[i], configs.containsKey(SparkSubmissionParameter.parameterList[i]) ?
                        configs.get(SparkSubmissionParameter.parameterList[i]) : "");
            }
        }

        if (!tableModel.hasEmptyRow()) {
            tableModel.addEmptyRow();
        }
    }
}
