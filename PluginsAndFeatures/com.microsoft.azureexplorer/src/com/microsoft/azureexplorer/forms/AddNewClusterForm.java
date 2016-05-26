package com.microsoft.azureexplorer.forms;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.AddHDInsightAdditionalClusterImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class AddNewClusterForm extends Dialog {
	
    private String clusterName;
    private String userName;
    private String password;

    private String storageName;
    private String storageKey;

    private HDStorageAccount storageAccount;

    private String errorMessage;
    private boolean isCarryOnNextStep;
	
	
	private Text clusterNameFiled;
    private Text userNameField;
    private Text storageNameField;
    private Text storageKeyField;
    private Text passwordField;
    private Label errorMessageField;
    private Button okButton;
    private Button cancelButton;
	
    private HDInsightRootModule hdInsightModule;

    private static final String URL_PREFIX = "https://";
    
	public AddNewClusterForm(Shell parentShell, HDInsightRootModule module) {
		super(parentShell);
		this.hdInsightModule = module;
	}
	
	@Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Link New HDInsight Cluster");
    }
	
    @Override
    protected Control createButtonBar(Composite parent) {
        GridData gridData = new GridData();
        gridData.verticalAlignment = SWT.FILL;
        gridData.horizontalAlignment = SWT.FILL;
        parent.setLayoutData(gridData);
        Control ctrl = super.createButtonBar(parent);
        okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(false);
//        okButton.setText("Create");
        cancelButton = getButton(IDialogConstants.CANCEL_ID);
//        buttonCancel.setText("Close");
        return ctrl;
    }
    
    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        container.setLayoutData(gridData);

        Label clusterNameLabel = new Label(container, SWT.LEFT);
        clusterNameLabel.setText("Cluster Name:");
        clusterNameFiled = new Text(container, SWT.LEFT | SWT.BORDER);
        
        Label storageNameLabel = new Label(container, SWT.LEFT);
        storageNameLabel.setText("Storage Name:");
        storageNameField = new Text(container, SWT.LEFT | SWT.BORDER);

        Label storageKeyLabel = new Label(container, SWT.LEFT);
        storageKeyLabel.setText("Storage Key:");
        storageKeyField = new Text(container, SWT.LEFT | SWT.BORDER);

        Label userNameLabel = new Label(container, SWT.LEFT);
        storageKeyLabel.setText("User Name:");
        userNameField = new Text(container, SWT.LEFT | SWT.BORDER);

        Label passwordLabel = new Label(container, SWT.LEFT);
        passwordLabel.setText("Password:");
        passwordField = new Text(container, SWT.PASSWORD | SWT.BORDER);
        
        return super.createContents(parent);
    }

    @Override
    protected void okPressed() {
    	synchronized (AddNewClusterForm.class) {
            isCarryOnNextStep = true;
            errorMessage = null;
            errorMessageField.setVisible(false);

            String clusterNameOrUrl = clusterNameFiled.getText().trim();
            userName = userNameField.getText().trim();
            storageName = storageNameField.getText().trim();

            storageKey = storageKeyField.getText().trim();

            password = passwordField.getText();

            if (StringHelper.isNullOrWhiteSpace(clusterNameOrUrl) || StringHelper.isNullOrWhiteSpace(storageName) || StringHelper.isNullOrWhiteSpace(storageKey) || StringHelper.isNullOrWhiteSpace(userName) || StringHelper.isNullOrWhiteSpace(password)) {
                errorMessage = "Cluster Name, Storage Key, User Name, or Password shouldn't be empty";
                isCarryOnNextStep = false;
            } else {
                clusterName = getClusterName(clusterNameOrUrl);

                if (clusterName == null) {
                    errorMessage = "Wrong cluster name or endpoint";
                    isCarryOnNextStep = false;
                } else if (ClusterManagerEx.getInstance().isHDInsightAdditionalStorageExist(clusterName, storageName)) {
                    errorMessage = "Storage already exist!";
                    isCarryOnNextStep = false;
                }
            }

            if (isCarryOnNextStep) {
                getStorageAccount();
            }

            if (isCarryOnNextStep) {
                if (storageAccount != null) {
                    HDInsightAdditionalClusterDetail hdInsightAdditionalClusterDetail = new HDInsightAdditionalClusterDetail(clusterName, userName, password, storageAccount);
                    ClusterManagerEx.getInstance().addHDInsightAdditionalCluster(hdInsightAdditionalClusterDetail);
                    hdInsightModule.refreshWithoutAsync();
                }
                super.okPressed();
            } else {
                errorMessageField.setText(errorMessage);
                errorMessageField.setVisible(true);
            }
        }

//        PluginUtil.showBusy(true, getShell());
////        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

//        PluginUtil.showBusy(false, getShell());
    }
    
  //format input string
    private static String getClusterName(String userNameOrUrl) {
        if (userNameOrUrl.startsWith(URL_PREFIX)) {
            return StringHelper.getClusterNameFromEndPoint(userNameOrUrl);
        } else {
            return userNameOrUrl;
        }
    }
    
    private void getStorageAccount() {
//        addNewClusterPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    	PluginUtil.showBusy(true, getShell());
    	Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    storageAccount = AddHDInsightAdditionalClusterImpl.getStorageAccount(clusterName, storageName, storageKey, userName, password);
                    isCarryOnNextStep = true;
                } catch (AzureCmdException | HDIException e) {
                    isCarryOnNextStep = false;
                    errorMessage = e.getMessage();
                }
            }
        });
        PluginUtil.showBusy(false, getShell());
//        addNewClusterPanel.setCursor(Cursor.getDefaultCursor());
    }
}
