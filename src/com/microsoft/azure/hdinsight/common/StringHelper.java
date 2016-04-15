package com.microsoft.azure.hdinsight.common;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

    private static final String URL_PREFIX = "https://";

    private static final Pattern pattern = Pattern.compile("https://([^/.]\\.)+[^/.]+/?$");

    public static boolean isNullOrWhiteSpace(String str) {
        if (str == null) {
            return true;
        } else {
            int len = str.length();

            for (int i = 0; i < len; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    public static String concat(@NotNull String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            stringBuilder.append(args[i]);
        }

        return stringBuilder.toString();
    }

    public static String join(@NotNull String delimiter, @NotNull List<String> args) {
        StringBuffer stringBuffer=new StringBuffer();

        for (int i=0; i < args.size(); ++i) {
            stringBuffer.append(args.get(i));
            if (i != args.size() - 1) {
                stringBuffer.append(delimiter);
            }
        }

        return stringBuffer.toString();
    }

    public static String getClusterNameFromEndPoint(@NotNull String endpoint) {
        if (pattern.matcher(endpoint).find()) {
            return endpoint.split("\\.")[0].substring(8);
        }

        return null;
    }
}