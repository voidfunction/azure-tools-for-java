package com.microsoft.azure.hdinsight.common;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenUrlAction implements ActionListener {
    private URI uri;

    public OpenUrlAction(String url) {
        try
        {
            uri = new URI(url);
        }catch (URISyntaxException exception) {
            uri = null;
            DefaultLoader.getUIHelper().showError(exception.getMessage(), exception.getClass().getName());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(uri == null) {
            return;
        }

        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(uri);
            }catch (IOException exception) {
                DefaultLoader.getUIHelper().showError(exception.getMessage(), exception.getClass().getName());
            }
        } else {
            DefaultLoader.getUIHelper().showError("Couldn't open browser in current OS system", "Open URL in Browser");
        }
    }
}
