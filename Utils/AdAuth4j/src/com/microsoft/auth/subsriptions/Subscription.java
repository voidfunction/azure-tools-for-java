package com.microsoft.auth.subsriptions;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscription {

    @JsonProperty
    String id;

    @JsonProperty
    String subscriptionId;

    @JsonProperty
    String displayName;

    @JsonProperty
    String state;

    @JsonProperty
    SubscriptionPolicies subscriptionPolicies;

    public String getId() {
        return id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getState() {
        return state;
    }

    public SubscriptionPolicies geSubscriptionPolicies() {
        return subscriptionPolicies;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SubscriptionPolicies {
    @JsonProperty
    public String locationPlacementId;
    @JsonProperty
    public String quotaId;
}


