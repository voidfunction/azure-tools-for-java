package com.microsoft.azure.hdinsight.common;

import com.intellij.openapi.application.ApplicationManager;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;

public class UIHelperImpl implements UIHelper {
    @Override
    public void showException(@NotNull final String message,
                              @Nullable final Throwable ex,
                              @NotNull final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                String headerMessage = getHeaderMessage(message, ex, appendEx, suggestDetail);

                String details = getDetails(ex);

                ErrorMessageForm em = new ErrorMessageForm(title);
                em.showErrorMessageForm(headerMessage, details);
                em.show();
            }
        });
    }

    @Override
    public void showError(@NotNull final String message, @NotNull final String title) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    @NotNull
    private static String getHeaderMessage(@NotNull String message, @Nullable Throwable ex,
                                           boolean appendEx, boolean suggestDetail) {
        String headerMessage = message.trim();

        if (ex != null && appendEx) {
            String exMessage = (ex.getLocalizedMessage() == null || ex.getLocalizedMessage().isEmpty()) ? ex.getMessage() : ex.getLocalizedMessage();
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + exMessage;
        }

        if (suggestDetail) {
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + "Click on '" + ErrorMessageForm.advancedInfoText + "' for detailed information on the cause of the error.";
        }

        return headerMessage;
    }

    @NotNull
    private static String getDetails(@Nullable Throwable ex) {
        String details = "";

        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            details = sw.toString();

            if (ex instanceof HDExploreException) {
                String errorLog = ((HDExploreException) ex).getErrorLog();
                if (!StringHelper.isNullOrWhiteSpace(errorLog)) {
                    details = errorLog;
                }
            }
        }

        return details;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}