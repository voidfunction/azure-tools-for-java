package com.microsoftopentechnologies.wacommon.adauth;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SwtBrowserWIndow implements com.microsoft.auth.IWebUi {
	
	private String res = null;
	
	private void setResult(String res) {
		this.res = res;
	}
	
	private String getResult() {
		return res;
	}
	
	public SwtBrowserWIndow(){
		//System.out.println("==> SwtBrowserWIndow ctor---------------");
	}

	@Override
	public Future<String> authenticateAsync(URI requestUri, URI redirectUri) {
		
		//System.out.println("==> run authenticateAsync ---------------");
		
		final String redirectUriStr = redirectUri.toString();
		final String requestUriStr = requestUri.toString();
		
		System.out.println("==> redirectUriStr: " + redirectUriStr);
		System.out.println("==> requestUriStr: " + requestUriStr);
		
		try {
			
			final Runnable gui = new Runnable() {
				@Override
				public void run() {
					
					try {
						//System.out.println("==> run gui ---------------");
				        Display display = Display.getDefault();
				        final Shell activeShell = display.getActiveShell();
				        final Shell shell = new Shell(activeShell, SWT.APPLICATION_MODAL | SWT.CLOSE | SWT.TITLE | SWT.BORDER);
				        shell.setLayout(new FillLayout());
				        final int HEIGHT = 700;
				        final int WIDTH = 500;
				        shell.setSize(WIDTH, HEIGHT);
				        shell.setActive(); 
				        
				        Browser.clearSessions();
				        final Browser browser = new Browser(shell, SWT.NONE);
				        
				        browser.addLocationListener(new LocationAdapter() {
				            @Override
				            public void changing(LocationEvent locationEvent) {
				            	//System.out.println("==> locationEvent.location: " + locationEvent.location);
				                if(locationEvent.location.startsWith(redirectUriStr)) {
				                	setResult(locationEvent.location);
				                	Display.getDefault().asyncExec(new Runnable() {
										@Override
										public void run() {
											//System.out.println("==> shell.close() ---------------");
											browser.stop();
						                	shell.close();
										}
				                	});
				                }
				            }
				        });
				        
				        browser.setUrl(requestUriStr);
				        shell.open();
				        while (shell != null && !shell.isDisposed()) {
				            if (!display.readAndDispatch())
				            	display.sleep ();
				        }
				        
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			
			final Callable<String> worker = new Callable<String>() {
	        	@Override
	        	public String call() {
	        		return getResult();
	        	}
	        };
       
	        Display.getDefault().syncExec(gui);
	        // just to return future to comply interface
	        return Executors.newSingleThreadExecutor().submit(worker);
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
