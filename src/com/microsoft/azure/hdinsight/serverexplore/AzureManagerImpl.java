package com.microsoft.azure.hdinsight.serverexplore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.PluginUtil;
import com.microsoft.azure.hdinsight.common.StringHelper;
import com.microsoft.azure.hdinsight.common.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.common.CommonConstant;
import com.microsoft.azure.hdinsight.sdk.subscription.Subscription;
import com.microsoft.azure.hdinsight.sdk.subscription.SubscriptionManager;
import com.microsoft.azure.hdinsight.sdk.subscription.Tenant;
import com.microsoft.azure.hdinsight.serverexplore.node.EventHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AzureManagerImpl implements AzureManager {

    private static class EventWaitHandleImpl implements EventHelper.EventWaitHandle {
        Semaphore eventSignal = new Semaphore(0, true);

        @Override
        public void waitEvent(@NotNull Runnable callback)
                throws HDExploreException {
            try {
                eventSignal.acquire();
                callback.run();
            } catch (InterruptedException e) {
                throw new HDExploreException("Unable to aquire permit", e);
            }
        }

        private synchronized void signalEvent() {
            if (eventSignal.availablePermits() == 0) {
                eventSignal.release();
            }
        }
    }

    private static AzureManager instance;
    private static Gson gson;

    private AADManager aadManager;

    private ReentrantReadWriteLock authDataLock = new ReentrantReadWriteLock(false);
    private Map<String, Subscription> subscriptions;
    private UserInfo userInfo;

    private ReentrantReadWriteLock subscriptionMapLock = new ReentrantReadWriteLock(false);
    private Map<String, ReentrantReadWriteLock> lockBySubscriptionId;
    private Map<String, UserInfo> userInfoBySubscriptionId;

    private ReentrantReadWriteLock userMapLock = new ReentrantReadWriteLock(false);
    private Map<UserInfo, ReentrantReadWriteLock> lockByUser;
    private Map<UserInfo, String> accessTokenByUser;

    private ReentrantReadWriteLock subscriptionsChangedLock = new ReentrantReadWriteLock(true);
    private Set<EventWaitHandleImpl> subscriptionsChangedHandles;

    private AzureManagerImpl() {
        authDataLock.writeLock().lock();

        try {
            aadManager = AADManagerImpl.getManager();
            loadSubscriptions();
            loadUserInfo();

            removeInvalidUserInfo();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();

            accessTokenByUser = new HashMap<>();
            lockByUser = new HashMap<>();
            subscriptionsChangedHandles = new HashSet<>();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    @NotNull
    public static synchronized AzureManager getManager() {
        if (instance == null) {
            gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            instance = new AzureManagerImpl();
        }

        return instance;
    }

    @Override
    public void authenticate() throws HDExploreException {
        final String managementUri = CommonConstant.managementUri;

        final UserInfo userInfo = aadManager.authenticate(managementUri, "Sign in to your Azure account");
        setUserInfo(userInfo);

        List<Tenant> tenantList = requestWithToken(userInfo, new AzureManagerRequestCallback<List<Tenant>>() {
            @Override
            public List<Tenant> execute() throws Exception {
                String accessToken = getAccessToken(userInfo);
                return SubscriptionManager.getInstance().getTenants(accessToken);
            }
        });

        List<Subscription> allSubscriptions = new ArrayList<>();
        for (Tenant tenant : tenantList) {
            final UserInfo tenantUser = new UserInfo(tenant.getTenantId(), userInfo.getUniqueName());
            List<Subscription> subscriptionList = requestWithToken(tenantUser, new AzureManagerRequestCallback<List<Subscription>>() {
                @Override
                public List<Subscription> execute() throws Exception {
                    String tenantAccessToken = getAccessToken(tenantUser);
                    return SubscriptionManager.getInstance().getSubscriptions(tenantAccessToken);
                }
            });
            allSubscriptions.addAll(subscriptionList);
        }

        for(Subscription subscription : allSubscriptions){
            authDataLock.readLock().lock();
            try {
                String subscriptionId = subscription.getSubscriptionId();
                ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
                subscriptionLock.writeLock().lock();

                try {
                    subscriptions.put(subscriptionId, subscription);
                    setUserInfo(subscriptionId, userInfo);
                    storeSubscriptions();
                } finally {
                    subscriptionLock.writeLock().unlock();
                }
            } finally {
                authDataLock.readLock().unlock();
            }
        }
    }

    @Override
    public boolean authenticated() {
        return getUserInfo() != null;
    }

    @Override
    public boolean authenticated(@NotNull String subscriptionId) {
        return hasUserInfo(subscriptionId);
    }

    @Override
    public void clearAuthentication() {
        setUserInfo(null);
    }

    @NotNull
    @Override
    public List<Subscription> getFullSubscriptionList() {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<>();

            for (Subscription subscription : subscriptions.values()) {
                result.add(subscription);
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @NotNull
    @Override
    public List<Subscription> getSubscriptionList() {
        authDataLock.readLock().lock();

        try {
            List<Subscription> result = new ArrayList<>();

            for (Subscription subscription : subscriptions.values()) {
                if (subscription.isSelected()) {
                    result.add(subscription);
                }
            }

            return result;
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    @Override
    public void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws HDExploreException {
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

        notifySubscriptionsChanged();
    }

    @NotNull
    @Override
    public EventHelper.EventWaitHandle registerSubscriptionsChanged()
            throws HDExploreException {
        subscriptionsChangedLock.writeLock().lock();

        try {
            EventWaitHandleImpl handle = new EventWaitHandleImpl();

            subscriptionsChangedHandles.add(handle);

            return handle;
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }
    }

    @Override
    public void unregisterSubscriptionsChanged(@NotNull EventHelper.EventWaitHandle handle)
            throws HDExploreException {
        if (!(handle instanceof EventWaitHandleImpl)) {
            throw new HDExploreException("Invalid handle instance");
        }

        subscriptionsChangedLock.writeLock().lock();

        try {
            subscriptionsChangedHandles.remove(handle);
        } finally {
            subscriptionsChangedLock.writeLock().unlock();
        }

        ((EventWaitHandleImpl) handle).signalEvent();
    }

    private void loadSubscriptions() {
        String json = DefaultLoader.getIdeHelper().getProperty(CommonConst.AZURE_SUBSCRIPTIONS);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
                }.getType();
                subscriptions = gson.fromJson(json, subscriptionsType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(CommonConst.AZURE_SUBSCRIPTIONS);
            }
        } else {
            subscriptions = new HashMap<>();
        }

        lockBySubscriptionId = new HashMap<>();

        for (String subscriptionId : subscriptions.keySet()) {
            lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
        }
    }


    private void loadUserInfo() {
        String json = DefaultLoader.getIdeHelper().getProperty(CommonConst.AZURE_USER_INFO);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                userInfo = gson.fromJson(json, UserInfo.class);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(CommonConst.AZURE_USER_INFO);
                DefaultLoader.getIdeHelper().unsetProperty(CommonConst.AZURE_USER_SUBSCRIPTIONS);
            }
        } else {
            DefaultLoader.getIdeHelper().unsetProperty(CommonConst.AZURE_USER_SUBSCRIPTIONS);
        }

        json = DefaultLoader.getIdeHelper().getProperty(CommonConst.AZURE_USER_SUBSCRIPTIONS);

        if (!StringHelper.isNullOrWhiteSpace(json)) {
            try {
                Type userInfoBySubscriptionIdType = new TypeToken<HashMap<String, UserInfo>>() {
                }.getType();
                userInfoBySubscriptionId = gson.fromJson(json, userInfoBySubscriptionIdType);
            } catch (JsonSyntaxException ignored) {
                DefaultLoader.getIdeHelper().unsetProperty(CommonConst.AZURE_USER_SUBSCRIPTIONS);
            }
        } else {
            userInfoBySubscriptionId = new HashMap<>();
        }
    }

    private void removeInvalidUserInfo() {
        List<String> invalidSubscriptionIds = new ArrayList<>();

        for (String subscriptionId : userInfoBySubscriptionId.keySet()) {
            if (!subscriptions.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            userInfoBySubscriptionId.remove(invalidSubscriptionId);
        }
    }

    private void removeUnusedSubscriptions() {
        List<String> invalidSubscriptionIds = new ArrayList<>();

        for (Map.Entry<String, Subscription> subscriptionEntry : subscriptions.entrySet()) {
            String subscriptionId = subscriptionEntry.getKey();
            if (!userInfoBySubscriptionId.containsKey(subscriptionId)) {
                invalidSubscriptionIds.add(subscriptionId);
            }
        }

        for (String invalidSubscriptionId : invalidSubscriptionIds) {
            lockBySubscriptionId.remove(invalidSubscriptionId);
            subscriptions.remove(invalidSubscriptionId);
        }
    }

    private void storeSubscriptions() {
        Type subscriptionsType = new TypeToken<HashMap<String, Subscription>>() {
        }.getType();
        String json = gson.toJson(subscriptions, subscriptionsType);
        DefaultLoader.getIdeHelper().setProperty(CommonConst.AZURE_SUBSCRIPTIONS, json);
    }

    private void storeUserInfo() {
        String json = gson.toJson(userInfo, UserInfo.class);
        DefaultLoader.getIdeHelper().setProperty(CommonConst.AZURE_USER_INFO, json);

        Type userInfoBySubscriptionIdType = new TypeToken<HashMap<String, UserInfo>>() {
        }.getType();
        json = gson.toJson(userInfoBySubscriptionId, userInfoBySubscriptionIdType);
        DefaultLoader.getIdeHelper().setProperty(CommonConst.AZURE_USER_SUBSCRIPTIONS, json);
    }

    private void notifySubscriptionsChanged() {
        subscriptionsChangedLock.readLock().lock();

        try {
            for (EventWaitHandleImpl handle : subscriptionsChangedHandles) {
                handle.signalEvent();
            }
        } finally {
            subscriptionsChangedLock.readLock().unlock();
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

    private void setUserInfo(@Nullable UserInfo userInfo) {
        authDataLock.writeLock().lock();

        try {
            this.userInfo = userInfo;
            userInfoBySubscriptionId.clear();
            removeUnusedSubscriptions();

            storeSubscriptions();
            storeUserInfo();
        } finally {
            authDataLock.writeLock().unlock();
        }
    }

    private void setUserInfo(@NotNull String subscriptionId, @NotNull UserInfo userInfo)
            throws HDExploreException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock subscriptionLock = getSubscriptionLock(subscriptionId, true);
            subscriptionLock.writeLock().lock();

            try {
                userInfoBySubscriptionId.put(subscriptionId, userInfo);

                storeUserInfo();
            } finally {
                subscriptionLock.writeLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasUserInfo(@NotNull String subscriptionId) {
        authDataLock.readLock().lock();

        try {
            com.google.common.base.Optional<ReentrantReadWriteLock> optionalRWLock = getSubscriptionLock(subscriptionId);

            if (!optionalRWLock.isPresent()) {
                return false;
            }

            ReentrantReadWriteLock subscriptionLock = optionalRWLock.get();
            subscriptionLock.readLock().lock();

            try {
                return userInfoBySubscriptionId.containsKey(subscriptionId);
            } finally {
                subscriptionLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private boolean hasAccessToken(@NotNull UserInfo userInfo) {
        authDataLock.readLock().lock();

        try {
            com.google.common.base.Optional<ReentrantReadWriteLock> optionalRWLock = getUserLock(userInfo);

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

    @NotNull
    private String getAccessToken(@NotNull UserInfo userInfo)
            throws HDExploreException {
        authDataLock.readLock().lock();

        try {
            ReentrantReadWriteLock userLock = getUserLock(userInfo, false);
            userLock.readLock().lock();

            try {
                if (!accessTokenByUser.containsKey(userInfo)) {
                    throw new HDExploreException("No access token for the specified User Information", "");
                }

                return accessTokenByUser.get(userInfo);
            } finally {
                userLock.readLock().unlock();
            }
        } finally {
            authDataLock.readLock().unlock();
        }
    }

    private void setAccessToken(@NotNull UserInfo userInfo,
                                @NotNull String accessToken)
            throws HDExploreException {
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

    @NotNull
    private com.google.common.base.Optional<ReentrantReadWriteLock> getSubscriptionLock(@NotNull String subscriptionId) {
        subscriptionMapLock.readLock().lock();

        try {
            if (lockBySubscriptionId.containsKey(subscriptionId)) {
                return com.google.common.base.Optional.of(lockBySubscriptionId.get(subscriptionId));
            } else {
                return com.google.common.base.Optional.absent();
            }
        } finally {
            subscriptionMapLock.readLock().unlock();
        }
    }

    @NotNull
    private ReentrantReadWriteLock getSubscriptionLock(@NotNull String subscriptionId, boolean createOnMissing)
            throws HDExploreException {
        Lock lock = createOnMissing ? subscriptionMapLock.writeLock() : subscriptionMapLock.readLock();
        lock.lock();

        try {
            if (!lockBySubscriptionId.containsKey(subscriptionId)) {
                if (createOnMissing) {
                    lockBySubscriptionId.put(subscriptionId, new ReentrantReadWriteLock(false));
                } else {
                    throw new HDExploreException("No authentication information for the specified Subscription Id");
                }
            }

            return lockBySubscriptionId.get(subscriptionId);
        } finally {
            lock.unlock();
        }
    }


    @NotNull
    private ReentrantReadWriteLock getUserLock(@NotNull UserInfo userInfo, boolean createOnMissing)
            throws HDExploreException {
        Lock lock = createOnMissing ? userMapLock.writeLock() : userMapLock.readLock();
        lock.lock();

        try {
            if (!lockByUser.containsKey(userInfo)) {
                if (createOnMissing) {
                    lockByUser.put(userInfo, new ReentrantReadWriteLock(false));
                } else {
                    throw new HDExploreException("No access token for the specified User Information");
                }
            }

            return lockByUser.get(userInfo);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    private com.google.common.base.Optional<ReentrantReadWriteLock> getUserLock(@NotNull UserInfo userInfo) {
        userMapLock.readLock().lock();

        try {
            if (lockByUser.containsKey(userInfo)) {
                return com.google.common.base.Optional.of(lockByUser.get(userInfo));
            } else {
                return com.google.common.base.Optional.absent();
            }
        } finally {
            userMapLock.readLock().unlock();
        }
    }

    @NotNull
    private <T> T requestWithToken(@NotNull final UserInfo userInfo, @NotNull final AzureManagerRequestCallback<T> azureManagerRequestCallback)
            throws HDExploreException {
                AADManagerRequestCallback<T> aadRequestCB = new AADManagerRequestCallback<T>() {
                    @NotNull
                    @Override
                    public T execute(@NotNull String accessToken) throws Exception {
                        if (!hasAccessToken(userInfo) ||
                                !accessToken.equals(getAccessToken(userInfo))) {
                            ReentrantReadWriteLock userLock = getUserLock(userInfo, true);
                            userLock.writeLock().lock();

                            try {
                                if (!hasAccessToken(userInfo) ||
                                        !accessToken.equals(getAccessToken(userInfo))) {
                                    setAccessToken(userInfo, accessToken);
                                }
                            } finally {
                                userLock.writeLock().unlock();
                            }
                        }

                        return azureManagerRequestCallback.execute();
                    }
                };

        return aadManager.request(userInfo,
                CommonConstant.managementUri,
                "Sign in to your Azure account",
                aadRequestCB);
    }
}