package com.microsoft.auth.tenants;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tenants {

    @JsonProperty("value")
    List<Tenant> tenants;

    @JsonProperty
    String nextLink;

    public List<Tenant> getTenants() {
        return tenants;
    }
}
