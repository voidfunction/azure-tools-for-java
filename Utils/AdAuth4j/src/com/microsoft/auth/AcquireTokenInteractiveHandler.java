package com.microsoft.auth;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AcquireTokenInteractiveHandler extends AcquireTokenHandlerBase {
    final static Logger log = Logger.getLogger(AcquireTokenInteractiveHandler.class.getName());
    private static AuthorizationResult authorizationResult;
    private static long authorizationResultTimestamp = -1;
    private URI redirectUri;
    private String redirectUriRequestParameter;
    private PromptBehavior promptBehavior;
    private final IWebUi webUi;
    private final UserIdentifier userId;

    AcquireTokenInteractiveHandler(Authenticator authenticator, TokenCache tokenCache, String resource,
            String clientId, String redirectUri, PromptBehavior promptBehavior, UserIdentifier userId, IWebUi webUi) throws Exception {
        super(authenticator, tokenCache, resource, new ClientKey(clientId), TokenSubjectType.User);
        if (redirectUri == null) {
            throw new IllegalArgumentException("redirectUri");
        }
        this.redirectUri = new URI(redirectUri);
        if (this.redirectUri.getFragment() != null && !this.redirectUri.getFragment().isEmpty()) {
            throw new IllegalArgumentException("redirectUri: " + AuthErrorMessage.RedirectUriContainsFragment);
        }
        this.setRedirectUriRequestParameter();
        if (userId == null) {
            throw new IllegalArgumentException("userId: " +  AuthErrorMessage.SpecifyAnyUser);
        }
        this.userId = userId;
        this.promptBehavior = promptBehavior;
        this.webUi = webUi;
        this.uniqueId = userId.uniqueId();
        this.displayableId = userId.displayableId();
        this.userIdentifierType = userId.type;
        this.loadFromCache = (tokenCache != null 
                && this.promptBehavior != PromptBehavior.Always 
                && this.promptBehavior != PromptBehavior.RefreshSession);
        this.supportADFS = true;
        log.log(Level.FINEST, String.format(
                "\n=== AcquireTokenInteractiveHandler params:\n\tpromptBehavior: %s\n\twebUi: %s\n\tuniqueId: %s\n\tdisplayableId: %s"
                , promptBehavior.toString()
                , webUi.getClass().getName()
                , uniqueId 
                , displayableId));
    }

    private void setRedirectUriRequestParameter() {
        this.redirectUriRequestParameter = redirectUri.toString();
    }    
    
    @Override
    protected void preTokenRequest() throws Exception {
        acquireAuthorization();
    }
    
    private void acquireAuthorization() throws Exception {
        log.log(Level.FINEST, "acquireAuthorization...");
        long allowedInterval = 300000; // authorization code ttl
        if(authorizationResult != null
            && !authenticator.getIsTenantless()
            && System.currentTimeMillis() - authorizationResultTimestamp < allowedInterval) {
            // use existing authorization code
            log.log(Level.FINEST, "using existing authorization code");
            return;
        }

        URI authorizationUri = this.createAuthorizationUri(false);
        log.log(Level.FINEST, "Starting web ui...");
        String resultUri = this.webUi.authenticateAsync(authorizationUri, this.redirectUri).get();
        if(resultUri == null) {
            String message = "Authorization failed";
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
        authorizationResult = ResponseUtils.parseAuthorizeResponse(resultUri, this.callState);
        log.log(Level.FINEST, "==> authorization code: " + authorizationResult.code);
        authorizationResultTimestamp = System.currentTimeMillis();
        verifyAuthorizationResult();    
   }
    
    private URI createAuthorizationUri(boolean includeFormsAuthParam) throws Exception {
        String loginHint = null;
        if (!userId.isAnyUser()
            && (userId.type == UserIdentifierType.OptionalDisplayableId
                || userId.type == UserIdentifierType.RequiredDisplayableId)) {
            loginHint = userId.displayableId();
        }
        Map<String, String> requestParameters = this.createAuthorizationRequest(loginHint, includeFormsAuthParam);
        return new URI(this.authenticator.authorizationUri + "?" + UriUtils.toQueryString(requestParameters));
    }

    private Map<String, String> createAuthorizationRequest(String loginHint, boolean includeFormsAuthParam) {
        Map<String, String> authorizationRequestParameters = new HashMap<>();
        authorizationRequestParameters.put(OAuthParameter.Resource, this.resource);
        authorizationRequestParameters.put(OAuthParameter.ClientId, this.clientKey.clientId);
        authorizationRequestParameters.put(OAuthParameter.ResponseType, OAuthResponseType.Code);
        authorizationRequestParameters.put(OAuthParameter.RedirectUri, this.redirectUriRequestParameter);
        if (loginHint != null 
                && !loginHint.isEmpty()) {
            authorizationRequestParameters.put(OAuthParameter.LoginHint, loginHint);
        }
        if (this.callState != null 
                && this.callState.correlationId != null) {
            authorizationRequestParameters.put(OAuthParameter.CorrelationId, this.callState.correlationId.toString());
            authorizationRequestParameters.put(OAuthHeader.RequestCorrelationIdInResponse, "true");
        }
        // ADFS currently ignores the parameter for now.
        if (promptBehavior == PromptBehavior.Always) {
            authorizationRequestParameters.put(OAuthParameter.Prompt, PromptValue.Login);
        } else if (promptBehavior == PromptBehavior.RefreshSession) {
            authorizationRequestParameters.put(OAuthParameter.Prompt, PromptValue.RefreshSession);
        } else if (promptBehavior == PromptBehavior.Never) {
            authorizationRequestParameters.put(OAuthParameter.Prompt, PromptValue.AttemptNone);
        }
        if (includeFormsAuthParam) {
            authorizationRequestParameters.put(OAuthParameter.FormsAuth, OAuthValue.FormsAuth);
        }
        return authorizationRequestParameters;
    }

    private void verifyAuthorizationResult() throws Exception {
        if (this.promptBehavior == PromptBehavior.Never 
                && authorizationResult.error.equals(OAuthError.LoginRequired)) {
            String message = AuthError.UserInteractionRequired;
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
        if (authorizationResult.status != AuthorizationStatus.Success) {
            String message = authorizationResult.error + ": " + authorizationResult.errorDescription;
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
    }

    @Override
    protected void addAditionalRequestParameters(Map<String, String> requestParameters) {
        requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.AuthorizationCode);
        requestParameters.put(OAuthParameter.Code, authorizationResult.code);
        requestParameters.put(OAuthParameter.RedirectUri, this.redirectUriRequestParameter);            
    }
    
    @Override
    protected void postTokenRequest(AuthenticationResult result) throws Exception {
        super.postTokenRequest(result);
        if ((this.displayableId == null && this.uniqueId == null) 
                || this.userIdentifierType == UserIdentifierType.OptionalDisplayableId) {
            return;
        }
        String uniqueId = (result.userInfo != null && result.userInfo.uniqueId != null) ? result.userInfo.uniqueId : "NULL";
        String displayableId = (result.userInfo != null) ? result.userInfo.displayableId : "NULL";
        if (this.userIdentifierType == UserIdentifierType.UniqueId 
                && uniqueId.compareTo(this.uniqueId) != 0) {
            String message = "Expected and returned userInfo.uniqueId doesn't match: " + this.uniqueId + " != " + uniqueId;
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
        if (this.userIdentifierType == UserIdentifierType.RequiredDisplayableId 
                && displayableId.compareToIgnoreCase(this.displayableId) != 0) {
            String message = "Expected and returned userInfo.displayableId doesn't match: " + this.displayableId + " != " + displayableId;
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
    }
}
