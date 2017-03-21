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


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

enum AuthorityType {
    AAD,
    ADFS
}

class Authenticator {
    private final static String tenantlessTenantName = "common";
    private final AuthenticatorTemplateList authenticatorTemplateList;
    private boolean updatedFromTemplate;
    AuthorityType authorityType;
    boolean validateAuthority;

    private String authority;
    boolean isTenantless;
    String authorizationUri;
    String tokenUri;
    String userRealmUri;
    String selfSignedJwtAudience;
    UUID correlationId;

    public String getAuthority() {
        return this.authority;
    }

    public void setAuthority(String val) {
        this.authority = val;
    }

    public boolean getIsTenantless() throws URISyntaxException {
    	URI authorityUri = new URI(this.getAuthority());
    	String path = authorityUri.getPath().substring(1);
        String tenant = path.substring(0, path.indexOf("/"));
        return (tenant.compareToIgnoreCase(tenantlessTenantName) == 0);
    }

    public Authenticator(String authority, boolean validateAuthority) throws IOException, URISyntaxException {
    	this.authenticatorTemplateList = new AuthenticatorTemplateList();
        setAuthority(canonicalizeUri(authority));
        this.authorityType = detectAuthorityType(this.getAuthority());

        if (this.authorityType != AuthorityType.AAD && validateAuthority) {
            throw new IllegalArgumentException(AuthErrorMessage.UnsupportedAuthorityValidation);
        }

        this.validateAuthority = validateAuthority;
    }

    public void updateFromTemplate(CallState callState) throws URISyntaxException, IOException, AuthException {
        if (!updatedFromTemplate) {
            URI authorityUri = new URI(this.getAuthority());
            String host = authorityUri.getAuthority();
            String path = authorityUri.getPath().substring(1);
            String tenant = path.substring(0, path.indexOf("/"));

            AuthenticatorTemplate matchingTemplate = authenticatorTemplateList.findMatchingItem(validateAuthority, host, tenant, callState);

            authorizationUri = matchingTemplate.authorizeEndpoint.replace("{tenant}", tenant);
            tokenUri = matchingTemplate.tokenEndpoint.replace("{tenant}", tenant);
            userRealmUri = canonicalizeUri(matchingTemplate.userRealmEndpoint);
            isTenantless = (tenant.compareToIgnoreCase(tenantlessTenantName) == 0);
            selfSignedJwtAudience = matchingTemplate.issuer.replace("{tenant}", tenant);
            updatedFromTemplate = true;
        }
    }

    public void updateTenantId(String tenantId) {
        if (this.isTenantless && tenantId != null && tenantId.isEmpty()) {
            this.setAuthority(replaceTenantlessTenant(this.getAuthority(), tenantId));
            this.updatedFromTemplate = false;
        }
    }

    static AuthorityType detectAuthorityType(String authority) throws URISyntaxException {
        if (StringUtils.isNullOrEmpty(authority)) {
            throw new IllegalArgumentException("authority");
        }

        URI authorityUri = new URI(authority);
        if (authorityUri.getScheme().compareToIgnoreCase("https") != 0) {
            throw new IllegalArgumentException(AuthErrorMessage.AuthorityUriInsecure);
        }

        String path = authorityUri.getPath().substring(1);
        if (StringUtils.isNullOrEmpty(path)) {
            throw new IllegalArgumentException(AuthErrorMessage.AuthorityUriInvalidPath);
        }

        String firstPath = path.substring(0, path.indexOf("/"));
        AuthorityType authorityType = isAdfsAuthority(firstPath) ? AuthorityType.ADFS : AuthorityType.AAD;

        return authorityType;
    }

    private static String canonicalizeUri(String uri) {
        if (uri != null && !uri.isEmpty() && !uri.endsWith("/")) {
            uri = uri + "/";
        }

        return uri;
    }

    private static String replaceTenantlessTenant(String authority, String tenantId) {
       return authority.replace(tenantlessTenantName, tenantId);
    }

    private static boolean isAdfsAuthority(String firstPath) {
        return firstPath.compareToIgnoreCase("adfs") == 0;
    }
}
