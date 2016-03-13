package com.microsoftopentechnologies.azure;


public class AzureCloudException extends Exception {
	
	public AzureCloudException(String message) {
        super(message);
    }
    
	public AzureCloudException() {
        super();
    }
    
	public AzureCloudException(String msg, Exception excep) {
        super(msg, excep);
    }
    
	private static final long serialVersionUID = -8157417759485046943L;

}
