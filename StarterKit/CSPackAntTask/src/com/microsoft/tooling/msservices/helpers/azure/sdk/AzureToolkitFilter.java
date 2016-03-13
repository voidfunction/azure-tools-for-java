package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestContext;
import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestFilter;

public class AzureToolkitFilter implements ServiceRequestFilter {
    private static String USER_AGENT = "Azure Toolkit";

    /**
     * Need this as a static method when we call this class directly from Eclipse or IntelliJ plugin to know plugin version
     */
    public static void setUserAgent(String userAgent) {
        USER_AGENT = userAgent;
    }

    public void filter(ServiceRequestContext request) {
        String userAgent;
        if (request.getHeader("User-Agent") != null) {
            String currentUserAgent = request.getHeader("User-Agent");
            userAgent = USER_AGENT + " " + currentUserAgent;
            request.removeHeader("User-Agent");
        } else {
            userAgent = USER_AGENT;
        }
        request.setHeader("User-Agent", userAgent);
    }
}
