package com.microsoft.auth;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AuthenticatorTemplate {
	private static final Logger log = Logger.getLogger(AuthenticatorTemplate.class.getName());
    private static final String authorizeEndpointTemplate = "https://{host}/{tenant}/oauth2/authorize";
    private static final String metadataTemplate = "{\"Host\":\"{host}\", \"Authority\":\"https://{host}/{tenant}/\", \"InstanceDiscoveryEndpoint\":\"https://{host}/common/discovery/instance\", \"AuthorizeEndpoint\":\"" + authorizeEndpointTemplate + "\", \"TokenEndpoint\":\"https://{host}/{tenant}/oauth2/token\", \"UserRealmEndpoint\":\"https://{host}/common/UserRealm\"}";

    @JsonProperty("Host")
    public String host;

    @JsonProperty("Authority")
    public String authority;

    @JsonProperty("InstanceDiscoveryEndpoint")
    public String instanceDiscoveryEndpoint;

    @JsonProperty("AuthorizeEndpoint")
    public String authorizeEndpoint;

    @JsonProperty("TokenEndpoint")
    public String tokenEndpoint;

    @JsonProperty("Issuer")
    public String issuer;

    @JsonProperty("UserRealmEndpoint")
    public String userRealmEndpoint;

    // factory method
    public static AuthenticatorTemplate createFromHost(String host) throws Exception {
        String metadata = metadataTemplate.replace("{host}", host);
        AuthenticatorTemplate authority = JsonHelper.deserialize(AuthenticatorTemplate.class, metadata);
        authority.issuer = authority.tokenEndpoint;
        return authority;
    }

    public void verifyAnotherHostByInstanceDiscoveryAsync(String host, String tenant, CallState callState) throws Exception {
        String instanceDiscoveryEndpoint = this.instanceDiscoveryEndpoint;
        instanceDiscoveryEndpoint += ("?api-version=1.0&authorization_endpoint=" + authorizeEndpointTemplate);
        instanceDiscoveryEndpoint = instanceDiscoveryEndpoint.replace("{host}", host);
        instanceDiscoveryEndpoint = instanceDiscoveryEndpoint.replace("{tenant}", tenant);

        // send a request
        URL url = new URL(instanceDiscoveryEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "AzureToolkit4j");
        HttpHelper.addCorrelationIdToRequestHeader(connection, callState);
        
        // process a response
        int responseCode = connection.getResponseCode();
        if(responseCode != 200) {
        	String message = AuthError.AuthorityNotInValidList;
        	log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
        
        HttpHelper.verifyCorrelationIdInReponseHeader(connection, callState);
        InstanceDiscoveryResponse discoveryResponse = JsonHelper.deserialize(InstanceDiscoveryResponse.class, connection.getInputStream());
        if (discoveryResponse.tenantDiscoveryEndpoint == null) {
        	String message = AuthError.AuthorityNotInValidList;
        	log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
    }

    final class InstanceDiscoveryResponse {
        @JsonProperty("tenant_discovery_endpoint")
        public String tenantDiscoveryEndpoint;
    }
}
