package com.microsoft.azuretools.authmanage;

import com.microsoft.rest.credentials.TokenCredentials;

/**
 * Created by vlashch on 11/9/16.
 */
public class RefreshableTokenCredentials extends TokenCredentials {

    private AdAuthManager authManager;
    private String tid;

    /**
     * Initializes a new instance of the TokenCredentials.
     *
     * @param authManager authz/auth manager
     * @param tid  tenant ID
     */
    public RefreshableTokenCredentials(AdAuthManager authManager, String tid) {
        super(null, null);
        this.authManager = authManager;
        this.tid = tid;
    }

    @Override
    public String getToken() {
        try {
            System.out.println("RefreshableTokenCredentials: getToken()");
            return authManager.getAccessToken(tid);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
