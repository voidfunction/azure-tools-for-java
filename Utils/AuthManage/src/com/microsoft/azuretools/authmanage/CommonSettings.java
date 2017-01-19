package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.authmanage.interact.IUIFactory;

/**
 * Created by shch on 10/10/2016.
 */
public class CommonSettings {
    public static String settingsBaseDir = null;
    public static final String authMethodDetailsFileName = "AuthMethodDetails.json";
    public static IUIFactory uiFactory;
    public static String USER_AGENT = "Azure Toolkit";

    /**
     * Need this as a static method when we call this class directly from Eclipse or IntelliJ plugin to know plugin version
     */
    public static void setUserAgent(String userAgent) {
        USER_AGENT = userAgent;
    }
}
