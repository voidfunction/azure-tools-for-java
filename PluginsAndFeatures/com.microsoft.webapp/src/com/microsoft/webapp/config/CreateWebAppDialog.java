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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.microsoft.azure.management.resources.models.ResourceGroupExtended;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebAppsContainers;
import com.microsoft.tooling.msservices.model.ws.WebHostingPlanCache;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.webapp.activator.Activator;
import com.microsoft.webapp.util.WebAppUtils;
import com.microsoftopentechnologies.azurecommons.roleoperations.JdkSrvConfigUtilMethods;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageAccountRegistry;
import com.microsoftopentechnologies.azurecommons.storageregistry.StorageRegistryUtilMethods;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.wacommon.commoncontrols.NewResourceGroupDialog;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class CreateWebAppDialog extends TitleAreaDialog {
	Text txtName;
	Combo subscriptionCombo;
	Combo groupCombo;
	Combo servicePlanCombo;
	Combo containerCombo;
	Combo jdkCombo;
	Button newGroupBtn;
	Button newPlanBtn;
	Button okButton;
	Button defaultJDK;
	Button customJDK;
	Button customJDKUser;
	Text customJDKUserUrl;
	Combo storageCombo;
	Label servicePlanDetailsLocationLbl;
	Label servicePlanDetailsPricingTierLbl;
	Label servicePlanDetailsInstanceSizeLbl;
	HashMap<String, WebHostingPlanCache> hostingPlanMap = new HashMap<String, WebHostingPlanCache>();
	Map<String, String> subMap = new HashMap<String, String>();
	List<String> plansAcrossSub = new ArrayList<String>();
	List<String> webSiteNames = new ArrayList<String>();
	// values to be used in WebAppDeployDialog
	String finalName = "";
	String finalSubId = "";
	WebHostingPlanCache finalPlan = null;
	String finalContainer = "";
	String finalJDK = "";
	String finalURL = "";
	String finalKey = "";
	File cmpntFile = new File(PluginUtil.getTemplateFile(com.microsoftopentechnologies.wacommon.utils.Messages.cmpntFileName));

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
		Image image = WebAppUtils.getImage(Messages.dlgImgPath);
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
				populateServicePlans(subId, resName, null);
			}
		}
		enableOkBtn();
		return ctrl;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(Messages.crtWebAppTtl);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;

		// Tab controls
		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayoutData(gridData);

		TabItem tabBasic = new TabItem(folder, SWT.NONE);
		tabBasic.setText("Basic");
		tabBasic.setControl(createBasicTab(folder));

		TabItem tabJDK = new TabItem(folder, SWT.NONE);
		tabJDK.setText("JDK");
		tabJDK.setControl(createJDKCmpnt(folder));
		
		//initialize
		defaultJDK.setSelection(true);
		
		return super.createDialogArea(parent);
	}

	public Control createBasicTab(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayout(gridLayout);
		container.setLayoutData(gridData);

		createNameCmpnt(container);
		createWebContainerCmpnt(container);
		createSubCmpnt(container);
		createResGrpCmpnt(container);
		createAppPlanCmpnt(container);
		return container;
	}

	private GridData gridDataForLbl(int span) {
		GridData gridData = new GridData();
		gridData.horizontalIndent = 5;
		gridData.verticalIndent = 10;
		if (span > 0) {
			gridData.horizontalSpan = span;
		}
		return gridData;
	}

	/**
	 * Method creates grid data for text field.
	 * @return
	 */
	private GridData gridDataForText(int width) {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = width;
		gridData.verticalIndent = 10;
		gridData.grabExcessHorizontalSpace = true;
		return gridData;
	}

	private void createNameCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl(0);
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
		gridData = gridDataForLbl(0);
		lblWebsite.setLayoutData(gridData);
		lblWebsite.setText(Messages.dnsWebsite);
	}

	private void createSubCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl(0);
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
						populateServicePlans(subId, resName, null);
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
		GridData gridData = gridDataForLbl(0);
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
					populateServicePlans(findKeyAsPerValue(subName), resName, null);
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
		gridData.verticalIndent = 10;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
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
						populateServicePlans(subId, group.getName(), null);
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
		GridData gridData = gridDataForLbl(0);
		lblName.setLayoutData(gridData);
		lblName.setText(Messages.appPlan);

		servicePlanCombo = new Combo(container, SWT.READ_ONLY);
		gridData = gridDataForText(180);
		servicePlanCombo.setLayoutData(gridData);
		servicePlanCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String resName = servicePlanCombo.getText();
				WebHostingPlanCache plan = hostingPlanMap.get(resName);
				pupulateServicePlanDetails(plan);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		newPlanBtn = new Button(container, SWT.PUSH);
		newPlanBtn.setText(Messages.newBtn);
		gridData = new GridData();
		gridData.verticalIndent = 10;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
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
		
		// placeholder
		new Label(container, SWT.LEFT);
		
		Composite appPlanDetailsCmpt = new Composite(container, SWT.NONE);
		
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		appPlanDetailsCmpt.setLayout(gl);
		
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		appPlanDetailsCmpt.setLayoutData(gridData);
		appPlanDetailsCmpt.setLayoutData(gridData);
		
		// pricing link
		Link linkPrice = new Link(appPlanDetailsCmpt, SWT.LEFT);
		linkPrice.setLayoutData(gridData);
		linkPrice.setText(Messages.lnkPrice);
		linkPrice.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().
					getExternalBrowser().openURL(new URL(event.text));
				}
				catch (Exception ex) {
					Activator.getDefault().log(ex.getMessage());
				}
			}
		});

		new Label(appPlanDetailsCmpt, SWT.NONE).setText(Messages.loc);
		servicePlanDetailsLocationLbl = new Label(appPlanDetailsCmpt, SWT.LEFT);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		servicePlanDetailsLocationLbl.setLayoutData(gridData);
		servicePlanDetailsLocationLbl.setText("-");

		new Label(appPlanDetailsCmpt, SWT.NONE).setText(Messages.price);
		servicePlanDetailsPricingTierLbl = new Label(appPlanDetailsCmpt, SWT.NONE);
		servicePlanDetailsPricingTierLbl.setLayoutData(gridData);
		servicePlanDetailsPricingTierLbl.setText("-");
		
		new Label(appPlanDetailsCmpt, SWT.NONE).setText(Messages.worker);
		servicePlanDetailsInstanceSizeLbl = new Label(appPlanDetailsCmpt, SWT.NONE);
		servicePlanDetailsInstanceSizeLbl.setLayoutData(gridData);
		servicePlanDetailsInstanceSizeLbl.setText("-");
	}

	private void createWebContainerCmpnt(Composite container) {
		Label lblName = new Label(container, SWT.LEFT);
		GridData gridData = gridDataForLbl(0);
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

	private Control createJDKCmpnt(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayout(gridLayout);
		container.setLayoutData(gridData);

		defaultJDK =  new Button(container, SWT.RADIO);
		defaultJDK.setText(Messages.defaultJdk);
		defaultJDK.setLayoutData(gridDataForLbl(2));

		customJDK = new Button(container, SWT.RADIO);
		customJDK.setText(Messages.thirdJdk);
		customJDK.setLayoutData(gridDataForLbl(2));

		jdkCombo = new Combo(container, SWT.READ_ONLY);
		jdkCombo.setLayoutData(gridDataForText(150));
		enableCustomJDK(false);

		Link link = new Link(container, SWT.LEFT);
		link.setText(Messages.dplDlgSerBtn);
		link.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				boolean choice = MessageDialog.openConfirm(getShell(), Messages.title, Messages.dplSerBtnMsg);
				if (choice) {
					try {
						// create new web app dialog
						getShell().close();
						// deploy web app dialog
						getParentShell().close();
						if (cmpntFile.exists() && cmpntFile.isFile()) {
							IFileStore store = EFS.getLocalFileSystem().
									getStore(cmpntFile.toURI());
							IWorkbenchPage benchPage = PlatformUI.
									getWorkbench().getActiveWorkbenchWindow()
									.getActivePage();
							IDE.openEditorOnFileStore(benchPage, store);
						}
					} catch (PartInitException e) {
						Activator.getDefault().log(e.getMessage(), e);
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		customJDKUser = new Button(container, SWT.RADIO);
		customJDKUser.setText(Messages.customJdk);
		customJDKUser.setLayoutData(gridDataForLbl(2));

		customJDKUserUrl = new Text(container, SWT.LEFT | SWT.BORDER);
		gridData = gridDataForText(150);
		customJDKUserUrl.setLayoutData(gridData);
		
		Link linkTemp = new Link(container, SWT.LEFT);
		linkTemp.setVisible(false);

		storageCombo = new Combo(container, SWT.READ_ONLY);
		storageCombo.setLayoutData(gridDataForText(150));
		enableCustomJDKUser(false);

		Link linkStorage = new Link(container, SWT.LEFT);
		linkStorage.setText(Messages.linkLblAcc);

		defaultJDK.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				enableCustomJDK(false);
				enableCustomJDKUser(false);
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		customJDK.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				enableCustomJDK(true);
				enableCustomJDKUser(false);
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		customJDKUser.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				enableCustomJDK(false);
				enableCustomJDKUser(true);
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		storageCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				storageComboListener();
				enableOkBtn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		customJDKUserUrl.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				String url = customJDKUserUrl.getText().trim();
				String nameInUrl = StorageRegistryUtilMethods.getAccNameFromUrl(url);
				storageCombo.setText(JdkSrvConfigUtilMethods.getNameToSet(
						url, nameInUrl, StorageRegistryUtilMethods.getStorageAccountNames(false)));
				enableOkBtn();
			}
		});
		return container;
	}

	private void storageComboListener() {
		int index = storageCombo.getSelectionIndex();
		String url = customJDKUserUrl.getText().trim();
		// check value is not none
		if (index > 0) {
			String newUrl = StorageAccountRegistry.
					getStrgList().get(index - 1).getStrgUrl();
			/*
			 * If URL is blank and new storage account selected
			 * then auto generate with storage accounts URL.
			 */
			if (url.isEmpty()) {
				customJDKUserUrl.setText(newUrl);
			} else {
				/*
				 * If storage account in combo box and URL
				 * are in sync then update
				 * corresponding portion of the URL
				 * with the URI of the newly selected storage account
				 * (leaving the container and blob name unchanged.
				 */
				String oldVal = StorageRegistryUtilMethods.
						getSubStrAccNmSrvcUrlFrmUrl(url);
				String newVal = StorageRegistryUtilMethods.
						getSubStrAccNmSrvcUrlFrmUrl(newUrl);
				if (oldVal.equalsIgnoreCase(url)) {
					// old URL is not correct blob storage URL then set new url
					customJDKUserUrl.setText(newUrl);
				} else {
					customJDKUserUrl.setText(url.replaceFirst(oldVal, newVal));
				}
			}
		}
	}

	private void enableCustomJDK(boolean enable) {
		if (!enable) {
			try {
				String [] thrdPrtJdkArr = WindowsAzureProjectManager.getThirdPartyJdkNames(cmpntFile, "");
				// check at least one element is present
				if (thrdPrtJdkArr.length >= 1) {
					jdkCombo.setItems(thrdPrtJdkArr);
					String valueToSet = "";
					valueToSet = WindowsAzureProjectManager.getFirstDefaultThirdPartyJdkName(cmpntFile);
					if (valueToSet.isEmpty()) {
						valueToSet = thrdPrtJdkArr[0];
					}
					jdkCombo.setText(valueToSet);
				}
			} catch (WindowsAzureInvalidProjectOperationException e) {
				Activator.getDefault().log(e.getMessage());
			}
		}
		jdkCombo.setEnabled(enable);
	}

	private void enableCustomJDKUser(boolean enable) {
		customJDKUserUrl.setEnabled(enable);
		storageCombo.setEnabled(enable);
		if (!enable) {
			customJDKUserUrl.setText("");
			String [] storageAccs = StorageRegistryUtilMethods.getStorageAccountNames(false);
			if (storageAccs.length >= 1) {
				storageCombo.setItems(storageAccs);
				storageCombo.setText(storageAccs[0]);
			}
		}
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
			hostingPlanMap.clear();
			servicePlanCombo.removeAll();
			List<WebHostingPlanCache> webHostingPlans = AzureManagerImpl.getManager().getWebHostingPlans(subId, group);
			if (webHostingPlans.size() > 0) {
				Collections.sort(webHostingPlans, new Comparator<WebHostingPlanCache>() {
					@Override
					public int compare(WebHostingPlanCache o1, WebHostingPlanCache o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});

				for (WebHostingPlanCache plan : webHostingPlans) {
					servicePlanCombo.add(plan.getName());
					hostingPlanMap.put(plan.getName(), plan);
				}	

				if (valToSet == null || valToSet.isEmpty()) {
					servicePlanCombo.setText(servicePlanCombo.getItem(0));
				} else {
					servicePlanCombo.setText(valToSet);
				}	
				pupulateServicePlanDetails(hostingPlanMap.get(servicePlanCombo.getText()));				
			} else {
				pupulateServicePlanDetails(null);
			}
		} catch (AzureCmdException e) {
			Activator.getDefault().log(Messages.errTtl, e);
		}
	}
	
	private void pupulateServicePlanDetails(WebHostingPlanCache plan){
		servicePlanDetailsLocationLbl.setText(plan == null ? "-" : plan.getLocation());
		servicePlanDetailsPricingTierLbl.setText(plan == null ? "-" : plan.getSku().name());
		servicePlanDetailsInstanceSizeLbl.setText(plan == null ? "-" : plan.getWorkerSize().name());
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
				} else if (customJDKUser.getSelection() && !(WAEclipseHelperMethods.isBlobStorageUrl(customJDKUserUrl.getText())
						&& customJDKUserUrl.getText().endsWith(".zip"))) {
					setErrorMessage(Messages.noURLMsg);
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
		finalPlan = hostingPlanMap.get(servicePlanCombo.getText());
		finalContainer = containerCombo.getText();
		if (customJDK.getSelection()) {
			finalJDK = jdkCombo.getText();
		} else if (customJDKUser.getSelection()) {
			finalURL = customJDKUserUrl.getText().trim();
			int strgAccIndex = storageCombo.getSelectionIndex();
			if (strgAccIndex > 0 && !storageCombo.getText().isEmpty()) {
				finalKey = StorageAccountRegistry.getStrgList().get(strgAccIndex - 1).getStrgKey();
			}
		}
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
	
	public String getFinalJDK() {
		return finalJDK;
	}

	public String getFinalURL() {
		return finalURL;
	}

	public String getFinalKey() {
		return finalKey;
	}
}
