package com.microsoft.auth;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class ResponseUtils {
    private final static Logger log = Logger.getLogger(ResponseUtils.class.getName());
    
    public static AuthorizationResult parseAuthorizeResponse(String webAuthenticationResult, CallState callState) throws Exception {
        AuthorizationResult result = null;

        URI resultUri = new URI(webAuthenticationResult);
        // NOTE: The Fragment property actually contains the leading '#' character and that must be dropped
        String resultData = resultUri.getQuery();
        if (resultData != null 
                && !resultData.isEmpty()) {
            // Remove the leading '?' first
            Map<String, String> map = UriUtils.formQueryStirng(resultData);
            
            if(map.containsKey(OAuthHeader.CorrelationId)) {
                String correlationIdHeader = (map.get(OAuthHeader.CorrelationId)).trim();
                try {
                    UUID correlationId = UUID.fromString(correlationIdHeader);
                    if (!correlationId.equals(callState.correlationId)) {
                        log.warn("Returned correlation id '" + correlationId + "' does not match the sent correlation id '" + callState.correlationId + "'");
                    }
                }
                catch(IllegalArgumentException ex) {
                    log.warn("Returned correlation id '" + correlationIdHeader + "' is not in GUID format.");
                }
            }

            if (map.containsKey(OAuthReservedClaim.Code)) {
                result = new AuthorizationResult(map.get(OAuthReservedClaim.Code));
            }
            else if (map.containsKey(OAuthReservedClaim.Error)) {
                result = new AuthorizationResult(map.get(OAuthReservedClaim.Error), map.get(OAuthReservedClaim.ErrorDescription));
            }
            else {
                result = new AuthorizationResult(AuthError.AuthenticationFailed, AuthErrorMessage.AuthorizationServerInvalidResponse);
            }
        }
        return result;
    }
    
    public static AuthenticationResult parseTokenResponse(TokenResponse tokenResponse) throws Exception  {
         AuthenticationResult result;

         if (tokenResponse.accessToken != null) {
             long expiresOn = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) +  tokenResponse.expiresIn;
             result = new AuthenticationResult(tokenResponse.tokenType, tokenResponse.accessToken, tokenResponse.refreshToken, expiresOn);
             result.resource = tokenResponse.resource;
             IdToken idToken = parseIdToken(tokenResponse.idToken);
             if (idToken != null) {
                 String tenantId = idToken.tenantId;
                 String uniqueId = null;
                 String displayableId = null;
                 if (!StringUtils.isNullOrWhiteSpace(idToken.objectId)) {
                     uniqueId = idToken.objectId;
                 }
                 else if (!StringUtils.isNullOrWhiteSpace(idToken.subject)) {
                     uniqueId = idToken.subject;
                 }
                 if (!StringUtils.isNullOrWhiteSpace(idToken.upn)) {
                     displayableId = idToken.upn;
                 }
                 else if (!StringUtils.isNullOrWhiteSpace(idToken.email)) {
                     displayableId = idToken.email;
                 }
                 String givenName = idToken.givenName;
                 String familyName = idToken.familyName;
                 String identityProvider = (idToken.identityProvider == null)
                         ? idToken.issuer
                        : idToken.identityProvider;
                 long passwordExpiresOffest = 0;
                 if (idToken.passwordExpiration > 0) {
                     passwordExpiresOffest = System.currentTimeMillis() + idToken.passwordExpiration;
                 }
                 URI changePasswordUri = null;
                 if (!StringUtils.isNullOrEmpty(idToken.passwordChangeUrl)) {
                     changePasswordUri = new URI(idToken.passwordChangeUrl);
                 }
                 result.updateTenantAndUserInfo(
                         tenantId, tokenResponse.idToken, 
                         new UserInfo (uniqueId, displayableId, givenName, familyName, identityProvider, passwordExpiresOffest, changePasswordUri));
             }
         }
         else if (tokenResponse.error != null) {
             throw new AuthException(tokenResponse.error, tokenResponse.errorDescription);
         }
         else {
             throw new AuthException(AuthError.Unknown, AuthErrorMessage.Unknown);
         }
         return result;
     }
     
     private static IdToken parseIdToken(String idToken) throws Exception {
         IdToken idTokenBody = null;
         if (!StringUtils.isNullOrWhiteSpace(idToken)) {
             log.info("idToken: " + idToken);
             String[] idTokenSegments = idToken.split("\\.");

             // If Id token format is invalid, we silently ignore the id token
             if (idTokenSegments.length == 2) {
                 byte[] decoded = Base64.decodeBase64(idTokenSegments[1]);
                 log.info("==> decoded idToken: " + new String(decoded));
                 idTokenBody = JsonHelper.deserialize(IdToken.class, new String(decoded));
             }
         }
         return idTokenBody;
     }
}
