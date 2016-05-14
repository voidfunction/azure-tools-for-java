package com.microsoft.auth.tenants;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class Tenant {

    @JsonProperty
    String id;

    @JsonProperty
    String tenantId;

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }
}