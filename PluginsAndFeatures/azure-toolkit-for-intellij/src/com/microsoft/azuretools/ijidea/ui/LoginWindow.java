package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;

/**
 * Created by vlashch on 10/17/16.
 */
class LoginWindow extends DialogWrapper {
    public final String redirectUri;
    public final String requestUri;
    private String res = null;

    private final JFXPanel fxPanel;

    private void setResult(String res) {
        this.res = res;
    }

    public String getResult() {
        return res;
    }

    public LoginWindow(String requestUri, String redirectUri) {
        super(null, false, IdeModalityType.IDE);

        this.redirectUri =  redirectUri;
        this.requestUri =  requestUri;

        fxPanel = new JFXPanel();
        setModal(true);
        setTitle("Azure Login Dialog");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        fxPanel.setPreferredSize(new Dimension(500, 750));
        Platform.setImplicitExit(false);
        Runnable fxWorker = new Runnable() {
            @Override
            public void run() {
                Group root = new Group();
                final WebView browser = new WebView();
                final WebEngine webEngine = browser.getEngine();
                webEngine.locationProperty().addListener(new ChangeListener<String>(){
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

                        if(newValue.startsWith(redirectUri)) {
                            setResult(newValue);
                            closeDlg();
                        }
                    }
                });

                Scene scene = new Scene(browser);
                fxPanel.setScene(scene);
                webEngine.load(requestUri);
            }
        };

        Platform.runLater(fxWorker);
        return  fxPanel;
    }

    private void closeDlg() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                doOKAction();
            }
        }, ModalityState.any());
    }

    @Override
    protected JComponent createSouthPanel() {
        return null;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "Auth4jLoginDialog";
    }
}