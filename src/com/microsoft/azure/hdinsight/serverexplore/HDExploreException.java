package com.microsoft.azure.hdinsight.serverexplore;

import java.io.PrintWriter;
import java.io.StringWriter;

public class HDExploreException extends Exception {
    private String mErrorLog;

    public HDExploreException(String message) {
        super(message);

        mErrorLog = "";
    }

    public HDExploreException(String message, String errorLog) {
        super(message);

        mErrorLog = errorLog;
    }

    public HDExploreException(String message, Throwable throwable) {
        super(message, throwable);

        if (throwable instanceof HDExploreException) {
            mErrorLog = ((HDExploreException) throwable).getErrorLog();
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);

            throwable.printStackTrace(writer);
            writer.flush();

            mErrorLog = sw.toString();
        }
    }

    public String getErrorLog() {
        return mErrorLog;
    }
}