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
package com.microsoftopentechnologies.windowsazure.tools.build;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoftopentechnologies.auth.browser.BrowserLauncher;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class is needed because AADManagerImpl and AzureManagerImpl heavily rely on IDEHelper
 */
public class AntIDEHelper implements IDEHelper {
    @Override
    public void closeFile(@NotNull Object projectObject, @NotNull Object openedFile) {
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
    }

    @Override
    public void runInBackground(@Nullable Object project, @NotNull String name, boolean canBeCancelled, boolean isIndeterminate, @Nullable String indicatorText, Runnable runnable) {
    }

    @Override
    public CancellableTask.CancellableTaskHandle runInBackground(@NotNull ProjectDescriptor projectDescriptor, @NotNull String name, @Nullable String indicatorText, @NotNull CancellableTask cancellableTask) throws AzureCmdException {
        return null;
    }

    @Override
    public String getProperty(@NotNull String name) {
        return null;
    }

    @Override
    public String getProperty(@NotNull String name, Object projectObject) {
        return null;
    }

    @Override
    public String getPropertyWithDefault(@NotNull String name, @NotNull String defaultValue) {
        return null;
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value) {
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value, Object projectObject) {
    }

    @Override
    public void unsetProperty(@NotNull String name) {
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return false;
    }

    @Override
    public void unsetProperty(@NotNull String name, Object projectObject) {

    }

    @Override
    public String[] getProperties(@NotNull String name) {
        return new String[0];
    }

    @Override
    public String[] getProperties(@NotNull String name, Object projectObject) {
        return new String[0];
    }

    @Override
    public void setProperties(@NotNull String name, @NotNull String[] value) {
    }

    @Override
    public List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor) throws AzureCmdException {
        return null;
    }

    @Override
    public ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor, @NotNull ArtifactDescriptor artifactDescriptor) {
        return null;
    }

    @Override
    public BrowserLauncher getBrowserLauncher() {
        return null;
    }

    @NotNull
    @Override
    public Object getCurrentProject() {
        return AzureManagerImpl.DEFAULT_PROJECT;
    }
}
