package com.microsoft.azure.hdinsight.serverexplore.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewClusterFrom;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

@Name("Add New Cluster")
public class AddNewClusterAction extends NodeActionListener {

    private HDInsightRootModule azureServiceModule;

    public AddNewClusterAction(HDInsightRootModule azureServiceModule) {
        this.azureServiceModule = azureServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        AddNewClusterFrom form = new AddNewClusterFrom((Project) azureServiceModule.getProject());
        form.show();
    }
}
