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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.wacommon.commoncontrols.NewResourceGroupDialog;

public class CreateWebAppDialog extends TitleAreaDialog {
	Text txtName;
	Combo subscriptionCombo;
	Combo groupCombo;
	Combo servicePlanCombo;
	Combo containerCombo;
	Button newGroupBtn;
	Button newPlanBtn;
	Button okButton;
	Map<String, String> subMap = new HashMap<String, String>();
	List<WebHostingPlanCache> webHostingPlans = new ArrayList<WebHostingPlanCache>();
	List<String> plansAcrossSub = new ArrayList<String>();
	List<String> webSiteNames = new ArrayList<String>();
	// values to be used in WebAppDeployDialog
	String finalName = "";
	String finalSubId = "";
	WebHostingPlanCache finalPlan = null;
	String finalContainer = "";

	public CreateWebAppDialog(Shell parentShell, List<WebSite> webSiteList) {
		super(parentShell);
		setHelpAvailable(false);
		for (WebSite ws : webSiteList) {
			webSiteNames.add(ws.getName());
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.crtWebAppTtl);
		Image image = WebAppUtils.getImage();
		if (image != null) {
			setTitleImage(image);
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		okButton = getButton(IDialogConstants.OK_ID);
		populateSubscriptions();
		String subName = subscriptionCombo.getText();
		if (subName != null && !subName.isEmpty()) {
			String subId = findKeyAsPerValue(subName);
			populateResourceGroups(subId, "");
			String resName = groupCombo.getText();
			if (resName != null && !resName.isEmpty()) {
				populateServicePlans(subId, resName, "");
			}
		}
		enableOkBtn();
		return ctrl;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.crtWebAppTtl);
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		GridData gridData = new GridData();
		gridLayout.numColumns = 3;
		gridLayout.marginBottom = 10;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayout(gridLayout);
		container.setLayoutData(gridData);
		createNameCmpnt(container);
		createWebContainerCmpnt(container);
		createSubCmpnt(container);
		createResGrpCmpnt(container);
		createAppPlanCmpnt(container);
		return super.createDialogArea(parent);
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

	private void createNameCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.dnsName);

		txtName = new Text(container, SWT.LEFT | SWT.BORDER);
		gridData = gridDataForText(180);
		txtName.setLayoutData(gridData);
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				enableOkBtn();
			}
		});

		Label lblWebsite = new Label(container, SWT.LEFT);
		gridData = gridDataForLbl();
		lblWebsite.setLayoutData(gridData);
		lblWebsite.setText(Messages.dnsWebsite);
	}

	private void createSubCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.sub);

		subscriptionCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		subscriptionCombo.setLayoutData(gridData);
		subscriptionCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String subName = subscriptionCombo.getText();
				if (subName != null && !subName.isEmpty()) {
					String subId = findKeyAsPerValue(subName);
					populateResourceGroups(subId, "");
					String resName = groupCombo.getText();
					if (resName != null && !resName.isEmpty()) {
						populateServicePlans(subId, resName, "");
					}
				}
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
			}
		});

		new Link(container, SWT.NO);
	}

	private void createResGrpCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.resGrp);

		groupCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		groupCombo.setLayoutData(gridData);
		groupCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String resName = groupCombo.getText();
				String subName = subscriptionCombo.getText();
				if (subName != null && !subName.isEmpty() && resName != null && !resName.isEmpty()) {
					populateServicePlans(findKeyAsPerValue(subName), resName, "");
				}
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		newGroupBtn = new Button(container, SWT.PUSH);
		newGroupBtn.setText(Messages.newBtn);
		gridData = new GridData();
		gridData.widthHint = 100;
		gridData.verticalIndent = 10;
		newGroupBtn.setLayoutData(gridData);
		newGroupBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				NewResourceGroupDialog dialog = new NewResourceGroupDialog(getShell(), subscriptionCombo.getText());
				int result = dialog.open();
				if (result == Window.OK) {
					ResourceGroupExtended group = NewResourceGroupDialog.getResourceGroup();
					if (group != null) {
						String subId = findKeyAsPerValue(subscriptionCombo.getText());
						populateResourceGroups(subId, group.getName());
						populateServicePlans(subId, group.getName(), "");
					}
				}
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
			}
		});
	}

	private void createAppPlanCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.appPlan);

		servicePlanCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		servicePlanCombo.setLayoutData(gridData);

		newPlanBtn = new Button(container, SWT.PUSH);
		newPlanBtn.setText(Messages.newBtn);
		gridData = new GridData();
		gridData.verticalIndent = 10;
		gridData.widthHint = 100;
		newPlanBtn.setLayoutData(gridData);
		newPlanBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String subId = findKeyAsPerValue(subscriptionCombo.getText());
				CreateAppServicePlanDialog dialog = new CreateAppServicePlanDialog(getShell(),
						subId, groupCombo.getText(), plansAcrossSub);
				int result = dialog.open();
				if (result == Window.OK) {
					String plan = dialog.getWebHostingPlan();
					if (plan != null) {
						populateServicePlans(subId, groupCombo.getText(), plan);
					}
				}
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}

	private void createWebContainerCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl();
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.container);

		containerCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		containerCombo.setLayoutData(gridData);
		List<String> containerList = new ArrayList<String>();
		for (WebAppsContainers type : WebAppsContainers.values()) {
			containerList.add(type.getName());
		}
		String[] containerArray = containerList.toArray(new String[containerList.size()]);
		containerCombo.setItems(containerArray);
		containerCombo.setText(containerArray[0]);

		new Link(container, SWT.NO);
	}

	private String findKeyAsPerValue(String subName) {
		String key = "";
		for (Map.Entry<String, String> entry : subMap.entrySet()) {
			if (entry.getValue().equalsIgnoreCase(subName)) {
				key = entry.getKey();
				break;
			}
		}
		return key;
	}

	private void populateSubscriptions() {
		try {
			List<Subscription> subList = AzureManagerImpl.getManager().getSubscriptionList();
			if (subList.size() > 0) {
				for (Subscription sub : subList) {
					subMap.put(sub.getId(), sub.getName());
				}
				Collection<String> values = subMap.values();
				String[] subNameArray = values.toArray(new String[values.size()]);
				subscriptionCombo.setItems(subNameArray);
				subscriptionCombo.setText(subNameArray[0]);
				newGroupBtn.setEnabled(true);
			} else {
				subscriptionCombo.removeAll();
				newGroupBtn.setEnabled(false);
				newPlanBtn.setEnabled(false);
			}
		} catch (AzureCmdException e) {
			Activator.getDefault().log(Messages.errTtl, e);
		}
	}

	private void populateResourceGroups(String subId, String valToSet) {
		try {
			List<String> groupList = AzureManagerImpl.getManager().getResourceGroupNames(subId);
			if (groupList.size() > 0) {
				String[] groupArray = groupList.toArray(new String[groupList.size()]);
				groupCombo.setItems(groupArray);
				if (valToSet.isEmpty()) {
					groupCombo.setText(groupArray[0]);
				} else {
					groupCombo.setText(valToSet);
				}
				newPlanBtn.setEnabled(true);
			} else {
				groupCombo.removeAll();
				newPlanBtn.setEnabled(false);
			}
			// prepare list of App Service plans for selected subscription
			PrepareListJob job = new PrepareListJob("");
			job.setGroupList(groupList);
			job.setSubId(subId);
			job.schedule();
		} catch (AzureCmdException e) {
			Activator.getDefault().log(Messages.errTtl, e);
		}
	}

	private class PrepareListJob extends Job {
		List<String> groupList;
		String subId;

		public PrepareListJob(String name) {
			super(name);
		}

		public void setGroupList(List<String> groupList) {
			this.groupList = groupList;
		}

		public void setSubId(String subId) {
			this.subId = subId;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("", IProgressMonitor.UNKNOWN);
			try {
				// prepare list of App Service plans for selected subscription
				plansAcrossSub = new ArrayList<String>();
				for (String groupName : groupList) {
					List<WebHostingPlanCache> plans = AzureManagerImpl.getManager().getWebHostingPlans(subId, groupName);
					for (WebHostingPlanCache plan : plans) {
						plansAcrossSub.add(plan.getName());
					}
				}
			} catch(Exception ex) {
				Activator.getDefault().log(Messages.loadErrMsg, ex);
				super.setName("");
				monitor.done();
				return Status.CANCEL_STATUS;
			}
			super.setName("");
			monitor.done();
			return Status.OK_STATUS;
		}
	}

	private void populateServicePlans(String subId, String group, String valToSet) {
		try {
			webHostingPlans = AzureManagerImpl.getManager().getWebHostingPlans(subId, group);
			List<String> plans = new ArrayList<String>(); 
			if (webHostingPlans.size() > 0) {
				for (WebHostingPlanCache plan : webHostingPlans) {
					plans.add(plan.getName());
				}
				String[] planArray = plans.toArray(new String[plans.size()]);
				servicePlanCombo.setItems(planArray);
				if (valToSet.isEmpty()) {
					servicePlanCombo.setText(planArray[0]);
				} else {
					servicePlanCombo.setText(valToSet);
				}
			} else {
				servicePlanCombo.removeAll();
			}
		} catch (AzureCmdException e) {
			Activator.getDefault().log(Messages.errTtl, e);
		}
	}

	private void enableOkBtn() {
		if (okButton != null) {
			String name = txtName.getText().trim();
			if (name.isEmpty()
					|| subscriptionCombo.getText().isEmpty()
					|| groupCombo.getText().isEmpty()
					|| servicePlanCombo.getText().isEmpty()
					|| containerCombo.getText().isEmpty()) {
				okButton.setEnabled(false);
				if (subscriptionCombo.getText().isEmpty() || subscriptionCombo.getItemCount() <= 0) {
					setErrorMessage(Messages.noSubErrMsg);
				} else if (groupCombo.getText().isEmpty() || groupCombo.getItemCount() <= 0) {
					setErrorMessage(Messages.noGrpErrMsg);
				} else if (servicePlanCombo.getText().isEmpty() || servicePlanCombo.getItemCount() <= 0) {
					setErrorMessage(Messages.noPlanErrMsg);
				} else {
					setErrorMessage(null);
				}
			} else {
				if (!WAEclipseHelperMethods.isAlphaNumericHyphen(name) || name.length() > 60) {
					setErrorMessage(Messages.nameErrMsg);
					okButton.setEnabled(false);
				} else if (webSiteNames.contains(name)) {
					setErrorMessage(Messages.inUseErrMsg);
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
					setErrorMessage(null);
				}
			}
		}
	}

	@Override
	protected void okPressed() {
		finalName = txtName.getText().trim();
		finalSubId = findKeyAsPerValue(subscriptionCombo.getText());
		finalPlan = webHostingPlans.get(servicePlanCombo.getSelectionIndex());
		finalContainer = containerCombo.getText();
		super.okPressed();
	}

	public String getFinalName() {
		return finalName;
	}

	public String getFinalSubId() {
		return finalSubId;
	}

	public WebHostingPlanCache getFinalPlan() {
		return finalPlan;
	}

	public String getFinalContainer() {
		return finalContainer;
	}
}
