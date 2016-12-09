package com.microsoft.azuretools.adauth;

public class StringUtils {
    public static boolean isNullOrWhiteSpace(final String str) {
        return str == null || str.trim().length() == 0;
    }
    public static boolean isNullOrEmpty(final String str) {
    	return str == null || str.length() == 0;
    }
}
