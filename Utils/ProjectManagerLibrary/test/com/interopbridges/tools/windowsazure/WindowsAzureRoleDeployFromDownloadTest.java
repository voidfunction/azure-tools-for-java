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
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class WindowsAzureRoleDeployFromDownloadTest {

    private WindowsAzureProjectManager waCompMgr;
    private List<WindowsAzureRole> listRoles;

    @Before
    public final void setUp() {
        try {
            waCompMgr = WindowsAzureProjectManager
                    .load(new File(Messages.getString("WinAzureTestConstants.WADeployDL")));
            listRoles = waCompMgr.getRoles();
        } catch (WindowsAzureInvalidProjectOperationException e) {
            fail("Exception occurred");
        }
    }

    //getJDKCloudURL
    @Test
    public final void testGetJDKCloudURL()
    throws WindowsAzureInvalidProjectOperationException {
    	String url = listRoles.get(0).getJDKCloudURL();
        assertEquals("https://interopdemosteststore.blob.core.windows.net/temp/jdk.zip",
        		url);
    }

    @Test
    public final void testGetJDKCloudURLWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	String url = listRoles.get(1).getJDKCloudURL();
        assertTrue(url.isEmpty());
    }

    @Test(expected=WindowsAzureInvalidProjectOperationException.class)
    public final void testGetJDKCloudURLWithException()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(2).getJDKCloudURL();
    }

    //setJDKCloudURL
    @Test
    public final void testSetJDKCloudURLWithNull()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setJDKCloudURL(null);
    	String url = listRoles.get(0).getJDKCloudURL();
        assertTrue(url.isEmpty());
    }

    @Test
    public final void testSetJDKCloudURLWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setJDKCloudURL("");
    	String url = listRoles.get(0).getJDKCloudURL();
        assertTrue(url.isEmpty());
    }

    @Test
    public final void testSetJDKCloudURL()
    throws WindowsAzureInvalidProjectOperationException {
    	String url = "https://interopdemosteststore.blob.core.windows.net/temp/jdk.zip";
    	listRoles.get(0).setJDKCloudURL(url);
    	String newUrl = listRoles.get(0).getJDKCloudURL();
        assertEquals(url, newUrl);
    }


    //getJDKCloudKey

    @Test
    public final void testGetJDKCloudkey()
    throws WindowsAzureInvalidProjectOperationException {
    	String key = listRoles.get(0).getJDKCloudKey();
        assertEquals("/AxKCsqVAiNFkIMBYc1k4BoQhn/6NAHqrAXjhYQYIfjEMbYN3M/PZltzRIEqSCSq/xPH6XefYaxqOJH3HRIBsg=",
        		key);
    }

    @Test
    public final void testGetJDKCloudKeyWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	String key = listRoles.get(1).getJDKCloudKey();
        assertTrue(key.isEmpty());
    }

    @Test(expected=WindowsAzureInvalidProjectOperationException.class)
    public final void testGetJDKCloudKeyWithException()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(2).getJDKCloudKey();
    }

    //setJDKCloudKey
    @Test
    public final void testSetJDKCloudKeyWithNull()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setJDKCloudKey(null);
    	String key = listRoles.get(0).getJDKCloudKey();
        assertTrue(key.isEmpty());
    }

    @Test
    public final void testSetJDKCloudKeyWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setJDKCloudKey("");
    	String key = listRoles.get(0).getJDKCloudKey();
        assertTrue(key.isEmpty());
    }

    @Test
    public final void testSetJDKCloudKey()
    throws WindowsAzureInvalidProjectOperationException {
    	String newKey = "/AAAAxKCsqVAiNFkIMBYc1k4BoQhn/6NAHqrAXjhYQYIfjEMbYN3M/PZltzRIEqSCSq/xPH6XefYaxqOJH3HRIBsg=";
    	listRoles.get(0).setJDKCloudKey(newKey);
    	String key = listRoles.get(0).getJDKCloudKey();
        assertEquals(newKey, key);
    }

    //getServerCloudURL
    @Test
    public final void testGetServerCloudURL()
    throws WindowsAzureInvalidProjectOperationException {
    	String url = listRoles.get(0).getServerCloudURL();
        assertEquals("https://interopdemosteststore.blob.core.windows.net/temp/server.zip",
        		url);
    }

    @Test
    public final void testGetServerCloudURLWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	String url = listRoles.get(1).getServerCloudURL();
        assertTrue(url.isEmpty());
    }

    @Test(expected=WindowsAzureInvalidProjectOperationException.class)
    public final void testGetServerCloudURLWithException()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(2).getServerCloudURL();
    }

    //setServerCloudURL
    @Test
    public final void testSetServerCloudURLWithNull()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setServerCloudURL(null);
    	String url = listRoles.get(0).getServerCloudURL();
        assertTrue(url.isEmpty());
    }

    @Test
    public final void testSetServerCloudURLWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setServerCloudURL("");
    	String url = listRoles.get(0).getServerCloudURL();
        assertTrue(url.isEmpty());
    }

    @Test
    public final void testSetServerCloudURL()
    throws WindowsAzureInvalidProjectOperationException {
    	String url = "https://interopdemosteststore.blob.core.windows.net/temp/jdk.zip";
    	listRoles.get(0).setServerCloudURL(url);
    	String newUrl = listRoles.get(0).getServerCloudURL();
        assertEquals(url, newUrl);
    }

    //getServerCloudKey

    @Test
    public final void testGetServerCloudkey()
    throws WindowsAzureInvalidProjectOperationException {
    	String key = listRoles.get(0).getServerCloudKey();
        assertEquals("/AAAAxKCsqVAiNFkIMBYc1k4BoQhn/6NAHqrAXjhYQYIfjEMbYN3M/PZltzRIEqSCSq/xPH6XefYaxqOJH3HRIBsg=",
        		key);
    }

    @Test
    public final void testGetServerCloudKeyWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	String key = listRoles.get(1).getServerCloudKey();
        assertTrue(key.isEmpty());
    }

    @Test(expected=WindowsAzureInvalidProjectOperationException.class)
    public final void testGetServerCloudKeyWithException()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(2).getServerCloudKey();
    }


    //setServerCloudKey
    @Test
    public final void testSetServerCloudKeyWithNull()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setServerCloudKey(null);
    	String key = listRoles.get(0).getServerCloudKey();
        assertTrue(key.isEmpty());
    }

    @Test
    public final void testSetServerCloudKeyWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	listRoles.get(0).setServerCloudKey("");
    	String key = listRoles.get(0).getServerCloudKey();
        assertTrue(key.isEmpty());
    }

    @Test
    public final void testSetServerCloudkey()
    throws WindowsAzureInvalidProjectOperationException {
    	String newKey = "/AAAAxKCsqVAiNFkIMBYc1k4BoQhn/6NAHqrAXjhYQYIfjEMbYN3M/PZltzRIEqSCSq/xPH6XefYaxqOJH3HRIBsg=";
    	listRoles.get(0).setServerCloudKey(newKey);
    	String key = listRoles.get(0).getServerCloudKey();
        assertEquals(newKey, key);
    }

}
