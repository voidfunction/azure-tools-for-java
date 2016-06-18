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
package com.microsoft.webapp.config;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.management.websites.models.SkuOptions;
import com.microsoft.azure.management.websites.models.WorkerSizeOptions;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class CreateAppServicePlanDialog extends TitleAreaDialog {
	Text txtName;
	Combo locCombo;
	Combo pricingCombo;
	Combo workerCombo;
	String subscriptionId;
	String resourceGroup;
	Button okButton;
	String webHostingPlan;
	List<String> plansAcrossSub = new ArrayList<String>();

	public CreateAppServicePlanDialog(Shell parentShell, String subscriptionId, String resourceGroup, List<String> plansAcrossSub) {
		super(parentShell);
		setHelpAvailable(false);
		this.subscriptionId = subscriptionId;
		this.resourceGroup = resourceGroup;
		this.plansAcrossSub = plansAcrossSub;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);	
		newShell.setText(Messages.crtAppPlanTtl);
		Image image = WebAppUtils.getImage(Messages.dlgImgPath);
		if (image != null) {
			setTitleImage(image);
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		okButton = getButton(IDialogConstants.OK_ID);
		fillGeoRegions();
		fillPricingComboBox();
		enableOkBtn();
		return ctrl;
	}

	private void enableOkBtn() {
		if (okButton != null) {
			String name = txtName.getText().trim();
			if (name.isEmpty()
					|| locCombo.getText().isEmpty()
					|| pricingCombo.getText().isEmpty()
					|| workerCombo.getText().isEmpty()) {
				okButton.setEnabled(false);
				setErrorMessage(null);
			} else {
				if (!WAEclipseHelperMethods.isAlphaNumericHyphen(name) || name.length() > 60) {
					setErrorMessage(Messages.nameErrMsg);
					okButton.setEnabled(false);
				} else if (plansAcrossSub.contains(name)) {
					setErrorMessage(Messages.appUniErrMsg);
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
					setErrorMessage(null);
				}
			}
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.crtAppPlanTtl);
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		GridData gridData = new GridData();
		gridLayout.numColumns = 2;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayout(gridLayout);
		container.setLayoutData(gridData);
		createNameCmpnt(container);
		createLocCmpnt(container);
		createPricingCmpnt(container);
		createWorkerCmpnt(container);
		return super.createDialogArea(parent);
	}

	private void createNameCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.name);

		txtName = new Text(container, SWT.LEFT | SWT.BORDER);
		gridData = gridDataForText(180);
		txtName.setLayoutData(gridData);
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				enableOkBtn();
			}
		});
	}

	private void createLocCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.loc);

		locCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		locCombo.setLayoutData(gridData);
	}

	private void createPricingCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.price);

		pricingCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		pricingCombo.setLayoutData(gridData);
		pricingCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
			}

			@Override
			public void widgetSelected(SelectionEvent event) {
				fillWorkerSize(pricingCombo.getText());
			}
		});
	}

	private void createWorkerCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.worker);

		workerCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		workerCombo.setLayoutData(gridData);
	}

	private GridData gridDataForLbl() {
		GridData gridData = new GridData();
		gridData.horizontalIndent = 5;
		gridData.verticalIndent = 10;
		return gridData;
	}

	/**
	 * Method creates grid data for text field.
	 * @return
	 */
	private GridData gridDataForText(int width) {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.END;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = width;
		gridData.verticalIndent = 10;
		gridData.grabExcessHorizontalSpace = true;
		return gridData;
	}

	private void fillGeoRegions() {
		try {
			List<Location> locationList = AzureManagerImpl.getManager().getLocations(subscriptionId);
			if (locationList.size() > 0) {
				List<String> locationNameList = new ArrayList<String>();
				for (Location location : locationList) {
					locationNameList.add(location.getName());
				}
				String[] locArray = locationNameList.toArray(new String[locationNameList.size()]);
				locCombo.setItems(locArray);
				locCombo.setText(locArray[0]);
			} else {
				locCombo.removeAll();
			}
		} catch (AzureCmdException e) {
			Activator.getDefault().log(Messages.errTtl, e);
		}
	}

	private void fillPricingComboBox() {
		List<String> skuOptions = new ArrayList<String>();
		for (SkuOptions sku : SkuOptions.values()) {
			skuOptions.add(sku.toString());
		}
		if (skuOptions.size() > 0) {
			String[] skuArray = skuOptions.toArray(new String[skuOptions.size()]);
			pricingCombo.setItems(skuArray);
			pricingCombo.setText(skuArray[0]);
			fillWorkerSize(skuArray[0]);
		} else {
			pricingCombo.removeAll();
		}
	}

	private void fillWorkerSize(String price) {
		List<String> sizeList = new ArrayList<String>();
		if (price.equalsIgnoreCase(SkuOptions.Free.name()) || price.equalsIgnoreCase(SkuOptions.Shared.name())) {
			sizeList.add(WorkerSizeOptions.Small.name());
		} else {
			for (WorkerSizeOptions size : WorkerSizeOptions.values()) {
				sizeList.add(size.toString());
			}
		}
		if (sizeList.size() > 0) {
			String[] sizeArray = sizeList.toArray(new String[sizeList.size()]);
			workerCombo.setItems(sizeArray);
			workerCombo.setText(sizeArray[0]);
		} else {
			workerCombo.removeAll();
		}
	}

	public String getWebHostingPlan() {
		return webHostingPlan;
	}

	@Override
	protected void okPressed() {
		boolean isValid = false;
		try {
			PluginUtil.showBusy(true, getShell());
			WebHostingPlanCache plan = new WebHostingPlanCache(txtName.getText().trim(), resourceGroup,
					subscriptionId, locCombo.getText(),
					SkuOptions.valueOf(pricingCombo.getText()),
					WorkerSizeOptions.valueOf(workerCombo.getText()));
			AzureManagerImpl.getManager().createWebHostingPlan(subscriptionId, plan);
			webHostingPlan = plan.getName();
			isValid = true;
		} catch (Exception ex) {
			PluginUtil.showBusy(false, getShell());
			String msg = Messages.createPlanMsg;
			if (ex.getMessage().contains("MissingSubscriptionRegistration: The subscription is not registered to use namespace")) {
				msg = msg + " " + Messages.tierErrMsg;
			} else if (ex.getMessage().contains("Conflict: The maximum number of")) {
				msg = msg + " " + Messages.maxPlanMsg;
			}
			msg = msg + "\n" + String.format(Messages.webappExpMsg, ex.getMessage());
			PluginUtil.displayErrorDialogAndLog(getShell(), Messages.errTtl, msg, ex);
		}
		if (isValid) {
			PluginUtil.showBusy(false, getShell());
			super.okPressed();
		}
	}
}
