package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AdAuthManager;

/**
 * Created by vlashch on 8/29/16.
 */
abstract class RequestFactoryBase implements IRequestFactory {
    protected String apiVersion;
    protected String urlPrefix;
    protected String tenantId;
    protected String resource;
    protected PromptBehavior promptBehavior = PromptBehavior.Auto;

    public String getApiVersion(){
        if (apiVersion == null) throw new NullPointerException("this.apiVersion is null");
        return apiVersion;
    }

    public String getUrlPattern() {
        return getUrlPrefix() + "%s?%s&" + getApiVersion();
    }

    public String getUrlPatternParamless() {
        return getUrlPrefix() + "%s?" + getApiVersion();
    }

    public String getUrlPrefix() {
        if (urlPrefix == null) throw new NullPointerException("this.urlPrefix is null");
        return urlPrefix;
    }
    public String getAccessToken() throws Exception {
        if (tenantId == null) throw new NullPointerException("this.tenantId is null");
        if (resource == null) throw new NullPointerException("this.resource is null");
        return AdAuthManager.getInstance().getAccessToken(tenantId, resource, promptBehavior);
    }
}
