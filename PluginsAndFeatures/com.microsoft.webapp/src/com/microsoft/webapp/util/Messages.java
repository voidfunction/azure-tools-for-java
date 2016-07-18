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
package com.microsoft.webapp.util;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.microsoft.webapp.util.messages";
	public static String natJavaEMF;
	public static String natMdCore;
	public static String natFctCore;
	public static String natJava;
	public static String natJs;
	public static String propWebProj;
	public static String propErr;
	public static String resCLJobName;
	public static String webAppPluginID;
	public static String debugBatName;
	public static String debugBatEntry;
	public static String debugJarName;
	public static String debugJarEntry;
	public static String configName;
	public static String configEntry;
	public static String washName;
	public static String downloadName;
	public static String edmDll;
	public static String odataDll;
	public static String clientDll;
	public static String configDll;
	public static String storageDll;
	public static String jsonDll;
	public static String spatialDll;
	public static String washCmd;
	public static String psConfig;
	public static String customEntry;
	public static String downloadAspx;
	public static String extractAspx;
	public static String configDownload;
	public static String configExtract;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
