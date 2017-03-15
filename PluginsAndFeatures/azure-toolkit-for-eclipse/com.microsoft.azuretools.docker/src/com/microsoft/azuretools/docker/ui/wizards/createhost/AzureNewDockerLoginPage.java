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
package com.microsoft.azuretools.docker.ui.wizards.createhost;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AzureNewDockerLoginPage extends WizardPage {
	private Text dockerHostUsernameTextField;
	private Text dockerHostFirstPwdField;
	private Text dockerHostSecondPwdField;
	private Text dockerHostImportSSHTextField;
	private Text dockerHostNewKeyvaultTextField;
	private Text dockerDaemonPortTextField;
	private Text dockerHostImportTLSTextField;

	private AzureNewDockerWizard wizard;
	private AzureDockerHostsManager dockerManager;
	private DockerHost newHost;
	private IProject project;

	/**
	 * Create the wizard.
	 */
	public AzureNewDockerLoginPage(AzureNewDockerWizard wizard) {
		super("Create Docker Host");
		setTitle("Configure log in credentials and port settings");
		setDescription("");

		this.wizard = wizard;
		this.dockerManager = wizard.getDockerManager();
		this.newHost = wizard.getDockerHost();
		this.project = wizard.getProject();
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite mainContainer = new Composite(parent, SWT.NULL);

//		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
//		Form form = toolkit.createForm(parent);
//		form.setBackground(mainContainer.getBackground());
//		form.addMessageHyperlinkListener(new HyperlinkAdapter());
//		form.setMessage("This is an different message", IMessageProvider.ERROR);
////		form.setVisible(false);

		setControl(mainContainer);
		mainContainer.setLayout(new GridLayout(2, false));
		
		Button dockerHostImportKeyvaultCredsRadioButton = new Button(mainContainer, SWT.RADIO);
		GridData gd_dockerHostImportKeyvaultCredsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostImportKeyvaultCredsRadioButton.horizontalIndent = 5;
		dockerHostImportKeyvaultCredsRadioButton.setLayoutData(gd_dockerHostImportKeyvaultCredsRadioButton);
		dockerHostImportKeyvaultCredsRadioButton.setText("Import credentials from Azure Key Vault:");
		
		Combo dockerHostImportKeyvaultComboBox = new Combo(mainContainer, SWT.READ_ONLY);
		GridData gd_dockerHostImportKeyvaultComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostImportKeyvaultComboBox.widthHint = 230;
		dockerHostImportKeyvaultComboBox.setLayoutData(gd_dockerHostImportKeyvaultComboBox);
		
		Button btnNewLogIn = new Button(mainContainer, SWT.RADIO);
		GridData gd_btnNewLogIn = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnNewLogIn.horizontalIndent = 5;
		btnNewLogIn.setLayoutData(gd_btnNewLogIn);
		btnNewLogIn.setText("New log in credentials:");
		new Label(mainContainer, SWT.NONE);
		
		TabFolder credsTabfolder = new TabFolder(mainContainer, SWT.NONE);
		GridData gd_credsTabfolder = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		gd_credsTabfolder.heightHint = 215;
		credsTabfolder.setLayoutData(gd_credsTabfolder);
		
		TabItem vmCredsTableItem = new TabItem(credsTabfolder, SWT.NONE);
		vmCredsTableItem.setText("VM Credentials");
		
		Composite vmCredsComposite = new Composite(credsTabfolder, SWT.NONE);
		vmCredsTableItem.setControl(vmCredsComposite);
		vmCredsComposite.setLayout(new GridLayout(6, false));
		
		Label lblUsername = new Label(vmCredsComposite, SWT.NONE);
		GridData gd_lblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblUsername.horizontalIndent = 5;
		lblUsername.setLayoutData(gd_lblUsername);
		lblUsername.setText("Username:");
		
		dockerHostUsernameTextField = new Text(vmCredsComposite, SWT.BORDER);
		GridData gd_dockerHostUsernameTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostUsernameTextField.widthHint = 150;
		dockerHostUsernameTextField.setLayoutData(gd_dockerHostUsernameTextField);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		
		Label lblPassword = new Label(vmCredsComposite, SWT.NONE);
		GridData gd_lblPassword = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblPassword.horizontalIndent = 5;
		lblPassword.setLayoutData(gd_lblPassword);
		lblPassword.setText("Password:");
		
		dockerHostFirstPwdField = new Text(vmCredsComposite, SWT.BORDER);
		GridData gd_dockerHostFirstPwdField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostFirstPwdField.widthHint = 150;
		dockerHostFirstPwdField.setLayoutData(gd_dockerHostFirstPwdField);
		
		Label lbloptional = new Label(vmCredsComposite, SWT.NONE);
		lbloptional.setText("(Optional)");
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		
		Label lblConfirm = new Label(vmCredsComposite, SWT.NONE);
		GridData gd_lblConfirm = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblConfirm.horizontalIndent = 5;
		lblConfirm.setLayoutData(gd_lblConfirm);
		lblConfirm.setText("Confirm:");
		
		dockerHostSecondPwdField = new Text(vmCredsComposite, SWT.BORDER);
		GridData gd_dockerHostSecondPwdField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostSecondPwdField.widthHint = 150;
		dockerHostSecondPwdField.setLayoutData(gd_dockerHostSecondPwdField);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		
		Label lblSsh = new Label(vmCredsComposite, SWT.NONE);
		GridData gd_lblSsh = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblSsh.horizontalIndent = 5;
		lblSsh.setLayoutData(gd_lblSsh);
		lblSsh.setText("SSH");
		
		Label label = new Label(vmCredsComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		
		Button dockerHostNoSshRadioButton = new Button(vmCredsComposite, SWT.RADIO);
		GridData gd_dockerHostNoSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
		gd_dockerHostNoSshRadioButton.horizontalIndent = 5;
		dockerHostNoSshRadioButton.setLayoutData(gd_dockerHostNoSshRadioButton);
		dockerHostNoSshRadioButton.setText("None");
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		
		Button dockerHostAutoSshRadioButton = new Button(vmCredsComposite, SWT.RADIO);
		GridData gd_dockerHostAutoSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
		gd_dockerHostAutoSshRadioButton.horizontalIndent = 5;
		dockerHostAutoSshRadioButton.setLayoutData(gd_dockerHostAutoSshRadioButton);
		dockerHostAutoSshRadioButton.setText("Auto-generate");
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		
		Button dockerHostImportSshRadioButton = new Button(vmCredsComposite, SWT.RADIO);
		GridData gd_dockerHostImportSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
		gd_dockerHostImportSshRadioButton.horizontalIndent = 5;
		dockerHostImportSshRadioButton.setLayoutData(gd_dockerHostImportSshRadioButton);
		dockerHostImportSshRadioButton.setText("Import from directory:");
		new Label(vmCredsComposite, SWT.NONE);
		new Label(vmCredsComposite, SWT.NONE);
		
		dockerHostImportSSHTextField = new Text(vmCredsComposite, SWT.BORDER);
		GridData gd_dockerHostImportSSHTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1);
		gd_dockerHostImportSSHTextField.horizontalIndent = 24;
		dockerHostImportSSHTextField.setLayoutData(gd_dockerHostImportSSHTextField);
		
		Button dockerHostImportSSHBrowseButton = new Button(vmCredsComposite, SWT.NONE);
		dockerHostImportSSHBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(dockerHostImportSSHBrowseButton.getShell());
				directoryDialog.setText("Select SSH Keys Directory");
				directoryDialog.setFilterPath(System.getProperty("user.home"));
				String path = directoryDialog.open();
				if (path == null) {
					return;
				}
				dockerHostImportSSHTextField.setText(path);
			}
		});
		dockerHostImportSSHBrowseButton.setText("Browse...");
		
		TabItem daemonCredsTableItem = new TabItem(credsTabfolder, SWT.NONE);
		daemonCredsTableItem.setText("Docker Daemon Credentials");
		
		Composite daemonCredsComposite = new Composite(credsTabfolder, SWT.NONE);
		daemonCredsTableItem.setControl(daemonCredsComposite);
		daemonCredsComposite.setLayout(new GridLayout(4, false));
		
		Label lblDockerDaemonPort = new Label(daemonCredsComposite, SWT.NONE);
		GridData gd_lblDockerDaemonPort = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_lblDockerDaemonPort.horizontalIndent = 5;
		lblDockerDaemonPort.setLayoutData(gd_lblDockerDaemonPort);
		lblDockerDaemonPort.setText("Docker daemon port:");
		
		dockerDaemonPortTextField = new Text(daemonCredsComposite, SWT.BORDER);
		GridData gd_dockerDaemonPortTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerDaemonPortTextField.widthHint = 50;
		dockerDaemonPortTextField.setLayoutData(gd_dockerDaemonPortTextField);
		new Label(daemonCredsComposite, SWT.NONE);
		
		Label lblTlsSecurity = new Label(daemonCredsComposite, SWT.NONE);
		GridData gd_lblTlsSecurity = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblTlsSecurity.horizontalIndent = 5;
		lblTlsSecurity.setLayoutData(gd_lblTlsSecurity);
		lblTlsSecurity.setText("TLS security");
		
		Label label_1 = new Label(daemonCredsComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Button dockerHostNoTlsRadioButton = new Button(daemonCredsComposite, SWT.RADIO);
		GridData gd_dockerHostNoTlsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		gd_dockerHostNoTlsRadioButton.horizontalIndent = 5;
		dockerHostNoTlsRadioButton.setLayoutData(gd_dockerHostNoTlsRadioButton);
		dockerHostNoTlsRadioButton.setText("None");
		new Label(daemonCredsComposite, SWT.NONE);
		
		Button dockerHostAutoTlsRadioButton = new Button(daemonCredsComposite, SWT.RADIO);
		GridData gd_dockerHostAutoTlsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		gd_dockerHostAutoTlsRadioButton.horizontalIndent = 5;
		dockerHostAutoTlsRadioButton.setLayoutData(gd_dockerHostAutoTlsRadioButton);
		dockerHostAutoTlsRadioButton.setText("Auto-generate");
		new Label(daemonCredsComposite, SWT.NONE);
		
		Button dockerHostImportTlsRadioButton = new Button(daemonCredsComposite, SWT.RADIO);
		GridData gd_dockerHostImportTlsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		gd_dockerHostImportTlsRadioButton.horizontalIndent = 5;
		dockerHostImportTlsRadioButton.setLayoutData(gd_dockerHostImportTlsRadioButton);
		dockerHostImportTlsRadioButton.setText("Import from directory:");
		new Label(daemonCredsComposite, SWT.NONE);
		
		dockerHostImportTLSTextField = new Text(daemonCredsComposite, SWT.BORDER);
		GridData gd_dockerHostImportTLSTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd_dockerHostImportTLSTextField.horizontalIndent = 24;
		dockerHostImportTLSTextField.setLayoutData(gd_dockerHostImportTLSTextField);
		
		Button dockerHostImportTLSBrowseButton = new Button(daemonCredsComposite, SWT.NONE);
		dockerHostImportTLSBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(dockerHostImportTLSBrowseButton.getShell());
				directoryDialog.setText("Select TLS Certificate Directory");
				directoryDialog.setFilterPath(System.getProperty("user.home"));
				String path = directoryDialog.open();
				if (path == null) {
					return;
				}
				dockerHostImportTLSTextField.setText(path);
			}
		});
		dockerHostImportTLSBrowseButton.setText("Browse...");
		
		Button dockerHostSaveCredsCheckBox = new Button(mainContainer, SWT.CHECK);
		GridData gd_dockerHostSaveCredsCheckBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostSaveCredsCheckBox.horizontalIndent = 5;
		dockerHostSaveCredsCheckBox.setLayoutData(gd_dockerHostSaveCredsCheckBox);
		dockerHostSaveCredsCheckBox.setText("Save credentials into a new Azure Key Vault:");
		
		dockerHostNewKeyvaultTextField = new Text(mainContainer, SWT.BORDER);
		GridData gd_dockerHostNewKeyvaultTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostNewKeyvaultTextField.widthHint = 210;
		dockerHostNewKeyvaultTextField.setLayoutData(gd_dockerHostNewKeyvaultTextField);
	}
	
	public boolean doValidate() {
		return true;
	}

}
