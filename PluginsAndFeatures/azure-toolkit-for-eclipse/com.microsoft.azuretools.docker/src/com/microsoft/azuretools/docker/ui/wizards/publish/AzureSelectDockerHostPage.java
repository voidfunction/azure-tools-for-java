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
package com.microsoft.azuretools.docker.ui.wizards.publish;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azuretools.docker.ui.dialogs.AzureViewDockerDialog;
import com.microsoft.azuretools.docker.ui.wizards.createhost.AzureNewDockerWizard;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;

public class AzureSelectDockerHostPage extends WizardPage {
	private static final Logger log =  Logger.getLogger(AzureSelectDockerHostPage.class.getName());

	private Composite mainContainer;
	private Text dockerArtifactPathTextField;
	private Text dockerImageNameTextField;
	private Button dockerArtifactPathBrowseButton;
	private TableViewer dockerHostsTableViewer;
	private Table dockerHostsTable;
	private Button dockerHostsRefreshButton;
	private Button dockerHostsViewButton;
	private Button dockerHostsAddButton;
	private Button dockerHostsDeleteButton;
	private Button dockerHostsEditButton;
	
	private ManagedForm managedForm;
	private ScrolledForm errMsgForm;
	private IMessageManager errDispatcher;

	private IProject project;
	private AzureDockerHostsManager dockerManager;
	private AzureDockerImageInstance dockerImageDescription;
	private AzureSelectDockerWizard wizard;
	
	private DockerHostsTableSelection dockerHostsTableSelection;
	private List<DockerHost> dockerHostsList;
	private List<DockerHost> testInput;

	/**
	 * Create the wizard.
	 */
	public AzureSelectDockerHostPage(AzureSelectDockerWizard wizard) {
		super("Deploying Docker Container on Azure");
		
		this.wizard = wizard;		
		this.dockerManager = wizard.getDockerManager();
		this.dockerImageDescription = wizard.getDockerImageInstance();
		this.project = wizard.getProject();
		
		setTitle("Type an image name, select the artifact's path and check a Docker host to be used");
		setDescription("");
		
		testInput = new ArrayList<>();
		testInput.add(dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName()))));
		testInput.add(dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName()))));
		testInput.add(dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName()))));
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		mainContainer = new Composite(parent, SWT.NULL);

		setControl(mainContainer);
		mainContainer.setLayout(new GridLayout(4, false));
		
		Label lblNewLabel = new Label(mainContainer, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblNewLabel.verticalIndent = 1;
		gd_lblNewLabel.horizontalIndent = 1;
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		lblNewLabel.setText("Docker image name:");
		
		dockerImageNameTextField = new Text(mainContainer, SWT.BORDER);
		GridData gd_dockerImageNameTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_dockerImageNameTextField.verticalIndent = 1;
		dockerImageNameTextField.setLayoutData(gd_dockerImageNameTextField);
		new Label(mainContainer, SWT.NONE);
		
		Label lblNewLabel_1 = new Label(mainContainer, SWT.NONE);
		GridData gd_lblNewLabel_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblNewLabel_1.horizontalIndent = 1;
		lblNewLabel_1.setLayoutData(gd_lblNewLabel_1);
		lblNewLabel_1.setText("Artifact to deploy (.WAR or .JAR):");
		
		dockerArtifactPathTextField = new Text(mainContainer, SWT.BORDER);
		dockerArtifactPathTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		dockerArtifactPathBrowseButton = new Button(mainContainer, SWT.NONE);
		dockerArtifactPathBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerArtifactPathBrowseButton.setText("Browse...");
		
		Label lblHosts = new Label(mainContainer, SWT.NONE);
		lblHosts.setText("Hosts");
		
		Label label = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		
		dockerHostsTableViewer = new TableViewer(mainContainer, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		
		dockerHostsTable = dockerHostsTableViewer.getTable();
		dockerHostsTable.setHeaderVisible(true);
		dockerHostsTable.setLinesVisible(true);
		dockerHostsTable.setToolTipText("Check a Docker host from the list or create a new host");
		GridData gd_dockerHostsTableView = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 7);
		gd_dockerHostsTableView.horizontalIndent = 1;
		gd_dockerHostsTableView.heightHint = 155;
		dockerHostsTable.setLayoutData(gd_dockerHostsTableView);
		
		dockerHostsRefreshButton = new Button(mainContainer, SWT.NONE);
		GridData gd_dockerHostsRefreshButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostsRefreshButton.verticalIndent = 1;
		dockerHostsRefreshButton.setLayoutData(gd_dockerHostsRefreshButton);
		dockerHostsRefreshButton.setText("Refresh");
		
		dockerHostsViewButton = new Button(mainContainer, SWT.NONE);
		dockerHostsViewButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsViewButton.setText("View");
		
		dockerHostsAddButton = new Button(mainContainer, SWT.NONE);
		dockerHostsAddButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsAddButton.setText("Add");
		
		dockerHostsDeleteButton = new Button(mainContainer, SWT.NONE);
		dockerHostsDeleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsDeleteButton.setText("Delete");
		
		dockerHostsEditButton = new Button(mainContainer, SWT.NONE);
		dockerHostsEditButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsEditButton.setText("Edit");
		new Label(mainContainer, SWT.NONE);
		new Label(mainContainer, SWT.NONE);
		
		FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		managedForm = new ManagedForm(mainContainer);
		errMsgForm = managedForm.getForm();
		errMsgForm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		errMsgForm.setBackground(mainContainer.getBackground());
		errDispatcher = managedForm.getMessageManager();
		errMsgForm.setMessage("This is an error message", IMessageProvider.ERROR);
		
		initUIMainContainer(mainContainer);
	}
	
	private void initUIMainContainer(Composite mainContainer) {
		
		dockerImageNameTextField.setText(dockerImageDescription.dockerImageName);
		dockerImageNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerImageNameTip());
		dockerImageNameTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				if (AzureDockerValidationUtils.validateDockerImageName(((Text) event.getSource()).getText())) {
					errDispatcher.removeMessage("dockerImageNameTextField", dockerImageNameTextField);
					setErrorMessage(null);
					setPageComplete(doValidate());
				} else {
					errDispatcher.addMessage("dockerImageNameTextField", AzureDockerValidationUtils.getDockerImageNameTip(), null, IMessageProvider.ERROR, dockerImageNameTextField);
					setErrorMessage("Invalid Docker image name");
					setPageComplete(false);
				}
			}
		});

	    dockerArtifactPathTextField.setToolTipText(AzureDockerValidationUtils.getDockerArtifactPathTip());
		dockerArtifactPathTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				if (AzureDockerValidationUtils.validateDockerArtifactPath(((Text) event.getSource()).getText())) {
					errDispatcher.removeMessage("dockerArtifactPathTextField", dockerArtifactPathTextField);
					setErrorMessage(null);
					setPageComplete(doValidate());
				} else {
					errDispatcher.addMessage("dockerArtifactPathTextField", AzureDockerValidationUtils.getDockerArtifactPathTip(), null, IMessageProvider.ERROR, dockerArtifactPathTextField);
					setErrorMessage("Invalid artifact path");
					setPageComplete(false);
				}
			}
		});

		dockerArtifactPathBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(dockerArtifactPathBrowseButton.getShell(), SWT.OPEN);
				fileDialog.setText("Select Artifact .WAR or .JAR");
				fileDialog.setFilterPath(System.getProperty("user.home"));
				fileDialog.setFilterExtensions(new String[] { "*.?ar", "*.*" });
				String path = fileDialog.open();
				if (path == null || path.toLowerCase().contains(".war") || path.toLowerCase().contains(".jar")) {
					return;
				}
				dockerArtifactPathTextField.setText(path);
				setPageComplete(doValidate());
			}
		});
		
		dockerHostsList = new ArrayList<>();
		for (DockerHost host : testInput) {
			dockerHostsList.add(host);
		}

		TableViewerColumn colHostName = createTableViewerColumn(dockerHostsTableViewer, "Name", 150, 1);
		colHostName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((DockerHost) element).name;
			}
		});
		TableViewerColumn colHostState = createTableViewerColumn(dockerHostsTableViewer, "State", 80, 2);
		colHostState.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				DockerHost dockerHost = (DockerHost) element;
				return dockerHost.hostVM.state != null ? dockerHost.hostVM.state.toString() : "TO_BE_CREATED";
			}
		});
		TableViewerColumn colHostOS = createTableViewerColumn(dockerHostsTableViewer, "OS", 200, 3);
		colHostOS.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((DockerHost) element).hostOSType.toString();
			}
		});
		TableViewerColumn colHostApiUrl = createTableViewerColumn(dockerHostsTableViewer, "API URL", 250, 4);
		colHostApiUrl.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((DockerHost) element).apiUrl.toString();
			}
		});
		dockerHostsTableViewer.setContentProvider(new ArrayContentProvider());
		dockerHostsList = new ArrayList<>();
		dockerHostsTableViewer.setInput(dockerHostsList);
		refreshDockerHostsTable(mainContainer);
		if (!dockerHostsList.isEmpty()) {
			dockerHostsTable.select(0);
			dockerHostsTable.getItem(0).setChecked(true);
			dockerHostsTableSelection = new DockerHostsTableSelection();
			dockerHostsTableSelection.row = 0;
			dockerHostsTableSelection.host = (DockerHost) dockerHostsTable.getItem(0).getData();
		} else {
			dockerHostsTableSelection = null;
		}

		dockerHostsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK) {
					DockerHost dockerHost = (DockerHost) ((TableItem) e.item).getData();
					
					MessageDialog.openInformation(mainContainer.getShell(), "AzureDockerPlugin","Click check " + dockerHost.name);
					if (dockerHostsTableSelection == null || dockerHostsTableSelection.host != dockerHost) {
						dockerHostsTableSelection = new DockerHostsTableSelection();
						dockerHostsTableSelection.row = 0;
						dockerHostsTableSelection.host = dockerHost;
						for (TableItem tableItem : dockerHostsTable.getItems()) {
							if (tableItem != ((TableItem) e.item) && tableItem.getChecked()) {
								tableItem.setChecked(false);
							}
						}
						dockerHostsTable.redraw();
					} else {
						dockerHostsTableSelection = null;
					}
				} else {
					MessageDialog.openInformation(mainContainer.getShell(), "AzureDockerPlugin", String.format("Click select on %d", dockerHostsTable.getSelectionIndex()));
				}
			}
		});

		dockerHostsRefreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    AzureDockerUIResources.updateAzureResourcesWithProgressDialog(mainContainer.getShell(), project);
			    refreshDockerHostsTable(mainContainer);
				setPageComplete(doValidate());
			}
		});
		
		dockerHostsViewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DockerHost dockerHost = (DockerHost) dockerHostsTable.getItem(dockerHostsTable.getSelectionIndex()).getData();
				if (dockerHost != null) {
					AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(mainContainer.getShell(), project, dockerHost, dockerManager);
					viewDockerDialog.open();
				}
			}
		});
		
		dockerHostsAddButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AzureNewDockerWizard newDockerWizard = new AzureNewDockerWizard(project, dockerManager);
				WizardDialog createNewDockerHostDialog = new WizardDialog(mainContainer.getShell(), newDockerWizard);
				if (createNewDockerHostDialog.open() == Window.OK) {
					MessageDialog.openInformation(mainContainer.getShell(), "AzureDockerPlugin", "OK");
//					newDockerWizard.createHost();
					// TODO add real thing here
					dockerHostsList.add(0, dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName()))));
					dockerHostsTable.setEnabled(false);
					dockerHostsRefreshButton.setEnabled(false);
					dockerHostsAddButton.setEnabled(false);
					dockerHostsDeleteButton.setEnabled(false);
					dockerHostsEditButton.setEnabled(false);
					dockerHostsTableViewer.refresh();
					dockerHostsTable.getItem(0).setChecked(true);
					dockerHostsTable.select(0);
				} else {
					MessageDialog.openInformation(mainContainer.getShell(), "AzureDockerPlugin", "Canceled");
				}
			}
		});
		
		dockerHostsDeleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		
		dockerHostsEditButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
	}
	
	private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		
		return viewerColumn;
	}
	
	private void refreshDockerHostsTable(Composite mainContainer) {
		dockerHostsList.clear();
		for (DockerHost host : testInput) {
			dockerHostsList.add(host);
		}

		dockerHostsTableViewer.refresh();
		if (dockerHostsTableSelection != null) {
			int idx = 0;
			boolean selected = false;
			for (DockerHost host : dockerHostsList) {
				if (dockerHostsTableSelection.host.apiUrl.equals(host.apiUrl)) {
					dockerHostsTableSelection.host = host;
					dockerHostsTableSelection.row = idx;
					selected = true;
					break;
				}
				idx++;
			}
			if (selected) {
				dockerHostsTable.getItem(idx).setChecked(true);
			} else {
				dockerHostsTableSelection = null;
			}
		}
	}
	
	public boolean doValidate() {
		return true;
	}
	
	private class DockerHostsTableSelection {
		int row;
		int column;
		DockerHost host;
	}

}
