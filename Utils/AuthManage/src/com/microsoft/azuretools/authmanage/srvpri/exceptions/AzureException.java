package com.microsoft.azuretools.authmanage.srvpri.exceptions;

import java.io.IOException;

/**
 * Created by vlashch on 8/22/16.
 */

public abstract class AzureException extends IOException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public AzureException(String json) {
        super(json);
    }

    public abstract String getCode();
    public abstract String getDescription();

}
