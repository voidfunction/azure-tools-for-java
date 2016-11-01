package com.microsoft.auth.ui;

import com.microsoft.auth.AuthException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

class Params implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final String redirectUri;
    public final String requestUri;
    boolean shouldExit = false;

    public Params(String requestUri, String redirectUri) {
        this(requestUri, redirectUri, false);
    }
    public Params(String requestUri, String redirectUri, boolean shouldExit) {
        this.redirectUri = redirectUri;
        this.requestUri = requestUri;
        this.shouldExit = shouldExit;
    }
}

class Response implements java.io.Serializable { 
    private static final long serialVersionUID = 1L;
    enum Status{
        Success,
        Failed,
        Canceled
    };
    Status status;
    String data = null;
    public Response(Status status, String data) {
        this.status = status;
        this.data = data;
    }
}

public class AuthDialog extends Application {
    final static Logger log = Logger.getLogger(AuthDialog.class.getName());
    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false);
        Map<String, String> map = getParameters().getNamed();
        final int communicationPort = Integer.parseInt(map.get("communicationPort"));
        final int hostReadyPort = Integer.parseInt(map.get("hostReadyPort"));
        Callable<Void> worker = new Callable<Void>() {
            int count = 1;
            @Override
            public Void call() throws Exception {
                ServerSocket listener = new ServerSocket(communicationPort);
                // let clients know the thread is ready
                Socket hostReadySocket = new Socket("127.0.0.1", hostReadyPort);
                hostReadySocket.close();

                try {
                    while (true) {
                        log.log(Level.FINEST, String.format("JavaFx host: listening port %d...", communicationPort));
                        final Socket socket = listener.accept();
                        log.log(Level.FINEST, String.format("JavaFx host: connecton accepted %d", count++));
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        final Params params = (Params) in.readObject();
                        
                        if(params.shouldExit) {
                            break;
                        }
                        
                        log.log(Level.FINEST, "JavaFx host: Staring a new window");
                        
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                buildWindow(new Stage(), params, socket);
                            }
                        });
                    }
                } finally {
                    listener.close();
                }
                return null;
            }
        };
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(worker);
        log.log(Level.FINEST, "==> JavaFx host is up and running!");
    }
    
    public void buildWindow(final Stage stage, final Params params, final Socket socket) {

        log.log(Level.FINEST, "redirectUri: " + params.redirectUri);
        log.log(Level.FINEST, "requestUri: " + params.requestUri);

        stage.setWidth(500);
        stage.setHeight(700);
        Scene scene = new Scene(new Group());
        final WebView browser = new WebView();

        final WebEngine webEngine = browser.getEngine();
        webEngine.locationProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(newValue.startsWith(params.redirectUri)) {
                    sendResponse(new Response(Response.Status.Success, newValue), socket);
                    stage.close();
                }
            }
        });
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                log.log(Level.FINEST, "setOnCloseRequest");
                sendResponse(new Response(Response.Status.Canceled, null), socket);
            }
        }); 
        webEngine.load(params.requestUri);
        scene.setRoot(browser);
        stage.setScene(scene);
        stage.show();
    }

    private void sendResponse(Response response, Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(response);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void buildStage(final Stage stage, String address) {
        log.log(Level.FINEST, "Inside dialog");
        stage.setTitle(address);
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
    
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        engine.load(address);
    
        javafx.scene.control.Button btn = new javafx.scene.control.Button();
        btn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                stage.close();
            }
        });
        root.getChildren().add(view);
        root.getChildren().add(btn);
    
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();    
    }
  
    
    @Override
    public void stop() throws AuthException{
        log.log(Level.FINEST, "Stage is closing");
//        Platform.exit();
    }
    
}