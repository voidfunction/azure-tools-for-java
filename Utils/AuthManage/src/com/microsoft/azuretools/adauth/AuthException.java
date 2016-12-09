package com.microsoft.azuretools.adauth;

public class AuthException extends Exception {
	
    private static final long serialVersionUID = 1L;

    public AuthException(String desc) {
        super(desc );
    }
    
    AuthException(String desc, String errorDescription) {
    	super(desc + ": " + errorDescription);
    }
}
