package com.microsoft.intellij.util;

import com.intellij.openapi.application.PathManager;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.wacommon.utils.WACommonException;

import java.io.File;

public class PluginHelper {
    /**
     * Gets location of Azure Libraries
     *
     * @throws WACommonException
     */
    public static String getAzureLibLocation() throws WACommonException {
        String libLocation;
        try {
            String pluginInstLoc = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, PluginUtil.PLUGIN_ID);
            libLocation = String.format(pluginInstLoc + "%s%s", File.separator, "lib");
            File file = new File(String.format(libLocation + "%s%s", File.separator, AzureBundle.message("sdkLibBaseJar")));
            if (!file.exists()) {
                throw new WACommonException(AzureBundle.message("SDKLocErrMsg"));
            }
        } catch (WACommonException e) {
            e.printStackTrace();
            throw e;
        }
        return libLocation;
    }
}
