package com.microsoft.auth;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class AcquireTokenInteractiveHandler extends AcquireTokenHandlerBase {
    
    private AuthorizationResult authorizationResult;
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
    }

    private void setRedirectUriRequestParameter() {
        this.redirectUriRequestParameter = redirectUri.toString();
    }    
    
    @Override
    protected void preTokenRequest() throws Exception {
        acquireAuthorization();
    }
    
    private void acquireAuthorization() throws Exception {
        URI authorizationUri = this.createAuthorizationUri(false);
        String resultUri = this.webUi.authenticateAsync(authorizationUri, this.redirectUri).get();
        if(resultUri == null) {
            throw new AuthException("Authorization failed");
        }
        authorizationResult = ResponseUtils.parseAuthorizeResponse(resultUri, this.callState);
        verifyAuthorizationResult();    
   }
    
    private URI createAuthorizationUri(boolean includeFormsAuthParam) throws Exception {
        String loginHint = null;
        if (!userId.isAnyUser()
            && (userId.type == UserIdentifierType.OptionalDisplayableId
                || userId.type == UserIdentifierType.RequiredDisplayableId)) {
            loginHint = userId.id;
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
                && this.authorizationResult.error.equals(OAuthError.LoginRequired)) {
            throw new AuthException(AuthError.UserInteractionRequired);
        }
        if (this.authorizationResult.status != AuthorizationStatus.Success) {
            throw new AuthException(this.authorizationResult.error + ": " + this.authorizationResult.errorDescription);
        }
    }

    @Override
    protected void addAditionalRequestParameters(Map<String, String> requestParameters) {
        requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.AuthorizationCode);
        requestParameters.put(OAuthParameter.Code, this.authorizationResult.code);
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
            throw new AuthException(this.uniqueId + " != " + uniqueId);
        }
        if (this.userIdentifierType == UserIdentifierType.RequiredDisplayableId 
                && displayableId.compareToIgnoreCase(this.displayableId) != 0) {
            throw new AuthException(this.displayableId + " != " + displayableId);
        }
    }
}
