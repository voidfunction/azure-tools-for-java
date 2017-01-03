package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.utils.Pair;

import java.util.List;

public interface AzureManager {
    Azure getAzure(String sid) throws Exception;
    List<Subscription> getSubscriptions() throws Throwable;
    List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() throws Exception;
    Settings getSettings();
    SubscriptionManager getSubscriptionManager();
    void drop() throws Exception;
//    public List<Tenant> getTenants() throws Throwable;
}
