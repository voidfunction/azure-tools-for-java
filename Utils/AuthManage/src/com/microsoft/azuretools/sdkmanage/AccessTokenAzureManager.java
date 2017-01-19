package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.credentials.TokenCredentials;

import java.util.LinkedList;
import java.util.List;

public class AccessTokenAzureManager implements AzureManager {

    private final SubscriptionManager subscriptionManager;

    public AccessTokenAzureManager() {
        this.subscriptionManager = new SubscriptionManager(this);
    }

    @Override
    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    @Override
    public void drop() throws Exception {
        subscriptionManager.cleanSubscriptions();
        AdAuthManager.getInstance().signOut();
    }

    private static Settings settings;

    static {
        settings = new Settings();
        settings.setSubscriptionsDetailsFileName("subscriptionsDetails-at.json");
    }

    @Override
    public Azure getAzure(String sid) throws Exception {
        String tid = subscriptionManager.getSubscriptionTenant(sid);
        Azure azure = authTid(tid).withSubscription(sid);
        return azure;
    }

    @Override
    public List<Subscription> getSubscriptions() throws Exception {
        List<Subscription> sl = new LinkedList<Subscription>();
        // could be multi tenant - return all subscriptions for the current account
        List<Tenant> tl = getTenants("common");
        for (Tenant t : tl) {
            try {
                sl.addAll(getSubscriptions(t.tenantId()));

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return sl;
    }

    @Override
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() throws Exception {
        List<Pair<Subscription, Tenant>> stl = new LinkedList<>();
        for (Tenant t : getTenants("common")) {
            String tid = t.tenantId();
            try {
                for (Subscription s : getSubscriptions(tid)) {
                    stl.add(new Pair<Subscription, Tenant>(s, t));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return stl;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    public static List<Subscription> getSubscriptions(String tid) throws Exception {
        List<Subscription> sl = authTid(tid).subscriptions().list();
        return sl;
    }

    public static List<Tenant> getTenants(String tid) throws Exception {
        List<Tenant> tl = authTid(tid).tenants().list();
        return tl;
    }

//    public static Azure.Authenticated auth(String accessToken) throws Exception {
//        return Azure.configure().authenticate(getTokenCredentials(accessToken));
//    }

//    private static TokenCredentials getTokenCredentials(String token) throws Exception {
//        return null;
//    }

    private static Azure.Authenticated authTid(String tid) throws Exception {
//        String token = AdAuthManager.getInstance().getAccessToken(tid);
//        return auth(token);
        return Azure.configure().authenticate(new RefreshableTokenCredentials(AdAuthManager.getInstance(), tid));
    }
}