package com.microsoft.azure.hdinsight.sdk.subscription;

public class Subscription {
    private String displayName;
    private String id;
    private String state;
    private String subscriptionId;
    private SubscriptionPolicies subscriptionPolicies;
    private String accessToken;
    private boolean selected = true;

    public String getDisplayName(){
        return displayName;
    }

    public String getId(){
        return id;
    }

    public String getState(){
        return state;
    }

    public String getSubscriptionId(){
        return subscriptionId;
    }

    public SubscriptionPolicies getSubscriptionPolicies(){
        return subscriptionPolicies;
    }

    public String getAccessToken(){
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isSelected(){
        return selected;
    }

    public void setSelected(boolean selected){
        this.selected = selected;
    }
}
