package com.microsoft.auth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;

public class ResponseUtils {
    final static Logger log = Logger.getLogger(ResponseUtils.class.getName());
    
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
    
    public static AuthenticationResult parseTokenResponse(TokenResponse tokenResponse, CallState callState) throws Exception  {
         AuthenticationResult result;

         if (tokenResponse.accessToken != null) {
             long expiresOn = OffsetDateTime.now(ZoneId.of("UTC")).plusSeconds(tokenResponse.expiresIn).toEpochSecond();
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
                     passwordExpiresOffest = OffsetDateTime.now(ZoneId.of("UTC")).toEpochSecond() + idToken.passwordExpiration;
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
                 byte[] decoded = Base64.getDecoder().decode(idTokenSegments[1].getBytes(StandardCharsets.UTF_8));
                 System.out.println("decoded: " + decoded);
                 idTokenBody = JsonHelper.deserialize(IdToken.class, new String(decoded));
             }
         }
         return idTokenBody;
     }
}
