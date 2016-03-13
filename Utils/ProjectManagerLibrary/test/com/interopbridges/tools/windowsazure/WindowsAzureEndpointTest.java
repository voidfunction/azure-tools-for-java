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

public class WindowsAzureEndpointTest {

    private WindowsAzureProjectManager wProj = null;
    private WindowsAzureRole role = null;
    @Before
    public final void setUp() {
        try {
            wProj = WindowsAzureProjectManager.load(
                    new File(Messages.getString(
                            "WinAzureTestConstants.WindowsAzureProj")));
            role = wProj.getRoles().get(0);

        } catch (WindowsAzureInvalidProjectOperationException e) {

            fail("test case failed");
        }
    }

    @Test(expected=WindowsAzureInvalidProjectOperationException.class)
    public void testGetWindowsAzureProjMgr()
    throws WindowsAzureInvalidProjectOperationException {
        WindowsAzureEndpoint endPoint = new WindowsAzureEndpoint(null,null);
        endPoint.getWindowsAzureProjMgr();
    }

    @Test
    public void testGetName()
    throws WindowsAzureInvalidProjectOperationException {
            //input
            assertEquals("http", role.getEndpoints().get(0).getName());
            //internal
            assertEquals("InternalEp", role.getEndpoints().get(1).getName());
            assertEquals("InstanceEp", role.getEndpoints().get(2).getName());

    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetNameWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setName("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetNameWithNull()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setName(null);
    }

    @Test
    public void testSetNameInput()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setName("newEp");
        assertEquals("newEp", role.getEndpoints().get(0).getName());
    }

    @Test
    public void testSetNameInternal()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(1).setName("newEp1");
        assertEquals("newEp1", role.getEndpoints().get(1).getName());
    }

    @Test
    public void testSetNameInstance()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(2).setName("newEp1");
        assertEquals("newEp1", role.getEndpoints().get(2).getName());
    }


    @Test
    public void testGetPort() throws WindowsAzureInvalidProjectOperationException {
        //input
        assertEquals("80", role.getEndpoints().get(0).getPort());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetPortWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setPort("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetPortWithNull()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setPort(null);
    }

    @Test
    public void testSetPort()
    throws WindowsAzureInvalidProjectOperationException {
        //input
        role.getEndpoints().get(0).setPort("13");
        assertEquals("13", role.getEndpoints().get(0).getPort());
        //instance
        assertEquals("89-92", role.getEndpoints().get(2).getPort());
    }

//    @Test(expected=IllegalArgumentException.class)
//    public void testSetInputPortWithNull()
//    throws WindowsAzureInvalidProjectOperationException {
//        role.getEndpoints().get(0).setInputPort(null);
//    }

    @Test
    public void testGetPrivatePort()
    throws WindowsAzureInvalidProjectOperationException {
        //input
        assertEquals("8080", role.getEndpoints().get(0).getPrivatePort());
        //internal
        assertEquals("34", role.getEndpoints().get(1).getPrivatePort());
        //instance
        assertEquals("93", role.getEndpoints().get(2).getPrivatePort());
    }


    @Test(expected=IllegalArgumentException.class)
    public void testSetPrivatePortWithEmpty()
    throws WindowsAzureInvalidProjectOperationException {
    	// only if its an instance endpoint exception would be thrown
        role.getEndpoints().get(2).setPrivatePort("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetPrivatePortWithNull()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(2).setPrivatePort(null);
    }

    @Test
    public void testSetPrivatePortInput()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setPrivatePort("22");
        assertEquals("22", role.getEndpoints().get(0).getPrivatePort());
    }
    @Test
    public void testSetPrivatePortInternal()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(1).setPrivatePort("35");
        assertEquals("35", role.getEndpoints().get(1).getPrivatePort());
    }

    @Test
    public void testSetPrivatePortInstance()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(2).setPrivatePort("99");
        assertEquals("99", role.getEndpoints().get(2).getPrivatePort());
    }

//    @Test(expected=IllegalArgumentException.class)
//    public void testsetInternalFixedPortWithNull()
//    throws WindowsAzureInvalidProjectOperationException {
//        role.getEndpoints().get(0).setInternalFixedPort(null);
//    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetInputLocalPortWithNull()
    throws WindowsAzureInvalidProjectOperationException {
        role.getEndpoints().get(0).setLocalPort(null);
    }

    @Test
    public void testGetEndPointType()
    throws WindowsAzureInvalidProjectOperationException {
            //input
            assertEquals(WindowsAzureEndpointType.Input,
                    role.getEndpoints().get(0).getEndPointType());
            //internal
            assertEquals(WindowsAzureEndpointType.Internal,
                    role.getEndpoints().get(1).getEndPointType());
            //instance
            assertEquals(WindowsAzureEndpointType.InstanceInput,
                    role.getEndpoints().get(2).getEndPointType());
    }

    @Test
    public void testSetEndPointType()
    throws WindowsAzureInvalidProjectOperationException {
            //input
            role.getEndpoints().get(0).setEndPointType(
                    WindowsAzureEndpointType.Input);
            assertEquals(WindowsAzureEndpointType.Input,
                    role.getEndpoints().get(0).getEndPointType());
            role.getEndpoints().get(0).setEndPointType(
                    WindowsAzureEndpointType.Internal);
            assertEquals(WindowsAzureEndpointType.Internal,
                    role.getEndpoints().get(0).getEndPointType());
            //internal
            role.getEndpoints().get(1).setEndPointType(
                    WindowsAzureEndpointType.Internal);
            assertEquals(WindowsAzureEndpointType.Internal,
                    role.getEndpoints().get(1).getEndPointType());
            role.getEndpoints().get(1).setEndPointType(
                    WindowsAzureEndpointType.Input);
            assertEquals(WindowsAzureEndpointType.Input,
                    role.getEndpoints().get(1).getEndPointType());

            //instance
            role.getEndpoints().get(2).setEndPointType(
                    WindowsAzureEndpointType.Internal);
            assertEquals(WindowsAzureEndpointType.Internal,
                    role.getEndpoints().get(2).getEndPointType());

            role.getEndpoints().get(2).setEndPointType(
                    WindowsAzureEndpointType.InstanceInput);
            assertEquals(WindowsAzureEndpointType.InstanceInput,
                    role.getEndpoints().get(2).getEndPointType());

            role.getEndpoints().get(2).setEndPointType(
                    WindowsAzureEndpointType.Input);
            assertEquals(WindowsAzureEndpointType.Input,
                    role.getEndpoints().get(2).getEndPointType());
    }

    @Test
    public void testDelete()
    throws WindowsAzureInvalidProjectOperationException {
        //removed input endpoint
            role.getEndpoints().get(0).delete();
            assertEquals(2, role.getEndpoints().size());

        //removed internal endpoint
            role.getEndpoints().get(0).delete();
            assertEquals(1, role.getEndpoints().size());

        //removed instance endpoint
        role.getEndpoints().get(0).delete();
        assertEquals(0, role.getEndpoints().size());
    }

}
