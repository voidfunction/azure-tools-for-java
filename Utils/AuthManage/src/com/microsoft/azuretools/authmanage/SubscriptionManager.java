package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.Pair;

import java.util.*;


/**
 * Created by shch on 10/3/2016.
 */
public class SubscriptionManager {
    protected AzureManager azureManager;


    // for user to select subscr to work with
    protected List<SubscriptionDetail> subscriptionDetails = new LinkedList<>();

    // to get tid for sid
    protected Map<String, String> sidToTid = new HashMap<String, String>();

    public SubscriptionManager(AzureManager azureManager) {
        this.azureManager = azureManager;
    }

    public List<SubscriptionDetail> getSubscriptionDetails() throws Exception {
        System.out.println("getSubscriptionDetails()");
        updateAccountSubscriptionList();
        return subscriptionDetails;
    }

    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) throws Exception {
        System.out.println("getSubscriptionDetails()");
        this.subscriptionDetails = subscriptionDetails;
//        saveSubscriptions(subscriptionDetails);
        updateSidToTidMap();
    }

//    public List<SubscriptionDetail> refreshSubscriptionDetails() throws Exception {
//        cleanSubscriptions();
//        return getSubscriptionDetails();
//    }
//
    public String getSubscriptionTenant(String sid) throws Exception {
        if (!sidToTid.containsKey(sid)) {
            updateAccountSubscriptionList();
            if (!sidToTid.containsKey(sid)) {
                throw new AuthException("sid was not found in the tenant: " +  sid);
            }
        }
        String tid = sidToTid.get(sid);
        return tid;
    }

    public Set<String> getAccountSidList() throws Exception {
        System.out.println("getAccountSidList()");
        if (sidToTid.isEmpty()) {
            updateAccountSubscriptionList();
        }
        return sidToTid.keySet();
    }

    public void cleanSubscriptions() throws Exception {
        System.out.println("cleanSubscriptions()");
        subscriptionDetails.clear();
        sidToTid.clear();
    }

    protected void updateSidToTidMap() {
        System.out.println("updateSidToTidMap()");
        sidToTid.clear();
        for (SubscriptionDetail sd : subscriptionDetails) {
            if (sd.isSelected())
                sidToTid.put(sd.getSubscriptionId(), sd.getTenantId());
        }
    }

    protected void updateAccountSubscriptionList() throws Exception {
        System.out.println("updateAccountSubscriptionList()");

        if (!subscriptionDetails.isEmpty()) {
            updateSidToTidMap();
            return;
        }

        if (azureManager == null) {
            throw new IllegalArgumentException("azureManager is null");
        }

        System.out.println("getting subscription list from Azure");
        List<Pair<Subscription, Tenant>> stpl = azureManager.getSubscriptionsWithTenant();
        for (Pair<Subscription, Tenant> stp : stpl) {
            subscriptionDetails.add(new SubscriptionDetail(
                    stp.first().subscriptionId(),
                    stp.first().displayName(),
                    stp.second().tenantId(),
                    true));
        }

        if (subscriptionDetails.isEmpty()) {
            throw new AuthException("No subscription found in the account");
        }
    }
}
