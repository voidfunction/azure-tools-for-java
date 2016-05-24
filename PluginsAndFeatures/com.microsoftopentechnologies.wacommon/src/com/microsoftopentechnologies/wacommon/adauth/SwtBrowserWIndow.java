package com.microsoftopentechnologies.wacommon.adauth;

import java.awt.Dimension;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
//import org.eclipse.ui.progress.UIJob;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.Status;
//import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;

public class SwtBrowserWIndow implements com.microsoft.auth.IWebUi {
	
	private String res = null;
	
	private void setResult(String res) {
		this.res = res;
	}
	
	private String getResult() {
		return res;
	}
	
	final Display display;
	
	public SwtBrowserWIndow(){
		System.out.println("==> SwtBrowserWIndow ---------------");
		display = Display.getDefault();
	}

	@Override
	public Future<String> authenticateAsync(URI requestUri, URI redirectUri) {
		
		System.out.println("==> run authenticateAsync ---------------");
		
		final String redirectUriStr = redirectUri.toString();
		final String requestUriStr = requestUri.toString();
		
		System.out.println("==> redirectUriStr: " + redirectUriStr);
		System.out.println("==> requestUriStr: " + requestUriStr);
		
		try {
			
			final Runnable gui = new Runnable() {
//			class BrowserWindow implements Runnable {
				@Override
				public void run() {
					
					try {
						System.out.println("==> run gui ---------------");
				        //Display display = Display.getCurrent();
				        final Shell shell = new Shell(display, SWT.APPLICATION_MODAL | SWT.CLOSE | SWT.TITLE | SWT.BORDER);
				        shell.setLayout(new FillLayout());
				        Monitor monitor = display.getPrimaryMonitor();
				        Rectangle bounds = monitor.getBounds();
				        Dimension size = new Dimension((int) (bounds.width * 0.40), (int) (bounds.height * 0.70));
				        shell.setSize(size.width, size.height);
				        shell.setLocation((bounds.width - size.width) / 2, (bounds.height - size.height) / 2);
				        shell.setActive(); 
				        
				        Browser browser = new Browser(shell, SWT.NONE);
				        browser.addLocationListener(new LocationAdapter() {
				            @Override
				            public void changing(LocationEvent locationEvent) {
				            	System.out.println("==> locationEvent.location: " + locationEvent.location);
				                if(locationEvent.location.startsWith(redirectUriStr)) {
				                	setResult(locationEvent.location);
				                	shell.close();
				                }
				            }
				        });
				        
				        browser.setUrl(requestUriStr);
				        Browser.clearSessions();
				        shell.open();
				        while (!shell.isDisposed()) {
				            if (!display.readAndDispatch())
				            	display.sleep ();
				        }
				        shell.dispose ();
		        
				        
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			
			final Callable<String> worker = new Callable<String>() {
	        	@Override
	        	public String call() {
	        		System.out.println("==> call -------------");
//					try {
//						ServerSocket listener = new ServerSocket(communicationPort);
//						final Socket socket = listener.accept();
//						listener.close();
//						socket.close();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
	        		return getResult();
	        			
	        	}
	        };
       
//	        Display.getDefault().asyncExec(gui);
	        display.syncExec(gui);
//	        display.syncExec(new BrowserWindow());
//	        new Thread(gui).start();
	        return Executors.newSingleThreadExecutor().submit(worker);
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
