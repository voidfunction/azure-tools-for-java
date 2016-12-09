package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.UUID;

/**
 * Created by vlashch on 8/17/16.
 */

// inconming, outgoing
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordCredentials {
    //@JsonProperty
    //public String startDate;
    @JsonProperty
    public String endDate;
    @JsonProperty
    public UUID keyId;
    @JsonProperty
    public String value;
    @JsonProperty
    public String customKeyIdentifier;
}
