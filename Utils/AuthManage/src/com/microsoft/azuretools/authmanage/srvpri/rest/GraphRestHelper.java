package com.microsoft.azuretools.authmanage.srvpri.rest;

public class GraphRestHelper extends RestHelperBase {

    public GraphRestHelper(String tenantId) {
        setRequestFactory(new GraphRequestFactory(tenantId));
    }
}
