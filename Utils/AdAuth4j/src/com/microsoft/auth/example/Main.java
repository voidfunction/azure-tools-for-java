package com.microsoft.auth.example;

import com.microsoft.auth.*;
import com.microsoft.auth.subsriptions.Subscription;
import com.microsoft.auth.subsriptions.SubscriptionsClient;
import com.microsoft.auth.tenants.Tenant;
import com.microsoft.auth.tenants.Tenants;
import com.microsoft.auth.tenants.TenantsClient;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.ResourceManagementService;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.azure.management.resources.models.ResourceGroupListResult;
import com.microsoft.azure.management.websites.WebSiteManagementClient;
import com.microsoft.azure.management.websites.WebSiteManagementService;
import com.microsoft.azure.management.websites.models.WebSite;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shch on 4/22/2016.
 */
public class Main {
    final static String authority = "https://login.windows.net";
    final static String tenant = "common";
    final static String resource = "https://management.core.windows.net/";
    final static String clientId = "61d65f5a-6e3b-468b-af73-a033f5098c5c";
    final static String redirectUri = "https://msopentech.com/";

    public static void main(String[] args) {
        try {

            final TokenFileStorage tokenFileStorage = new TokenFileStorage();

            final TokenCache cache = new TokenCache();

            cache.setOnBeforeAccessCallback(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] data = tokenFileStorage.read();
                        cache.deserialize(data);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            cache.setOnAfterAccessCallback(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(cache.getHasStateChanged()) {
                            tokenFileStorage.write(cache.serialize());
                            cache.setHasStateChanged(false);
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            });

            final URI baseUri = new URI("https://management.azure.com/");

            Runnable worker = new Runnable() {
                @Override
                public void run() {
                    try {
                        getDataFromAzure(cache, baseUri);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            };

            //for (int i = 0; i < 16; ++i) {

               // new Thread(worker).start();
               // new Thread(worker).start();
                worker.run();
            //}

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.exit(1);
        }
    }

    private static void getDataFromAzure(TokenCache cache, URI baseUri) throws Exception{

        AuthContext ac = new AuthContext(String.format("%s/%s", authority, tenant), cache);
        AuthenticationResult result = ac.acquireTokenAsync(resource, clientId, redirectUri, PromptBehavior.Auto, null).get();
        System.out.println("token: " + result.getAccessToken());

        List<Tenant> tenants = TenantsClient.getByToken(result.getAccessToken());
        for (Tenant t : tenants) {
            String tid = t.getTenantId();
            System.out.println(String.format("\n======> tenantId: %s ======================\n", tid));

            AuthContext ac1 = new AuthContext(String.format("%s/%s", authority, tid), cache);
            AuthenticationResult result1 = ac1.acquireTokenAsync(resource, clientId, redirectUri, PromptBehavior.Auto, null).get();
            System.out.println("token: " + result1.getAccessToken());

            System.out.println("\n=== Subscriptions:");
            List<Subscription> subscriptions = SubscriptionsClient.getByToken(result1.getAccessToken());
            for (Subscription s : subscriptions) {
                        String sid = s.getSubscriptionId().toString();
                        System.out.println(String.format("\t======> %s: %s ======================", s.getSubscriptionName(), sid ));
    ////                    listSitesForSubscription(baseUri, sid, result.getAccessToken());
    //                    System.out.println();
            }

        }


    }

    private static void listSitesForSubscription(URI baseUri, String sid, String token) {

        try {
            Configuration config  = ManagementConfiguration.configure(
                    null,
                    Configuration.getInstance(),
                    baseUri,
                    sid,
                    token);
            ResourceManagementClient resourceManagementClient = ResourceManagementService.create(config);

            ResourceGroupListResult rgr = resourceManagementClient.getResourceGroupsOperations().list(null);
            WebSiteManagementClient webSiteManagementClient = WebSiteManagementService.create(config);
            List<ResourceGroupExtended> groups = resourceManagementClient.getResourceGroupsOperations().list(null).getResourceGroups();

            for(ResourceGroupExtended rge : groups) {
                System.out.println("Resource group name : " + rge.getName());
                ArrayList<WebSite> webSites =  webSiteManagementClient.getWebSitesOperations().list(rge.getName(), null, null).getWebSites();
                for(WebSite ws : webSites) {
                    System.out.println("\tWeb site name : " + ws.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
