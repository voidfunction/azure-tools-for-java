package com.microsoft.azure.hdinsight.common;

import com.microsoft.applicationinsights.TelemetryClient;

import java.util.HashMap;
import java.util.Map;

public class TelemetryManager {

    private static final String Instrumentation_Key = "9de190e1-4c5f-4d76-9883-68bc53af267a";

    public static void postEvent(String eventName, Map<String, String> properties, Map<String, Double> metrics){
        if(properties == null){
            properties = new HashMap<>();
        }

        if(metrics == null){
            metrics = new HashMap<>();
        }

        TelemetryClient telemetry = new TelemetryClient();
        telemetry.getContext().setInstrumentationKey(Instrumentation_Key);
        telemetry.trackEvent(eventName, properties, metrics);
        telemetry.flush();
    }
}
