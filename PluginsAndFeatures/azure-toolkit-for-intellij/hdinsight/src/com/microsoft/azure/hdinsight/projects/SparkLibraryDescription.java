package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.impl.libraries.LibraryTypeServiceImpl;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.CommonConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class SparkLibraryDescription extends CustomLibraryDescription {

    private String localPath;

    @NotNull
    public String getLocalPath() {
        return localPath;
    }

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Collections.singleton(SparkLibraryKind.getInstance());
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent jComponent, @Nullable VirtualFile virtualFile) {
        return getSparkSDKConfigurationFromLocalFile();
    }

    private NewLibraryConfiguration getSparkSDKConfigurationFromLocalFile() {
        FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(false, false, true, false, true, false);
        chooserDescriptor.setTitle("Select Spark SDK");

        IdeaPluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID));
        String pluginPath = ideaPluginDescriptor.getPath().getAbsolutePath();
        VirtualFile pluginVfs = LocalFileSystem.getInstance().findFileByPath(pluginPath);

        VirtualFile chooseFile = FileChooser.chooseFile(chooserDescriptor, null, pluginVfs);
        if (chooseFile == null) {
            return null;
        }
        this.localPath = chooseFile.getPath();

        final List<OrderRoot> roots = RootDetectionUtil.detectRoots(Arrays.asList(chooseFile), null, null, new DefaultLibraryRootsComponentDescriptor());

        if (roots.isEmpty()) {
            return null;
        }

        return new NewLibraryConfiguration(LibraryTypeServiceImpl.suggestLibraryName(roots), SparkLibraryType.getInstance(), new SparkLibraryProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor libraryEditor) {
                libraryEditor.addRoots(roots);
            }
        };
    }
}
