package com.microsoft.azuretools.adauth;

public class ClientKey {

	String clientId;
	boolean hasCredential;
    
	public ClientKey(String clientId) {
        if (clientId == null || clientId.isEmpty())  {
            throw new IllegalArgumentException("clientId");
        }

        this.clientId = clientId;
        this.hasCredential = false;
    }

}
