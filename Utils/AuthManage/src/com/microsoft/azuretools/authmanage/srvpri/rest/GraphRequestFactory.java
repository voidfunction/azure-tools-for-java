package com.microsoft.azuretools.authmanage.srvpri.rest;

/**
 * Created by vlashch on 8/29/16.
 */


import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureGraphException;

public class GraphRequestFactory extends RequestFactoryBase {

    public GraphRequestFactory(String tenantId) {
        this.tenantId = tenantId;
        this.urlPrefix = Constants.resourceGraph + this.tenantId + "/";
        this.resource =  Constants.resourceGraph;;
        apiVersion = "api-version=1.6";
    }

    @Override
    public AzureException newAzureException(String message) {
        return new AzureGraphException(message);
    }
}
