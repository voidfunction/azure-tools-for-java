package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by vlashch on 11/15/16.
 */
public class SubscriptionManagerPersist extends SubscriptionManager {

//    private String subscriptionsDetailsFileName;

    public SubscriptionManagerPersist(AzureManager azureManager) {
        super(azureManager);
        //this.subscriptionsDetailsFileName = subsriptionDetailsFilepath;
    }

    @Override
    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) throws Exception {
        saveSubscriptions(subscriptionDetails);
        super.setSubscriptionDetails(subscriptionDetails);
    }

    @Override
    protected void updateAccountSubscriptionList() throws Exception {
        loadSubscriptions();
        super.updateAccountSubscriptionList();
    }

    @Override
    public void cleanSubscriptions() throws Exception {
        String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        deleteSubscriptions(subscriptionsDetailsFileName);
        super.cleanSubscriptions();
    }

    public static void deleteSubscriptions(String subscriptionsDetailsFileName) throws Exception {
        System.out.println("cleaning " + subscriptionsDetailsFileName + " file");
        //String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        FileStorage fs = new FileStorage(subscriptionsDetailsFileName, CommonSettings.settingsBaseDir);
        fs.cleanFile();

    }

    public void loadSubscriptions() throws Exception {
        System.out.println("loadSubscriptions()");
        String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        subscriptionDetails.clear();
        FileStorage subscriptionsDetailsFileStorage = new FileStorage(subscriptionsDetailsFileName, CommonSettings.settingsBaseDir);
        byte[] data = subscriptionsDetailsFileStorage.read();
        String json = new String(data);
        if (json.isEmpty()) {
            System.out.println(subscriptionsDetailsFileName + " file is empty");
            return;
        }
        SubscriptionDetail[] sdl = JsonHelper.deserialize(SubscriptionDetail[].class, json);
        for(SubscriptionDetail sd : sdl) {
            subscriptionDetails.add(sd);
        }
    }

    public void saveSubscriptions(List<SubscriptionDetail> sdl) throws Exception {
        System.out.println("saveSubscriptions()");
        String sd = JsonHelper.serialize(subscriptionDetails);
        String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        FileStorage subscriptionsDetailsFileStorage = new FileStorage(subscriptionsDetailsFileName, CommonSettings.settingsBaseDir);
        subscriptionsDetailsFileStorage.write(sd.getBytes(Charset.forName("utf-8")));
    }

}
