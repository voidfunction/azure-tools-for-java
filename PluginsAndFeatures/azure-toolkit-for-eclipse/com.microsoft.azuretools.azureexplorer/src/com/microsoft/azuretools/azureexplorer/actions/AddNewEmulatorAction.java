package com.microsoft.azuretools.azureexplorer.actions;

import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azureexplorer.forms.AddNewClusterForm;
import com.microsoft.azuretools.azureexplorer.forms.AddNewEmulatorForm;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

@Name("Link A Emulator")
public class AddNewEmulatorAction extends NodeActionListener {
private HDInsightRootModule hdInsightRootModule;
	
	public AddNewEmulatorAction(HDInsightRootModuleImpl module) {
		this.hdInsightRootModule = module;
	}
	
	@Override
	protected void actionPerformed(NodeActionEvent arg0) throws AzureCmdException {
		AddNewEmulatorForm form = new AddNewEmulatorForm(PluginUtil.getParentShell(), hdInsightRootModule);
		form.open();
	}
}
