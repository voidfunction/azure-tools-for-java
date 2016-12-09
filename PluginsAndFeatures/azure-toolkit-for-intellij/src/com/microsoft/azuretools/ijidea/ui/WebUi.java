package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.azuretools.adauth.IWebUi;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by vlashch on 10/17/16.
 */
class WebUi implements IWebUi {
    LoginWindow loginWindow;
    @Override
    public Future<String> authenticateAsync(URI requestUri, URI redirectUri) {
        System.out.println("==> requestUri: " + requestUri);
        final String requestUriStr = requestUri.toString();
        final String redirectUriStr = redirectUri.toString();

        if(ApplicationManager.getApplication().isDispatchThread()) {
            buildAndShow(requestUri.toString(), redirectUri.toString());
        } else {
            ApplicationManager.getApplication().invokeAndWait( new Runnable() {
                @Override
                public void run() {
                    buildAndShow(requestUriStr, redirectUriStr);
                }
            }, ModalityState.any());
        }

        final Callable<String> worker = new Callable<String>() {
            @Override
            public String call() {
                return loginWindow.getResult();
            }
        };

        // just to return future to comply interface
        return Executors.newSingleThreadExecutor().submit(worker);
    }

    private void buildAndShow(String requestUri, String redirectUri) {
        loginWindow = new LoginWindow(requestUri, redirectUri);
        loginWindow.show();
    }
}
