package com.microsoft.intellij.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryClient;

import com.microsoftopentechnologies.azurecommons.xmlhandling.DataOperations;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AppInsightsCustomEvent {
    static String key = "9ee9694d-128e-4c2b-903f-dbe694548bf0";
    static String dataFile = WAHelper.getTemplateFile(message("dataFileName"));

    public static void create(String eventName, String version) {
        if (new File(dataFile).exists()) {
            String prefValue = DataOperations.getProperty(dataFile, message("prefVal"));
            if (prefValue != null && !prefValue.isEmpty() && prefValue.equalsIgnoreCase("true")) {
                TelemetryClient telemetry = new TelemetryClient();
                telemetry.getContext().setInstrumentationKey(key);
                Map<String, String> properties = new HashMap<String, String>();
                if (version != null && !version.isEmpty()) {
                    properties.put("Library Version", version);
                }
                String pluginVersion = DataOperations.getProperty(dataFile, message("pluginVersion"));
                if (pluginVersion != null && !pluginVersion.isEmpty()) {
                    properties.put("Plugin Version", pluginVersion);
                }

                Map<String, Double> metrics = new HashMap<String, Double>();
                String instID = DataOperations.getProperty(dataFile, message("instID"));
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
        if (new File(dataFile).exists()) {
            String pluginVersion = DataOperations.getProperty(dataFile, message("pluginVersion"));
            if (pluginVersion != null && !pluginVersion.isEmpty()) {
                properties.put("Plugin Version", pluginVersion);
            }

            String instID = DataOperations.getProperty(dataFile, message("instID"));
            if (instID != null && !instID.isEmpty()) {
                metrics.put("Installation ID", Double.parseDouble(instID));
            }
        }
        telemetry.trackEvent(eventName, properties, metrics);
        telemetry.flush();
    }
}
