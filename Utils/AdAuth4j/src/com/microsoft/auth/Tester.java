package com.microsoft.auth;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.microsoft.auth.subsriptions.Subscription;
import com.microsoft.auth.subsriptions.SubscriptionsClient;


public class Tester {
    final static String authority = "https://login.windows.net/common";
    final static String resource = "https://management.core.windows.net/";
    final static String clientId = "61d65f5a-6e3b-468b-af73-a033f5098c5c";
    final static String redirectUri = "https://msopentech.com/";
    static ExecutorService service = Executors.newFixedThreadPool(2);
    
   
    public static void main(String[] args) {
    	try {
    		
    		TokenCache cache = new FileTokenCache();
			AuthContext ac = new AuthContext(authority, cache);
			AuthenticationResult result = ac.acquireTokenAsync(resource, clientId, redirectUri, PromptBehavior.Auto).get();
			List<Subscription> subs = SubscriptionsClient.getByToken(result.getAccessToken());
			for(Subscription s : subs) {
				System.out.println("name: " + s.getSubscriptionName());
				System.out.println("sudId: " + s.getSubscriptionId());
				System.out.println();
			}

    	} catch (Exception e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    }
}
