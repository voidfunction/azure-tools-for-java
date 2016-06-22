package com.microsoft.azureexplorer.helpers;

import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.hdinsight.common.HDInsightHelper;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azureexplorer.Activator;
import com.microsoft.azureexplorer.editors.JobViewInput;

public class HDInsightHelperImpl implements HDInsightHelper {

    public void openJobViewEditor(Object projectObject, String uuid) {
        IClusterDetail clusterDetail = JobViewManager.getCluster(uuid);
        IWorkbench workbench=PlatformUI.getWorkbench();
        IEditorDescriptor editorDescriptor=workbench.getEditorRegistry().findEditor("com.microsoft.azure.hdinsight.jobview");
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IEditorPart newEditor = page.openEditor(new JobViewInput(clusterDetail, uuid), editorDescriptor.getId());
        } catch (PartInitException e) {
            Activator.getDefault().log("Error opening " + clusterDetail.getName(), e);
        }
        
        
//
//        Project project = (Project)projectObject;
//        VirtualFile openedFile = getOpenedItem(project);
//        if(openedFile == null || isNeedReopen(openedFile, clusterDetail)) {
//            openItem(project,clusterDetail, uuid, openedFile);
//        } else {
//            openItem(project, openedFile, null);
//        }
    }

//    private boolean isNeedReopen(@NotNull VirtualFile virtualFile, @NotNull IClusterDetail myClusterDetail) {
//        IClusterDetail detail = virtualFile.getUserData(JobViewEditorProvider.JOB_VIEW_KEY);
//        return detail != null && !detail.getName().equalsIgnoreCase(myClusterDetail.getName());
//    }

//    private static VirtualFile getOpenedItem(Project project) {
//        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
//        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
//            IClusterDetail detail = editedFile.getUserData(JobViewEditorProvider.JOB_VIEW_KEY);
//            if (detail != null) {
//                return editedFile;
//            }
//        }
//        return null;
//    }

//    private void openItem(@NotNull final Project project, @NotNull final VirtualFile virtualFile, @Nullable final VirtualFile closeableVirtualFile) {
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                if(closeableVirtualFile != null) {
//                    FileEditorManager.getInstance(project).closeFile(closeableVirtualFile);
//                }
//                FileEditorManager.getInstance(project).openFile((VirtualFile) virtualFile, true, false);
//            }
//        }, ModalityState.any());
//    }

//    private void openItem(@NotNull final Project project, @NotNull IClusterDetail myClusterDetail,@NotNull String uuid, @Nullable VirtualFile closeableFile) {
//        final LightVirtualFile virtualFile = new LightVirtualFile("Spark JobView");
//        virtualFile.putUserData(JobViewEditorProvider.JOB_VIEW_KEY, myClusterDetail);
//        virtualFile.setFileType(new FileType() {
//            @NotNull
//            @Override
//            public String getName() {
//                return this.getClass().getName();
//            }
//
//            @NotNull
//            @Override
//            public String getDescription() {
//                return "job view dummy file";
//            }
//
//            @NotNull
//            @Override
//            public String getDefaultExtension() {
//                return "";
//            }
//
//            @Nullable
//            @Override
//            public Icon getIcon() {
//                return StreamUtil.getImageResourceFile(CommonConst.SPARK_JOBVIEW_ICONPATH);
//            }
//
//            @Override
//            public boolean isBinary() {
//                return true;
//            }
//
//            @Override
//            public boolean isReadOnly() {
//                return true;
//            }
//
//            @Nullable
//            @Override
//            public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
//                return "UTF8";
//            }
//        });
//        virtualFile.putUserData(JobViewEditorProvider.JOB_VIEW_UUID, uuid);
//        openItem(project, virtualFile, closeableFile);
//    }
}
