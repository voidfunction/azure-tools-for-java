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
package com.gigaspaces.azure.util;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.persistent.util.MessageUtil;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gigaspaces.azure.runnable.CacheAccountWithProgressWindow;
import com.gigaspaces.azure.runnable.LoadAccountWithProgressWindow;
import com.gigaspaces.azure.wizards.WizardCacheManager;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccount;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.wacommon.commoncontrols.Messages;
import com.microsoftopentechnologies.wacommon.storageregistry.PreferenceUtilStrg;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

import javax.xml.bind.JAXBException;

/**
 * Class has common methods which
 * handles publish settings file and extract data.
 * Methods get called whenever user clicks
 * "Import from publish settings file..." button
 * on publish wizard or preference page dialog.
 */
public class MethodUtils {
	/**
	 * Method checks file selected by user is valid
	 * and call method which extracts data from it.
	 * @param fileName
	 */
	public static PublishData handleFile(String fileName, TableViewer tableViewer) {
		if (fileName != null && !fileName.isEmpty()) {
			File file = new File(fileName);
			PublishData publishDataToCache = null;
			//TODO: Check if this is publish settings
			publishDataToCache = handlePublishSettings(file);
			

			if (publishDataToCache == null) {
				return null;
			}
			WizardCacheManager.
			setCurrentPublishData(publishDataToCache);
			// Make centralized storage registry.
			prepareListFromPublishData();
			if (tableViewer != null) {
				tableViewer.refresh();
			}
			return publishDataToCache;
		} else {
			return null;
		}
	}

	/**
	 * Method extracts data from publish settings file.
	 * @param file
	 * @return
	 */
	public static PublishData handlePublishSettings(File file) {
		PublishData data = MethodUtils.createPublishDataObj(file);
		/*
		 * If data is equal to null,
		 * then publish settings file already exists.
		 * So don't load information again.
		 */
		if (data != null) {
			Display.getDefault().syncExec(new
					CacheAccountWithProgressWindow(file, data, new Shell(),
							Messages.loadingCred));
			PreferenceUtil.save();
		}
		return data;
	}

	/**
	 * Method prepares storage account list.
	 * Adds data from publish settings file.
	 */
	public static void prepareListFromPublishData() {
		List<StorageAccount> strgList = StorageAccountRegistry.getStrgList();
		Collection<PublishData> publishDatas = WizardCacheManager.getPublishDatas();
		strgList = com.microsoftopentechnologies.azurecommons.deploy.util.MethodUtils.
				prepareListFromPublishData(strgList, publishDatas);
		PreferenceUtilStrg.save();
	}

	/**
	 * When we start new eclipse session,
	 * reload the subscription and storage account
	 * registry information just for once.
	 * @param tableViewer
	 */
	public static void loadSubInfoFirstTime(TableViewer tableViewer) {
		if (!PreferenceUtil.isLoaded()) {
			Display.getDefault().syncExec(new LoadAccountWithProgressWindow(null, new Shell()));
			PreferenceUtilStrg.load();
			prepareListFromPublishData();
		}
		tableViewer.refresh();
	}

	/**
	 * Method extracts data from publish settings file
	 * and create Publish data object.
	 * @param file
	 * @return
	 */
	public static PublishData createPublishDataObj(
			File file) {
		PublishData data;
		try {
			data = com.microsoftopentechnologies.azurecommons.deploy.util.UIUtils.parse(file);
		} catch (JAXBException e) {
			MessageUtil.displayErrorDialog(PluginUtil.getParentShell(),
					Messages.importDlgTitle,
					String.format(Messages.importDlgMsg,
							file.getName(),
							Messages.failedToParse));
			return null;
		}
//		try {
//			// I am of the opinion that this can be completely removed - need to revisit
//			WindowsAzureRestUtils.getConfiguration(file, data.getSubscriptionIds().get(0));
//		} catch (Exception e) {
//            String errorMessage;
//            Throwable cause = e.getCause();
//            if (e instanceof RuntimeException && cause != null && cause instanceof ClassNotFoundException
//                    && cause.getMessage() != null && cause.getMessage().contains("org.bouncycastle.jce.provider.BouncyCastleProvider")) {
//                errorMessage = Messages.importDlgMsgJavaVersion;
//            } else {
//                errorMessage = Messages.importDlgMsg;
//            }
//			MessageUtil.displayErrorDialog(PluginUtil.getParentShell(),
//					Messages.importDlgTitle,
//					String.format(errorMessage,
//							file.getName(),
//							Messages.failedToParse));
//			return null;
//		}
		if (WizardCacheManager.findPublishDataBySubscriptionId(data.getSubscriptionIds().get(0)) != null) {
            MessageDialog.openInformation(PluginUtil.getParentShell(), Messages.loadingCred, Messages.credentialsExist);
		}
		data.setCurrentSubscription(data.getPublishProfile().
				getSubscriptions().get(0));
		return data;
	}
}
