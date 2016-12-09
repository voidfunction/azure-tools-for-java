package com.microsoft.azuretools.authmanage.srvpri.report;

import com.microsoft.azuretools.authmanage.FileStorage;

import java.io.IOException;

/**
 * Created by vlashch on 10/20/16.
 */
public class FileListener implements IListener<String> {
    private final FileStorage fsReport;
    public FileListener(String filename, String path) throws Exception {
        fsReport = new FileStorage(filename, path);
    }

    @Override
    public void listen(String message) {
        try {
            fsReport.appendln(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
