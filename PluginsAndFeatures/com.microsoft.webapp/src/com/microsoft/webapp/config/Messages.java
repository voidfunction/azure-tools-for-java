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
package com.microsoft.webapp.config;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.microsoft.webapp.config.messages";
	public static String webAppTtl;
	public static String dlgImgPath;
	public static String webAppLbl;
	public static String newBtn;
	public static String linkLblSub;
	public static String errTtl;
	public static String loadErrMsg;
	public static String noSubErrMsg;
	public static String noWebAppErrMsg;
	public static String loadSubErrMsg;
	public static String loadWebApps;
	public static String chkLbl;
	public static String deplyErrMsg;
	public static String dplyWebApp;
	public static String crtWebAppTtl;
	public static String name;
	public static String dnsName;
	public static String sub;
	public static String resGrp;
	public static String appPlan;
	public static String container;
	public static String noGrpErrMsg;
	public static String noPlanErrMsg;
	public static String nameErrMsg;
	public static String createErrMsg;
	public static String createWebApps;
	public static String crtAppPlanTtl;
	public static String loc;
	public static String price;
	public static String worker;
	public static String createPlanMsg;
	public static String appUniErrMsg;
	public static String inUseErrMsg;
	public static String activityView;
	public static String deplDesc;
	public static String dnsWebsite;
	public static String selWebAppMsg;
	public static String jobStart;
	public static String tierErrMsg;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
