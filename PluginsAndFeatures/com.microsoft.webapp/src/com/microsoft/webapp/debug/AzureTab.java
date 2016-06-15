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
package com.microsoft.webapp.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.gigaspaces.azure.util.PreferenceWebAppUtil;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.config.Messages;
import com.microsoft.webapp.config.WebAppDeployDialog;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;


public class AzureTab extends AbstractLaunchConfigurationTab {
	Combo txtName;
	Map<WebSite, WebSiteConfiguration> webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
	List<WebSite> webSiteList = new ArrayList<WebSite>();
	Link lnkWebApp;

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;

		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;

		container.setLayout(gridLayout);
		container.setLayoutData(gridData);
		setControl(container);

		Label lblName = new Label(container, SWT.LEFT);
		lblName.setText(Messages.appComboLbl);
		gridData = new GridData();
		gridData.verticalIndent = 10;
		lblName.setLayoutData(gridData);

		txtName = new Combo(container, SWT.READ_ONLY);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalIndent = 10;
		txtName.setLayoutData(gridData);
		txtName.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				updateLaunchConfigurationDialog();
			}
		});


		lnkWebApp = new Link(container, SWT.RIGHT);
		lnkWebApp.setText(Messages.lnkWebApp);
		gridData = new GridData();
		gridData.verticalIndent = 10;
		lnkWebApp.setLayoutData(gridData);
		lnkWebApp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String prevSelection = txtName.getText();
				WebAppDeployDialog dialog = new WebAppDeployDialog(getShell());
				dialog.create();
				dialog.open();
				List<String> listToDisplay = loadWebApps();
				if (!listToDisplay.isEmpty()) {
					if (!prevSelection.isEmpty() && listToDisplay.contains(prevSelection)) {
						txtName.setText(prevSelection);
					} else {
						txtName.setText(listToDisplay.get(0));
					}
				}
				updateLaunchConfigurationDialog();
			}
		});
	}

	@Override
	public String getName() {
		return "Azure";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			List<String> listToDisplay = loadWebApps();
			String website = config.getAttribute(AzureLaunchConfigurationAttributes.WEBSITE_DISPLAY, "");
			if (!listToDisplay.isEmpty()) {
				if (!website.isEmpty() && listToDisplay.contains(website)) {
					txtName.setText(website);
				} else {
					txtName.setText(listToDisplay.get(0));
				}
			}
		} catch (CoreException e) {
			Activator.getDefault().log(e.getMessage(), e);
		}
	}

	private List<String> loadWebApps() {
		List<String> listToDisplay = new ArrayList<>();
		try {
			webSiteConfigMap = PreferenceWebAppUtil.load();
			if (webSiteConfigMap != null) {
				// filter out Java Web Apps
				for (Iterator<Entry<WebSite, WebSiteConfiguration>> it = webSiteConfigMap.entrySet().iterator(); it.hasNext();) {
					Entry<WebSite, WebSiteConfiguration> entry = it.next();
					if (entry.getValue().getJavaContainer().isEmpty()) {
						it.remove();
					}
				}
				webSiteList = new ArrayList<WebSite>(webSiteConfigMap.keySet());
				Collections.sort(webSiteList, new Comparator<WebSite>() {
					@Override
					public int compare(WebSite ws1, WebSite ws2) {
						return ws1.getName().compareTo(ws2.getName());
					}
				});

				listToDisplay = WAEclipseHelperMethods.prepareListToDisplay(webSiteConfigMap, webSiteList);
				txtName.setItems(listToDisplay.toArray(new String[listToDisplay.size()]));
				Map<String, Boolean> mp = Activator.getDefault().getWebsiteDebugPrep();
				for (WebSite webSite : webSiteList) {
					String name = webSite.getName();
					if (!mp.containsKey(name)) {
						mp.put(name, false);
					}
				}
				Activator.getDefault().setWebsiteDebugPrep(mp);
			}
		} catch (Exception e) {
			Activator.getDefault().log(e.getMessage(), e);
		}
		return listToDisplay;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(AzureLaunchConfigurationAttributes.WEBSITE_DISPLAY, txtName.getText());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		String webapp = txtName.getText().trim();
		if (webapp.isEmpty()) {
			setErrorMessage(Messages.noDebugApp);
			return false;
		} else {
			setErrorMessage(null);
			return true;
		}
	}

	@Override
	public Image getImage() {
		return WebAppUtils.getImage(Messages.dlgImgPathSmall);
	}
}
