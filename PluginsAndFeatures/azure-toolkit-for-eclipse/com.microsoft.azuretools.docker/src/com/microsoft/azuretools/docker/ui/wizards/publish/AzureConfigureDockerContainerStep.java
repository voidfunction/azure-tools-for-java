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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AzureConfigureDockerContainerStep extends WizardPage {
	private Text customDockerfileTextField;
	private Text dockerContainerNameTextField;
	private Text dockerContainerPortSettings;

	/**
	 * Create the wizard.
	 */
	public AzureConfigureDockerContainerStep() {
		super("Deploying Docker Container on Azure");
		setTitle("Configure the Docker container to be created");
		setDescription("");
		
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite mainContainer = new Composite(parent, SWT.NULL);

		setControl(mainContainer);
		mainContainer.setLayout(new GridLayout(3, false));
		
		Label lblDockerContainerName = new Label(mainContainer, SWT.NONE);
		GridData gd_lblDockerContainerName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblDockerContainerName.horizontalIndent = 1;
		lblDockerContainerName.setLayoutData(gd_lblDockerContainerName);
		lblDockerContainerName.setText("Container name:");
		
		dockerContainerNameTextField = new Text(mainContainer, SWT.BORDER);
		dockerContainerNameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(mainContainer, SWT.NONE);
		
		Label lblNewLabel = new Label(mainContainer, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.horizontalIndent = 1;
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		lblNewLabel.setText("Dockerfile settings");
		
		Label lblDockerfileSettings = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblDockerfileSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		
		Button predefinedDockerfileRadioButton = new Button(mainContainer, SWT.RADIO);
		GridData gd_predefinedDockerfileRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_predefinedDockerfileRadioButton.horizontalIndent = 3;
		predefinedDockerfileRadioButton.setLayoutData(gd_predefinedDockerfileRadioButton);
		predefinedDockerfileRadioButton.setText("Predefined Docker image");
		new Label(mainContainer, SWT.NONE);
		
		Combo dockerfileComboBox = new Combo(mainContainer, SWT.NONE);
		GridData gd_dockerfileComboBox = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_dockerfileComboBox.horizontalIndent = 22;
		dockerfileComboBox.setLayoutData(gd_dockerfileComboBox);
		new Label(mainContainer, SWT.NONE);
		
		Button customDockerfileRadioButton = new Button(mainContainer, SWT.RADIO);
		GridData gd_customDockerfileRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_customDockerfileRadioButton.horizontalIndent = 3;
		customDockerfileRadioButton.setLayoutData(gd_customDockerfileRadioButton);
		customDockerfileRadioButton.setText("Custom Dockerfile");
		new Label(mainContainer, SWT.NONE);
		
		customDockerfileTextField = new Text(mainContainer, SWT.BORDER);
		GridData gd_customDockerfileTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_customDockerfileTextField.horizontalIndent = 22;
		customDockerfileTextField.setLayoutData(gd_customDockerfileTextField);
		
		Button customDockerfileBrowseButton = new Button(mainContainer, SWT.NONE);
		customDockerfileBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(customDockerfileBrowseButton.getShell(), SWT.OPEN);
				fileDialog.setText("Select Custom Dockerfile");
				fileDialog.setFilterPath(System.getProperty("user.home"));
				fileDialog.setFilterExtensions(new String[] { "Dockerfile", "*.*" });
				String path = fileDialog.open();
				if (path == null) {
					return;
				}
				customDockerfileTextField.setText(path);
			}
		});
		customDockerfileBrowseButton.setText("Browse...");
		
		Label label = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Label lblPortSettings = new Label(mainContainer, SWT.NONE);
		GridData gd_lblPortSettings = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblPortSettings.horizontalIndent = 1;
		lblPortSettings.setLayoutData(gd_lblPortSettings);
		lblPortSettings.setText("Port settings:");
		
		dockerContainerPortSettings = new Text(mainContainer, SWT.BORDER);
		dockerContainerPortSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(mainContainer, SWT.NONE);
		
		Label lblForExampletcp = new Label(mainContainer, SWT.NONE);
		GridData gd_lblForExampletcp = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblForExampletcp.horizontalIndent = 22;
		lblForExampletcp.setLayoutData(gd_lblForExampletcp);
		lblForExampletcp.setText("For example \"10022:22\"");
		new Label(mainContainer, SWT.NONE);
	}

}
