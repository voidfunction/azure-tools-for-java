package com.microsoft.auth;

import java.util.ArrayList;
import java.util.List;

public class AuthenticatorTemplateList {
    final List<AuthenticatorTemplate> list = new ArrayList<AuthenticatorTemplate>();
    final String[] trustedHostList = { "login.windows.net", "login.chinacloudapi.cn", "login.cloudgovapi.us", "login.microsoftonline.com" };

   
    public AuthenticatorTemplateList() throws Exception {
    	for(String host : trustedHostList) {
    		list.add(AuthenticatorTemplate.createFromHost(host));
    	}
    }

    public AuthenticatorTemplate findMatchingItem(boolean validateAuthority, String host, String tenant, CallState callState) throws Exception
    {
        AuthenticatorTemplate matchingAuthenticatorTemplate = null;
        if (validateAuthority)
        {
            for(AuthenticatorTemplate at : list) {
                if (at.host.compareToIgnoreCase(host) == 0) {
                    matchingAuthenticatorTemplate = at;
                    break;
                }
            }

            if(matchingAuthenticatorTemplate == null)
            {
                // We only check with the first trusted authority (login.windows.net) for instance discovery
                list.get(0).verifyAnotherHostByInstanceDiscoveryAsync(host, tenant, callState);
            }
        }

        return (matchingAuthenticatorTemplate != null)
            ? matchingAuthenticatorTemplate
            : AuthenticatorTemplate.createFromHost(host);
    }
}
