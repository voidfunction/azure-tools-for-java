/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

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
    private Set<ISubscriptionSelectionListener> listners = new HashSet<>();
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
        if (subscriptionDetails.isEmpty()) {
            updateAccountSubscriptionList();
        }
        return subscriptionDetails;
    }

    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) throws Exception {
        System.out.println("getSubscriptionDetails()");
        this.subscriptionDetails = subscriptionDetails;
//        saveSubscriptions(subscriptionDetails);
        updateSidToTidMap();
        notifyAllListeners();
    }

    public void addListener(ISubscriptionSelectionListener l) {
        if (!listners.contains(l)) {
            listners.add(l);
        }
    }

    public void notifyAllListeners() {
        for (ISubscriptionSelectionListener l : listners) {
            l.update(subscriptionDetails.isEmpty());
        }
    }

    public String getSubscriptionTenant(String sid) throws Exception {
        if (!sidToTid.containsKey(sid)) {
            updateSidToTidMap();
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
            updateSidToTidMap();
        }
        return sidToTid.keySet();
    }

    public void cleanSubscriptions() throws Exception {
        System.out.println("cleanSubscriptions()");
        subscriptionDetails.clear();
        sidToTid.clear();
        notifyAllListeners();
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

//        if (!subscriptionDetails.isEmpty()) {
//            updateSidToTidMap();
//            return;
//        }

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
