package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.Azure;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.SubscriptionManagerPersist;
import com.microsoft.azuretools.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ServicePrincipalAzureManager implements AzureManager {

    private static Settings settings;
    private final SubscriptionManager subscriptionManager;
    private final File credFile;
    private final ApplicationTokenCredentials atc;

    static {
        settings = new Settings();
        settings.setSubscriptionsDetailsFileName("subscriptionsDetails-sp.json");
    }

    public static void cleanPersist() throws Exception {
        String subscriptionsDetailsFileName = settings.getSubscriptionsDetailsFileName();
        SubscriptionManagerPersist.deleteSubscriptions(subscriptionsDetailsFileName);
    }

    public ServicePrincipalAzureManager(String tid, String appId, String appKey) {
        this.credFile = null;
        this.atc = new ApplicationTokenCredentials(appId, tid, appKey, null);
        this.subscriptionManager = new SubscriptionManagerPersist(this);
    }

    public ServicePrincipalAzureManager(File credFile) {
        this.credFile = credFile;
        this.atc = null;
        this.subscriptionManager = new SubscriptionManagerPersist(this);
    }

    private Azure.Authenticated auth() throws IOException {
        return (atc == null)
                ? Azure.configure().authenticate(credFile)
                : Azure.configure().authenticate(atc);
    }

    public Azure getAzure(String sid) throws Exception {
        return auth().withSubscription(sid);
    }

    public List<Subscription> getSubscriptions() throws Exception {
        List<Subscription> sl = auth().subscriptions().list();
        return sl;
    }

    @Override
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() throws Exception {
        List<Pair<Subscription, Tenant>> stl = new LinkedList<>();
        for (Tenant t : getTenants()) {
            //String tid = t.tenantId();
            try {
                for (Subscription s : getSubscriptions()) {
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

    @Override
    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    @Override
    public void drop() throws Exception {
        System.out.println("ServicePrincipalAzureManager.drop()");
        subscriptionManager.cleanSubscriptions();
    }

    public List<Tenant> getTenants() throws Exception {
        List<Tenant> tl = auth().tenants().list();
        return tl;
    }
}
