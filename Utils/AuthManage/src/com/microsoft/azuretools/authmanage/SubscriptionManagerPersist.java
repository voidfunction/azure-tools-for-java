/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vlashch on 11/15/16.
 */
public class SubscriptionManagerPersist extends SubscriptionManager {

    public SubscriptionManagerPersist(AzureManager azureManager) {
        super(azureManager);
    }

    @Override
    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) throws Exception {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManagerPersist.setSubscriptionDetails()");
        synchronized (this) {
            String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
            saveSubscriptions(subscriptionDetails, subscriptionsDetailsFileName);
        }
        super.setSubscriptionDetails(subscriptionDetails);
    }

    @Override
    protected List<SubscriptionDetail> updateAccountSubscriptionList() throws Exception {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManagerPersist.updateAccountSubscriptionList()");
        List<SubscriptionDetail> sdl = null;
        synchronized (this) {
            String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
            sdl = loadSubscriptions(subscriptionsDetailsFileName);
        }

        if (sdl == null) {
            return super.updateAccountSubscriptionList();
        }
        return sdl;
    }

    @Override
    public synchronized void cleanSubscriptions() throws Exception {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManagerPersist.cleanSubscriptions()");
        String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        deleteSubscriptions(subscriptionsDetailsFileName);
        super.cleanSubscriptions();
    }

    public synchronized static void deleteSubscriptions(String subscriptionsDetailsFileName) throws Exception {
        System.out.println("cleaning " + subscriptionsDetailsFileName + " file");
        //String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        FileStorage fs = new FileStorage(subscriptionsDetailsFileName, CommonSettings.settingsBaseDir);
        fs.cleanFile();
    }

    private static List<SubscriptionDetail> loadSubscriptions(String subscriptionsDetailsFileName) throws Exception {
        System.out.println("SubscriptionManagerPersist.loadSubscriptions()");

        //subscriptionDetails.clear();
        FileStorage subscriptionsDetailsFileStorage = new FileStorage(subscriptionsDetailsFileName, CommonSettings.settingsBaseDir);
        byte[] data = subscriptionsDetailsFileStorage.read();
        String json = new String(data);
        if (json.isEmpty()) {
            System.out.println(subscriptionsDetailsFileName + " file is empty");
            return null;
        }
        SubscriptionDetail[] sda = JsonHelper.deserialize(SubscriptionDetail[].class, json);
        List<SubscriptionDetail> sdl = new ArrayList<>();
        for (SubscriptionDetail sd : sda) {
            sdl.add(sd);
        }
        return sdl;
    }

    private static void saveSubscriptions(List<SubscriptionDetail> sdl, String subscriptionsDetailsFileName) throws Exception {
        System.out.println("SubscriptionManagerPersist.saveSubscriptions()");
        String sd = JsonHelper.serialize(sdl);
        //String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        FileStorage subscriptionsDetailsFileStorage = new FileStorage(subscriptionsDetailsFileName, CommonSettings.settingsBaseDir);
        subscriptionsDetailsFileStorage.write(sd.getBytes(Charset.forName("utf-8")));
    }

}
