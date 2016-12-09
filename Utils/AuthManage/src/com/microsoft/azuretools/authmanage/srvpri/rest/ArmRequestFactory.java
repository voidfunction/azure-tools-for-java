package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureArmException;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;

/**
 * Created by vlashch on 8/29/16.
 */
class ArmRequestFactory extends RequestFactoryBase {

    public ArmRequestFactory(String tenantId) {
        this.urlPrefix = Constants.resourceARM + "subscriptions/";
        this.tenantId = tenantId;
        this.resource =  Constants.resourceARM;;
        this.apiVersion = "api-version=2015-07-01";
    }

    @Override
    public AzureException newAzureException(String message) {
        return new AzureArmException(message);
    }
}
