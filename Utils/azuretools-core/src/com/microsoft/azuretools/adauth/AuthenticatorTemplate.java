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

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.Constants;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static AuthenticatorTemplate createFromHost(String host) throws IOException {
        String metadata = metadataTemplate.replace("{host}", host);
        AuthenticatorTemplate authority = JsonHelper.deserialize(AuthenticatorTemplate.class, metadata);
        authority.issuer = authority.tokenEndpoint;
        return authority;
    }

    public void verifyAnotherHostByInstanceDiscoveryAsync(String host, String tenant, CallState callState) throws IOException, AuthException {
        String instanceDiscoveryEndpoint = this.instanceDiscoveryEndpoint;
        instanceDiscoveryEndpoint += ("?api-version=1.0&authorization_endpoint=" + authorizeEndpointTemplate);
        instanceDiscoveryEndpoint = instanceDiscoveryEndpoint.replace("{host}", host);
        instanceDiscoveryEndpoint = instanceDiscoveryEndpoint.replace("{tenant}", tenant);

        // send a request
        URL url = new URL(instanceDiscoveryEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(Constants.connection_read_timeout_ms);
        //connection.setRequestProperty("User-Agent", "AzureToolkit4");
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
