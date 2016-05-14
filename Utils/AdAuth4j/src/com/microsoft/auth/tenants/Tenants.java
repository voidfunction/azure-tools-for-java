package com.microsoft.auth.tenants;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Created by vlashch on 5/13/16.
 */
public class Tenants {

    @JsonProperty("value")
    List<Tenant> tenants;
    String nextLink;

    public List<Tenant> getTenants() {
        return tenants;
    }
}
