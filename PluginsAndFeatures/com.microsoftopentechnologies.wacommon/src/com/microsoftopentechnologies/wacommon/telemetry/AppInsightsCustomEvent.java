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
package com.microsoftopentechnologies.wacommon.telemetry;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoftopentechnologies.azurecommons.xmlhandling.DataOperations;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class AppInsightsCustomEvent {
	static String pluginInstLoc = String.format("%s%s%s",
			PluginUtil.pluginFolder, File.separator, Messages.commonPluginID);
	static String dataFile = String.format("%s%s%s", pluginInstLoc,
			File.separator, Messages.dataFileName);
	static String key = "824aaa4c-052b-4c43-bdcb-48f915d71b3f";

	public static void create(String eventName, String version) {
		if (new File(pluginInstLoc).exists() && new File(dataFile).exists()) {
			String prefValue = DataOperations.getProperty(dataFile, Messages.prefVal);
			if (prefValue != null && !prefValue.isEmpty() && prefValue.equalsIgnoreCase("true")) {
				TelemetryClient telemetry = new TelemetryClient();
				telemetry.getContext().setInstrumentationKey(key);

				Map<String, String> properties = new HashMap<String, String>();

				if (version != null && !version.isEmpty()) {
					properties.put("Library Version", version);
				}

				String pluginVersion = DataOperations.getProperty(dataFile, Messages.version);
				if (pluginVersion != null && !pluginVersion.isEmpty()) {
					properties.put("Plugin Version", pluginVersion);
				}

				Map<String, Double> metrics = new HashMap<String, Double>();
				String instID = DataOperations.getProperty(dataFile, Messages.instID);
				if (instID != null && !instID.isEmpty()) {
					metrics.put("Installation ID", Double.parseDouble(instID));
				}

				telemetry.trackEvent(eventName, properties, metrics);
				telemetry.flush();
			}
		}
	}

	public static void createFTPEvent(String eventName, String uri, String appName, String subId) {
		TelemetryClient telemetry = new TelemetryClient();
		telemetry.getContext().setInstrumentationKey(key);

		Map<String, String> properties = new HashMap<String, String>();
		Map<String, Double> metrics = new HashMap<String, Double>();

		if (uri != null && !uri.isEmpty()) {
			properties.put("WebApp URI", uri);
		}
		if (appName != null && !appName.isEmpty()) {
			properties.put("Java app name", appName);
		}
		if (subId != null && !subId.isEmpty()) {
			properties.put("Subscription ID", subId);
		}
		if (new File(pluginInstLoc).exists() && new File(dataFile).exists()) {
			String pluginVersion = DataOperations.getProperty(dataFile, Messages.version);
			if (pluginVersion != null && !pluginVersion.isEmpty()) {
				properties.put("Plugin Version", pluginVersion);
			}

			String instID = DataOperations.getProperty(dataFile, Messages.instID);
			if (instID != null && !instID.isEmpty()) {
				metrics.put("Installation ID", Double.parseDouble(instID));
			}
		}
		telemetry.trackEvent(eventName, properties, metrics);
		telemetry.flush();
	}
}
