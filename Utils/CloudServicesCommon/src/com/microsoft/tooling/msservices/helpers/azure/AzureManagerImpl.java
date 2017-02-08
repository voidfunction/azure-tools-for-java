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
package com.microsoft.tooling.msservices.helpers.azure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.tooling.msservices.components.AppSettingsNames;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.model.Subscription;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AzureManagerImpl extends AzureManager {
    static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

    public AzureManagerImpl(Object projectObject) {
        super(projectObject);
    }

    public AzureManagerImpl() {
        this(DEFAULT_PROJECT);
    }

    @NotNull
    public static synchronized AzureManagerImpl getManager() {
        return getManager(DEFAULT_PROJECT);
    }

    @NotNull
    public static synchronized AzureManagerImpl getManager(Object currentProject) {
        if (currentProject == null) {
            currentProject = DEFAULT_PROJECT;
        }
        if (instances.get(currentProject) == null) {
            AzureManager instance = new AzureManagerImpl(currentProject);
            instances.put(currentProject, instance);
        }
        return (AzureManagerImpl) instances.get(currentProject);
    }

    /**
     * Because different IntelliJ windows share same static class information, need to associate
     */
    public static synchronized void initAzureManager(Object projectObject) {
        if (instances.get(projectObject) == null) {
            AzureManager instance = new AzureManagerImpl(projectObject);
            instances.put(projectObject, instance);
        }
    }

    protected void storeSubscriptions() {
        Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
        }.getType();
        String json = gson.toJson(subscriptions, subscriptionsType);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS, json, projectObject);
    }

    protected void loadSubscriptions() {
        String json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS, projectObject);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
                }.getType();
                subscriptions = gson.fromJson(json, subscriptionsType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS, projectObject);
            }
        } else {
            subscriptions = new HashMap<String, Subscription>();
        }

        for (String subscriptionId : subscriptions.keySet()) {
            lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
        }
    }

    public void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws AzureCmdException {
        authDataLock.writeLock().lock();

        try {
            for (String subscriptionId : subscriptions.keySet()) {
                Subscription subscription = subscriptions.get(subscriptionId);
                subscription.setSelected(selectedList.contains(subscriptionId));
            }

            storeSubscriptions();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    public void executeOnPooledThread(@NotNull Runnable runnable) {
        DefaultLoader.getIdeHelper().executeOnPooledThread(runnable);
    }
}
