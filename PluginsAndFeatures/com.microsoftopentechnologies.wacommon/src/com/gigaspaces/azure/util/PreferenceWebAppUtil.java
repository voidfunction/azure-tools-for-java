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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.wacommon.Activator;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class PreferenceWebAppUtil {
	private static final String PREF_FILE_NAME = Messages.prefFileName;
	private static final String PREF_KEY = PREF_FILE_NAME + ".webapps";
	private static final PreferenceWebAppUtil INSTANCE = new PreferenceWebAppUtil();
	private static boolean loaded;

	public synchronized static void save(Map<WebSite, WebSiteConfiguration> map) {
		INSTANCE.savePreferences(map);
	}

	private void savePreferences(Map<WebSite, WebSiteConfiguration> map) {
		ByteArrayOutputStream buffer = null;
		try {
			Preferences prefs = PluginUtil.getPrefs(PREF_FILE_NAME);
			buffer = new ByteArrayOutputStream();
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				if (map != null) {
					output.writeObject(map);
				}
			} finally {
				output.close();
			}
			prefs.putByteArray(PREF_KEY, buffer.toByteArray());
			prefs.flush();
		} catch (BackingStoreException e) {
			Activator.getDefault().log(Messages.err,e);
		} catch (IOException e) {
			Activator.getDefault().log(Messages.err,e);
		} finally {
			try {
				buffer.reset();
				buffer.close();
			} catch (IOException ioException) {
				//just ignore
			}
		}
	}

	public static boolean isLoaded() {
		return loaded;
	}

	public static void setLoaded(boolean bool) {
		loaded = bool;
	}

	public static Map<WebSite, WebSiteConfiguration> load() throws RestAPIException {
		return INSTANCE.loadPreferences();
	}

	private Map<WebSite, WebSiteConfiguration> loadPreferences() {
		Preferences prefs = PluginUtil.getPrefs(PREF_FILE_NAME);
		Map<WebSite, WebSiteConfiguration> mapToRet = null;
		try {
			byte[] data = prefs.getByteArray(PREF_KEY, null);
			if (data != null) {
				ByteArrayInputStream buffer = new ByteArrayInputStream(data);
				ObjectInput input = new ObjectInputStream(buffer);
				try {
					@SuppressWarnings("unchecked")
					Map<WebSite, WebSiteConfiguration> map = (Map<WebSite, WebSiteConfiguration>) input.readObject();
					mapToRet = map;
				} finally {
					input.close();
				}
			}
		} catch (IOException e) {
			Activator.getDefault().log(Messages.err,e);
		} catch (ClassNotFoundException e) {
			Activator.getDefault().log(Messages.err,e);
		}
		return mapToRet;
	}
}
