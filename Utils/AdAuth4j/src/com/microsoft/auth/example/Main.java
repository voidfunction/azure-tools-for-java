package com.microsoft.auth.example;

import com.microsoft.auth.*;
import com.microsoft.auth.subsriptions.Subscription;
import com.microsoft.auth.subsriptions.SubscriptionsClient;
import com.microsoft.auth.tenants.Tenant;
import com.microsoft.auth.tenants.TenantsClient;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.ResourceManagementService;
import com.microsoft.azure.management.resources.models.GenericResourceExtended;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.azure.management.resources.models.ResourceListParameters;
import com.microsoft.azure.management.storage.StorageManagementClient;
import com.microsoft.azure.management.storage.StorageManagementService;
import com.microsoft.azure.management.storage.models.StorageAccount;
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
    static String authority = "https://login.windows.net";
    static String tenant = "common";
    static String resource = "https://management.core.windows.net/";
    static String clientId = "61d65f5a-6e3b-468b-af73-a033f5098c5c";
    static String redirectUri = "https://msopentech.com/";

    public static void main(String[] args) {
        try {

            final TokenFileStorage tokenFileStorage = new TokenFileStorage();

            final TokenCache cache = new TokenCache();
            byte[] data = tokenFileStorage.read();
            cache.deserialize(data);

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

            Runnable worker = new Runnable() {
                @Override
                public void run() {
                    try {
                        getDataFromAzure(cache);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            };

            worker.run();
//            for (int i = 0; i < 16; ++i)  {
//                new Thread(worker).start();
//                new Thread(worker).start();
//            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.exit(1);
        }
    }

    private static void getDataFromAzure(TokenCache cache) throws Exception {

        System.out.println(String.format("\n======> tenantId: %s ======================\n", tenant));
        AuthContext ac = new AuthContext(String.format("%s/%s", authority, tenant), cache);
        AuthenticationResult result = ac.acquireToken(resource, clientId, redirectUri, PromptBehavior.Auto, null);
        System.out.println("token: " + result.getAccessToken());
//        printSubsriptins(result.getAccessToken());

        List<Tenant> tenants = TenantsClient.getByToken(result.getAccessToken());
        for (Tenant t : tenants) {
            String tid = t.getTenantId();
            System.out.println(String.format("\n======> tenantId: %s ======================\n", tid));

            try {
                AuthContext ac1 = new AuthContext(String.format("%s/%s", authority, tid), cache);
                AuthenticationResult result1 = ac1.acquireToken(resource, clientId, redirectUri, PromptBehavior.Auto, null);
                System.out.println("token: " + result1.getAccessToken());
                printSubsriptins(result1.getAccessToken());
            } catch (Exception e) {
                System.out.println("Auth exeption: " + e.getMessage());
            }
        }
    }
    

    private static void printSubsriptins(String accessToken) throws Exception {
        List<Subscription> subscriptions = SubscriptionsClient.getByToken(accessToken);
        System.out.println(String.format("\n=== Subscriptions: [%d]", subscriptions.size()));
        for (Subscription s : subscriptions) {
            String sid = s.getSubscriptionId().toString();
            System.out.println(String.format("\t======> %s: %s ======================", s.getDisplayName(), sid ));
            //listSitesForSubscription(sid, accessToken);
            //listStorageAccountsForSubscriptionClassic(sid, accessToken);
            listStorageAccountsForSubscriptionClassicRmc(sid, accessToken);
        }
    }
    
    private static void listStorageAccountsForSubscription(String sid, String token)  {
    	try {
    		final URI baseUri = new URI("https://management.azure.com/");
    		Configuration config  = ManagementConfiguration.configure(
    				null,
    				Configuration.getInstance(),
    				baseUri,
    				sid,
    				token);
    		StorageManagementClient storageManagementClient = StorageManagementService.create(config);
    		ArrayList<StorageAccount> saList = storageManagementClient.getStorageAccountsOperations().list().getStorageAccounts();
    		for(StorageAccount sa : saList) {
    			System.out.println("\tStorage accoutn name : " + sa.getName());
    		}
    		
    	} catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private static void listStorageAccountsForSubscriptionClassic(String sid, String token)  {
    	try {
    		final URI baseUri = new URI("https://management.azure.com/");
    		Configuration config  = ManagementConfiguration.configure(
    				null,
    				Configuration.getInstance(),
    				baseUri,
    				sid,
    				token);
    		com.microsoft.windowsazure.management.storage.
    		StorageManagementClient storageManagementClient = com.microsoft.windowsazure.management.storage.StorageManagementService.create(config);
    		ArrayList<com.microsoft.windowsazure.management.storage.models.StorageAccount> saList = storageManagementClient.getStorageAccountsOperations().list().getStorageAccounts();
    		for(com.microsoft.windowsazure.management.storage.models.StorageAccount sa : saList) {
    			System.out.println("\tStorage accoutn name : " + sa.getName());
    		}
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    private static void listStorageAccountsForSubscriptionClassicRmc(String sid, String token)  {
    	System.out.println("listStorageAccountsForSubscriptionClassicRmc");
    	try {
    		final URI baseUri = new URI("https://management.azure.com/");
    		Configuration config  = ManagementConfiguration.configure(
    				null,
    				Configuration.getInstance(),
    				baseUri,
    				sid,
    				token);    		
    		ResourceManagementClient managementClient = ResourceManagementService.create(config);
    		ResourceListParameters resourceListParameters = new ResourceListParameters();
//    		resourceListParameters.setResourceType("Microsoft.ClassicStorage/storageAccounts");
    		
    		ArrayList<GenericResourceExtended> list = managementClient.getResourcesOperations().list(resourceListParameters).getResources();
    		for(GenericResourceExtended item : list) {
    			System.out.println("\tGenericResourceExtended name : " + item.getName() + " [" + item.getType() + "]");
    		}
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    
    
    private static void listSitesForSubscription(String sid, String token) {

        try {
            final URI baseUri = new URI("https://management.azure.com/");
            Configuration config  = ManagementConfiguration.configure(
                    null,
                    Configuration.getInstance(),
                    baseUri,
                    sid,
                    token);
            ResourceManagementClient resourceManagementClient = ResourceManagementService.create(config);
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
