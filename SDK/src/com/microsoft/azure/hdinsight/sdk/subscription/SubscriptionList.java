package com.microsoft.azure.hdinsight.sdk.subscription;

import java.util.List;

public class SubscriptionList {

    private List<Subscription> value;

    public List<Subscription> getValue(){
        return value;
    }

    public SubscriptionList(List<Subscription> subscriptionList){
        value = subscriptionList;
    }
}
