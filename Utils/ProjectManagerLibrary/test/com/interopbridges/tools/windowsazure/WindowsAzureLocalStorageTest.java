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

/**
 * The class <code>WindowsAzureLocalStorageTest</code> contains tests for the
 * class {@link <code>WindowsAzureLocalStorage</code>}
 */

public class WindowsAzureLocalStorageTest  {


    private WindowsAzureProjectManager wProj = null;
    private WindowsAzureRole role = null;
    private WindowsAzureLocalStorage wLoSto= null;

    @Before
    public final void setUp() {
        try {
            wProj = WindowsAzureProjectManager.load(
                    new File(Messages.getString(
                            "WinAzureTestConstants.WindowsAzureProj")));
            role = wProj.getRoles().get(2);
            wLoSto = role.getLocalStorage().get("WAStorage");

        } catch (WindowsAzureInvalidProjectOperationException e) {
            fail("test case failed");
        }
    }

    @Test
    public final void testGetName() {
        assertEquals("WAStorage",wLoSto.getName());
    }



    @Test(expected=IllegalArgumentException.class)
    public final void testSetNameWithEmpty()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setName("");
        assertEquals("WAStorage",wLoSto.getName());
    }

    @Test(expected=IllegalArgumentException.class)
    public final void testSetNameWithNull()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setName("");
        assertEquals("WAStorage",wLoSto.getName());
    }
    @Test
    public final void testSetName()
            throws WindowsAzureInvalidProjectOperationException {
        role.getLocalStorage();
        wLoSto.setName("WAStorage_new");
        assertEquals("WAStorage_new", wLoSto.getName());
    }

    @Test
    public final void testGetSize() {
        assertEquals(2, wLoSto.getSize());
    }


    @Test
    public final void testSetSize() throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setSize(3);
        assertEquals(3, wLoSto.getSize());
    }


    @Test(expected=IllegalArgumentException.class)
    public final void testSetSizeWithLessThan1Value()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setSize(0);
        assertEquals(0, wLoSto.getSize());
    }


    @Test
    public final void testGetCleanOnRecycle() {
        assertEquals(true, wLoSto.getCleanOnRecycle());
    }

    @Test
    public final void testSetCleanOnRecycle()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setCleanOnRecycle(false);
        assertEquals(false, wLoSto.getCleanOnRecycle());
    }

//    @Test
//    public final void testGetPathenv()
//            throws WindowsAzureInvalidProjectOperationException {
//        assertEquals("WAStorage_Path", wLoSto.getPathEnv());
//    }

    @Test
    public final void testSetPathenv()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setPathEnv("WAStorage_Path_new");
        assertEquals("WAStorage_Path_new", wLoSto.getPathEnv());
    }

    @Test
    public final void testSetPathenvWithEmpty()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setPathEnv("");
        assertEquals("", wLoSto.getPathEnv());
    }

    @Test(expected=IllegalArgumentException.class)
    public final void testSetPathenvWithNull()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.setPathEnv(null);
        assertEquals("", wLoSto.getPathEnv());
    }

//    @Test(expected=WindowsAzureInvalidProjectOperationException.class)
//    public final void testSetPathenvWithExistingVarName()
//            throws WindowsAzureInvalidProjectOperationException {
//        role.getLocalStorage();
//        wLoSto.setPathEnv("_JAVA_OPTIONS");
//        assertEquals("", wLoSto.getPathEnv());
//    }


    @Test
    public final void testGetPathenvWithNoPathExist()
            throws WindowsAzureInvalidProjectOperationException {
        assertEquals("", role.getLocalStorage().get("WAStorage1").getPathEnv());
    }

    @Test
    public final void testDelete()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.delete();
        assertEquals(null, role.getLocalStorage().get("WAStorage"));
    }

    @Test(expected=IllegalArgumentException.class)
    public final void testDeleteLsEnvWithNull()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.deleteLsEnv(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public final void testDeleteLsEnvWithEmpty()
            throws WindowsAzureInvalidProjectOperationException {
        wLoSto.deleteLsEnv(null);
    }
}
