/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.interopbridges.tools.windowsazure;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class WindowsAzureEndpointSATest {

    private WindowsAzureProjectManager wProj = null;
    private WindowsAzureRole role = null;
    @Before
    public final void setUp() {
        try {
            wProj = WindowsAzureProjectManager.load(
                    new File(Messages.getString("WinAzureTestConstants.SAWindowsAzureProj")));
            role = wProj.getRoles().get(0);
        } catch (WindowsAzureInvalidProjectOperationException e) {
            e.printStackTrace();
            fail("test case failed");
        }
    }

    @Test
    public void testConfigureSessionAffinity()
    throws WindowsAzureInvalidProjectOperationException {
        WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
        role.setSessionAffinityInputEndpoint(windowsAzureEndpoint);
        WindowsAzureEndpoint saEndPt = role.getSessionAffinityInputEndpoint();
        assertEquals(windowsAzureEndpoint.getName(),saEndPt.getName());
    }
    
    @Test
    public void testConfigureSslProxy()
    throws WindowsAzureInvalidProjectOperationException {
        WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
        WindowsAzureCertificate cert = new WindowsAzureCertificate("Azure tools 2", "E7A1173C9A56791633EB9B49B45409D757BE92BF");
        role.setSslOffloading(windowsAzureEndpoint, cert);
        WindowsAzureEndpoint saEndPt = role.getSslOffloadingInputEndpoint() ;
        assertEquals(windowsAzureEndpoint.getName(), saEndPt.getName());
    }
    
    @Test
    public void testGetSslCert()
    throws WindowsAzureInvalidProjectOperationException {
        WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http") ; 
        WindowsAzureCertificate cert = new WindowsAzureCertificate("Azure tools 2", "23545644");
        role.setSslOffloading(windowsAzureEndpoint, cert);
        WindowsAzureCertificate newCert =  role.getSslOffloadingCert();
        assertEquals(cert.getName(), newCert.getName());
        assertEquals(cert.getFingerPrint(), newCert.getFingerPrint());
    }
    
    @Test
    public void tesSetSslCert()
    		throws WindowsAzureInvalidProjectOperationException {
    	WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http") ; 
    	WindowsAzureCertificate cert = new WindowsAzureCertificate("Azure tools 2", "23545644");
    	role.setSslOffloading(windowsAzureEndpoint, cert);

    	WindowsAzureCertificate newCert = new WindowsAzureCertificate("Azure tools 3", "11111111");
    	role.setSslOffloadingCert(newCert);
    	WindowsAzureCertificate getCert =  role.getSslOffloadingCert();
    	assertEquals(newCert.getName(), getCert.getName());
    	assertEquals(newCert.getFingerPrint(), getCert.getFingerPrint());
    }
    
    
    @Test
    public void testDisableSslProxy()
    throws WindowsAzureInvalidProjectOperationException {
        role.setSslOffloading(null, null);
        WindowsAzureEndpoint saEndPt = role.getSslOffloadingInputEndpoint() ;
        assertNull(saEndPt);
    }




    @Test
    public void testGetSessionAffinityInputEndPoint()
    throws WindowsAzureInvalidProjectOperationException {
    	WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
        role.setSessionAffinityInputEndpoint(windowsAzureEndpoint);
        WindowsAzureEndpoint saEndPt =  role.getSessionAffinityInputEndpoint() ;
        assertEquals("http", saEndPt.getName());
    }

    @Test
    public void testGetSessionAffinityInternalEndPoint()
    throws WindowsAzureInvalidProjectOperationException {
    	WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
        role.setSessionAffinityInputEndpoint(windowsAzureEndpoint);
        WindowsAzureEndpoint saEndPt = role.getSessionAffinityInternalEndpoint() ;
        assertEquals("http_ARR_PROXY", saEndPt.getName());
    }

    @Test
    public void testSetInputEndPointName()
    throws WindowsAzureInvalidProjectOperationException {
           WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
           role.setSessionAffinityInputEndpoint(windowsAzureEndpoint);
           windowsAzureEndpoint.setName("http1") ;
           WindowsAzureEndpoint saEndPt = role.getSessionAffinityInputEndpoint() ;
           assertEquals(windowsAzureEndpoint.getName(),saEndPt.getName());
     }

    @Test
    public void testSetInternalEndPointName()
    		throws WindowsAzureInvalidProjectOperationException {
    	WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
    	role.setSessionAffinityInputEndpoint(windowsAzureEndpoint);
    	WindowsAzureEndpoint saEndPt = role.getEndpoint("http_ARR_PROXY") ;
    	saEndPt.setName("http1_ARR_PROXY") ;
    	assertEquals("http1_ARR_PROXY", saEndPt.getName());
    }

    @Test
    public void testDisableSessionAffinity()
    		throws WindowsAzureInvalidProjectOperationException {
    	WindowsAzureEndpoint windowsAzureEndpoint = role.getEndpoint("http");
    	role.setSessionAffinityInputEndpoint(windowsAzureEndpoint);
    	role.setSessionAffinityInputEndpoint(null);
    	assertNull(role.getSessionAffinityInputEndpoint());
    	wProj.save();
    }

     @Test
     public void testChangeRoleName()
     throws WindowsAzureInvalidProjectOperationException {
         role.setName("newworkerrole");
         assertEquals("newworkerrole",role.getName());
      }
}
