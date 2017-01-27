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

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.SubscriptionManagerPersist;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.credentials.ServiceClientCredentials;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ServicePrincipalAzureManager extends AzureManagerBase {

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
        Azure.Configurable azureConfigurable = Azure.configure().withUserAgent(CommonSettings.USER_AGENT);
        return (atc == null)
                ? azureConfigurable.authenticate(credFile)
                : azureConfigurable.authenticate(atc);
    }

    public Azure getAzure(String sid) throws Exception {
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        Azure azure = auth().withSubscription(sid);
        sidToAzureMap.put(sid, azure);
        return azure;
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

    @Override
    public KeyVaultClient getKeyVaultClient(String tid) throws Exception {
        ServiceClientCredentials creds = new KeyVaultCredentials() {
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                try {
                    ExecutorService service = null;
                    AuthenticationResult authenticationResult;
                    try {
                        ApplicationTokenCredentials credentials = (atc == null)
                            ? ApplicationTokenCredentials.fromFile(credFile)
                            : atc;

                        service = Executors.newFixedThreadPool(1);
                        AuthenticationContext context = new AuthenticationContext(authorization, false, service);
                        ClientCredential clientCredential = new ClientCredential(credentials.getClientId(), credentials.getSecret());
                        Future<AuthenticationResult> future = context.acquireToken(resource, clientCredential,null);
                        authenticationResult = future.get();
                    } finally {
                        if (service != null) {
                            service.shutdown();
                        }
                    }

                    return authenticationResult.getAccessToken();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        return new KeyVaultClient(creds);
    }

    @Override
    public String getCurrentUserId() throws  Exception{
        ApplicationTokenCredentials credentials = (atc == null)
            ? ApplicationTokenCredentials.fromFile(credFile)
            : atc;

        return credentials.getClientId();
    }
}
