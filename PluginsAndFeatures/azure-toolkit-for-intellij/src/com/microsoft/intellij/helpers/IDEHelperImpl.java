/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.helpers;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.helpers.tasks.CancellableTaskHandleImpl;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.ServiceCodeReferenceHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask.CancellableTaskHandle;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoftopentechnologies.auth.browser.BrowserLauncher;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class IDEHelperImpl implements IDEHelper {
    private final Project project;

    public IDEHelperImpl(Project project) {
        this.project = project;
    }

    @Override
    public void openFile(@NotNull File file, @NotNull final Object n) {
        final Node node = (Node) n;
        final VirtualFile finalEditfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    openFile(node.getProject(), finalEditfile);
                } finally {
                    node.setLoading(false);
                }
            }
        });
    }

    @Override
    public void saveFile(@NotNull final File file, @NotNull final ByteArrayOutputStream buff, @NotNull final Object n) {
        final Node node = (Node) n;
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    final VirtualFile editfile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

                    if (editfile != null) {
                        editfile.setWritable(true);
                        editfile.setBinaryContent(buff.toByteArray());

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                FileEditorManager.getInstance((Project) node.getProject()).openFile(editfile, true);
                            }
                        });
                    }
                } catch (Throwable e) {
                    DefaultLoader.getUIHelper().showException("An error occurred while attempting to write temporal " +
                                    "editable file.", e,
                            "Azure Services Explorer - Error Writing Temp File", false, true);
                } finally {
                    node.setLoading(false);
                }
            }
        });
    }

    @Override
    public void replaceInFile(@NotNull Object moduleObject, @NotNull Pair<String, String>... replace) {
        Module module = (Module) moduleObject;

        if (module.getModuleFile() != null && module.getModuleFile().getParent() != null) {
            VirtualFile vf = module.getModuleFile().getParent().findFileByRelativePath(ServiceCodeReferenceHelper.STRINGS_XML);

            if (vf != null) {
                FileDocumentManager fdm = FileDocumentManager.getInstance();
                com.intellij.openapi.editor.Document document = fdm.getDocument(vf);

                if (document != null) {
                    String content = document.getText();

                    for (Pair<String, String> pair : replace) {
                        content = content.replace(pair.getLeft(), pair.getRight());
                    }

                    document.setText(content);
                    fdm.saveDocument(document);
                }
            }
        }
    }

    @Override
    public void copyJarFiles2Module(@NotNull Object moduleObject, @NotNull File zipFile, @NotNull String zipPath)
            throws IOException {
        Module module = (Module) moduleObject;
        final VirtualFile moduleFile = module.getModuleFile();

        if (moduleFile != null) {
            moduleFile.refresh(false, false);

            final VirtualFile moduleDir = module.getModuleFile().getParent();

            if (moduleDir != null) {
                moduleDir.refresh(false, false);

                copyJarFiles(module, moduleDir, zipFile, zipPath);
            }
        }
    }

    @Override
    public boolean isFileEditing(@NotNull Object projectObject, @NotNull File file) {
        VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        boolean fileIsEditing = false;

        if (scriptFile != null) {
            fileIsEditing = FileEditorManager.getInstance((Project) projectObject).getEditors(scriptFile).length != 0;
        }

        return fileIsEditing;
    }

    @Override
    public void closeFile(@NotNull final Object projectObject, @NotNull final Object openedFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).closeFile((VirtualFile) openedFile);
            }
        }, ModalityState.any());
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @Override
    public void runInBackground(@Nullable final Object project, @NotNull final String name, final boolean canBeCancelled,
                                final boolean isIndeterminate, @Nullable final String indicatorText,
                                final Runnable runnable) {
        // background tasks via ProgressManager can be scheduled only on the
        // dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable((Project) project,
                        name, canBeCancelled) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        if (isIndeterminate) {
                            indicator.setIndeterminate(true);
                        }

                        if (indicatorText != null) {
                            indicator.setText(indicatorText);
                        }

                        runnable.run();
                    }
                });
            }
        }, ModalityState.any());
    }

    @NotNull
    @Override
    public CancellableTaskHandle runInBackground(@NotNull ProjectDescriptor projectDescriptor,
                                                 @NotNull final String name,
                                                 @Nullable final String indicatorText,
                                                 @NotNull final CancellableTask cancellableTask)
            throws AzureCmdException {
        final CancellableTaskHandleImpl handle = new CancellableTaskHandleImpl();
        final Project project = findOpenProject(projectDescriptor);

        // background tasks via ProgressManager can be scheduled only on the
        // dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(getCancellableBackgroundTask(project, name, indicatorText, handle, cancellableTask));
            }
        }, ModalityState.any());

        return handle;
    }

    @Nullable
    @Override
    public String getProperty(@NotNull String name) {
        return AzureSettings.getSafeInstance(project).getProperty(name);
//        return PropertiesComponent.getInstance().getValue(name);
    }

    @NotNull
    @Override
    public String getProperty(@NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance().getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value) {
        AzureSettings.getSafeInstance(project).setProperty(name, value);
//        PropertiesComponent.getInstance().setValue(name, value);
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                ApplicationManager.getApplication().saveSettings();
//            }
//        }, ModalityState.any());
    }

    @Override
    public void unsetProperty(@NotNull String name) {
        AzureSettings.getSafeInstance(project).unsetProperty(name);
//        PropertiesComponent.getInstance().unsetValue(name);
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                ApplicationManager.getApplication().saveSettings();
//            }
//        }, ModalityState.any());
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return AzureSettings.getSafeInstance(project).isPropertySet(name);
//        return PropertiesComponent.getInstance().isValueSet(name);
    }

    @Nullable
    @Override
    public String[] getProperties(@NotNull String name) { // todo!!!
        return AzureSettings.getSafeInstance(project).getProperties(name);
//        return PropertiesComponent.getInstance().getValues(name);
    }

    @Override
    public void setProperties(@NotNull String name, @NotNull String[] value) {
        AzureSettings.getSafeInstance(project).setProperties(name, value);
//        PropertiesComponent.getInstance().setValues(name, value);
//        ApplicationManager.getApplication().saveSettings();
    }

    @NotNull
    @Override
    public List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor)
            throws AzureCmdException {
        Project project = findOpenProject(projectDescriptor);

        List<ArtifactDescriptor> artifactDescriptors = new ArrayList<ArtifactDescriptor>();

        for (Artifact artifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            artifactDescriptors.add(new ArtifactDescriptor(artifact.getName(), artifact.getArtifactType().getId()));
        }

        return artifactDescriptors;
    }

    @NotNull
    @Override
    public ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor,
                                                  @NotNull ArtifactDescriptor artifactDescriptor) {
        try {
            Project project = findOpenProject(projectDescriptor);

            final Artifact artifact = findProjectArtifact(project, artifactDescriptor);

            final SettableFuture<String> future = SettableFuture.create();

            Futures.addCallback(buildArtifact(project, artifact, false), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean succeded) {
                    if (succeded != null && succeded) {
                        future.set(artifact.getOutputFilePath());
                    } else {
                        future.setException(new AzureCmdException("An error occurred while building the artifact"));
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable instanceof ExecutionException) {
                        future.setException(new AzureCmdException("An error occurred while building the artifact",
                                throwable.getCause()));
                    } else {
                        future.setException(new AzureCmdException("An error occurred while building the artifact",
                                throwable));
                    }
                }
            });

            return future;
        } catch (AzureCmdException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public BrowserLauncher getBrowserLauncher() {
        return null;
    }

    private static void openFile(@NotNull final Object projectObject, @Nullable final VirtualFile finalEditfile) {
        try {
            if (finalEditfile != null) {
                finalEditfile.setWritable(true);

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        FileEditorManager.getInstance((Project) projectObject).openFile(finalEditfile, true);
                    }
                });
            }
        } catch (Throwable e) {
            DefaultLoader.getUIHelper().showException("An error occurred while attempting to write temporal editable " +
                            "file.", e,
                    "Azure Services Explorer - Error Writing Temp File", false, true);
        }
    }

    private static void copyJarFiles(@NotNull final Module module, @NotNull VirtualFile baseDir,
                                     @NotNull File zipFile, @NotNull String zipPath)
            throws IOException {
        if (baseDir.isDirectory()) {
            final ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();

                if (!zipEntry.isDirectory() && zipEntry.getName().startsWith(zipPath) &&
                        zipEntry.getName().endsWith(".jar") &&
                        !(zipEntry.getName().endsWith("-sources.jar") || zipEntry.getName().endsWith("-javadoc.jar"))) {
                    VirtualFile libsVf = null;

                    for (VirtualFile vf : baseDir.getChildren()) {
                        if (vf.getName().equals("libs")) {
                            libsVf = vf;
                            break;
                        }
                    }

                    if (libsVf == null) {
                        libsVf = baseDir.createChildDirectory(module.getProject(), "libs");
                    }

                    final VirtualFile libs = libsVf;
                    final String fileName = zipEntry.getName().split("/")[1];

                    if (libs.findChild(fileName) == null) {
                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            InputStream mobileserviceInputStream = zip.getInputStream(zipEntry);
                                            VirtualFile msVF = libs.createChildData(module.getProject(), fileName);
                                            msVF.setBinaryContent(getArray(mobileserviceInputStream));
                                        } catch (Throwable ex) {
                                            DefaultLoader.getUIHelper().showException("An error occurred while attempting " +
                                                            "to configure Azure Mobile Services.", ex,
                                                    "Azure Services Explorer - Error Configuring Mobile Services", false, true);
                                        }
                                    }
                                });
                            }
                        }, ModalityState.defaultModalityState());
                    }
                }
            }
        }
    }

    @NotNull
    private static byte[] getArray(@NotNull InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static ListenableFuture<Boolean> buildArtifact(@NotNull Project project, final @NotNull Artifact artifact, boolean rebuild) {
        final SettableFuture<Boolean> future = SettableFuture.create();

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>(1);
        artifacts.add(artifact);
        CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, rebuild);
        ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

        CompilerManager.getInstance(project).make(scope, new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                future.set(!aborted && errors == 0);
            }
        });

        return future;
    }

    @NotNull
    private static Project findOpenProject(@NotNull ProjectDescriptor projectDescriptor)
            throws AzureCmdException {
        Project project = null;

        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (projectDescriptor.getName().equals(openProject.getName())
                    && projectDescriptor.getPath().equals(openProject.getBasePath())) {
                project = openProject;
                break;
            }
        }

        if (project == null) {
            throw new AzureCmdException("Unable to find an open project with the specified description.");
        }

        return project;
    }

    @NotNull
    private static Artifact findProjectArtifact(@NotNull Project project, @NotNull ArtifactDescriptor artifactDescriptor)
            throws AzureCmdException {
        Artifact artifact = null;

        for (Artifact projectArtifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            if (artifactDescriptor.getName().equals(projectArtifact.getName())
                    && artifactDescriptor.getArtifactType().equals(projectArtifact.getArtifactType().getId())) {
                artifact = projectArtifact;
                break;
            }
        }

        if (artifact == null) {
            throw new AzureCmdException("Unable to find an artifact with the specified description.");
        }

        return artifact;
    }

    @org.jetbrains.annotations.NotNull
    private static Task.Backgroundable getCancellableBackgroundTask(final Project project,
                                                                    @NotNull final String name,
                                                                    @Nullable final String indicatorText,
                                                                    final CancellableTaskHandleImpl handle,
                                                                    @NotNull final CancellableTask cancellableTask) {
        return new Task.Backgroundable(project,
                name, true) {
            private final Semaphore lock = new Semaphore(0);

            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                handle.setProgressIndicator(indicator);

                if (indicatorText != null) {
                    indicator.setText(indicatorText);
                }

                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cancellableTask.run(handle);
                        } catch (Throwable t) {
                            handle.setException(t);
                        } finally {
                            lock.release();
                        }
                    }
                });

                try {
                    while (!lock.tryAcquire(1, TimeUnit.SECONDS)) {
                        if (handle.isCancelled()) {
                            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                                @Override
                                public void run() {
                                    cancellableTask.onCancel();
                                }
                            });

                            return;
                        }
                    }

                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            if (handle.getException() == null) {
                                cancellableTask.onSuccess();
                            } else {
                                cancellableTask.onError(handle.getException());
                            }
                        }
                    });
                } catch (InterruptedException ignored) {
                }
            }
        };
    }
}