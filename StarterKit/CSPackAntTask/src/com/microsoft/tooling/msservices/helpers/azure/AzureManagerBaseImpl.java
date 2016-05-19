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

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.tooling.msservices.components.AppSettingsNames;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.OpenSSLHelper;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.auth.AADManager;
import com.microsoft.tooling.msservices.helpers.auth.UserInfo;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.serviceexplorer.EventHelper;
import sun.misc.BASE64Decoder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class AzureManagerBaseImpl {
    public static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";

    @NotNull
    protected Subscription getSubscription(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                return subscriptions.get(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    static class EventWaitHandleImpl implements EventHelper.EventWaitHandle {
        Semaphore eventSignal = new Semaphore(0, true);

        @Override
        public void waitEvent(@NotNull Runnable callback)
                throws AzureCmdException {
            try {
                eventSignal.acquire();
                callback.run();
            } catch (InterruptedException e) {
                throw new AzureCmdException("Unable to aquire permit", e);
            }
        }

        synchronized void signalEvent() {
            if (eventSignal.availablePermits() == 0) {
                eventSignal.release();
            }
        }
    }

    static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();;

    Map<String, Subscription> subscriptions;
    Map<String, ReentrantReadWriteLock> lockBySubscriptionId = new HashMap<String, ReentrantReadWriteLock>();
    Map<String, UserInfo> userInfoBySubscriptionId;
    Map<String, SSLSocketFactory> sslSocketFactoryBySubscriptionId;
    ReentrantReadWriteLock subscriptionMapLock = new ReentrantReadWriteLock(false);
    Map<UserInfo, String> accessTokenByUser;
    private ReentrantReadWriteLock userMapLock = new ReentrantReadWriteLock(false);
    Map<UserInfo, ReentrantReadWriteLock> lockByUser;
    AADManager aadManager;
    ReentrantReadWriteLock authDataLock = new ReentrantReadWriteLock(false);
    UserInfo userInfo;
    Set<EventWaitHandleImpl> subscriptionsChangedHandles;

    Object projectObject;

    protected AzureManagerBaseImpl(Object projectObject) {
        this.projectObject = projectObject;
    }

    protected void loadUserInfo() {
        String json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.AZURE_USER_INFO, projectObject);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                userInfo = gson.fromJson(json, UserInfo.class);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_INFO, projectObject);
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS, projectObject);
            }
        } else {
            DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS, projectObject);
        }

        json = DefaultLoader.getIdeHelper().getProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS, projectObject);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type userInfoBySubscriptionIdType = new TypeToken<HashMap<String, UserInfo>>() {
                }.getType();
                userInfoBySubscriptionId = gson.fromJson(json, userInfoBySubscriptionIdType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS, projectObject);
            }
        } else {
            userInfoBySubscriptionId = new HashMap<String, UserInfo>();
        }
    }

    @Nullable
    public UserInfo getUserInfo() {
        authDataLock.readLock().lock();

        try {
            return userInfo;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    public UserInfo getUserInfo(@NotNull String subscriptionId)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                if (!userInfoBySubscriptionId.containsKey(subscriptionId)) {
                    throw new AzureCmdException("No User Information for the specified Subscription Id");
                }

                return userInfoBySubscriptionId.get(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    public String getAccessToken(String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, false);
            subscriptionLock.readLock().lock();

            try {
                if (!userInfoBySubscriptionId.containsKey(subscriptionId)) {
                    return "";
                }

                UserInfo userInfo = userInfoBySubscriptionId.get(subscriptionId);
                return getAccessToken(userInfo);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } catch (AzureCmdException ex) {
            // return empty string
            return "";
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    String getAccessToken(@NotNull UserInfo userInfo)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock userLock = getUserLock(userInfo, false);
            userLock.readLock().lock();

            try {
                if (!accessTokenByUser.containsKey(userInfo)) {
                    throw new AzureCmdException("No access token for the specified User Information", "");
                }

                return accessTokenByUser.get(userInfo);
            } finally {
                userLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    ReentrantReadWriteLock getSubscriptionLock(@NotNull String subscriptionId, boolean createOnMissing)
            throws AzureCmdException {
        Lock lock = createOnMissing ? subscriptionMapLock.writeLock() : subscriptionMapLock.readLock();
        lock.lock();

        try {
            if (!lockBySubscriptionId.containsKey(subscriptionId)) {
                if (createOnMissing) {
                    lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
                } else {
                    throw new AzureCmdException("No authentication information for the specified Subscription Id");
                }
            }

            return lockBySubscriptionId.get(subscriptionId);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    Optional<ReentrantReadWriteLock> getUserLock(@NotNull UserInfo userInfo) {
        userMapLock.readLock().lock();

        try {
            if (lockByUser.containsKey(userInfo)) {
                return Optional.of(lockByUser.get(userInfo));
            } else {
                return Optional.absent();
            }
        } finally {
            userMapLock.readLock().unlock();
        }
    }

    @NotNull
    ReentrantReadWriteLock getUserLock(@NotNull UserInfo userInfo, boolean createOnMissing)
            throws AzureCmdException {
        Lock lock = createOnMissing ? userMapLock.writeLock() : userMapLock.readLock();
        lock.lock();

        try {
            if (!lockByUser.containsKey(userInfo)) {
                if (createOnMissing) {
                    lockByUser.put(userInfo, new ReentrantReadWriteLock(false));
                } else {
                    throw new AzureCmdException("No access token for the specified User Information");
                }
            }

            return lockByUser.get(userInfo);
        } finally {
            lock.unlock();
        }
    }

    protected boolean hasAccessToken(@NotNull UserInfo userInfo) {
        authDataLock.readLock().lock();

        try {
            Optional<ReentrantReadWriteLock> optionalRWLock = getUserLock(userInfo);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReadWriteLock userLock = optionalRWLock.get();
            userLock.readLock().lock();

            try {
                return accessTokenByUser.containsKey(userInfo);
            } finally {
                userLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    protected void setAccessToken(@NotNull UserInfo userInfo,
                                  @NotNull String accessToken)
            throws AzureCmdException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
            userLock.writeLock().lock();

            try {
                accessTokenByUser.put(userInfo, accessToken);
            } finally {
                userLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
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

    protected void loadSSLSocketFactory() {
        sslSocketFactoryBySubscriptionId = new HashMap<String, SSLSocketFactory>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            Subscription subscription = subscriptionEntry.getValue();
            String managementCertificate = subscription.getManagementCertificate();

            if (!StringHelper.isNullOrWhiteSpace(managementCertificate)) {
                try {
                    SSLSocketFactory sslSocketFactory = initSSLSocketFactory(managementCertificate);
                    sslSocketFactoryBySubscriptionId.put(subscriptionId, sslSocketFactory);
                } catch (Exception e) {
                    subscription.setManagementCertificate(null);
                }
            }
        }
    }

    protected void removeInvalidUserInfo() {
        List<String> invalidSubscriptionIds = new ArrayList<String>();

        for (String subscriptionId : userInfoBySubscriptionId.keySet()) {
            if (!subscriptions.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            userInfoBySubscriptionId.remove(invalidSubscriptionId);
        }
    }

    protected void removeUnusedSubscriptions() {
        List<String> invalidSubscriptionIds = new ArrayList<String>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            Subscription subscription = subscriptionEntry.getValue();

            if (!userInfoBySubscriptionId.containsKey(subscriptionId) &&
                    !sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
            } else if (!userInfoBySubscriptionId.containsKey(subscriptionId)) {
                subscription.setTenantId(null);
            } else if (!sslSocketFactoryBySubscriptionId.containsKey(subscriptionId)) {
                subscription.setManagementCertificate(null);
                subscription.setServiceManagementUrl(null);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            lockBySubscriptionId.remove(invalidSubscriptionId);
            subscriptions.remove(invalidSubscriptionId);
        }
    }

    protected void storeSubscriptions() {
        Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
        }.getType();
        String json = gson.toJson(subscriptions, subscriptionsType);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_SUBSCRIPTIONS, json, projectObject);
    }

    protected void storeUserInfo() {
        String json = gson.toJson(userInfo, UserInfo.class);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_USER_INFO, json, projectObject);

        Type userInfoBySubscriptionIdType = new TypeToken<HashMap<String, UserInfo>>() {
        }.getType();
        json = gson.toJson(userInfoBySubscriptionId, userInfoBySubscriptionIdType);
        DefaultLoader.getIdeHelper().setProperty(AppSettingsNames.AZURE_USER_SUBSCRIPTIONS, json, projectObject);
    }

    protected SSLSocketFactory initSSLSocketFactory(@NotNull String managementCertificate)
            throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        byte[] decodeBuffer = new BASE64Decoder().decodeBuffer(managementCertificate);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");

        InputStream is = new ByteArrayInputStream(decodeBuffer);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, OpenSSLHelper.PASSWORD.toCharArray());
        keyManagerFactory.init(ks, OpenSSLHelper.PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        return sslContext.getSocketFactory();
    }
}
