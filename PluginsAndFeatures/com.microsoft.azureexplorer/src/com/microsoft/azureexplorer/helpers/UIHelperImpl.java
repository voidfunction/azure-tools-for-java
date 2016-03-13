/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azureexplorer.helpers;

import com.microsoft.azureexplorer.Activator;
import com.microsoft.azureexplorer.editors.BlobExplorerFileEditor;
import com.microsoft.azureexplorer.editors.QueueFileEditor;
import com.microsoft.azureexplorer.editors.StorageEditorInput;
import com.microsoft.azureexplorer.editors.TableFileEditor;
import com.microsoft.azureexplorer.forms.OpenSSLFinderForm;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.model.storage.*;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import com.google.common.collect.ImmutableMap;

public class UIHelperImpl implements UIHelper {
    private Map<Class<? extends StorageServiceTreeItem>, String> type2Editor = ImmutableMap.of(BlobContainer.class, "com.microsoft.azureexplorer.editors.BlobExplorerFileEditor",
            Queue.class, "com.microsoft.azureexplorer.editors.QueueFileEditor",
            Table.class, "com.microsoft.azureexplorer.editors.TableFileEditor");

    @Override
    public void showException(final String message,
                              final Throwable ex,
                              final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                PluginUtil.displayErrorDialog(null, title, message);
            }
        });
    }

    @Override
    public void showError(final String message, final String title) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                PluginUtil.displayErrorDialog(null, title, message);
            }
        });
    }

    @Override
    public boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption) {
        boolean choice = MessageDialog.openConfirm(new Shell(),
                title,
                message);

        return choice;
    }

    @Override
    public File showFileChooser(String title) {
        FileDialog dialog = new FileDialog(new Shell(), SWT.SAVE);
        dialog.setOverwrite(true);
//        IProject selProject = PluginUtil.getSelectedProject();
//        if (selProject != null) {
//            String path = selProject.getLocation().toPortableString();
//            dialog.setFilterPath(path);
//        }
        dialog.setText(title);
        String fileName = dialog.open();
        if (fileName == null || fileName.isEmpty()) {
            return null;
        } else {
            return new File(fileName);
        }
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(Object projectObject, final ClientStorageAccount storageAccount, final T item, String itemType, String itemName, String iconName) {
//        Display.getDefault().syncExec(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    BlobExplorerView view = (BlobExplorerView) PlatformUI
//                            .getWorkbench().getActiveWorkbenchWindow()
//                            .getActivePage().showView("com.microsoft.azureexplorer.views.BlobExplorerView");
//                    view.init(storageAccount, (BlobContainer) blobContainer);
//                } catch (PartInitException e) {
//                    Activator.getDefault().log("Error opening container", e);
//                }
//            }
//        });
        IWorkbench workbench=PlatformUI.getWorkbench();
        IEditorDescriptor editorDescriptor=workbench.getEditorRegistry().findEditor(type2Editor.get(item.getClass()));
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IEditorPart newEditor = page.openEditor(new StorageEditorInput(storageAccount, item), editorDescriptor.getId());
        } catch (PartInitException e) {
            Activator.getDefault().log("Error opening " + item.getName(), e);
        }
    }

    @Override
    public void openItem(Object projectObject, Object itemVirtualFile) {
    }

    @Override
    public void refreshQueue(Object projectObject, final ClientStorageAccount storageAccount, final Queue queue) {
        IWorkbench workbench=PlatformUI.getWorkbench();
        final IEditorDescriptor editorDescriptor=workbench.getEditorRegistry()
                .findEditor("com.microsoft.azureexplorer.editors.QueueFileEditor");
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            public void run() {
                try {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    QueueFileEditor newEditor = (QueueFileEditor) page.openEditor(new StorageEditorInput(storageAccount, queue), editorDescriptor.getId());
                    newEditor.fillGrid();
                } catch (PartInitException e) {
                    Activator.getDefault().log("Error opening container", e);
                }
            }
        });
    }

    @Override
    public void refreshBlobs(Object projectObject, final ClientStorageAccount storageAccount, final BlobContainer container) {
        IWorkbench workbench=PlatformUI.getWorkbench();
        final IEditorDescriptor editorDescriptor=workbench.getEditorRegistry()
                .findEditor("com.microsoft.azureexplorer.editors.BlobExplorerFileEditor");
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            public void run() {
                try {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    BlobExplorerFileEditor newEditor = (BlobExplorerFileEditor) page.openEditor(new StorageEditorInput(storageAccount, container), editorDescriptor.getId());
                    newEditor.fillGrid();
                } catch (PartInitException e) {
                    Activator.getDefault().log("Error opening container", e);
                }
            }
        });
    }

    @Override
    public void refreshTable(Object projectObject, final ClientStorageAccount storageAccount, final Table table) {
        IWorkbench workbench=PlatformUI.getWorkbench();
        final IEditorDescriptor editorDescriptor=workbench.getEditorRegistry()
                .findEditor("com.microsoft.azureexplorer.editors.TableFileEditor");
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            public void run() {
                try {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    TableFileEditor newEditor = (TableFileEditor) page.openEditor(new StorageEditorInput(storageAccount, table), editorDescriptor.getId());
                    newEditor.fillGrid();
                } catch (PartInitException e) {
                    Activator.getDefault().log("Error opening container", e);
                }
            }
        });
    }

    @Override
    public String promptForOpenSSLPath() {
        OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm(new Shell());
        openSSLFinderForm.open();

        return DefaultLoader.getIdeHelper().getProperty("MSOpenSSLPath", "");
    }

    @Override
    public <T extends StorageServiceTreeItem> Object getOpenedFile(Object projectObject, ClientStorageAccount storageAccount, T blobContainer) {
        return null;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
