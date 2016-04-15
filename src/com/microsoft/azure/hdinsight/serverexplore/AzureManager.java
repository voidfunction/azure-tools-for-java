package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.sdk.subscription.Subscription;
import com.microsoft.azure.hdinsight.serverexplore.node.EventHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AzureManager {
    void authenticate() throws HDExploreException;

    boolean authenticated();

    boolean authenticated(@NotNull String subscriptionId);

    @Nullable
    UserInfo getUserInfo();

    void clearAuthentication();

    @NotNull
    List<Subscription> getFullSubscriptionList();

    @NotNull
    List<Subscription> getSubscriptionList();

    void setSelectedSubscriptions(@NotNull List<String> selectedList)
            throws HDExploreException;

    @NotNull
    EventHelper.EventWaitHandle registerSubscriptionsChanged()
            throws HDExploreException;

    void unregisterSubscriptionsChanged(@NotNull EventHelper.EventWaitHandle handle)
            throws HDExploreException;

}
