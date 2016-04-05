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
package com.microsoftopentechnologies.wacommon.commoncontrols;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.gigaspaces.azure.util.MethodUtils;
import com.gigaspaces.azure.util.PreferenceUtil;
import com.gigaspaces.azure.util.PreferenceWebAppUtil;
import com.gigaspaces.azure.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishProfile;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.wacommon.Activator;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class ManageSubscriptionPanel extends Composite {
	private Label lblSubscriptions;
	private Table tblSubscriptions;
	private CheckboxTableViewer tableViewer;
	private Button removeButton;
	private Button signInButton;
	private Button btnImpFrmPubSetFile;
	private Button closeButton;

	private ManageSubscriptionDialog myDialog;

	private List<Subscription> subscriptionList;

	public ManageSubscriptionPanel(Composite parent, int style, ManageSubscriptionDialog manageSubscriptionDialog) {
		super(parent, style);
		this.myDialog = manageSubscriptionDialog;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		setLayout(gridLayout);
		setLayoutData(gridData);

		createSubscriptionTable(this);
		createButtons(this);
		if (myDialog != null && !myDialog.needBtnPubSetFile) {
			btnImpFrmPubSetFile.setEnabled(false);
			removeButton.setEnabled(false);
		}
		loadList();
	}

	private void createButtons(Composite parent) {
		signInButton = createButton(parent, "Sign In...");
		signInButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {

				if (AzureManagerImpl.getManager().authenticated()) {
					clearSubscriptions(false);
				} else {
					BusyIndicator.showWhile(Display.getDefault(), new Runnable(){
						public void run(){
							try {
								List<Subscription> oldSubList = AzureManagerImpl.getManager().getFullSubscriptionList();
								AzureManager apiManager = AzureManagerImpl.getManager();
								apiManager.clearImportedPublishSettingsFiles();
								apiManager.authenticate();

								loadList();
								WizardCacheManager.clearSubscriptions();
								List<Subscription> subscriptions = apiManager.getSubscriptionList();
								PublishData pd  = new PublishData();
								PublishProfile publishProfile = new PublishProfile();
								pd.setPublishProfile(publishProfile);
								Map<WebSite, WebSiteConfiguration> webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
								for (Subscription subscription : subscriptions) {
									com.microsoftopentechnologies.azuremanagementutil.model.Subscription profileSubscription =
											new com.microsoftopentechnologies.azuremanagementutil.model.Subscription();
									profileSubscription.setSubscriptionID(subscription.getId());
									profileSubscription.setSubscriptionName(subscription.getName());
									publishProfile.getSubscriptions().add(profileSubscription);
									List<String> resList = apiManager.getResourceGroupNames(subscription.getId());
									for (String res : resList) {
										List<WebSite> webList = apiManager.getWebSites(subscription.getId(), res);
										for (WebSite webSite : webList) {
											WebSiteConfiguration webSiteConfiguration = apiManager.
													getWebSiteConfiguration(webSite.getSubscriptionId(),
															webSite.getWebSpaceName(), webSite.getName());
											webSiteConfigMap.put(webSite, webSiteConfiguration);
										}
									}
								}
								PreferenceWebAppUtil.save(webSiteConfigMap);
								PreferenceWebAppUtil.setLoaded(true);
								pd.setCurrentSubscription(publishProfile.getSubscriptions().get(0));
								try {
									WizardCacheManager.cachePublishData(null, pd, null);
									PreferenceUtil.save();
								} catch (RestAPIException e1) {
									Activator.getDefault().log(e1.getMessage(), e1);
								} catch (IOException e1) {
									Activator.getDefault().log(e1.getMessage(), e1);
								}
								PluginUtil.createSubscriptionTelemetryEvent(oldSubList, "Azure Explorer login");
							} catch (AzureCmdException e1) {
								PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.error,
										"An error occurred while attempting to sign in to your account.", e1);
							}
						}
					});
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent selectionEvent) {

			}
		});
		btnImpFrmPubSetFile = createButton(parent, "Import Subscriptions");
		btnImpFrmPubSetFile.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ImportSubscriptionDialog dlg = new ImportSubscriptionDialog(getShell());
				dlg.open();
				String fileName = ImportSubscriptionDialog.getPubSetFilePath();
				if (fileName != null && !fileName.isEmpty() && new File(fileName).exists()) {
					try {
						PluginUtil.showBusy(true, getShell());
						List<Subscription> oldSubList = AzureManagerImpl.getManager().getFullSubscriptionList();
						AzureManager apiManager = AzureManagerImpl.getManager();
						if (apiManager.authenticated()) {
							apiManager.clearAuthentication();
							WizardCacheManager.clearSubscriptions();
						}
						apiManager.importPublishSettingsFile(fileName);
						// todo: remove tableViewer?
						MethodUtils.handleFile(ImportSubscriptionDialog.getPubSetFilePath(), tableViewer);
						PluginUtil.createSubscriptionTelemetryEvent(oldSubList, "Azure Explorer import publish settings");
						loadList();
					} catch (Exception e) {
						DefaultLoader.getUIHelper().showException("Error: " + e.getMessage(), e, "Error", false, true);
						Activator.getDefault().log("Error: " + e.getMessage(), e);
					} finally {
						PluginUtil.showBusy(false, getShell());
					}
				} else {
					DefaultLoader.getUIHelper().showException("The specified Subscriptions File does not exist.",
							null,
							"Invalid Subscriptions File Path",
							false,
							false);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		removeButton = createButton(parent, "Clear Subscriptions");
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				clearSubscriptions(true);
			}
		});

		closeButton = createButton(parent, "Close");
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		closeButton.setLayoutData(gridData);
		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (myDialog != null) {
					myDialog.cancelPressed();
				}
			}
		});
	}

	void close() {
		try {
			java.util.List<String> selectedList = new ArrayList<String>();
			for (Object s : tableViewer.getCheckedElements()) {
				selectedList.add(((Subscription) s).getId());
			}
			AzureManagerImpl.getManager().setSelectedSubscriptions(selectedList);

			//Saving the project is necessary to save the changes on the PropertiesComponent
			//            if (project != null) {
			//                project.save();
			//            }


		} catch (AzureCmdException e) {
			PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.error,
        			"An error occurred while setting selected subscriptions", e);
		}
	}

	private void loadList() {
		refreshSignInCaption();

		tblSubscriptions.removeAll();

		//        Vector<Object> vector = new Vector<Object>();
		//        vector.add("");
		//        vector.add("(loading... )");
		//        vector.add("");
		//        model.addRow(vector);

		DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					PluginUtil.showBusy(true, getShell());
					tblSubscriptions.removeAll();

					subscriptionList = AzureManagerImpl.getManager().getFullSubscriptionList();
					if (subscriptionList != null && subscriptionList.size() > 0) {
						tableViewer.setInput(getTableContent());
						if (myDialog.needBtnPubSetFile) {
							removeButton.setEnabled(true);
						}
					} else {
						removeButton.setEnabled(false);
					}
				} catch (AzureCmdException e) {
					PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.error,
		        			"An error occurred while getting subscription list.", e);
				} finally {
					tableViewer.refresh();
					PluginUtil.showBusy(false, getShell());
				}
			}
		});
	}

	public static Button createButton(Composite parent, String name) {
		Button button = new Button(parent, SWT.PUSH | SWT.CENTER);
		button.setText(name);
		return button;
	}

	private void createSubscriptionTable(Composite composite) {
		GridData gridData = new GridData(SWT.LEFT, SWT.BOTTOM, true, false, 4, 1);
		lblSubscriptions = new Label(composite, SWT.LEFT);
		lblSubscriptions.setText("Choose the subscription to use:");
		lblSubscriptions.setLayoutData(gridData);

		tblSubscriptions = new Table(composite, SWT.CHECK | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);

		tblSubscriptions.setHeaderVisible(true);

		tblSubscriptions.setLinesVisible(true);

		gridData = new GridData();
		gridData.heightHint = 120;
		gridData.horizontalIndent = 3;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.horizontalSpan = 4;

		GridLayout gridLayoutTable = new GridLayout();
		gridLayoutTable.numColumns = 2;
		gridLayoutTable.marginRight = 0;
		tblSubscriptions.setLayout(gridLayoutTable);
		tblSubscriptions.setLayoutData(gridData);

		TableColumn subscriptionNameCol = new TableColumn(tblSubscriptions, SWT.FILL);

		subscriptionNameCol.setText("Name");
		subscriptionNameCol.setWidth(160);

		TableColumn subscriptionIdCol = new TableColumn(tblSubscriptions, SWT.FILL);
		subscriptionIdCol.setText("Id");
		subscriptionIdCol.setWidth(300);

		tableViewer = new CheckboxTableViewer(tblSubscriptions);
		tableViewer.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isChecked(Object o) {
				return ((Subscription) o).isSelected();
			}

			public boolean isGrayed(Object o) {
				return false;
			}
		});
		tableViewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object obj, Object obj1) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object obj) {
				return getTableContent();
			}
		});

		tableViewer.setLabelProvider(new ITableLabelProvider() {

			@Override
			public void removeListener(
					ILabelProviderListener ilabelproviderlistener) {
			}

			@Override
			public boolean isLabelProperty(Object element, String s) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener ilabelproviderlistener) {
			}

			@Override
			public String getColumnText(Object element, int i) {
				Subscription rowElement = (Subscription) element;
				String result = "";

				switch (i) {
				case 0:
					result = rowElement.getName();
					break;

				case 1:
					result = rowElement.getId().toString();
					break;
				default:
					break;
				}
				return result;
			}

			@Override
			public Image getColumnImage(Object element, int i) {
				return null;
			}
		});

		tblSubscriptions.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				if (myDialog.needBtnPubSetFile) {
					removeButton.setEnabled(true);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
			}
		});

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(
					SelectionChangedEvent selectionchangedevent) {

				if (selectionchangedevent.getSelection().isEmpty()) {
					removeButton.setEnabled(false);
				}
			}
		});
	}

	private void refreshSignInCaption() {
		boolean isNotSigned = !AzureManagerImpl.getManager().authenticated();
		signInButton.setText(isNotSigned ? "Sign In..." : "Sign Out");
	}

	private void clearSubscriptions(boolean isSigningOut) {
		boolean choice = MessageDialog.openConfirm(getShell(),
				(isSigningOut
						? "Clear Subscriptions"
								: "Sign out"),
				(isSigningOut
						? "Are you sure you would like to clear all subscriptions?"
								: "Are you sure you would like to sign out?"));
		if (choice) {
			AzureManager apiManager = AzureManagerImpl.getManager();
			apiManager.clearAuthentication();
			apiManager.clearImportedPublishSettingsFiles();
			WizardCacheManager.clearSubscriptions();
			PreferenceUtil.save();
			subscriptionList.clear();
			tableViewer.refresh();
			removeButton.setEnabled(false);
			refreshSignInCaption();
			PreferenceWebAppUtil.save(new HashMap<WebSite, WebSiteConfiguration>());
			PreferenceWebAppUtil.setLoaded(false);
		}
	}

	private Object[] getTableContent() {
		return subscriptionList.toArray();
	}

	public Button getSignInButton() {
		return signInButton;
	}
}
