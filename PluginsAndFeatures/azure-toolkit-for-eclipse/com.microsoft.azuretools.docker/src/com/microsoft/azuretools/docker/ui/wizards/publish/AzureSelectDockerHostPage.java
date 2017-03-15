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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AzureSelectDockerHostPage extends WizardPage {
	private Text dockerArtifactPathTextField;
	private Text dockerImageNameTextField;
	private Table dockerHostsTableView;

	/**
	 * Create the wizard.
	 */
	public AzureSelectDockerHostPage() {
		super("Deploying Docker Container on Azure");
		setTitle("Type an image name, select the artifact's path and check a Docker host to be used");
		setDescription("");
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite mainContainer = new Composite(parent, SWT.NULL);

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
		
		Button dockerArtifactPathBrowseButton = new Button(mainContainer, SWT.NONE);
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
			}
		});
		dockerArtifactPathBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerArtifactPathBrowseButton.setText("Browse...");
		
		Label lblHosts = new Label(mainContainer, SWT.NONE);
		lblHosts.setText("Hosts");
		
		Label label = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		
		TableViewer tableViewer = new TableViewer(mainContainer, SWT.BORDER | SWT.FULL_SELECTION);
		dockerHostsTableView = tableViewer.getTable();
		dockerHostsTableView.setToolTipText("Check a Docker host from the list or create a new host");
		GridData gd_dockerHostsTableView = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 7);
		gd_dockerHostsTableView.horizontalIndent = 1;
		gd_dockerHostsTableView.heightHint = 155;
		dockerHostsTableView.setLayoutData(gd_dockerHostsTableView);
		
		Button dockerHostsRefreshButton = new Button(mainContainer, SWT.NONE);
		GridData gd_dockerHostsRefreshButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostsRefreshButton.verticalIndent = 1;
		dockerHostsRefreshButton.setLayoutData(gd_dockerHostsRefreshButton);
		dockerHostsRefreshButton.setText("Refresh");
		
		Button dockerHostsViewButton = new Button(mainContainer, SWT.NONE);
		dockerHostsViewButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsViewButton.setText("View");
		
		Button dockerHostsAddButton = new Button(mainContainer, SWT.NONE);
		dockerHostsAddButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsAddButton.setText("Add");
		
		Button dockerHostsDeleteButton = new Button(mainContainer, SWT.NONE);
		dockerHostsDeleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsDeleteButton.setText("Delete");
		
		Button dockerHostsEditButton = new Button(mainContainer, SWT.NONE);
		dockerHostsEditButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dockerHostsEditButton.setText("Edit");
		new Label(mainContainer, SWT.NONE);
		new Label(mainContainer, SWT.NONE);
	}
}
