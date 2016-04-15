package com.microsoft.azure.hdinsight.common;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.hdinsight.ToolWindow.*;
import com.microsoft.azure.hdinsight.serverexplore.ServerExplorerToolWindowFactory;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class PluginUtil {
    private static final Object LOCK = new Object();

    public static boolean isModuleRoot(VirtualFile moduleFolder, Module module) {
        return moduleFolder != null && ProjectRootsUtil.isModuleContentRoot(moduleFolder, module.getProject());
    }

    public static final Icon getIcon(String iconPath) {
        return IconLoader.getIcon(iconPath);
    }

    private static HashMap<ToolWindowKey, IToolWindowProcessor> toolWindowManagerCollection = new HashMap<>();

    public static void registerToolWindowManager(ToolWindowKey toolWindowFactoryKey, IToolWindowProcessor IToolWindowProcessor) {
        synchronized (PluginUtil.class) {
            toolWindowManagerCollection.put(toolWindowFactoryKey, IToolWindowProcessor);
        }
    }

    private static IToolWindowProcessor getToolWindowManager(ToolWindowKey toolWindowKey) {
        return toolWindowManagerCollection.get(toolWindowKey);
    }

    public static HDInsightRootModule getServerExplorerRootModule(Project project) {
        IToolWindowProcessor IToolWindowProcessor = getToolWindowManager(
                new ToolWindowKey(project, ServerExplorerToolWindowFactory.TOOLWINDOW_FACTORY_ID));

        if (IToolWindowProcessor != null) {
            return ((ServerExploreToolWindowProcessor) IToolWindowProcessor).getAzureServiceModule();

        }

        return null;
    }

    @Nullable
    public static JobStatusManager getJobStatusManager(@NotNull Project project) {
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        if(toolWindowManagerCollection.containsKey(key)){
           return ((SparkSubmissionToolWindowProcessor)getToolWindowManager(key)).getJobStatusManager();
        } else {
            return null;
        }
    }

    public static SparkSubmissionToolWindowProcessor getSparkSubmissionToolWindowManager(Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if(!toolWindowManagerCollection.containsKey(key)) {
            SparkSubmissionToolWindowProcessor sparkSubmissionToolWindowProcessor = new SparkSubmissionToolWindowProcessor(toolWindow);
            registerToolWindowManager(key, sparkSubmissionToolWindowProcessor);
            sparkSubmissionToolWindowProcessor.Initialize();
        }

        return (SparkSubmissionToolWindowProcessor)getToolWindowManager(key);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message, boolean isNeedClear) {
        showInfoOnSubmissionMessageWindow(project, MessageInfoType.Info, message, isNeedClear);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, MessageInfoType.Info, message, false);
    }

    public static void showErrorMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message, @NotNull final boolean isNeedClear) {
        showInfoOnSubmissionMessageWindow(project, MessageInfoType.Error, message, isNeedClear);
    }
    public static void showErrorMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, MessageInfoType.Error, message, false);
    }

    public static void showWarningMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message, @NotNull final boolean isNeedClear) {
        showInfoOnSubmissionMessageWindow(project, MessageInfoType.Warning, message, isNeedClear);
    }

    public static void showWarningMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, MessageInfoType.Warning, message, false);
    }

    private static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final MessageInfoType type, @NotNull final String message, @NotNull final boolean isNeedClear) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if (!toolWindow.isVisible()) {
            synchronized (LOCK) {
                if (!toolWindow.isVisible()) {
                    if (ApplicationManager.getApplication().isDispatchThread()) {
                        toolWindow.show(null);
                    } else {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    toolWindow.show(null);
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        showSubmissionMessage(getSparkSubmissionToolWindowManager(project), message, type, isNeedClear);
    }

    private static void showSubmissionMessage(SparkSubmissionToolWindowProcessor processor, @NotNull String message, @NotNull MessageInfoType type, @NotNull final boolean isNeedClear) {
        if (isNeedClear) {
            processor.clearAll();
        }

        switch (type) {
            case Error:
                processor.setError(message);
                break;
            case Info:
                processor.setInfo(message);
                break;
            case Warning:
                processor.setWarning(message);
                break;
        }
    }

    public static String getPluginRootDirectory() {
        IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID));

        return pluginDescriptor.getPath().getAbsolutePath();
    }

}