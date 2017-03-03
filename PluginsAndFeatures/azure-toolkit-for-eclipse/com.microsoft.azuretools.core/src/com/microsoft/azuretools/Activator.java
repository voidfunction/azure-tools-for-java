/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.microsoftopentechnologies.wacommon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.gigaspaces.azure.views.Messages;
import com.google.gson.Gson;
import com.microsoft.azureexplorer.helpers.IDEHelperImpl;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.eclipse.ui.UIFactory;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginComponent;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import com.microsoft.azuretools.azurecommons.deploy.UploadProgressEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.UploadProgressEventListener;
import org.osgi.framework.FrameworkUtil;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements PluginComponent {

	// The plug-in ID
	public static final String PLUGIN_ID = "WACommon"; //$NON-NLS-1$

	public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

	// User-agent header for Azure SDK calls
	public static final String USER_AGENT = "Azure Toolkit for Eclipse, v%s";

	// The shared instance
	private static Activator plugin;
	// save temporary value of telemetry preference in case of preference page navigation
	public static String prefState = "";

	private PluginSettings settings;
	public static final String CONSOLE_NAME = Messages.consoleName;
	
	private static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();

	private boolean isHDInsightEnabled = false;
	
	private static final EventListenerList UPLOAD_PROGRESS_EVENT_LISTENERS = new EventListenerList();
	public static List<DeploymentEventListener> depEveList = new ArrayList<DeploymentEventListener>();

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
		super.start(context);
		plugin = this;
		DefaultLoader.setPluginComponent(this);
		DefaultLoader.setIdeHelper(new IDEHelperImpl());
		CommonSettings.setUserAgent(String.format(USER_AGENT, FrameworkUtil.getBundle(getClass()).getVersion()));
		// load up the plugin settings
		try {
			loadPluginSettings();
		} catch (IOException e) {
			DefaultLoader.getUIHelper().showException("An error occurred while attempting to load " +
					"settings for the WACommon plugin.", e, "WACommon", false, true);
		}
		isHDInsightEnabled = isHDInsightEnabled(context);
		try {
            if (CommonSettings.getUiFactory() == null)
                CommonSettings.setUiFactory(new UIFactory());
            String wd = "EclipseActionWorkingDir";
            Path dirPath = Paths.get(System.getProperty("user.home"), wd);
            if (!Files.exists(dirPath)) {
                    Files.createDirectory(dirPath);
            }
            CommonSettings.settingsBaseDir = dirPath.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		super.start(context);
	}
	
	public boolean isHDInsightEnabled() {
		return isHDInsightEnabled;
	}
	
	private boolean isHDInsightEnabled(BundleContext context) {
		Bundle [] bundles = context.getBundles();
		boolean isScalaEnabled = false, isHDIEnabled = false;
		for(int i = 0; i < bundles.length; ++i) {
			String symbolicName = bundles[i].getSymbolicName().toLowerCase();
			if(symbolicName.contains("scala-ide")) {
				isScalaEnabled = true;
			} else if(symbolicName.equals("com.microsoft.hdinsights")) {
				isHDIEnabled = true;
			}
		}
		return isScalaEnabled && isHDIEnabled;
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

	private void loadPluginSettings() throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(getResourceAsFile("/resources/settings.json")));
//            reader = new BufferedReader(
//                    new InputStreamReader(
//                            MSOpenTechToolsApplication.class.getResourceAsStream("/resources/settings.json")));
			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}

			Gson gson = new Gson();
			settings = gson.fromJson(sb.toString(), PluginSettings.class);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	public static File getResourceAsFile(String fileEntry) {
		File file = null;
		try {
			URL url = Activator.getDefault().getBundle().getEntry(fileEntry);
			URL fileURL = FileLocator.toFileURL(url);
			URL resolve = FileLocator.resolve(fileURL);
			file = new File(resolve.getFile());
		} catch (Exception e) {
			Activator.getDefault().log(e.getMessage(), e);
		}
		return file;
	}

	/**
     * Logs a message and exception.
     *
     * @param message
     * @param excp : exception.
     */
    public void log(String message, Throwable excp) {
    	getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, excp));
    }

    public void log(String message) {
    	getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    public static String getPrefState() {
    	return prefState;
    }

    public static void setPrefState(String prefState) {
    	Activator.prefState = prefState;
    }

	public PluginSettings getSettings() {
		return settings;
	}

	public String getPluginId() {
		return PLUGIN_ID;
	}
	
	public static void removeUnNecessaryListener() {
		for (int i = 0 ; i < depEveList.size(); i++) {
			removeDeploymentEventListener(depEveList.get(i));
		}
		depEveList.clear();
	}

	public void addDeploymentEventListener(DeploymentEventListener listener) {
		DEPLOYMENT_EVENT_LISTENERS.add(DeploymentEventListener.class, listener);
	}

	public static void removeDeploymentEventListener(DeploymentEventListener listener) {
		DEPLOYMENT_EVENT_LISTENERS.remove(DeploymentEventListener.class, listener);
	}
	
	public void addUploadProgressEventListener(UploadProgressEventListener listener) {
		UPLOAD_PROGRESS_EVENT_LISTENERS.add(UploadProgressEventListener.class, listener);
	}
	
	public void removeUploadProgressEventListener(UploadProgressEventListener listener) {
		UPLOAD_PROGRESS_EVENT_LISTENERS.remove(UploadProgressEventListener.class, listener);		
	}

	public void fireDeploymentEvent(DeploymentEventArgs args) {
		Object[] list = DEPLOYMENT_EVENT_LISTENERS.getListenerList();

		for (int i = 0; i < list.length; i += 2) {
			if (list[i] == DeploymentEventListener.class) {
				((DeploymentEventListener) list[i + 1]).onDeploymentStep(args);
			}
		}
	}

	public void fireUploadProgressEvent(UploadProgressEventArgs args) {
		Object[] list = UPLOAD_PROGRESS_EVENT_LISTENERS.getListenerList();

		for (int i = 0; i < list.length; i += 2) {
			if (list[i] == UploadProgressEventListener.class) {
				((UploadProgressEventListener) list[i + 1]).onUploadProgress(args);
			}
		}
	}
	
	public static MessageConsole findConsole(String name) {
		ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = consolePlugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		}
		// no console found, so create a new one
		MessageConsole messageConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { messageConsole });
		return messageConsole;
	}
}
