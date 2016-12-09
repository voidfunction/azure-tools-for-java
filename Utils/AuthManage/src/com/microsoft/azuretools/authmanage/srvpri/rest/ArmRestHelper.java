package com.microsoft.azuretools.authmanage.srvpri.rest;

/**
 * Created by vlashch on 8/29/16.
 */
public class ArmRestHelper extends RestHelperBase {

    public ArmRestHelper(String tenantId) {
        setRequestFactory(new ArmRequestFactory(tenantId));
    }
}