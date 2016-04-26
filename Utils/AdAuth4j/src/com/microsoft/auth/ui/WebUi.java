package com.microsoft.auth.ui;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.microsoft.auth.IWebUi;

import javafx.application.Application;
import org.apache.log4j.Logger;

public class WebUi implements IWebUi {
    final static Logger log = Logger.getLogger(WebUi.class.getName());
    private static boolean javafxAppStarted = false;
    private static int communicationPort;

    private static WebUi instance =  null;
    private boolean useCookie = false;

    public void setUseCookie(boolean useCookie) {
        this.useCookie = useCookie;
    }
    public static synchronized WebUi getInstance() throws Exception {
        if(instance == null) {
            instance = new WebUi();
        }
        return instance;
    }

    @Override
    public  Future<String> authenticateAsync(URI requestUri, URI redirectUri) {
        Callable<String> worker = new Callable<String>() {
            @Override
            public String call() throws Exception {
                synchronized (this) {
                    if (!javafxAppStarted) {
                        // requesting available ports
                        ServerSocket hostReadySocket = new ServerSocket(0);
                        int hostReadyPort =  hostReadySocket.getLocalPort();
                        ServerSocket communicationSocket = new ServerSocket(0);
                        communicationPort = communicationSocket.getLocalPort();
                        communicationSocket.close();

                        log.info(String.format("Starting JavaFx host on port: %d...", communicationPort));
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Application.launch(AuthDialog.class,
                                        String.format("--communicationPort=%d", communicationPort), String.format("--hostReadyPort=%d", hostReadyPort));
                            }}).start();

                        log.info(String.format("JavaFx Client: Waiting on port %d for the JavaFx host to start up ...", hostReadyPort));
                        hostReadySocket.accept();
                        hostReadySocket.close();
                        javafxAppStarted = true;
                    }
                }

                if(useCookie) {
                    java.net.CookieManager cm = new java.net.CookieManager();
                    java.net.CookieHandler.setDefault(cm);
                }

                log.info(String.format("JavaFx Client: connecting to JavaFx host port %d...", communicationPort));
                String serverAddress = "127.0.0.1";
                Socket socket = new Socket(serverAddress, communicationPort);
                Response response = null;
                try {
                    // sending parameters to the JavaFx host
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new Params(requestUri.toString(), redirectUri.toString()));
                    // getting response
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    response = (Response) in.readObject();
                } catch(Exception e) {
                    log.info("WebUi: " + e.getMessage());
                } finally {
                    socket.close();
                }
                
                if(response != null) {
                    if(response.status == Response.Status.Canceled) {
                        log.info("Auth canceled by user");
                    } else if (response.status == Response.Status.Failed) {
                        log.info("Auth failed");
                    } else {
                        log.info("Auth succeeded");
                    }
                }
                return (response == null) 
                        ? null
                        : response.data;
            }
        };
        
        ExecutorService executor = Executors.newWorkStealingPool();
        return executor.submit(worker);
    }
}


