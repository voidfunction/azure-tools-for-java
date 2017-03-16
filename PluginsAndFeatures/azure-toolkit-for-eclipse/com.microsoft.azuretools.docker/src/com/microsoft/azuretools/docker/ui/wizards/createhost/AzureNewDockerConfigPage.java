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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerSubscription;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;

import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Display;

public class AzureNewDockerConfigPage extends WizardPage {
	private Text dockerHostNameTextField;
	private Text dockerNewStorageTextField;
	private Text dockerHostRGTextField;
	private Text dockerHostNewVNetNameTextField;
	private Text dockerHostNewVNetAddrSpaceTextField;
	
	private String prefferedLocation;
	private final String SELECT_REGION = "<select region>";
	
	private AzureNewDockerWizard wizard;
	private AzureDockerHostsManager dockerManager;
	private DockerHost newHost;
	private IProject project;
	
	private final FormToolkit formToolkit = new FormToolkit(Display.getDefault());
	private ManagedForm managedForm;
	private ScrolledForm errMsgForm;
	private IMessageManager errDispatcher;
	private Text dockerSubscriptionIdTextField;

	/**
	 * Create the wizard.
	 */
	public AzureNewDockerConfigPage(AzureNewDockerWizard wizard) {
		super("Create Docker Host");
		setTitle("Configure the new virtual machine");
		setDescription("");

		this.wizard = wizard;		
		this.dockerManager = wizard.getDockerManager();
		this.newHost = wizard.getDockerHost();
		this.project = wizard.getProject();

		prefferedLocation = null;
	}

	/**
	 * Create contents of the wizard.
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite mainContainer = new Composite(parent, SWT.NO_BACKGROUND);
		setControl(mainContainer);
		mainContainer.setLayout(new GridLayout(3, false));
		
		Label lblName = new Label(mainContainer, SWT.NONE);
		GridData gd_lblName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblName.horizontalIndent = 5;
		lblName.setLayoutData(gd_lblName);
		lblName.setText("Name:");
		
		dockerHostNameTextField = new Text(mainContainer, SWT.BORDER);
		GridData gd_dockerHostNameTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_dockerHostNameTextField.horizontalIndent = 3;
		gd_dockerHostNameTextField.widthHint = 200;
		dockerHostNameTextField.setLayoutData(gd_dockerHostNameTextField);
		dockerHostNameTextField.setText(newHost.name);
		dockerHostNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostNameTip());
		dockerHostNameTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				if (AzureDockerValidationUtils.validateDockerHostName(((Text) event.getSource()).getText())) {
					errDispatcher.removeMessage("dockerHostNameTextField", dockerHostNameTextField);
				} else {
					errDispatcher.addMessage("dockerHostNameTextField", AzureDockerValidationUtils.getDockerHostNameTip(), null, IMessageProvider.ERROR, dockerHostNameTextField);
				}
			}
		});
		
		Label lblNewLabel = new Label(mainContainer, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.horizontalIndent = 5;
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		lblNewLabel.setText("Subscription:");
		
		ComboViewer dockerSubscriptionComboViewer = new ComboViewer(mainContainer, SWT.READ_ONLY);
		Combo dockerSubscriptionCombo = dockerSubscriptionComboViewer.getCombo();
//		formToolkit.paintBordersFor(dockerSubscriptionCombo);
		dockerSubscriptionComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (selection.size() > 0){
                	AzureDockerSubscription currentSubscription = (AzureDockerSubscription) selection.getFirstElement();
                    dockerSubscriptionIdTextField.setText(currentSubscription != null ? currentSubscription.id : "");
					errDispatcher.removeMessage("dockerSubscriptionCombo", dockerSubscriptionCombo);
                } else {
					errDispatcher.addMessage("dockerSubscriptionCombo", "No active subscriptions found", null, IMessageProvider.ERROR, dockerSubscriptionCombo);
                }
			}
		});
		dockerSubscriptionComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		dockerSubscriptionComboViewer.setInput(dockerManager.getSubscriptionsList());
		if (dockerManager.getSubscriptionsList() != null && !dockerManager.getSubscriptionsList().isEmpty()) {
			dockerSubscriptionCombo.select(0);
		}
		dockerSubscriptionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(mainContainer, SWT.NONE);
		
		
		Label lblId = new Label(mainContainer, SWT.NONE);
		GridData gd_lblId = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblId.horizontalIndent = 5;
		lblId.setLayoutData(gd_lblId);
		lblId.setText("Id:");
		
		dockerSubscriptionIdTextField = new Text(mainContainer, SWT.BORDER);
		GridData gd_dockerSubscriptionIdTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerSubscriptionIdTextField.widthHint = 250;
		dockerSubscriptionIdTextField.setLayoutData(gd_dockerSubscriptionIdTextField);
		dockerSubscriptionIdTextField.setEditable(false);
		formToolkit.adapt(dockerSubscriptionIdTextField, true, true);
		new Label(mainContainer, SWT.NONE);
		
		
		Label lblRegion = new Label(mainContainer, SWT.NONE);
		GridData gd_lblRegion = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblRegion.horizontalIndent = 5;
		lblRegion.setLayoutData(gd_lblRegion);
		lblRegion.setText("Region:");
		
		Combo dockerLocationComboBox = new Combo(mainContainer, SWT.READ_ONLY);
		GridData gd_dockerLocationComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerLocationComboBox.widthHint = 200;
		dockerLocationComboBox.setLayoutData(gd_dockerLocationComboBox);
		new Label(mainContainer, SWT.NONE);
		
		TabFolder hostDetailsTabFolder = new TabFolder(mainContainer, SWT.NONE);
		GridData gd_hostDetailsTabFolder = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
		gd_hostDetailsTabFolder.heightHint = 140;
		hostDetailsTabFolder.setLayoutData(gd_hostDetailsTabFolder);
		
		TabItem vmKindTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
		vmKindTableItem.setText("OS and Size");
		
		Composite vmKindComposite = new Composite(hostDetailsTabFolder, SWT.NO_BACKGROUND);
		vmKindTableItem.setControl(vmKindComposite);
		vmKindComposite.setLayout(new GridLayout(2, false));
		
		Label lblNewLabel_1 = new Label(vmKindComposite, SWT.NONE);
		lblNewLabel_1.setText("Host OS:");
		
		Combo dockerHostOSTypeComboBox = new Combo(vmKindComposite, SWT.READ_ONLY);
		GridData gd_dockerHostOSTypeComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostOSTypeComboBox.widthHint = 200;
		dockerHostOSTypeComboBox.setLayoutData(gd_dockerHostOSTypeComboBox);
		
		Label lblSize = new Label(vmKindComposite, SWT.NONE);
		lblSize.setText("Size:");
		
		Combo dockerHostVMSizeComboBox = new Combo(vmKindComposite, SWT.READ_ONLY);
		GridData gd_dockerHostVMSizeComboBox = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_dockerHostVMSizeComboBox.widthHint = 200;
		dockerHostVMSizeComboBox.setLayoutData(gd_dockerHostVMSizeComboBox);
		
		Button dockerHostVMPreferredSizesCheckBox = new Button(vmKindComposite, SWT.CHECK);
		dockerHostVMPreferredSizesCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		dockerHostVMPreferredSizesCheckBox.setText("Show preferred sizes only");
		
		TabItem rgTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
		rgTableItem.setText("Resource Group");
		
		Composite rgComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
		rgTableItem.setControl(rgComposite);
		rgComposite.setLayout(new GridLayout(2, false));
		
		Button btnNewResourceGroup = new Button(rgComposite, SWT.RADIO);
		btnNewResourceGroup.setText("New resource group:");
		
		dockerHostRGTextField = new Text(rgComposite, SWT.BORDER);
		GridData gd_dockerHostRGTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostRGTextField.horizontalIndent = 3;
		gd_dockerHostRGTextField.widthHint = 200;
		dockerHostRGTextField.setLayoutData(gd_dockerHostRGTextField);
		
		Button btnExistingResourceGroup = new Button(rgComposite, SWT.RADIO);
		btnExistingResourceGroup.setText("Existing resource group:");
		
		Combo dockerHostSelectRGComboBox = new Combo(rgComposite, SWT.READ_ONLY);
		GridData gd_dockerHostSelectRGComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostSelectRGComboBox.widthHint = 220;
		dockerHostSelectRGComboBox.setLayoutData(gd_dockerHostSelectRGComboBox);
		
		TabItem networkTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
		networkTableItem.setText("Network");
		
		Composite networkComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
		networkTableItem.setControl(networkComposite);
		networkComposite.setLayout(new GridLayout(2, false));
		
		Button btnRadioButton = new Button(networkComposite, SWT.RADIO);
		btnRadioButton.setText("New virtual network");
		new Label(networkComposite, SWT.NONE);
		
		Label lblName_1 = new Label(networkComposite, SWT.NONE);
		GridData gd_lblName_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblName_1.horizontalIndent = 18;
		lblName_1.setLayoutData(gd_lblName_1);
		lblName_1.setText("Name:");
		
		dockerHostNewVNetNameTextField = new Text(networkComposite, SWT.BORDER);
		GridData gd_dockerHostNewVNetNameTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostNewVNetNameTextField.horizontalIndent = 3;
		gd_dockerHostNewVNetNameTextField.widthHint = 200;
		dockerHostNewVNetNameTextField.setLayoutData(gd_dockerHostNewVNetNameTextField);
		
		Label lblAddressSpacecdir = new Label(networkComposite, SWT.NONE);
		GridData gd_lblAddressSpacecdir = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblAddressSpacecdir.horizontalIndent = 18;
		lblAddressSpacecdir.setLayoutData(gd_lblAddressSpacecdir);
		lblAddressSpacecdir.setText("Address space (CDIR):");
		
		dockerHostNewVNetAddrSpaceTextField = new Text(networkComposite, SWT.BORDER);
		GridData gd_dockerHostNewVNetAddrSpaceTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostNewVNetAddrSpaceTextField.horizontalIndent = 3;
		gd_dockerHostNewVNetAddrSpaceTextField.widthHint = 200;
		dockerHostNewVNetAddrSpaceTextField.setLayoutData(gd_dockerHostNewVNetAddrSpaceTextField);
		
		Button btnExistingVirtualNetwork = new Button(networkComposite, SWT.RADIO);
		btnExistingVirtualNetwork.setText("Existing virtual network:");
		
		Combo dockerHostSelectVnetComboBox = new Combo(networkComposite, SWT.READ_ONLY);
		GridData gd_dockerHostSelectVnetComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostSelectVnetComboBox.widthHint = 220;
		dockerHostSelectVnetComboBox.setLayoutData(gd_dockerHostSelectVnetComboBox);
		
		Label lblSubnet = new Label(networkComposite, SWT.NONE);
		GridData gd_lblSubnet = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblSubnet.horizontalIndent = 18;
		lblSubnet.setLayoutData(gd_lblSubnet);
		lblSubnet.setText("Subnet:");
		
		Combo dockerHostSelectSubnetComboBox = new Combo(networkComposite, SWT.READ_ONLY);
		GridData gd_dockerHostSelectSubnetComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerHostSelectSubnetComboBox.widthHint = 220;
		dockerHostSelectSubnetComboBox.setLayoutData(gd_dockerHostSelectSubnetComboBox);
		
		TabItem storageTableItem = new TabItem(hostDetailsTabFolder, SWT.NONE);
		storageTableItem.setText("Storage");
		
		Composite storageComposite = new Composite(hostDetailsTabFolder, SWT.NONE);
		storageTableItem.setControl(storageComposite);
		storageComposite.setLayout(new GridLayout(2, false));
		
		Button dockerHostNewStorageRadioButton = new Button(storageComposite, SWT.RADIO);
		dockerHostNewStorageRadioButton.setText("New storage account:");
		
		dockerNewStorageTextField = new Text(storageComposite, SWT.BORDER);
		GridData gd_dockerNewStorageTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerNewStorageTextField.horizontalIndent = 3;
		gd_dockerNewStorageTextField.widthHint = 200;
		dockerNewStorageTextField.setLayoutData(gd_dockerNewStorageTextField);
		
		Button dockerHostSelectStorageRadioButton = new Button(storageComposite, SWT.RADIO);
		dockerHostSelectStorageRadioButton.setText("Existing storage account:");
		
		Combo dockerSelectStorageComboBox = new Combo(storageComposite, SWT.READ_ONLY);
		GridData gd_dockerSelectStorageComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_dockerSelectStorageComboBox.widthHint = 220;
		dockerSelectStorageComboBox.setLayoutData(gd_dockerSelectStorageComboBox);
		
		FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		managedForm = new ManagedForm(mainContainer);
		errMsgForm = managedForm.getForm();
		errMsgForm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		errMsgForm.setBackground(mainContainer.getBackground());
		errDispatcher = managedForm.getMessageManager();
//		errDispatcher.addMessage("dockerHostNameTextField", "Test error", null, IMessageProvider.ERROR, dockerHostNameTextField);
		errMsgForm.setMessage("This is an error message", IMessageProvider.ERROR);
		
		//dockerHostNameTextField
//		Form errMsgForm = formToolkit.createForm(mainContainer);
//		errMsgForm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
////		formToolkit.paintBordersFor(frmNewForm);
////		frmNewForm.setText("New Form");
//		errMsgForm.setBackground(mainContainer.getBackground());

//		Form form = toolkit.createForm(parent);
//		form.setBackground(mainContainer.getBackground());
////		form.addMessageHyperlinkListener(new HyperlinkAdapter());
//		form.setMessage("This is an error message", IMessageProvider.ERROR);
//		form.setVisible(false);
		
	}

	public boolean doValidate() {
		return false;
	}
}
