package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;

/**
 * Created by vlashch on 8/29/16.
 */

interface IRequestFactory {
    String getApiVersion();
    String getUrlPattern();
    String getUrlPatternParamless();
    String getUrlPrefix();
    String getAccessToken() throws Exception;
    AzureException newAzureException(String message);
}