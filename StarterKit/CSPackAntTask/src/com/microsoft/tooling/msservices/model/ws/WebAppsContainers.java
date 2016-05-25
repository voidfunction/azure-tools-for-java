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
package com.microsoft.tooling.msservices.model.ws;

public enum WebAppsContainers {
    TOMCAT_8("8.0","Apache Tomcat 8 (Latest)", "8.0.23"),
    TOMCAT_7("7.0", "Apache Tomcat 7 (Latest)", "7.0.62"),
    JETTY_9("9.1", "Jetty 9 (Latest)", "9.1.0.20131115");

    private String value;
    private String name;
    private String currentVersion;

    private WebAppsContainers(String value, String name, String currentVersion){
        this.value = value;
        this.name  = name;
        this.currentVersion = currentVersion;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

	public String getCurrentVersion() {
		return currentVersion;
	}
}
