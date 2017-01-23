/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.adauth;

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
