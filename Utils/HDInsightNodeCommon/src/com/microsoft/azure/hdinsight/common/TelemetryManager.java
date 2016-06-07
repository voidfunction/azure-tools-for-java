/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
