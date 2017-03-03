package com.microsoft.azuretools.azureexplorer.actions;


import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azureexplorer.forms.AddNewClusterForm;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;

@Name("Link A Cluster")
public class AddNewClusterAction extends NodeActionListener {
	private HDInsightRootModule hdInsightRootModule;
	
	public AddNewClusterAction(HDInsightRootModuleImpl module) {
		this.hdInsightRootModule = module;
	}
	
	@Override
	protected void actionPerformed(NodeActionEvent arg0) throws AzureCmdException {
		AddNewClusterForm form = new AddNewClusterForm(PluginUtil.getParentShell(), hdInsightRootModule);
		form.open();
	}

}
