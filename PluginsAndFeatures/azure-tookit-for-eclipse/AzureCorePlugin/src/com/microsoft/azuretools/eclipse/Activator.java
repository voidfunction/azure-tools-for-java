package com.microsoft.azuretools.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.ISubscriptionSelectionListener;
import com.microsoft.azuretools.eclipse.ui.UIFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.microsoft.azuretools.webapp-plugin"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
        try {
            if (CommonSettings.getUiFactory() == null)
                CommonSettings.setUiFactory(new UIFactory());
            String wd = "EclipseActionWorkingDir";
            Path dirPath = Paths.get(System.getProperty("user.home"), wd);
            if (!Files.exists(dirPath)) {
                    Files.createDirectory(dirPath);
            }
            CommonSettings.settingsBaseDir = dirPath.toString();
            
            if (AuthMethodManager.getInstance().isSignedIn()) {
            	System.out.println("==> You are signed IN!!!");
            	registerSignOutListener();
            } else {
            	System.out.println("==> You are signed OUT!!!");
            	registerSignInListener();
            }
            
            AuthMethodManager.getInstance().addSignInEventListener(new Runnable() {
				
				@Override
				public void run() {
					System.out.println("==> You have been signed IN!!!");
					registerSignOutListener();
				}
			});
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		super.start(context);
		plugin = this;
	}
	
	private void registerSignOutListener() {
		try {
			AuthMethodManager.getInstance().getAzureManager().getSubscriptionManager().addListener(new ISubscriptionSelectionListener() {
				
				@Override
				public void update(boolean isSignedOut) {
					if (isSignedOut) {
						System.out.println("==> You have been signed OUT!!!");
					}
					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	private void registerSignInListener() {
        try {
			AuthMethodManager.getInstance().addSignInEventListener(new Runnable() {
				
				@Override
				public void run() {
					System.out.println("==> You have been signed IN!!!");
					registerSignOutListener();
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
